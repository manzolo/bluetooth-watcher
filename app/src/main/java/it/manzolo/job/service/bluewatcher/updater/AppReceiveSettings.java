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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import it.manzolo.job.service.enums.BluetoothEvents;

public class AppReceiveSettings {

    private static final String TAG = "AppReceiveSettings";

    private Context context;
    private String url;
    private String username;
    private String password;

    public AppReceiveSettings(Context context, String url, String username, String password) {
        this.context = context;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private String httpGet(String webserverurl) throws IOException, JSONException {
        URL loginurl = new URL(webserverurl + "/api/login_check");
        URL url = new URL(webserverurl + "/api/get/settings/app.json");

        // 1. create HttpURLConnection
        String usernameapi = username;
        String passwordapi = password;

        HttpURLConnection loginConn = (HttpURLConnection) loginurl.openConnection();
        loginConn.setUseCaches(false);
        loginConn.setAllowUserInteraction(false);
        loginConn.setConnectTimeout(10000);
        loginConn.setReadTimeout(10000);
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        JSONObject jsonLoginObject = buidLoginJsonObject(usernameapi, passwordapi);
        // 3. add JSON content to POST request body
        setPostRequestContent(loginConn, jsonLoginObject);

        // 4. make POST request to the given URL
        loginConn.connect();
        if (loginConn.getResponseCode() >= 200 && loginConn.getResponseCode() < 400) {
            InputStream responseLogin = loginConn.getInputStream();
            String tokenObject = convertStreamToString(responseLogin);
            JSONObject obj = new JSONObject(tokenObject);
            String token = obj.getString("token");
            Log.d("TOKEN", token);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            InputStream response = conn.getInputStream();
            JSONObject jsonObject = new JSONObject(convertStreamToString(response));
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);

            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("seconds", jsonObject.get("seconds").toString());
            editor.putString("devices", jsonObject.get("devices").toString());

            if (jsonObject.get("enabled").toString().equals("1")) {
                editor.putBoolean("enabled", true);
            } else {
                editor.putBoolean("enabled", false);
            }

            editor.commit();

            Log.d(TAG, "Settings updated");
            return conn.getResponseMessage() + "";
        }
        Log.e(TAG, "Unable to update settings");
        return "";
    }

    public void receive() {
        new HTTPAsyncTask().execute(this.url, username, password);
    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    private JSONObject buidLoginJsonObject(String username, String password) throws JSONException {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("username", username);
        jsonObject.put("password", password);
        return jsonObject;
    }

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                try {
                    return httpGet(urls[0]);
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