package it.manzolo.bluetoothwatcher.updater;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import it.manzolo.bluetoothwatcher.enums.WebserverEvents;

public class UpdateApp extends AsyncTask<String, Void, Void> {
    private Context context;

    public void setContext(Context contextf) {
        context = contextf;
    }

    @Override
    protected Void doInBackground(String... arg0) {
        try {
            URL url = new URL(arg0[0]);
            String filepath = arg0[1];
            Log.d("url", url.toString());
            Log.d("filepath", filepath);

            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.connect();

            File file = new File(context.getCacheDir(), "app.apk");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);

            InputStream is = c.getInputStream();

            byte[] buffer = new byte[1024];
            int len1;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
            fos.close();
            is.close();

            Log.i("ManzoloUpdate", "Download complete");
            Intent intent = new Intent(WebserverEvents.APP_UPDATE);
            // You can also include some extra data.
            intent.putExtra("message", "Download complete");
            intent.putExtra("file", file.getAbsolutePath());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("UpdateAPP", "Update error! " + e.getMessage());
        }
        return null;
    }
}

