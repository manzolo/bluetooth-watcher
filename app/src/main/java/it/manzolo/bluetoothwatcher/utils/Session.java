package it.manzolo.bluetoothwatcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Session {

    private final SharedPreferences prefs;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
    }

    public String getWebserviceToken() {
        return prefs.getString("token", "");
    }

    public void setWebserviceToken(String token) {
        prefs.edit().putString("token", token).apply();
    }

    public String getLongitude() {
        return prefs.getString("longitude", "");
    }

    public void setLongitude(String longitude) {
        prefs.edit().putString("longitude", longitude).apply();
    }

    public String getLatitude() {
        return prefs.getString("latitude", "");
    }

    public void setLatitude(String latitude) {
        prefs.edit().putString("latitude", latitude).apply();
    }

    public String getUpdateApkUrl() {
        return prefs.getString("updateapkurl", "");
    }

    public void setUpdateApkUrl(String updateapkurl) {
        prefs.edit().putString("updateapkurl", updateapkurl).apply();
    }

}