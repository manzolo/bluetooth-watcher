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
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserverEvents
import it.manzolo.bluetoothwatcher.updater.AppReceiveSettings
import it.manzolo.bluetoothwatcher.utils.BluetoothClient
import it.manzolo.bluetoothwatcher.utils.DateUtils
import it.manzolo.bluetoothwatcher.utils.DbVoltwatcherAdapter
import it.manzolo.bluetoothwatcher.utils.Session


class BluetoothService : Service() {
    companion object {
        val TAG: String = BluetoothService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onBluetoothStartJob")
        if (Build.FINGERPRINT.contains("generic")) {
            // Dummy reading volt for emulator
            val session = Session(applicationContext)
            val dbVoltWatcherAdapter = DbVoltwatcherAdapter(applicationContext)
            val dummyDevice = "00:00:00:00:00:00"
            val dummyVolt = "18.99"
            val dummyTemperatureC = "30"
            val dummyTemperatureF = "100"
            val dummyAmpere = "1"
            val dummyDate = DateUtils.now()
            dbVoltWatcherAdapter.open()
            dbVoltWatcherAdapter.createRow(dummyDevice, dummyVolt, dummyTemperatureC, dummyDate, session.getlongitude(), session.getlatitude(), "0")
            dbVoltWatcherAdapter.close()
            val intentBt = Intent(BluetoothEvents.DATA_RETRIEVED)

            intentBt.putExtra("device", dummyDevice)
            intentBt.putExtra("volt", dummyVolt)
            intentBt.putExtra("data", dummyDate)
            intentBt.putExtra("tempC", dummyTemperatureC)
            intentBt.putExtra("tempF", dummyTemperatureF)
            intentBt.putExtra("amp", dummyAmpere)

            intentBt.putExtra("message", dummyDevice + " " + dummyVolt + "v " + dummyTemperatureC + "°")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentBt)

            val intent = Intent(BluetoothEvents.CLOSECONNECTION)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            //mLogs.add(0, Bluelog(DateUtils.now(), "Debug data set", Bluelog.logEvents.WARNING))
            //mLogs.add(0, Bluelog(DateUtils.now(), "Debug data set long message string set long message string set long message string set long message string set long message string set long message string ", Bluelog.logEvents.WARNING))
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
        val url = preferences.getString("webserviceurl", "")
        val debug = preferences.getBoolean("debug", false)
        if (url!!.replace("\\s".toRegex(), "").isEmpty()) {
            if (debug) {
                Toast.makeText(this, "Web server in setting not set", Toast.LENGTH_LONG).show()
            }
            Log.e(TAG, "Web server in setting not set")
            val intent = Intent(WebserverEvents.ERROR)
            // You can also include some extra data.
            intent.putExtra("message", "Web server in setting not set")
            LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(intent)
        } else {
            val autoSettingsUpdate = preferences.getBoolean("auto_settings_update", true)
            val webserverurl = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt")
            val username = preferences.getString("webserviceusername", "username")
            val password = preferences.getString("webservicepassword", "password")

            if (autoSettingsUpdate) {
                val appsettings = AppReceiveSettings(this.applicationContext, webserverurl, username, password)
                appsettings.receive()
                //Log.d(TAG, "Settings updated")
            }
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
                        Log.e(TAG, e.message)
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
            Log.e(BluetoothService.TAG, e.message)
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