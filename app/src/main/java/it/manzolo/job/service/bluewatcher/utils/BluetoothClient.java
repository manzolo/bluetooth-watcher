package it.manzolo.job.service.bluewatcher.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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

public final class BluetoothClient {
    public static final String TAG = "BluetoothClient";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocketWrapper bluetoothSocketWrapper;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private static boolean busy = false;
    private OutputStream bluetoothOutputStream;
    private Thread workerThread;
    private InputStream bluetoothInputStream;
    private byte[] readBuffer;
    volatile boolean stopWorker;
    private int readBufferPosition;
    private String deviceaddress;

    public BluetoothClient(Context context, String deviceaddress) {
        this.deviceaddress = deviceaddress;
        this.context = context;
    }

    public void retrieveData() throws Exception {
        if (busy) {
            throw new Exception(TAG + " is busy");
        }
        busy = true;
        try {

            this.openBT();
            Thread.sleep(500);
            this.setBacklight();
            Thread.sleep(500);
            this.setScreenTimeout();
            Thread.sleep(500);
            this.getData();
            Thread.sleep(500);
        } catch (Exception e) {
            busy = false;
            this.closeBT();
            throw e;
        } finally {
            busy = false;
        }
    }

    private boolean openBT() throws Exception {
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
            //throw new Exception("Unable to connect to " + this.deviceaddress);
            try {
                bluetoothSocketWrapper = new FallbackBluetoothSocket(bluetoothSocketWrapper.getUnderlyingSocket());
                Thread.sleep(500);
                bluetoothSocketWrapper.connect();
                bluetoothOutputStream = bluetoothSocketWrapper.getOutputStream();
                bluetoothInputStream = bluetoothSocketWrapper.getInputStream();
            } catch (Exception fallback_e) {
                //fallback_e.printStackTrace();
                throw new Exception("Unable to connect to " + this.deviceaddress);
            }
        }
        return true;
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

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(this.deviceaddress);
        Log.d(TAG, "Bluetooth Device Found");
    }

    private void listen() {
        final Handler handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Listen...");

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[130];
        final String device = this.deviceaddress;
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
                                            Date date = Calendar.getInstance().getTime();
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            String now = dateFormat.format(date);

                                            Intent intentBt = new Intent(BluetoothEvents.DATA_RETRIEVED);
                                            // You can also include some extra data.
                                            intentBt.putExtra("device", device);
                                            intentBt.putExtra("volt", voltstr);
                                            intentBt.putExtra("temp", tempstr);
                                            intentBt.putExtra("data", now);

                                            intentBt.putExtra("message", "Device: " + device + " Volt:" + voltstr + " Temp:" + tempstr);
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBt);
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

    private void getData() {
        try {
            Log.d(TAG, "Request bluetooth data...");
            byte dataDump = (byte) 0xf0;
            this.bluetoothOutputStream.write(dataDump);
            this.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeBT() throws IOException {
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
        //if (bluetoothSocket.isConnected()){

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
