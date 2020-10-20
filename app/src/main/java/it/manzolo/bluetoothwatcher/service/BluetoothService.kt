package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.bluetooth.BluetoothClient
import it.manzolo.bluetoothwatcher.device.DebugData
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents


class BluetoothService : Service() {
    companion object {
        val TAG: String = BluetoothService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onBluetoothStartJob")
        if (Build.FINGERPRINT.contains("generic")) {
            DebugData().insertDebugData(applicationContext)
        } else {
            startBluetoothTask()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun startBluetoothTask() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val url = preferences.getString("webserviceUrl", "")
        val debug = preferences.getBoolean("debugApp", false)
        if (url!!.replace("\\s".toRegex(), "").isEmpty()) {
            if (debug) {
                Toast.makeText(this, "Web server in setting not set", Toast.LENGTH_LONG).show()
            }
            Log.e(TAG, "Web server in setting not set")
            val intent = Intent(WebserviceEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", "Web server in setting not set")
            LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(intent)
        } else {
            val enabled = preferences.getBoolean("enabled", true)
            val address = preferences.getString("devices", "")

            if (address!!.replace("\\s".toRegex(), "").isEmpty()) {
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", "No devices in settings")
                LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(intent)
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
                        Log.e(TAG, e.message.toString())
                        //Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    val intent = Intent(BluetoothEvents.ERROR)
                    // You can also include some extra data.
                    intent.putExtra("message", "Service disabled in settings")
                    LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(intent)
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
        return try {
            val bluetoothClient = BluetoothClient(context, addr)
            bluetoothClient.retrieveData()
            Thread.sleep(2500)
            //bluetoothClient.close()
            true
        } catch (e: Exception) {
            //e.printStackTrace()
            Log.e(BluetoothService.TAG, e.message.toString())
            val intent = Intent(BluetoothEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", e.message)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            false
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