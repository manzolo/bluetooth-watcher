package it.manzolo.job.service.bluewatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.enums.BluetoothEvents


class MainService : JobService() {
    private var workerThread: Thread? = null

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

        /*val appUpdaterUtils = AppUpdaterUtils(this.applicationContext)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("manzolo", "bluetooth-watcher")
                .withListener(object : AppUpdaterUtils.UpdateListener {
                    override fun onSuccess(update: Update, isUpdateAvailable: Boolean?) {
                        Log.d("Latest Version", update.latestVersion)
                        //Log.d("Latest Version Code", update.latestVersionCode)
                        //Log.d("Release notes", update.releaseNotes)
                        Log.d("URL", update.urlToDownload.toString())

                        //Log.d("Is update available?", java.lang.Boolean.toString(isUpdateAvailable!!))
                        val updateapp = UpdateApp();
                        updateapp.setContext(applicationContext)
                        //updateapp.execute(update.urlToDownload.toString(),getApplicationInfo().dataDir);
                        //val dir = Environment.getExternalStorageDirectory();
                        //val dir = "/sdcard";
                        //val dir = getApplicationInfo().dataDir;
                        val dir = "/data/data/it.manzolo.job.service.bluewatcher/cache"

                        updateapp.execute("https://github.com/manzolo/bluetooth-watcher/releases/download/v1.0.0/app.apk",dir.toString());

                    }
                    override fun onFailed(error: AppUpdaterError) {
                        //Log.d("AppUpdater Error", "Something went wrong")
                    }
                })
        appUpdaterUtils.start()*/


        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val url = preferences.getString("webserviceurl", "")
        val debug = preferences.getBoolean("debug", false)
        if (url.replace("\\s".toRegex(), "").length === 0) {
            if (debug) {
                Toast.makeText(this, "Web server in setting not set", Toast.LENGTH_LONG).show()
            }
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
                if (debug) {
                    Toast.makeText(this, "No devices in settings", Toast.LENGTH_LONG).show()
                }
            } else {
                if (enabled) {
                    try {
                        btTask().execute(this.applicationContext)
                    } catch (e: InterruptedException) {
                        //e.printStackTrace()
                        Log.e(TAG, e.message)
                        //Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Service disabled in settings")
                    if (debug) {
                        Toast.makeText(this, "Service disabled in settings", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

private class btTask : AsyncTask<Context, Void, String>() {
    override fun doInBackground(vararg args: Context): String {
        val context = args[0]

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val address = preferences.getString("devices", "")
        val items = address.split(",")
        for (i in 0 until items.size) {
            val addr = items[i].replace("\\s".toRegex(), "")
            try {
                val btclient = Btclient(addr, context)
                btclient.retrieveData()
                Thread.sleep(2500)

            } catch (e: Exception) {
                //e.printStackTrace()
                Log.e(MainService.TAG, e.message)
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }
        }
        return "OK"
    }

    @Override
    override fun onPreExecute() {
        super.onPreExecute()

    }

    @Override
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        //Log.d(MainService.TAG, result)
    }
}