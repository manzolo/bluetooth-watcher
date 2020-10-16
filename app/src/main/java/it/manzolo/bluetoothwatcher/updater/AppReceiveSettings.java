package it.manzolo.bluetoothwatcher.updater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import it.manzolo.bluetoothwatcher.enums.BluetoothEvents;
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents;
import it.manzolo.bluetoothwatcher.utils.HttpUtils;

public class AppReceiveSettings {

    private static final String TAG = "AppReceiveSettings";

    private final Context context;
    private final String webserviceUrl;
    private final String webserviceUsername;
    private final String webservicePassword;

    public AppReceiveSettings(Context context, String webserviceUrl, String webserviceUsername, String webservicePassword) {
        this.context = context;
        this.webserviceUrl = webserviceUrl;
        this.webserviceUsername = webserviceUsername;
        this.webservicePassword = webservicePassword;
    }

    private String httpGet() throws IOException, JSONException {
        URL url = new URL(this.webserviceUrl + HttpUtils.getSettingsUrl);

        // 1. create HttpURLConnection
        HttpURLConnection loginConn = HttpUtils.loginWebservice(this.webserviceUrl, this.webserviceUsername, this.webservicePassword);

        if (loginConn.getResponseCode() >= 200 && loginConn.getResponseCode() < 400) {
            JSONObject tokenObject = new JSONObject(new HttpUtils().convertStreamToString(loginConn.getInputStream()));
            String token = tokenObject.getString("token");
            Log.d("TOKEN", token);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(HttpUtils.connectionTimeout);
            conn.setReadTimeout(HttpUtils.connectionTimeout);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            JSONObject jsonObject = new JSONObject(new HttpUtils().convertStreamToString(conn.getInputStream()));
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);

            SharedPreferences.Editor editor = preferences.edit();
            try {
                editor.putString("webservice_service_every_seconds", jsonObject.get("webservice_service_every_seconds").toString());
            } catch (JSONException e) {
                Log.w(TAG, "webservice_service_every_seconds setting not found");
            }
            try {
                editor.putString("bluetooth_service_every_seconds", jsonObject.get("bluetooth_service_every_seconds").toString());
            } catch (JSONException e) {
                Log.w(TAG, "bluetooth_service_every_seconds setting not found");
            }
            try {
                editor.putString("location_service_every_seconds", jsonObject.get("location_service_every_seconds").toString());
            } catch (JSONException e) {
                Log.w(TAG, "location_service_every_seconds setting not found");
            }
            try {
                editor.putString("update_service_every_seconds", jsonObject.get("update_service_every_seconds").toString());
            } catch (JSONException e) {
                Log.w(TAG, "update_service_every_seconds setting not found");
            }
            try {
                editor.putString("restart_app_service_every_seconds", jsonObject.get("restart_app_service_every_seconds").toString());
            } catch (JSONException e) {
                Log.w(TAG, "restart_app_service_every_seconds setting not found");
            }
            try {
                editor.putString("devices", jsonObject.get("devices").toString());
            } catch (JSONException e) {
                Log.w(TAG, "devices setting not found");
            }
            try {
                editor.putBoolean("enabled", jsonObject.get("enabled").toString().equals("1"));
            } catch (JSONException e) {
                Log.w(TAG, "enabled service setting not found");
            }

            editor.apply();

            Log.d(TAG, "Settings updated");
            return conn.getResponseMessage() + "";
        } else {
            Intent intentWs = new Intent(WebserviceEvents.ERROR);
            intentWs.putExtra("message", "Server login response: " + loginConn.getResponseCode() + " " + loginConn.getResponseMessage());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
        }
        Log.e(TAG, "Unable to update settings");
        return "";
    }

    public void receive() {
        new HTTPAsyncTask().execute();
    }

    private JSONObject buidLoginJsonObject() throws JSONException {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("username", this.webserviceUsername);
        jsonObject.put("password", this.webservicePassword);
        return jsonObject;
    }

    class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                try {
                    return httpGet();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    //e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, e.getMessage());
                Intent intent = new Intent(BluetoothEvents.ERROR);
                // You can also include some extra data.
                intent.putExtra("message", e.getMessage());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return "Unable to retrieve web page: " + e.getMessage();
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Webserver response: " + result);
        }
    }


}