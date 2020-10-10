package it.manzolo.bluetoothwatcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Session {

    private SharedPreferences prefs;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
    }

    public void setlongitude(String longitude) {
        prefs.edit().putString("longitude", longitude).apply();
    }

    public void setlatitude(String latitude) {
        prefs.edit().putString("latitude", latitude).apply();
    }

    public String getlongitude() {
        return prefs.getString("longitude", "");
    }

    public String getlatitude() {
        return prefs.getString("latitude", "");
    }

    public String getUpdateApkUrl() {
        return prefs.getString("updateapkurl", "");
    }

    public void setUpdateApkUrl(String updateapkurl) {
        prefs.edit().putString("updateapkurl", updateapkurl).apply();
    }

}