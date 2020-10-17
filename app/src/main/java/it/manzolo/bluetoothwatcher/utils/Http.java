package it.manzolo.bluetoothwatcher.utils;

import android.content.Context;
import android.util.Log;

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

public class Http {
    final public static String loginUrl = "/api/login_check";
    final public static String sendVoltUrl = "/api/volt/record.json";
    final public static String getSettingsUrl = "/api/get/settings/app.json";
    final public static int connectionTimeout = 15000;

    public static String getNewWebserviceToken(Context context, String url, String username, String password) throws Exception {
        Session sessionPreferences = new Session(context);
        String token;
        HttpURLConnection loginConn = Http.loginWebservice(url, username, password);
        int responseCode = loginConn.getResponseCode();
        String responseMessage = loginConn.getResponseMessage();

        if (responseCode >= 200 && responseCode < 400) {
            JSONObject tokenObject = new JSONObject(new Http().streamToString(loginConn.getInputStream()));
            token = tokenObject.getString("token");
            Log.d("TOKEN", token);
            sessionPreferences.setWebserviceToken(token);
        } else {
            Log.e("TOKEN", "Unable to retrieve token");
            throw new Exception(responseCode + " " + responseMessage + ", unable to retrieve token from " + url);
        }
        return token;
    }

    public void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    public static HttpURLConnection loginWebservice(String url, String username, String password) throws IOException, JSONException {
        URL loginUrl = new URL(url + Http.loginUrl);
        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setUseCaches(false);
        loginConn.setAllowUserInteraction(false);
        loginConn.setConnectTimeout(Http.connectionTimeout);
        loginConn.setReadTimeout(Http.connectionTimeout);
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("username", username);
        jsonObject.put("password", password);
        // 3. add JSON content to POST request body
        new Http().setPostRequestContent(loginConn, jsonObject);
        // 4. make POST request to the given URL
        loginConn.connect();
        return loginConn;
    }

    public static String getWebserviceToken(Context context, String url, String username, String password) throws Exception {
        Session sessionPreferences = new Session(context);
        String lastToken = sessionPreferences.getWebserviceToken();
        assert lastToken != null;
        if (lastToken.isEmpty()) {
            Log.d("TOKEN", "Try to get new token");
            lastToken = Http.getNewWebserviceToken(context, url, username, password);
        } else {
            Log.d("TOKEN", "Used last token");
        }
        return lastToken;
    }

    public String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
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


}
