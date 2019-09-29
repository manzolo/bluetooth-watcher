package it.manzolo.job.service.bluewatcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Btclient {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    private String addr;

    public Btclient(String addr) {
        this.addr = addr;
        this.findBT();
    }

    boolean openBT() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            Log.i("Manzolo", "Connecting to " + uuid);
            mmSocket.connect();
            Log.i("Manzolo", "Connected to " + uuid);
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

        } catch (IOException e) {
            Log.e("Manzolo", "unable to connect to " + this.addr);
            return false;
        }
        return true;


    }

    private boolean findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e("Manzolo", "No bluetooth adapter available");
            return false;

        }

        if (!mBluetoothAdapter.isEnabled()) {
            Log.e("Manzolo", "Bluetooth not enabled");
            return false;
        }

        mmDevice = mBluetoothAdapter.getRemoteDevice(this.addr);
        Log.i("Manzolo", "Bluetooth Device Found");
        return true;
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        Log.i("Manzolo", "Listen");

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[130];
        final String device = this.addr;
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        //Log.i("Manzolo", new StringBuilder().append(bytesAvailable).toString());
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


                                    Log.i("Manzolo", "Device: " + device);
                                    Log.i("Manzolo", voltstr + " Volt");
                                    Log.i("Manzolo", new StringBuilder().append(tempstr) + "°");

                                    readBufferPosition = 0;
                                    bytereaded = -1;
                                    readBuffer = new byte[130];
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Date date = Calendar.getInstance().getTime();
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            String now = dateFormat.format(date);

                                            Log.i("Manzolo", "Post finish -> " + now);
                                            try {
                                                Manzolosender sender = new Manzolosender(device, now, voltstr, tempstr);
                                                sender.send();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            //myLabel.setText(voltstr);
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

    void getData() {
        try {
            byte data = (byte) 0xf0;
            mmOutputStream.write(data);
            this.beginListenForData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //closeBT();
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.i("Manzolo", "Bluetooth Closed");
    }
}
