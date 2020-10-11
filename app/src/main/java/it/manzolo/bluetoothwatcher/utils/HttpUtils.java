package it.manzolo.bluetoothwatcher.utils;

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

public class HttpUtils {
    final public static String loginUrl = "/api/login_check";
    final public static String sendVoltUrl = "/api/volt/record.json";
    final public static String getSettingsUrl = "/api/get/settings/app.json";
    final public static int connectionTimeout = 30000;


    public String convertStreamToString(InputStream is) {
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

    public void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    public static HttpURLConnection loginWebservice(String url, String username, String password) throws IOException, JSONException {
        URL loginUrl = new URL(url + HttpUtils.loginUrl);
        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setUseCaches(false);
        loginConn.setAllowUserInteraction(false);
        loginConn.setConnectTimeout(HttpUtils.connectionTimeout);
        loginConn.setReadTimeout(HttpUtils.connectionTimeout);
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("username", username);
        jsonObject.put("password", password);
        // 3. add JSON content to POST request body
        new HttpUtils().setPostRequestContent(loginConn, jsonObject);
        // 4. make POST request to the given URL
        loginConn.connect();
        return loginConn;
    }

}
