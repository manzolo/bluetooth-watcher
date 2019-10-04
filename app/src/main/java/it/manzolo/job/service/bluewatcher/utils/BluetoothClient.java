package it.manzolo.job.service.bluewatcher.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import it.manzolo.job.service.enums.BluetoothEvents;
import it.manzolo.job.service.enums.WebserverEvents;

import static it.manzolo.job.service.bluewatcher.utils.BatteryKt.getBatteryPercentage;

public final class BluetoothClient {
    public static final String TAG = "BluetoothClient";
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocketWrapper bluetoothSocket;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    volatile boolean stopWorker;
    private static boolean busy = false;
    private String addr;

    public BluetoothClient(String addr, Context context) {
        this.addr = addr;
        this.context = context;
    }

    public void retrieveData() throws Exception {
        if (busy) {
            throw new Exception(TAG + " is busy");
        }
        busy = true;
        try {
            this.openBT();
            this.getData();
            Thread.sleep(1000);
        } catch (Exception e) {
            busy = false;
            this.closeBT();
            throw e;
        } finally {
            busy = false;
        }
    }

    private void findBT() throws Exception {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothAdapter.cancelDiscovery();

        if (mBluetoothAdapter == null) {
            throw new Exception("No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            throw new Exception("Bluetooth not enabled");
        }

        mmDevice = mBluetoothAdapter.getRemoteDevice(this.addr);
        Log.d(TAG, "Bluetooth Device Found");
    }

    private boolean openBT() throws Exception {
        this.findBT();
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            Log.d(TAG, "Connecting to " + uuid);
            mmSocket.connect();
            Log.d(TAG, "Connected to " + uuid);
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

        } catch (IOException normal_e) {
            normal_e.printStackTrace();
            //throw new Exception("Unable to connect to " + this.addr);
            try {
                bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
                Thread.sleep(500);
                bluetoothSocket.connect();
                mmOutputStream = bluetoothSocket.getOutputStream();
                mmInputStream = bluetoothSocket.getInputStream();
            } catch (Exception fallback_e) {
                fallback_e.printStackTrace();
                throw new Exception("Unable to connect to " + this.addr);
            }
        }
        return true;
    }

    private void beginListenForData() {
        final Handler handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Listen...");

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[130];
        final String device = this.addr;
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {

                            int bytereaded = -1;
                            byte[] packetBytes = new byte[130];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                bytereaded++;

                                if (bytereaded == 130) {

                                    //volt
                                    byte[] voltarray = Arrays.copyOfRange(readBuffer, 2, 4);
                                    Struct struct = new Struct();
                                    final long[] volt = struct.unpack(">h", voltarray);

                                    //volt
                                    byte[] temparray = Arrays.copyOfRange(readBuffer, 10, 12);
                                    long[] temp = struct.unpack(">h", temparray);
                                    if (volt[0] == 0 && temp[0] == 0) {
                                        Log.w("Manzolo", "Errata lettura");
                                        return;
                                    }
                                    final String voltstr = new StringBuilder().append(volt[0] / 100.0).toString();
                                    final String tempstr = new StringBuilder().append(temp[0]).toString();


                                    Log.d(TAG, "Device: " + device);
                                    Log.d(TAG, voltstr + " Volt");
                                    Log.d(TAG, new StringBuilder().append(tempstr) + "°");

                                    readBufferPosition = 0;
                                    bytereaded = -1;
                                    readBuffer = new byte[130];

                                    handler.post(new Runnable() {
                                        public void run() {
                                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                                            String url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt");
                                            Date date = Calendar.getInstance().getTime();
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            String now = dateFormat.format(date);

                                            Intent intentBt = new Intent(BluetoothEvents.DATA_RETRIEVED);
                                            // You can also include some extra data.
                                            intentBt.putExtra("message", "Device: " + device + " Volt:" + voltstr + " Temp:" + tempstr);
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBt);

                                            Log.d(TAG, "Send data to webserver");
                                            try {
                                                Integer bp = getBatteryPercentage(context);
                                                WebserverSender sender = new WebserverSender(context, url, device, now, voltstr, tempstr, bp.toString());
                                                sender.send();
                                                Intent intentWs = new Intent(WebserverEvents.DATA_SENT);
                                                // You can also include some extra data.
                                                intentWs.putExtra("message", "Device: " + device + " Volt:" + voltstr + " Temp:" + tempstr);
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                                                //Toast.makeText(context, "Data sent", Toast.LENGTH_SHORT).show();
                                                //Log.i(TAG, "Data sent");
                                            } catch (Exception e) {
                                                //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, e.getMessage());
                                                Intent intent = new Intent(WebserverEvents.ERROR);
                                                // You can also include some extra data.
                                                intent.putExtra("message", e.getMessage());
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                                //e.printStackTrace();
                                            }
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

    private void getData() {
        try {
            byte data = (byte) 0xf0;
            mmOutputStream.write(data);
            this.beginListenForData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeBT() throws IOException {
        stopWorker = true;
        if (mmOutputStream != null) {
            mmOutputStream.close();
        }
        if (mmInputStream != null) {
            mmInputStream.close();
        }
        //if (mmSocket.isConnected()){
        mmSocket.close();
        //}
        busy = false;
        Log.d(TAG, "Bluetooth Closed");
    }


    private interface BluetoothSocketWrapper {

        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        String getRemoteDeviceName();

        void connect() throws IOException;

        String getRemoteDeviceAddress();

        void close() throws IOException;

        BluetoothSocket getUnderlyingSocket();

    }


    private static class NativeBluetoothSocket implements BluetoothSocketWrapper {

        private BluetoothSocket socket;

        public NativeBluetoothSocket(BluetoothSocket tmp) {
            this.socket = tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public String getRemoteDeviceName() {
            return socket.getRemoteDevice().getName();
        }

        @Override
        public void connect() throws IOException {
            socket.connect();
        }

        @Override
        public String getRemoteDeviceAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        @Override
        public BluetoothSocket getUnderlyingSocket() {
            return socket;
        }

    }

    private static class FallbackException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public FallbackException(Exception e) {
            super(e);
        }

    }

    private class FallbackBluetoothSocket extends NativeBluetoothSocket {

        private BluetoothSocket fallbackSocket;

        public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
            super(tmp);
            try {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            } catch (Exception e) {
                throw new FallbackException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fallbackSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return fallbackSocket.getOutputStream();
        }


        @Override
        public void connect() throws IOException {
            fallbackSocket.connect();
        }


        @Override
        public void close() throws IOException {
            fallbackSocket.close();
        }

    }
}
