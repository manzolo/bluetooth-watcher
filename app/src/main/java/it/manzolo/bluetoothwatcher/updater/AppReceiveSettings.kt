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
    @Throws(IOException::class, JSONException::class)
    private fun httpGet(): String {
        val url = URL(webserviceUrl + Http.getSettingsUrl)

        // 1. create HttpURLConnection
        val loginConn = Http.loginWebservice(webserviceUrl, webserviceUsername, webservicePassword)
        if (loginConn.responseCode >= 200 && loginConn.responseCode < 400) {
            val tokenObject = JSONObject(Http().convertStreamToString(loginConn.inputStream))
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
            val jsonObject = JSONObject(Http().convertStreamToString(conn.inputStream))
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            try {
                editor.putString("webservice_service_every_seconds", jsonObject["webservice_service_every_seconds"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "webservice_service_every_seconds setting not found")
            }
            try {
                editor.putString("bluetooth_service_every_seconds", jsonObject["bluetooth_service_every_seconds"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "bluetooth_service_every_seconds setting not found")
            }
            try {
                editor.putString("location_service_every_seconds", jsonObject["location_service_every_seconds"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "location_service_every_seconds setting not found")
            }
            try {
                editor.putString("update_service_every_seconds", jsonObject["update_service_every_seconds"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "update_service_every_seconds setting not found")
            }
            try {
                editor.putString("restart_app_service_every_seconds", jsonObject["restart_app_service_every_seconds"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "restart_app_service_every_seconds setting not found")
            }
            try {
                editor.putString("devices", jsonObject["devices"].toString())
            } catch (e: JSONException) {
                Log.w(TAG, "devices setting not found")
            }
            try {
                editor.putBoolean("enabled", jsonObject["enabled"].toString() == "1")
            } catch (e: JSONException) {
                Log.w(TAG, "enabled service setting not found")
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

    fun receive() {
        HTTPAsyncTask().execute()
    }

    @Throws(JSONException::class)
    private fun buidLoginJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("username", webserviceUsername)
        jsonObject.put("password", webservicePassword)
        return jsonObject
    }

    internal inner class HTTPAsyncTask : AsyncTask<String?, Void?, String>() {
        override fun doInBackground(vararg params: String?): String? {
            // params comes from the execute() call: params[0] is the url.
            return try {
                try {
                    httpGet()
                } catch (e: JSONException) {
                    Log.e(TAG, e.message.toString())
                    //e.printStackTrace();
                    "Error: " + e.message.toString()
                }
            } catch (e: IOException) {
                //e.printStackTrace();
                Log.e(TAG, e.message.toString())
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message.toString())
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                "Unable to retrieve web page: " + e.message.toString()
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        override fun onPostExecute(result: String) {
            Log.d(TAG, "Webserver response: $result")
        }
    }

    companion object {
        private const val TAG = "AppReceiveSettings"
    }
}