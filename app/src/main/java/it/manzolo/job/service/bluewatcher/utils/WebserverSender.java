package it.manzolo.job.service.bluewatcher.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
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

    public WebserverSender(Context context, String url) {
        this.context = context;
        this.url = url;
    }

    public void send() {
        new HTTPAsyncTask().execute(this.url + "/api/sendvolt");
    }

    private String httpPost(String myUrl) throws IOException, JSONException {
        URL url = new URL(myUrl);



        DbVoltwatcherAdapter dbVoltwatcherAdapter = new DbVoltwatcherAdapter(context);
        dbVoltwatcherAdapter.open();
        Cursor cursor = dbVoltwatcherAdapter.fetchRowsNotSent();
        Integer cursorCount = cursor.getCount();
        Log.d(TAG, "Found " + cursorCount + " rows to send");
        if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to " + myUrl);

            boolean trysend = false;
            try {
                while (cursor.moveToNext()) {

                    // 1. create HttpURLConnection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

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

                    String responseText = conn.getResponseMessage() + "";
                    if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 400) {
                        dbVoltwatcherAdapter.deleteSent();
                        dbVoltwatcherAdapter.updateSent(device, data);
                        trysend = true;
                    } else {
                        trysend = false;
                    }
                }
                if (cursorCount > 0 && trysend) {
                    Log.d(TAG, "Data sent");
                    Intent intentWs = new Intent(WebserverEvents.DATA_SENT);
                    intentWs.putExtra("message", cursorCount + " rows");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
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