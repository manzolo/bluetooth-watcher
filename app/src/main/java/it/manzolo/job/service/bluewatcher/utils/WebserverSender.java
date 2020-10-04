package it.manzolo.job.service.bluewatcher.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
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
import it.manzolo.job.service.enums.WebserverEvents;

public class WebserverSender {

    public static final String TAG = "WebserverSender";

    private Context context;
    private String url;
    private String username;
    private String password;

    public WebserverSender(Context context, String url, String username, String password) {
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

    public void send() {
        new HTTPAsyncTask().execute(this.url, username, password);
    }

    private String httpPost(String myUrl) throws IOException, JSONException {
        URL loginurl = new URL(myUrl + "/api/login_check");
        URL url = new URL(myUrl + "/api/volt/record.json");
        boolean trysend = false;

        DbVoltwatcherAdapter dbVoltwatcherAdapter = new DbVoltwatcherAdapter(context);
        dbVoltwatcherAdapter.open();
        dbVoltwatcherAdapter.deleteSent();
        Cursor cursor = dbVoltwatcherAdapter.fetchRowsNotSent();
        Integer cursorCount = cursor.getCount();
        Log.d(TAG, "Found " + cursorCount + " rows to send");
        if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to " + myUrl);

            try {
                while (cursor.moveToNext()) {

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
                        InputStream response = loginConn.getInputStream();
                        String tokenObject = convertStreamToString(response);
                        JSONObject obj = new JSONObject(tokenObject);
                        String token = obj.getString("token");
                        Log.d("TOKEN", token);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PUT");
                        conn.setUseCaches(false);
                        conn.setAllowUserInteraction(false);
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(10000);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        conn.setRequestProperty("Authorization", "Bearer " + token);

                        //Log.e("TAG",cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DEVICE)));
                        //Log.e("TAG",cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DATA)));
                        String device = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DEVICE));
                        String data = cursor.getString(cursor.getColumnIndex("grData"));
                        String volt = cursor.getString(cursor.getColumnIndex("volts"));
                        String temp = cursor.getString(cursor.getColumnIndex("temps"));
                        String detecotrbattery = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DETECTORBATTERY));
                        String longitude = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_LON));
                        String latitude = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_LAT));
                        // 2. build JSON object
                        JSONObject jsonObject = buidJsonObject(device, data + ":00", volt, temp, detecotrbattery, longitude, latitude);

                        Log.d(TAG, "Sending data=" + jsonObject.toString());

                        // 3. add JSON content to POST request body
                        setPostRequestContent(conn, jsonObject);

                        // 4. make POST request to the given URL
                        conn.connect();
                        InputStream responseObject = conn.getInputStream();

                        JSONObject jsonResponseObject = new JSONObject(convertStreamToString(responseObject));
                        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 400 && jsonResponseObject.get("errcode").equals(0)) {
                            dbVoltwatcherAdapter.updateSent(device, data);
                            Log.d(TAG, "Updated records sent");
                            trysend = true;
                        }
                    }
                    if (cursorCount > 0 && trysend) {
                        Log.d(TAG, "Data sent");
                        Intent intentWs = new Intent(WebserverEvents.DATA_SENT);
                        intentWs.putExtra("message", cursorCount + " rows");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                    }
                }
            } finally {
                cursor.close();
            }
            dbVoltwatcherAdapter.close();

            //Log.d(TAG, conn.getResponseMessage());
            // 5. return response message
            if (trysend) {
                return "OK";
            } else {
                return "KO";
            }
        } else {
            cursor.close();
            return "No data found to send";
        }
    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    private JSONObject buidJsonObject(String device, String data, String volt, String temp, String detectorbattery, String longitude, String latitude) throws JSONException {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("device", device);
        jsonObject.put("data", data);
        jsonObject.put("volt", volt);
        jsonObject.put("temp", temp);
        jsonObject.put("batteryperc", detectorbattery);
        jsonObject.put("longitude", longitude);
        jsonObject.put("latitude", latitude);
        return jsonObject;
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
                    return httpPost(urls[0]);
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