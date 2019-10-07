package it.manzolo.job.service.bluewatcher.service

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
import it.manzolo.job.service.bluewatcher.utils.Session
import it.manzolo.job.service.bluewatcher.utils.WebserverSender
import it.manzolo.job.service.bluewatcher.utils.getBatteryPercentage
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

    private fun startWebsendTask() {

        Log.d(TAG, "WebsendStart")
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt")
        val debug = preferences.getBoolean("debug", false)

        try {
            val bp = getBatteryPercentage(applicationContext)
            val session = Session(applicationContext)

            if (isNetworkAvailable(applicationContext)) {
                Log.d(TAG, "Send data to webserver")
                val sender = WebserverSender(applicationContext, url)
                sender.send()
                val intentWs = Intent(WebserverEvents.DATA_SENT)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentWs)
                // You can also include some extra data.
                if (debug) {
                    Toast.makeText(applicationContext, "Data sent", Toast.LENGTH_LONG).show()
                }
                Log.d(TAG, "Data sent")
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


