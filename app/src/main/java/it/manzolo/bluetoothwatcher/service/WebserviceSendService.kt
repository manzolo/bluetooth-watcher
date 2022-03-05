package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Network
import it.manzolo.bluetoothwatcher.webservice.WebServerParameters
import it.manzolo.bluetoothwatcher.webservice.WebserviceSender


class WebserviceSendService : Service() {
    companion object {
        val TAG: String = WebserviceSendService::class.java.simpleName
        lateinit var webserviceparameter: WebServerParameters
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onWebserviceCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
        val debug = preferences.getBoolean("debugApp", false)

        if (preferences.getString("webserviceUrl", "").toString().isEmpty()) {
            val intent = Intent(WebserviceEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", "No webservice url in settings")
            applicationContext.sendBroadcast(intent)
            return
        }
        webserviceparameter = WebServerParameters(
            preferences.getString("webserviceUrl", "").toString(),
            preferences.getString("webserviceUsername", "username").toString(),
            preferences.getString("webservicePassword", "password").toString()
        )
        try {
            if (Network().isNetworkAvailable(applicationContext)) {
                val sender = WebserviceSender(applicationContext, webserviceparameter)
                sender.execute()
            } else {
                val intent = Intent(WebserviceEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", "No internet connection")
                applicationContext.sendBroadcast(intent)
                if (debug) {
                    Toast.makeText(applicationContext, "No internet connection", Toast.LENGTH_LONG).show()
                }
                Log.e(TAG, "No internet connection")
            }

        } catch (e: Exception) {
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            //Log.e(TAG, e.message.toString())
            val intent = Intent(WebserviceEvents.ERROR)
            intent.putExtra("message", e.message)
            applicationContext.sendBroadcast(intent)
        }

    }


}


