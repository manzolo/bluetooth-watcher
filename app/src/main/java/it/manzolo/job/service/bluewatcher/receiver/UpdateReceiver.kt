package it.manzolo.job.service.bluewatcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.manzolo.job.service.bluewatcher.activity.MainActivity
import java.io.File

class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val file = File(context.cacheDir, "app.apk")

        if (file.exists()) {
            file.delete()
        }
        // Restart your app here
        val i = Intent(context, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}