package it.manzolo.job.service.bluewatcher;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Sender {

    public static final String TAG = "Sender";

    private String url;
    private String device;
    private String data;
    private String volt;
    private String temp;

    public Sender(String url, String device, String data, String volt, String temp) {
        this.url = url;
        this.device = device;
        this.data = data;
        this.volt = volt;
        this.temp = temp;
    }

    private String httpPost(String myUrl) throws IOException, JSONException {
        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();
        //Log.d(TAG, conn.getResponseMessage());
        // 5. return response message
        return conn.getResponseMessage() + "";

    }

    public void send() {
        // perform HTTP POST request
        //Log.d(TAG, "/api/sendvolt");

        new HTTPAsyncTask().execute(this.url + "/api/sendvolt");

    }

    private JSONObject buidJsonObject() throws JSONException {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("device", this.device);
        jsonObject.put("data", this.data);
        jsonObject.put("volt", this.volt);
        jsonObject.put("temp", this.temp);

        return jsonObject;
    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        Log.d(TAG, "Sending data=" + jsonObject.toString());
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
        Log.d(TAG, "Data sent");
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
                    return "Error!";
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
    }


}