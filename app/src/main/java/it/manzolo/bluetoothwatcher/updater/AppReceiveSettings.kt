package it.manzolo.bluetoothwatcher.updater

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Http
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AppReceiveSettings(private val context: Context, private val webserviceUrl: String, private val webserviceUsername: String, private val webservicePassword: String) {

    fun receive() {
        HTTPAsyncTask(context, webserviceUrl, webserviceUsername, webservicePassword).execute()
    }

    companion object {
        private const val TAG = "AppReceiveSettings"

        class HTTPAsyncTask(val context: Context, private val webserviceUrl: String, private val webserviceUsername: String, private val webservicePassword: String) : AsyncTask<String?, Void?, String>() {

            override fun doInBackground(vararg params: String?): String? {
                // params comes from the execute() call: params[0] is the url.
                return try {
                    try {
                        httpGet(context, webserviceUrl, webserviceUsername, webservicePassword)
                    } catch (e: JSONException) {
                        Log.e(TAG, e.message.toString() + " from " + webserviceUrl)
                        //e.printStackTrace();
                        "Error: " + e.message.toString() + " from " + webserviceUrl
                }
            } catch (e: IOException) {
                //e.printStackTrace();
                Log.e(TAG, e.message.toString())
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message.toString() + " from " + webserviceUrl)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    "Unable to retrieve web page: " + e.message.toString() + " from " + webserviceUrl
                }
            }

            // onPostExecute displays the results of the AsyncTask.
            override fun onPostExecute(result: String) {
                Log.d(TAG, "Webserver response: $result")
            }
        }

        @Throws(IOException::class, JSONException::class)
        private fun httpGet(context: Context, webserviceUrl: String, webserviceUsername: String, webservicePassword: String): String {

            val url = URL(webserviceUrl + Http.getSettingsUrl)

            // 1. create HttpURLConnection
            val loginConn = Http.loginWebservice(webserviceUrl, webserviceUsername, webservicePassword)
            if (loginConn.responseCode in 200..399) {
                val tokenObject = JSONObject(Http().streamToString(loginConn.inputStream))
                val token = tokenObject.getString("token")
                Log.d("TOKEN", token)
                val conn = url.openConnection() as HttpURLConnection
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
                    editor.putString("webserviceServiceEverySeconds", jsonObject["webserviceServiceEverySeconds"].toString())
                } catch (e: JSONException) {
                    Log.w(TAG, "webserviceServiceEverySeconds setting not found")
                }
                try {
                    editor.putString("bluetoothServiceEverySeconds", jsonObject["bluetoothServiceEverySeconds"].toString())
                } catch (e: JSONException) {
                    Log.w(TAG, "bluetoothServiceEverySeconds setting not found")
                }
                try {
                    editor.putString("locationServiceEverySeconds", jsonObject["locationServiceEverySeconds"].toString())
                } catch (e: JSONException) {
                    Log.w(TAG, "locationServiceEverySeconds setting not found")
                }
                try {
                    editor.putString("updateServiceEverySeconds", jsonObject["updateServiceEverySeconds"].toString())
                } catch (e: JSONException) {
                    Log.w(TAG, "updateServiceEverySeconds setting not found")
                }
                try {
                    editor.putString("restartAppServiceEverySeconds", jsonObject["restartAppServiceEverySeconds"].toString())
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
                    editor.putBoolean("autoSettingsUpdate", jsonObject["autoSettingsUpdate"].toString() == "1")
                } catch (e: JSONException) {
                    Log.w(TAG, "autoSettingsUpdate service setting not found")
                }
                try {
                    editor.putBoolean("autoAppUpdate", jsonObject["autoAppUpdate"].toString() == "1")
                } catch (e: JSONException) {
                    Log.w(TAG, "autoAppUpdate service setting not found")
                }
                try {
                    editor.putBoolean("autoAppRestart", jsonObject["autoAppRestart"].toString() == "1")
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
                return conn.responseMessage + ""
            } else {
                val intentWs = Intent(WebserviceEvents.ERROR)
                intentWs.putExtra("message", "Server login response: " + loginConn.responseCode + " " + loginConn.responseMessage)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
            }
            Log.e(TAG, "Unable to update settings")
            return ""
        }


    }
}