package it.manzolo.job.service.bluewatcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.job.service.bluewatcher.updater.AppReceiveSettings
import it.manzolo.job.service.bluewatcher.utils.BluetoothClient
import it.manzolo.job.service.enums.BluetoothEvents


class MainService : Service() {
    companion object {
        val TAG: String = MainService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onWebserverStartJob")
        startWatcherTask()
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun startWatcherTask() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val url = preferences.getString("webserviceurl", "")
        val debug = preferences.getBoolean("debug", false)
        if (url!!.replace("\\s".toRegex(), "").length === 0) {
            if (debug) {
                Toast.makeText(this, "Web server in setting not set", Toast.LENGTH_LONG).show()
            }
            Log.e(TAG, "Web server in setting not set")
        } else {
            val autoupdate = preferences.getBoolean("autoupdate", true)
            if (autoupdate) {
                val appsettings = AppReceiveSettings(this.applicationContext)
                appsettings.receive()
                Log.d(TAG, "Settings updated")
            }
            val enabled = preferences.getBoolean("enabled", true)
            val address = preferences.getString("devices", "")

            if (address!!.replace("\\s".toRegex(), "").length === 0) {
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
        val bluetoothDevices = address!!.split(",")
        for (i in 0 until bluetoothDevices.size) {
            val bluetoothDeviceAddress = bluetoothDevices[i].replace("\\s".toRegex(), "")
            loopok@ for (idxretry in 1..5) {
                if (btConnectionRetry(context, bluetoothDeviceAddress)) {
                    Thread.sleep(1000)
                    break@loopok
                }
            }
        }
        return "OK"
    }

    fun btConnectionRetry(context: Context, addr: String): Boolean {
        try {
            val bluetoothClient = BluetoothClient(context, addr)
            bluetoothClient.retrieveData()
            Thread.sleep(2500)
            //bluetoothClient.close()
            return true
        } catch (e: Exception) {
            //e.printStackTrace()
            Log.e(MainService.TAG, e.message)
            val intent = Intent(BluetoothEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", e.message)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            return false
        }
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