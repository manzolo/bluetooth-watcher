package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.updater.AppReceiveSettings
import it.manzolo.bluetoothwatcher.utils.Network
import it.manzolo.bluetoothwatcher.webservice.WebserviceSender


class WebserviceSendService : Service() {
    companion object {
        val TAG: String = WebserviceSendService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onWebserviceCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onWebserviceStartJob")
        startWebserviceTask()
        //App.scheduleWebserviceService(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onWebserviceDestroy")
    }

    private fun startWebserviceTask() {

        Log.d(TAG, "WebserviceStart")
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val debug = preferences.getBoolean("debug", false)
        val webserviceUrl = preferences.getString("webserviceurl", "http://localhost:8080").toString()
        val webserviceUsername = preferences.getString("webserviceusername", "username").toString()
        val webservicePassword = preferences.getString("webservicepassword", "password").toString()

        try {
            if (Network().isNetworkAvailable(applicationContext)) {
                val autoSettingsUpdate = preferences.getBoolean("auto_settings_update", true)
                if (autoSettingsUpdate) {
                    val appSettings = AppReceiveSettings(this.applicationContext, webserviceUrl, webserviceUsername, webservicePassword)
                    appSettings.receive()
                    //Log.d(TAG, "Settings updated")
                }

                val sender = WebserviceSender(applicationContext, webserviceUrl, webserviceUsername, webservicePassword)
                sender.send()
            } else {
                val intent = Intent(WebserviceEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", "No internet connection")
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                if (debug) {
                    Toast.makeText(applicationContext, "No internet connection", Toast.LENGTH_LONG).show()
                }
                Log.e(TAG, "No internet connection")
            }

        } catch (e: Exception) {
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, e.message.toString())
            val intent = Intent(WebserviceEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", e.message)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            //e.printStackTrace();
        }

    }


}


