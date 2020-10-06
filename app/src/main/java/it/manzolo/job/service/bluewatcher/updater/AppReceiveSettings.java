package it.manzolo.job.service.bluewatcher.updater;

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

import it.manzolo.job.service.bluewatcher.utils.HttpUtils;
import it.manzolo.job.service.enums.BluetoothEvents;
import it.manzolo.job.service.enums.WebserverEvents;

public class AppReceiveSettings {

    private static final String TAG = "AppReceiveSettings";

    private Context context;
    private String webserviceUrl;
    private String webserviceUsername;
    private String webservicePassword;

    public AppReceiveSettings(Context context, String webserviceUrl, String webserviceUsername, String webservicePassword) {
        this.context = context;
        this.webserviceUrl = webserviceUrl;
        this.webserviceUsername = webserviceUsername;
        this.webservicePassword = webservicePassword;
    }

    private String httpGet() throws IOException, JSONException {
        URL loginUrl = new URL(this.webserviceUrl + HttpUtils.loginUrl);
        URL url = new URL(this.webserviceUrl + HttpUtils.getSettingsUrl);

        // 1. create HttpURLConnection

        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setUseCaches(false);
        loginConn.setAllowUserInteraction(false);
        loginConn.setConnectTimeout(HttpUtils.connectionTimeout);
        loginConn.setReadTimeout(HttpUtils.connectionTimeout);
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        JSONObject jsonLoginObject = buidLoginJsonObject();
        // 3. add JSON content to POST request body
        new HttpUtils().setPostRequestContent(loginConn, jsonLoginObject);

        // 4. make POST request to the given URL
        loginConn.connect();
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
            } finally {
                Log.w(TAG, "webservice_service_every_seconds setting not found");
            }
            try {
                editor.putString("location_service_every_seconds", jsonObject.get("location_service_every_seconds").toString());
            } finally {
                Log.w(TAG, "location_service_every_seconds setting not found");
            }
            try {
                editor.putString("update_service_every_seconds", jsonObject.get("update_service_every_seconds").toString());
            } finally {
                Log.w(TAG, "update_service_every_seconds setting not found");
            }
            try {
                editor.putString("restart_app_service_every_seconds", jsonObject.get("restart_app_service_every_seconds").toString());
            } finally {
                Log.w(TAG, "restart_app_service_every_seconds setting not found");
            }
            try {
                editor.putString("devices", jsonObject.get("devices").toString());
            } finally {
                Log.w(TAG, "devices setting not found");
            }
            try {
                if (jsonObject.get("enabled").toString().equals("1")) {
                    editor.putBoolean("enabled", true);
                } else {
                    editor.putBoolean("enabled", false);
                }
            } finally {
                Log.w(TAG, "enabled service setting not found");
            }

            editor.apply();

            Log.d(TAG, "Settings updated");
            return conn.getResponseMessage() + "";
        } else {
            Intent intentWs = new Intent(WebserverEvents.ERROR);
            intentWs.putExtra("message", "Server login response: " + loginConn.getResponseCode());
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

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
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