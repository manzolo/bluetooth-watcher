package it.manzolo.bluetoothwatcher.updater

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Bluelog
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateApp(private val context: Context) : AsyncTask<String?, Void?, Void?>() {
    override fun doInBackground(vararg params: String?): Void? {
        try {
            val url = URL(params[0])
            val filepath = params[1]
            Log.d("url", url.toString())
            Log.d("filepath", filepath.toString())
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connect()
            val file = File(context.cacheDir, "app.apk")
            if (file.exists()) {
                if (!file.delete()) {
                    val intent = Intent(WebserviceEvents.APP_UPDATE_ERROR)
                    // You can also include some extra data.
                    intent.putExtra("message", "Unable to delete " + file.absolutePath)
                    intent.putExtra("type", Bluelog.logEvents.ERROR)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return null
                }
            }
            val fos = FileOutputStream(file)
            val `is` = c.inputStream
            val buffer = ByteArray(1024)
            var len1: Int
            while (`is`.read(buffer).also { len1 = it } != -1) {
                fos.write(buffer, 0, len1)
            }
            fos.close()
            `is`.close()
            Log.i("ManzoloUpdate", "Download complete")
            val intent = Intent(WebserviceEvents.APP_UPDATE)
            // You can also include some extra data.
            intent.putExtra("message", "Download complete")
            intent.putExtra("file", file.absolutePath)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        } catch (e: Exception) {
            //e.printStackTrace();
            Log.e("UpdateAPP", "Update error! " + e.message)
            val intent = Intent(WebserviceEvents.APP_UPDATE_ERROR)
            // You can also include some extra data.
            intent.putExtra("message", e.message)
            intent.putExtra("type", Bluelog.logEvents.ERROR)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
        return null
    }
}