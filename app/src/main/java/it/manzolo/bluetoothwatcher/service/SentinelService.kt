package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.updater.AppReceiveSettings


class SentinelService : Service() {

    companion object {
        val TAG: String = SentinelService::class.java.simpleName

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onSentinelStartJob")
        starSentinelTask()
        return START_NOT_STICKY
    }

    private fun starSentinelTask() {
        Log.d(TAG, "onSentinelStartCommand")
        val webserviceUrl = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webserviceUrl", "").toString()
        if (webserviceUrl.isEmpty()) {
            val intent = Intent(WebserviceEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", "No webservice url in settings")
            applicationContext.sendBroadcast(intent)
            return
        }
        val webserviceUsername = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webserviceUsername", "username").toString()
        val webservicePassword = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webservicePassword", "password").toString()
        val autoSettingsUpdate = PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("autoSettingsUpdate", true)
        if (autoSettingsUpdate) {
            val appSettings = AppReceiveSettings(this.applicationContext, webserviceUrl, webserviceUsername, webservicePassword)
            appSettings.receive()
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}