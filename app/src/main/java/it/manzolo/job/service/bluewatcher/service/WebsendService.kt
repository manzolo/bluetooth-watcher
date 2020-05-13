package it.manzolo.job.service.bluewatcher.service

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.bluewatcher.App
import it.manzolo.job.service.bluewatcher.utils.WebserverSender
import it.manzolo.job.service.enums.WebserverEvents

class WebsendService : JobService() {
    companion object {
        val TAG: String = WebsendService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onWebsendCreate")
    }

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        Log.d(TAG, "onWebsendStartJob : " + jobParameters.toString())
        val startService = startService(Intent(this, LocationService::class.java))
        startService.run { }
        startWebsendTask()
        App.scheduleWebsendService(this)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.d(TAG, "onWebsendStopJob")
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onWebsendDestroy")
    }

    @SuppressLint("MissingPermission")
    private fun startWebsendTask() {

        Log.d(TAG, "WebsendStart")
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val debug = preferences.getBoolean("debug", false)
        val url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt")

        try {
            if (isNetworkAvailable(applicationContext)) {
                val sender = WebserverSender(applicationContext, url)
                sender.send()
            } else {
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
    /*val intent = Intent(WebserverEvents.APP_CHECK_UPDATE)
    val sendBroadcast = LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    val githubup = GithubUpdater()
    githubup.checkUpdate(applicationContext)*/

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}


