package it.manzolo.job.service.bluewatcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Session {

    private SharedPreferences prefs;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
    }

    public void setlongitude(String longitude) {
        prefs.edit().putString("longitude", longitude).commit();
    }

    public void setlatitude(String latitude) {
        prefs.edit().putString("latitude", latitude).commit();
    }

    public String getlongitude() {
        String longitude = prefs.getString("longitude", "");
        return longitude;
    }

    public String getlatitude() {
        String latitude = prefs.getString("latitude", "");
        return latitude;
    }

    public String getUpdateApkUrl() {
        String updateapkurl = prefs.getString("updateapkurl", "");
        return updateapkurl;
    }

    public void setUpdateApkUrl(String updateapkurl) {
        prefs.edit().putString("updateapkurl", updateapkurl).commit();
    }

}