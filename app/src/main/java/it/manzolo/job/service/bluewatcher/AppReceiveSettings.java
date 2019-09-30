package it.manzolo.job.service.bluewatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppReceiveSettings {

    public static final String TAG = "AppReceiveSettings";

    Context ctx;
    String url;

    public AppReceiveSettings(Context ctx, String url) {
        this.ctx = ctx;
        this.url = url;
    }

    private String httpGet() throws IOException, JSONException {
        URL url = new URL(this.url);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        //Log.i(TAG, sb.toString());

        JSONObject jsonObject = new JSONObject(sb.toString());
        //Log.i(TAG, jsonObject.get("seconds").toString());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("seconds", jsonObject.get("seconds").toString());
        editor.commit();
        return conn.getResponseMessage() + "";

    }

    public void receive() {
        // perform HTTP POST request
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        String url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt"); //"" is the default String to return if the preference isn't found
        Log.i(TAG, url);
        new HTTPAsyncTask().execute(url + "/api/appgetsettings");

    }


    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return httpGet();
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
    }


}