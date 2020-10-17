package it.manzolo.bluetoothwatcher.utils;

import java.util.Arrays;

public class DeviceInfo {
    private final String address;
    private final byte[] data;

    private Double volt;
    private Double amp;
    private Double mW;
    private Integer tempF;
    private Integer tempC;

    public DeviceInfo(String address, byte[] data) throws Exception {
        this.address = address;
        this.data = data;
        this.load();

    }

    private void load() throws Exception {
        Struct struct = new Struct();
        long[] volts = struct.unpack("!H", Arrays.copyOfRange(this.data, 2, 4));
        long[] amps = struct.unpack("!H", Arrays.copyOfRange(this.data, 4, 6));
        long[] mWs = struct.unpack("!I", Arrays.copyOfRange(this.data, 6, 10));

        long[] tempCs = struct.unpack("!H", Arrays.copyOfRange(this.data, 10, 12));
        long[] tempFs = struct.unpack("!H", Arrays.copyOfRange(this.data, 12, 14));

        volt = Double.parseDouble(String.valueOf(volts[0] / 100.0));
        amp = Double.parseDouble(String.valueOf(amps[0] / 1000.0));
        tempC = Integer.parseInt(String.valueOf(tempCs[0]));
        tempF = Integer.parseInt(String.valueOf(tempFs[0]));
        mW = Double.parseDouble(String.valueOf(mWs[0] / 1000.0));

    }

    public Double getVolt() {
        return this.volt;
    }

    public String getAddress() {
        return address;
    }

    public Double getAmp() {
        return amp;
    }

    public Double getmW() {
        return mW;
    }

    public Integer getTempF() {
        return tempF;
    }

    public Integer getTempC() {
        return tempC;
    }
}
