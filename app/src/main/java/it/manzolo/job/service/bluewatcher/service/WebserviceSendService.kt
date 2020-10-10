package it.manzolo.job.service.bluewatcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.job.service.bluewatcher.utils.WebserverSender
import it.manzolo.job.service.enums.WebserverEvents


class WebserviceSendService : Service() {
    companion object {
        val TAG: String = WebserviceSendService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onWebsendCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onWebsendStartJob")
        startWebsendTask()
        //App.scheduleWebsendService(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onWebsendDestroy")
    }

    private fun startWebsendTask() {

        Log.d(TAG, "WebsendStart")
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val debug = preferences.getBoolean("debug", false)
        val url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt")
        val username = preferences.getString("webserviceusername", "username")
        val password = preferences.getString("webservicepassword", "password")

        try {
            if (isNetworkAvailable()) {
                val sender = WebserverSender(applicationContext, url, username, password)
                sender.send()
            } else {
                val intent = Intent(WebserverEvents.ERROR)
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
            Log.e(TAG, e.message)
            val intent = Intent(WebserverEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", e.message)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            //e.printStackTrace();
        }

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    /*
    private fun isNetworkAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }
    */
}


