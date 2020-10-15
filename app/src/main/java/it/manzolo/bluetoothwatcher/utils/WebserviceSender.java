package it.manzolo.bluetoothwatcher.utils;

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
import java.util.Objects;

import it.manzolo.bluetoothwatcher.enums.BluetoothEvents;
import it.manzolo.bluetoothwatcher.enums.WebserverEvents;

public class WebserviceSender {

    public static final String TAG = "WebserviceSender";

    private final Context context;
    private final String webserviceUrl;
    private final String webserviceUsername;
    private final String webservicePassword;

    public WebserviceSender(Context context, String webserviceUrl, String webserviceUsername, String webservicePassword) {
        this.context = context;
        this.webserviceUrl = webserviceUrl;
        this.webserviceUsername = webserviceUsername;
        this.webservicePassword = webservicePassword;
    }

    public void send() {
        new HTTPAsyncTask().execute();
    }

    private String httpPost() throws IOException {

        URL url = new URL(this.webserviceUrl + HttpUtils.sendVoltUrl);
        boolean sendSuccessfully = false;

        DatabaseVoltwatcher databaseVoltwatcher = new DatabaseVoltwatcher(context);
        databaseVoltwatcher.open();
        databaseVoltwatcher.deleteOldSent();
        Cursor cursor = databaseVoltwatcher.fetchRowsNotSent();
        int cursorCount = cursor.getCount();
        Log.d(TAG, "Found " + cursorCount + " rows to send");
        if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to " + this.webserviceUrl);
            try {
                while (cursor.moveToNext()) {
                    // 1. create HttpURLConnection
                    HttpURLConnection loginConn = HttpUtils.loginWebservice(this.webserviceUrl, this.webserviceUsername, this.webservicePassword);

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
                        String device = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DEVICE));
                        String data = cursor.getString(cursor.getColumnIndex("grData"));
                        String volt = cursor.getString(cursor.getColumnIndex("volts"));
                        String temp = cursor.getString(cursor.getColumnIndex("temps"));
                        String detectorBattery = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DETECTOR_BATTERY));
                        String longitude = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LONGITUDE));
                        String latitude = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LATITUDE));
                        // 2. build JSON object
                        JSONObject jsonObject = buidJsonObject(device, data + ":00", volt, temp, detectorBattery, longitude, latitude);

                        Log.d(TAG, "Sending data=" + jsonObject.toString());

                        // 3. add JSON content to POST request body
                        new HttpUtils().setPostRequestContent(conn, jsonObject);

                        // 4. make POST request to the given URL
                        conn.connect();

                        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 400) {
                            JSONObject jsonResponseObject = new JSONObject(new HttpUtils().convertStreamToString(conn.getInputStream()));
                            if (jsonResponseObject.get("errcode").equals(0)) {
                                databaseVoltwatcher.updateSent(device, data);
                                Log.d(TAG, "Updated records sent");
                                sendSuccessfully = true;
                            } else {
                                Intent intentWs = new Intent(WebserverEvents.ERROR);
                                intentWs.putExtra("message", jsonResponseObject.get("errcode").toString() + " " + jsonResponseObject.get("message").toString());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                            }
                        } else {
                            Intent intentWs = new Intent(WebserverEvents.ERROR);
                            intentWs.putExtra("message", conn.getResponseCode() + " " + conn.getResponseMessage());
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                        }
                        Intent intentWs = new Intent(WebserverEvents.INFO);
                        intentWs.putExtra("message", jsonObject.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                    } else {
                        Intent intentWs = new Intent(WebserverEvents.ERROR);
                        intentWs.putExtra("message", "Server login response: " + loginConn.getResponseCode() + " " + loginConn.getResponseMessage());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                    }
                }
                if (cursorCount > 0 && sendSuccessfully) {
                    Log.d(TAG, "Data sent");
                    Intent intentWs = new Intent(WebserverEvents.DEBUG);
                    intentWs.putExtra("message", cursorCount + " rows sent");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
                }
            } catch (Exception e) {
                Intent intentWs = new Intent(WebserverEvents.ERROR);
                intentWs.putExtra("message", "Unable to send data to " + this.webserviceUrl);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);
            } finally {
                cursor.close();
            }
            databaseVoltwatcher.close();

            //Log.d(TAG, conn.getResponseMessage());
            // 5. return response message
            if (sendSuccessfully) {
                return "OK";
            } else {
                return "KO";
            }
        } else {
            cursor.close();

            Intent intentWs = new Intent(WebserverEvents.DEBUG);
            intentWs.putExtra("message", "No data found to send");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs);

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

    class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return httpPost();
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
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
            Log.d(TAG, "Webservice response: " + result);
        }
    }


}