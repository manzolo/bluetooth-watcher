package it.manzolo.job.service.bluewatcher.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import it.manzolo.job.service.enums.BluetoothEvents;

public final class BluetoothClient {
    public static final String TAG = "BluetoothClient";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocketWrapper bluetoothSocketWrapper;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream bluetoothOutputStream;
    private InputStream bluetoothInputStream;
    volatile boolean stopWorker;
    private Thread workerThread;
    private int readBufferPosition;
    private byte[] readBuffer;
    private String deviceAddress;

    public BluetoothClient(Context context, String deviceAddress) {
        this.deviceAddress = deviceAddress;
        this.context = context;
        //Register event for bluetooth close connection
        LocalBroadcastManager.getInstance(context).registerReceiver(closeBluetoothReceiver, new IntentFilter(BluetoothEvents.CLOSECONNECTION));
    }

    private BroadcastReceiver closeBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d(TAG, "Try closing bluetooth...");
            try {
                close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    };

    public void retrieveData() throws Exception {
        try {
            if (this.open()) {
                Thread.sleep(500);
                this.setBacklight();
                Thread.sleep(500);
                this.setScreenTimeout();
                Thread.sleep(500);
                this.dataDump();
            }
            Thread.sleep(500);
        } catch (Exception e) {
            this.close();
            throw e;
        } finally {
            //Close qui chiude la connessione metre il bluetooth riceve i dati
            //this.close();
        }
    }

    private void findBT() throws Exception {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothAdapter.cancelDiscovery();

        if (bluetoothAdapter == null) {
            throw new Exception("No bluetooth adapter available");
        }

        if (!bluetoothAdapter.isEnabled()) {
            throw new Exception("Bluetooth not enabled");
        }

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(this.deviceAddress);
        Log.d(TAG, "Bluetooth Device Found");
    }

    private void setBacklight() {
        try {
            Log.d(TAG, "setBacklight");
            byte backlight = (byte) 0xd0;
            this.bluetoothOutputStream.write(backlight);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setScreenTimeout() {
        try {
            Log.d(TAG, "setScreenTimeout");
            byte screentimeout = (byte) 0xe0;
            this.bluetoothOutputStream.write(screentimeout);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean open() throws Exception {
        this.findBT();
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            Log.d(TAG, "Connecting to " + bluetoothDevice.getAddress() + " UUID:" + uuid);
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            Log.d(TAG, "Connected to " + uuid);
            bluetoothOutputStream = bluetoothSocket.getOutputStream();
            bluetoothInputStream = bluetoothSocket.getInputStream();

        } catch (IOException normal_e) {
            //normal_e.printStackTrace();
            //throw new Exception("Unable to connect to " + this.deviceAddress);
            try {
                bluetoothSocketWrapper = new FallbackBluetoothSocket(bluetoothSocketWrapper.getUnderlyingSocket());
                Thread.sleep(500);
                bluetoothSocketWrapper.connect();
                bluetoothOutputStream = bluetoothSocketWrapper.getOutputStream();
                bluetoothInputStream = bluetoothSocketWrapper.getInputStream();
            } catch (Exception fallback_e) {
                //fallback_e.printStackTrace();
                throw new Exception("Unable to connect to " + this.deviceAddress);
            }
        }
        return true;
    }

    private void dataDump() {
        try {
            Log.d(TAG, "Request bluetooth data...");
            byte dataDump = (byte) 0xf0;
            this.bluetoothOutputStream.write(dataDump);
            this.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        readBufferPosition = 0;
        readBuffer = new byte[130];
        final String device = this.deviceAddress;
        final Handler handler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Listen...");

        stopWorker = false;
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = bluetoothInputStream.available();
                        if (bytesAvailable > 0) {
                            int bytereaded = -1;
                            byte[] packetBytes = new byte[130];
                            bluetoothInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                bytereaded++;

                                if (bytereaded == 130) {

                                    final DeviceInfo deviceInfo = new DeviceInfo(device, readBuffer);

                                    if (deviceInfo.getVolt() == 0.00) {
                                        Log.w(TAG, "Wrong data");
                                        Intent intentBtError = new Intent(BluetoothEvents.ERROR);
                                        intentBtError.putExtra("message", "Wrong data received from device " + deviceInfo.getAddress());
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBtError);
                                        return;
                                    }

                                    Log.d(TAG, "Device: " + deviceInfo.getAddress());
                                    Log.d(TAG, deviceInfo.getVolt() + " Volt");
                                    Log.d(TAG, deviceInfo.getAmp() + " A");
                                    Log.d(TAG, deviceInfo.getmW() + " mW");

                                    Log.d(TAG, deviceInfo.getTempC() + "°");
                                    Log.d(TAG, deviceInfo.getTempF() + "°F");

                                    readBufferPosition = 0;
                                    bytereaded = -1;
                                    readBuffer = new byte[130];

                                    handler.post(new Runnable() {
                                        public void run() {
                                            Date date = Calendar.getInstance().getTime();
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            String now = dateFormat.format(date);

                                            Intent intentBt = new Intent(BluetoothEvents.DATA_RETRIEVED);

                                            intentBt.putExtra("device", deviceInfo.getAddress());
                                            intentBt.putExtra("volt", deviceInfo.getVolt().toString());
                                            intentBt.putExtra("temp", deviceInfo.getTempC().toString());
                                            intentBt.putExtra("data", now);

                                            intentBt.putExtra("message", "Device: " + deviceInfo.getAddress() + " Volt:" + deviceInfo.getVolt().toString() + " Temp:" + deviceInfo.getTempC().toString());
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBt);


                                            Intent intent = new Intent(BluetoothEvents.CLOSECONNECTION);
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                    bytereaded++;

                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        workerThread.start();
    }

    private void close() throws IOException {
        stopWorker = true;
        if (bluetoothOutputStream != null) {
            bluetoothOutputStream.close();
        }
        if (bluetoothInputStream != null) {
            bluetoothInputStream.close();
        }
        if (bluetoothSocket != null) {
            bluetoothSocket.close();
        }
        if (bluetoothSocketWrapper != null) {
            bluetoothSocketWrapper.close();
        }
        bluetoothDevice = null;

        LocalBroadcastManager.getInstance(context).unregisterReceiver(closeBluetoothReceiver);

        Log.d(TAG, "Bluetooth Closed!");
    }
}

