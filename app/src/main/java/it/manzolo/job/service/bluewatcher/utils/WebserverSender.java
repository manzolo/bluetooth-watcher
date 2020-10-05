package it.manzolo.job.service.bluewatcher.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import it.manzolo.job.service.enums.BluetoothEvents;
import it.manzolo.job.service.enums.WebserverEvents;

public class WebserverSender {

    public static final String TAG = "WebserverSender";

    private Context context;
    private String webserviceUrl;
    private String webserviceUsername;
    private String webservicePassword;

    public WebserverSender(Context context, String webserviceUrl, String webserviceUsername, String webservicePassword) {
        this.context = context;
        this.webserviceUrl = webserviceUrl;
        this.webserviceUsername = webserviceUsername;
        this.webservicePassword = webservicePassword;
    }


    public void send() {
        new HTTPAsyncTask().execute();
    }

    private String httpPost() throws IOException, JSONException {
        URL loginUrl = new URL(this.webserviceUrl + HttpUtils.loginUrl);
        URL url = new URL(this.webserviceUrl + HttpUtils.sendVoltUrl);
        boolean trysend = false;

        DbVoltwatcherAdapter dbVoltwatcherAdapter = new DbVoltwatcherAdapter(context);
        dbVoltwatcherAdapter.open();
        dbVoltwatcherAdapter.deleteSent();
        Cursor cursor = dbVoltwatcherAdapter.fetchRowsNotSent();
        int cursorCount = cursor.getCount();
        Log.d(TAG, "Found " + cursorCount + " rows to send");
        if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to " + this.webserviceUrl);
            try {
                while (cursor.moveToNext()) {

                    // 1. create HttpURLConnection

                    HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
                    loginConn.setUseCaches(false);
                    loginConn.setAllowUserInteraction(false);
                    loginConn.setConnectTimeout(HttpUtils.connectionTimeout);
                    loginConn.setReadTimeout(HttpUtils.connectionTimeout);
                    loginConn.setRequestMethod("POST");
                    loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    // 3. add JSON content to POST request body

                    new HttpUtils().setPostRequestContent(loginConn, buidLoginJsonObject());

                    // 4. make POST request to the given URL
                    loginConn.connect();
                    if (loginConn.getResponseCode() >= 200 && loginConn.getResponseCode() < 400) {
                        JSONObject tokenObject = new JSONObject(new HttpUtils().convertStreamToString(loginConn.getInputStream()));
                        String token = tokenObject.getString("token");
                        Log.d("TOKEN", token);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PUT");
                        conn.setUseCaches(false);
                        conn.setAllowUserInteraction(false);
                        conn.setConnectTimeout(HttpUtils.connectionTimeout);
                        conn.setReadTimeout(HttpUtils.connectionTimeout);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        conn.setRequestProperty("Authorization", "Bearer " + token);

                        //Log.e("TAG",cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DEVICE)));
                        //Log.e("TAG",cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DATA)));
                        String device = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DEVICE));
                        String data = cursor.getString(cursor.getColumnIndex("grData"));
                        String volt = cursor.getString(cursor.getColumnIndex("volts"));
                        String temp = cursor.getString(cursor.getColumnIndex("temps"));
                        String detectorBattery = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_DETECTORBATTERY));
                        String longitude = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_LON));
                        String latitude = cursor.getString(cursor.getColumnIndex(DbVoltwatcherAdapter.KEY_LAT));
                        // 2. build JSON object
                        JSONObject jsonObject = buidJsonObject(device, data + ":00", volt, temp, detectorBattery, longitude, latitude);

                        Log.d(TAG, "Sending data=" + jsonObject.toString());

                        // 3. add JSON content to POST request body
                        new HttpUtils().setPostRequestContent(conn, jsonObject);

                        // 4. make POST request to the given URL
                        conn.connect();

                        JSONObject jsonResponseObject = new JSONObject(new HttpUtils().convertStreamToString(conn.getInputStream()));
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
                    return httpPost();
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