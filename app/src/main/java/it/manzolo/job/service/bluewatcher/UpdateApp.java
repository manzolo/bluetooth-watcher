package it.manzolo.job.service.bluewatcher;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
            //c.setDoOutput(true);
            c.connect();

            File file = new File(context.getCacheDir(), "app.apk");
            //FileProvider photoURI = new FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName()+".provider",file);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);

            InputStream is = c.getInputStream();

            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
            fos.close();
            is.close();

            Log.i("UpdateAPP", "Start update");


        } catch (Exception e) {
            e.printStackTrace();
            Log.e("UpdateAPP", "Update error! " + e.getMessage());
        }
        return null;
    }
}   