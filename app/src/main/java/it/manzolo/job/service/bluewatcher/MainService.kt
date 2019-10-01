package it.manzolo.job.service.bluewatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast


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

        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val url = preferences.getString("webserviceurl", "")
        if (url.replace("\\s".toRegex(), "").length === 0) {
            Toast.makeText(this, "Web server in setting not set", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Web server in setting not set")
        } else {
            val autoupdate = preferences.getBoolean("autoupdate", true)
            if (autoupdate) {
                val appsettings = AppReceiveSettings(this.applicationContext, url + "/api/appgetsettings")
                appsettings.receive()
                Log.d(TAG, "Settings updated")
            }
            val enabled = preferences.getBoolean("enabled", true)
            val address = preferences.getString("devices", "")

            if (address.replace("\\s".toRegex(), "").length === 0) {
                Log.e(TAG, "No devices in settings")
                Toast.makeText(this, "No devices in settings", Toast.LENGTH_LONG).show()
            } else {
                if (enabled) {
                    try {
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
                        //e.printStackTrace()
                        Log.e(TAG, e.message)
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        //e.printStackTrace()
                        Log.e(TAG, e.message)
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Service disabled in settings")
                    Toast.makeText(this, "Service disabled in settings", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}