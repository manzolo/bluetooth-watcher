package it.manzolo.job.service.bluewatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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

    Context ctx;
    private String device;
    private String data;
    private String volt;
    private String temp;

    public Sender(Context ctx, String device, String data, String volt, String temp) {
        this.ctx = ctx;
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
        Log.i(TAG, conn.getResponseMessage());
        // 5. return response message
        return conn.getResponseMessage() + "";

    }

    public void send() {
        // perform HTTP POST request
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        String url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt"); //"" is the default String to return if the preference isn't found
        Log.i(TAG, "/api/sendvolt");
        new HTTPAsyncTask().execute(url + "/api/sendvolt");

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
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), "data=" + jsonObject.toString());
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
                    e.printStackTrace();
                    return "Error!";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
    }


}