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
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Date


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
            // Dummy reading volt for emulator
            val dummyDevice = "00:00:00:00:00:00"
            val dummyVolt = "18.99"
            val dummyTemperatureC = "30"
            val dummyTemperatureF = "100"
            val dummyAmpere = "1"
            val dummyDate = Date.now()
            //val dummyDate = "2000-01-01 00:00:00"
            val intentBt = Intent(BluetoothEvents.DATA_RETRIEVED)

            intentBt.putExtra("device", dummyDevice)
            intentBt.putExtra("volt", dummyVolt)
            intentBt.putExtra("data", dummyDate)
            intentBt.putExtra("tempC", dummyTemperatureC)
            intentBt.putExtra("tempF", dummyTemperatureF)
            intentBt.putExtra("amp", dummyAmpere)

            intentBt.putExtra("message", dummyDevice + " " + dummyVolt + "v " + dummyTemperatureC + "°")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentBt)

            val intentBluetoothCloseConnection = Intent(BluetoothEvents.CLOSECONNECTION)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentBluetoothCloseConnection)
            //val session = Session(applicationContext);
            //session.webserviceToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE2MDIwMjAzNDQsImV4cCI6MTYwMjAyMzk0NCwicm9sZXMiOlsiUk9MRV9TVVBFUl9BRE1JTiIsIlJPTEVfVVNFUiJdLCJ1c2VybmFtZSI6ImFkbWluIn0.D8_eH7IeXJ77W2KSs5QW6QpRjYvuwO08A1H0tG2l3rHCWkQ6EyWpp07tB9WNMpdtlHXaPYGm_zKRFW61PkRQPZn6Q6rN1QENTO1nBGImLWG3144LGHUoOjKaAK9b_Jtd49Heu35akGrXKbXd8gPQv0GqKPZab2uKCXMkVWK6I5tvy4MkfEQEJCdCctJDMJHWQ6bPb-Orb8nGA7smNV1psJFVurgSlMw-Ao5r4zvcc4bVJ55Yb8MqREfo0UPTB1_Fr3MTpYDRozc0obEHoZsNuE6Wk3kKySrP6MaIyDc0eoiNH2tdLvnGi7mYXMviTaZFD7NTMnsEsWg3uCH-0YO9Zhkks9uKJycC9aFj8LM5_820egEQR5scytP403XQm0l9fPoOojGfFjkAF7nmxmV8zt80CZBDqWoGXFsOONMvUSPJIgS_JIl-wY1NCanZCekvN16DceBkHFTcqDNpOHXdxQ61m_MaC3n4qIrg4pbUsnyWm0fx_rsqod6wAA7QBUuacgr3hFrX9C-T6DFvX32roxpgemU3kKngg3Kp22clN4MRVm-G1g_HskXz8hkzoK7W1YM4tFL8PcBO9H_jUm2IRonXywem_tVtZYr7XbMZH-yNQ1JfaFyxtkh1tPhC9Yzuqii_pNy7SfmePmTC3N5Ak3DmbJ7U6ZnaCvRnoV42QO4"
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