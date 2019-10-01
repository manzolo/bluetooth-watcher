package it.manzolo.job.service.bluewatcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
    public static final String TAG = "Btclient";
    static Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    volatile boolean stopWorker;
    private String addr;

    public Btclient(Context context, String addr) {
        this.addr = addr;
        Btclient.context = context;

    }

    public void retrieveData() throws Exception {
        this.openBT();
        this.getData();
        Thread.sleep(1000);
        this.closeBT();
    }

    private void findBT() throws Exception {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

        } catch (IOException e) {
            //Log.e(TAG, "Unable to connect to " + this.addr);
            throw new Exception("Unable to connect to " + this.addr);
        }
        return true;
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
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

                                            Log.d(TAG, "Send data to webserver");
                                            try {
                                                Sender sender = new Sender(url, device, now, voltstr, tempstr);
                                                sender.send();
                                                Toast.makeText(context, "Data sent", Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, e.getMessage());
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
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d(TAG, "Bluetooth Closed");
    }
}
