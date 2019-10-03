package it.manzolo.job.service.bluewatcher.utils;

import android.content.Context;
import android.content.Intent;
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

public class WebserverSender {

    public static final String TAG = "WebserverSender";

    private Context context;
    private String url;
    private String device;
    private String data;
    private String volt;
    private String temp;
    private String batteryperc;

    public WebserverSender(Context context, String url, String device, String data, String volt, String temp, String batteryperc) {
        this.context = context;
        this.url = url;
        this.device = device;
        this.data = data;
        this.volt = volt;
        this.temp = temp;
        this.batteryperc = batteryperc;
    }

    public void send() {
        new HTTPAsyncTask().execute(this.url + "/api/sendvolt");
    }

    private String httpPost(String myUrl) throws IOException, JSONException {
        URL url = new URL(myUrl);

        Log.d(TAG, "Try connecting to " + myUrl);
        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        Log.d(TAG, "Sending data=" + jsonObject.toString());

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();

        String responseText = conn.getResponseMessage() + "";

        //Log.d(TAG, conn.getResponseMessage());
        // 5. return response message
        return responseText;

    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
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
    private JSONObject buidJsonObject() throws JSONException {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("device", this.device);
        jsonObject.put("data", this.data);
        jsonObject.put("volt", this.volt);
        jsonObject.put("temp", this.temp);
        jsonObject.put("batteryperc", this.batteryperc);

        return jsonObject;
    }


}