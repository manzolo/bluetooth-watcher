package it.manzolo.bluetoothwatcher.updater

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Http
import it.manzolo.bluetoothwatcher.webservice.WebServiceParameters
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext

class AppReceiveSettings(
    private val context: Context,
    private val webserviceparameter: WebServiceParameters
) : CoroutineScope {
    private val TAG = "AppReceiveSettings"
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    // call this method to cancel a coroutine when you don't need it anymore,
    // e.g. when user closes the screen
    fun cancel() {
        job.cancel()
    }

    fun execute() = launch {
        //onPreExecute()
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        Log.d(TAG, "Webserver response: $result")
        //onPostExecute(result)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doInBackground(): String =
        withContext(Dispatchers.IO) { // to run code in Background Thread
            try {
                try {
                    // do async work
                    // 1. create HttpURLConnection
                    val loginConn: HttpURLConnection = Http.loginWebservice(webserviceparameter)
                    if (loginConn.responseCode in 200..399) {
                        val tokenObject = JSONObject(Http().streamToString(loginConn.inputStream))
                        val token = tokenObject.getString("token")
                        Log.d("TOKEN", token)
                        val conn =
                            URL(webserviceparameter.getUrl() + Http.getSettingsUrl).openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.useCaches = false
                        conn.allowUserInteraction = false
                        conn.connectTimeout = Http.connectionTimeout
                        conn.readTimeout = Http.connectionTimeout
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        val jsonObject = JSONObject(Http().streamToString(conn.inputStream))
                        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                        val editor = preferences.edit()
                        try {
                            editor.putString(
                                "webserviceServiceEverySeconds",
                                jsonObject["webserviceServiceEverySeconds"].toString()
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "webserviceServiceEverySeconds setting not found")
                        }
                        try {
                            editor.putString(
                                "bluetoothServiceEverySeconds",
                                jsonObject["bluetoothServiceEverySeconds"].toString()
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "bluetoothServiceEverySeconds setting not found")
                        }
                        try {
                            editor.putString(
                                "locationServiceEverySeconds",
                                jsonObject["locationServiceEverySeconds"].toString()
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "locationServiceEverySeconds setting not found")
                        }
                        try {
                            editor.putString(
                                "updateServiceEverySeconds",
                                jsonObject["updateServiceEverySeconds"].toString()
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "updateServiceEverySeconds setting not found")
                        }
                        try {
                            editor.putString(
                                "restartAppServiceEverySeconds",
                                jsonObject["restartAppServiceEverySeconds"].toString()
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "restartAppServiceEverySeconds setting not found")
                        }
                        try {
                            editor.putString("devices", jsonObject["devices"].toString())
                        } catch (e: JSONException) {
                            Log.w(TAG, "devices setting not found")
                        }
                        /*try {
                            editor.putBoolean("enabled", jsonObject["enabled"].toString() == "1")
                        } catch (e: JSONException) {
                            Log.w(TAG, "enabled service setting not found")
                        }*/
                        try {
                            editor.putBoolean(
                                "autoSettingsUpdate",
                                jsonObject["autoSettingsUpdate"].toString() == "1"
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "autoSettingsUpdate service setting not found")
                        }
                        try {
                            editor.putBoolean(
                                "autoAppUpdate",
                                jsonObject["autoAppUpdate"].toString() == "1"
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "autoAppUpdate service setting not found")
                        }
                        try {
                            editor.putBoolean(
                                "autoAppRestart",
                                jsonObject["autoAppRestart"].toString() == "1"
                            )
                        } catch (e: JSONException) {
                            Log.w(TAG, "autoAppRestart service setting not found")
                        }
                        try {
                            editor.putBoolean("debugApp", jsonObject["debugApp"].toString() == "1")
                        } catch (e: JSONException) {
                            Log.w(TAG, "debug service setting not found")
                        }
                        editor.apply()
                        Log.d(TAG, "Settings updated")
                        return@withContext conn.responseMessage + ""
                    } else {
                        val intentWs = Intent(WebserviceEvents.ERROR)
                        intentWs.putExtra(
                            "message",
                            "Server login response: " + loginConn.responseCode + " " + loginConn.responseMessage
                        )
                        context.sendBroadcast(intentWs)
                    }
                    Log.e(TAG, "Unable to update settings")
                    return@withContext ""
                } catch (e: JSONException) {
                    Log.e(TAG, e.message.toString() + " from " + webserviceparameter.getUrl())
                    "Error: " + e.message.toString() + " from " + webserviceparameter.getUrl()
                }
            } catch (e: IOException) {
                //e.printStackTrace();
                Log.e(TAG, e.message.toString())
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra(
                    "message",
                    e.message.toString() + " from " + webserviceparameter.getUrl()
                )
                context.sendBroadcast(intent)
                "Unable to retrieve web page: " + e.message.toString() + " from " + webserviceparameter.getUrl()
            }

        }

}

// Runs on the Main(UI) Thread
//private fun onPreExecute() {
// show progress
//}

// Runs on the Main(UI) Thread
//private fun onPostExecute(result: String) {
// hide progress
//}

