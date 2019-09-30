package it.manzolo.job.service.bluewatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.preference.PreferenceManager
import android.util.Log


class MainService : JobService() {

    companion object {
        val TAG: String = MainService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        Log.d(TAG, "onStartJob : " + jobParameters.toString())
        startWatcherTask()
        App.scheduleJobService(this)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.d(TAG, "onStopJob")
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun startWatcherTask() {
        //Log.d(TAG, "startWatcherTask")
        try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            val url = preferences.getString("webserviceurl", "")
            val appsettings = AppReceiveSettings(this.applicationContext, url + "/api/appgetsettings")
            appsettings.receive()
            val address = preferences.getString("devices", "")
            val items = address.split(",")
            for (i in 0 until items.size) {
                val addr = items[i].replace("\\s".toRegex(), "")
                val btclient = Btclient(this.applicationContext, addr)
                if (btclient.openBT()) {
                    Thread.sleep(1500)
                    btclient.getData()
                    Thread.sleep(1500)
                    btclient.closeBT()
                }
                Thread.sleep(2000)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}