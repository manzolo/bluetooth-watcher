package it.manzolo.bluetoothwatcher.utils

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceResponse
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class WebserviceSender(private val context: Context, private val webserviceUrl: String, private val webserviceUsername: String, private val webservicePassword: String) {
    fun send() {
        HTTPAsyncTask().execute()
    }

    @Throws(IOException::class, JSONException::class)
    private fun sendData(url: URL, token: String, jsonObject: JSONObject): String {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.useCaches = false
        conn.allowUserInteraction = false
        conn.connectTimeout = HttpUtils.connectionTimeout
        conn.readTimeout = HttpUtils.connectionTimeout
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer $token")

        // 3. add JSON content to POST request body
        HttpUtils().setPostRequestContent(conn, jsonObject)

        // 4. make POST request to the given URL
        conn.connect()
        return if (conn.responseCode >= 200 && conn.responseCode < 400) {
            val jsonResponseObject = JSONObject(HttpUtils().convertStreamToString(conn.inputStream))
            if (jsonResponseObject["errcode"] == 0) {
                val intentWebserviceSent = Intent(WebserviceEvents.INFO)
                intentWebserviceSent.putExtra("message", jsonObject.toString())
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWebserviceSent)
                WebserviceResponse.OK
            } else {
                val intentWs = Intent(WebserviceEvents.ERROR)
                intentWs.putExtra("message", jsonResponseObject["errcode"].toString() + " " + jsonResponseObject["message"].toString())
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
                WebserviceResponse.ERROR
            }
        } else if (conn.responseCode == 401) {
            //String response = new HttpUtils().convertStreamToString(conn.getInputStream());
            //JSONObject jsonResponseObject = new JSONObject(response);
            //if (jsonResponseObject.get("code").equals(401) && jsonResponseObject.get("message").equals("Expired JWT Token")){
            WebserviceResponse.TOKEN_EXPIRED
            //}else{
            //    return WebserviceResponse.ERROR;
            //}
        } else {
            val intentWs = Intent(WebserviceEvents.ERROR)
            intentWs.putExtra("message", conn.responseCode.toString() + " " + conn.responseMessage)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
            WebserviceResponse.ERROR
        }
    }

    @Throws(IOException::class)
    private fun httpWebserviceSendData(): String {
        val url = URL(webserviceUrl + HttpUtils.sendVoltUrl)
        var sendSuccessfully = false
        val databaseVoltwatcher = DatabaseVoltwatcher(context)
        databaseVoltwatcher.open()
        databaseVoltwatcher.deleteOldSent()
        val cursor = databaseVoltwatcher.fetchRowsNotSent()
        val cursorCount = cursor.count
        Log.d(TAG, "Found $cursorCount rows to send")
        return if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to " + webserviceUrl)
            try {
                while (cursor.moveToNext()) {
                    // 1. create HttpURLConnection
                    var token = HttpUtils.getWebserviceToken(context, webserviceUrl, webserviceUsername, webservicePassword)
                    val device = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DEVICE))
                    val data = cursor.getString(cursor.getColumnIndex("grData"))
                    val volt = cursor.getString(cursor.getColumnIndex("volts"))
                    val temp = cursor.getString(cursor.getColumnIndex("temps"))
                    val detectorBattery = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DETECTOR_BATTERY))
                    val longitude = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LONGITUDE))
                    val latitude = cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LATITUDE))
                    // 2. build JSON object
                    val jsonObject = buidJsonObject(device, "$data:00", volt, temp, detectorBattery, longitude, latitude)
                    Log.d(TAG, "Build sending data=$jsonObject")
                    when (sendData(url, token, jsonObject)) {
                        WebserviceResponse.ERROR -> {
                        }
                        WebserviceResponse.OK -> {
                            databaseVoltwatcher.updateSent(device, data)
                            Log.d(TAG, "Updated record sent")
                            sendSuccessfully = true
                        }
                        WebserviceResponse.TOKEN_EXPIRED -> {
                            token = HttpUtils.getNewWebserviceToken(context, webserviceUrl, webserviceUsername, webservicePassword)
                            if (sendData(url, token, jsonObject) == WebserviceResponse.OK) {
                                databaseVoltwatcher.updateSent(device, data)
                                Log.d(TAG, "Updated record sent")
                                sendSuccessfully = true
                            }
                        }
                        else -> sendSuccessfully = false
                    }
                }
                if (cursorCount > 0 && sendSuccessfully) {
                    Log.d(TAG, "Data sent")
                    val intentWs = Intent(WebserviceEvents.DEBUG)
                    intentWs.putExtra("message", "$cursorCount rows sent")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
                }
            } catch (e: Exception) {
                val intentWs = Intent(WebserviceEvents.ERROR)
                intentWs.putExtra("message", "Unable to send data to " + webserviceUrl + " : " + e.message)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
            } finally {
                cursor.close()
                databaseVoltwatcher.close()
            }

            //Log.d(TAG, conn.getResponseMessage());
            // 5. return response message
            if (sendSuccessfully) {
                "OK"
            } else {
                "KO"
            }
        } else {
            cursor.close()
            val intentWs = Intent(WebserviceEvents.DEBUG)
            intentWs.putExtra("message", "No data found to send")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentWs)
            "No data found to send"
        }
    }

    @Throws(JSONException::class)
    private fun buidJsonObject(device: String, data: String, volt: String, temp: String, detectorbattery: String, longitude: String, latitude: String): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("device", device)
        jsonObject.put("data", data)
        jsonObject.put("volt", volt)
        jsonObject.put("temp", temp)
        jsonObject.put("batteryperc", detectorbattery)
        jsonObject.put("longitude", longitude)
        jsonObject.put("latitude", latitude)
        return jsonObject
    }

    internal inner class HTTPAsyncTask : AsyncTask<String?, Void?, String>() {
        override fun doInBackground(vararg params: String?): String? {
            // params comes from the execute() call: params[0] is the url.
            return try {
                httpWebserviceSendData()
            } catch (e: IOException) {
                //e.printStackTrace();
                Log.e(TAG, Objects.requireNonNull(e.message.toString()))
                val intent = Intent(BluetoothEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                "Unable to retrieve web page: " + e.message
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        override fun onPostExecute(result: String) {
            Log.d(TAG, "Webservice response: $result")
        }
    }

    companion object {
        const val TAG = "WebserviceSender"
    }
}