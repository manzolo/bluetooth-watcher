package it.manzolo.bluetoothwatcher.utils;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

interface BluetoothSocketWrapper {

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    String getRemoteDeviceName();

    void connect() throws IOException;

    String getRemoteDeviceAddress();

    void close() throws IOException;

    BluetoothSocket getUnderlyingSocket();

}

public class FallbackBluetoothSocket extends NativeBluetoothSocket {

    private final BluetoothSocket fallbackSocket;

    public FallbackBluetoothSocket(BluetoothSocket fallbackBluetoothSocket) throws FallbackException {
        super(fallbackBluetoothSocket);
        try {
            Class<?> fallbackBluetoothSocketClass = fallbackBluetoothSocket.getRemoteDevice().getClass();
            Class<?>[] fallbackBluetoothSocketParamTypes = new Class<?>[]{Integer.TYPE};
            Method m = fallbackBluetoothSocketClass.getMethod("createRfcommSocket", fallbackBluetoothSocketParamTypes);
            Object[] params = new Object[]{1};
            fallbackSocket = (BluetoothSocket) m.invoke(fallbackBluetoothSocket.getRemoteDevice(), params);
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

class NativeBluetoothSocket implements BluetoothSocketWrapper {

    private final BluetoothSocket socket;

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

class FallbackException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public FallbackException(Exception e) {
        super(e);
    }

}