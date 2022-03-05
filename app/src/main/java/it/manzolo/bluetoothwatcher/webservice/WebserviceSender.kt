package it.manzolo.bluetoothwatcher.webservice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import it.manzolo.bluetoothwatcher.database.DatabaseVoltwatcher
import it.manzolo.bluetoothwatcher.enums.MainEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceResponse
import it.manzolo.bluetoothwatcher.utils.Http
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.coroutines.CoroutineContext


class WebserviceSender(
    private val context: Context,
    private val webServiceParameter: WebServiceParameters,

    ) : CoroutineScope {
    companion object {
        val TAG: String = WebserviceSender::class.java.simpleName

    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    // call this method to cancel a coroutine when you don't need it anymore,
    // e.g. when user closes the screen
    /*fun cancel() {
        job.cancel()
    }*/

    fun execute() = launch {
        //onPreExecute()
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        Log.d(TAG, "Webserver response: $result")
        //onPostExecute(result)
    }

    private suspend fun doInBackground() =
        withContext(Dispatchers.IO) { // to run code in Background Thread
            return@withContext try {
                httpWebserviceSendData(
                    context,
                    webServiceParameter
                )
            } catch (e: IOException) {
                Log.e(TAG, Objects.requireNonNull(e.message.toString()))
                val intent = Intent(MainEvents.ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message)
                context.sendBroadcast(intent)
                "Unable to retrieve web page: " + e.message
            }
        }

    @SuppressLint("Range")
    //@Throws(IOException::class)
    fun httpWebserviceSendData(
        context: Context,
        webServiceParameter: WebServiceParameters
    ): String {
        val url = URL(webServiceParameter.getUrl() + Http.sendVoltUrl)
        var sendSuccessfully = false
        val databaseVoltwatcher = DatabaseVoltwatcher(context)
        databaseVoltwatcher.open()
        databaseVoltwatcher.deleteOldSent()
        val cursor = databaseVoltwatcher.fetchRowsNotSent()
        val cursorCount = cursor.count
        Log.d(TAG, "Found $cursorCount rows to send")
        return if (cursorCount > 0) {
            Log.d(TAG, "Try connecting to ${webServiceParameter.getUrl()}")
            try {
                while (cursor.moveToNext()) {
                    // create HttpURLConnection
                    var token = Http.getWebserviceToken(
                        context,
                        webServiceParameter
                    )
                    val device =
                        cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DEVICE))
                    val data = cursor.getString(cursor.getColumnIndex("grData"))
                    val volt = cursor.getString(cursor.getColumnIndex("volts"))
                    val temp = cursor.getString(cursor.getColumnIndex("temps"))
                    val detectorBattery =
                        cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_DETECTOR_BATTERY))
                    val longitude =
                        cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LONGITUDE))
                    val latitude =
                        cursor.getString(cursor.getColumnIndex(DatabaseVoltwatcher.KEY_LATITUDE))
                    // build JSON object
                    val jsonObject = buildJsonObject(
                        device,
                        "$data:00",
                        volt,
                        temp,
                        detectorBattery,
                        longitude,
                        latitude
                    )
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
                            token = Http.getNewWebserviceToken(
                                context,
                                webServiceParameter
                            )
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
                    context.sendBroadcast(intentWs)
                }
            } catch (e: Exception) {
                val intentWs = Intent(WebserviceEvents.ERROR)
                intentWs.putExtra(
                    "message",
                    "Unable to send data to " + webServiceParameter.getUrl() + " : " + e.message
                )
                context.sendBroadcast(intentWs)
            } finally {
                cursor.close()
                databaseVoltwatcher.close()
            }

            if (sendSuccessfully) {
                "OK"
            } else {
                "KO"
            }
        } else {
            cursor.close()
            databaseVoltwatcher.close()
            val intentWs = Intent(MainEvents.DEBUG)
            intentWs.putExtra("message", "No data found to send")
            context.sendBroadcast(intentWs)
            "No data found to send"
        }
    }

    @Throws(JSONException::class)
    private fun buildJsonObject(
        device: String,
        data: String,
        volt: String,
        temp: String,
        detectorBattery: String,
        longitude: String,
        latitude: String
    ): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("device", device)
        jsonObject.put("data", data)
        jsonObject.put("volt", volt)
        jsonObject.put("temp", temp)
        jsonObject.put("batteryperc", detectorBattery)
        jsonObject.put("longitude", longitude)
        jsonObject.put("latitude", latitude)
        return jsonObject
    }

    @Throws(IOException::class, JSONException::class)
    private fun sendData(url: URL, token: String, jsonObject: JSONObject): String {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.useCaches = false
        conn.allowUserInteraction = false
        conn.connectTimeout = Http.connectionTimeout
        conn.readTimeout = Http.connectionTimeout
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer $token")

        // add JSON content to POST request body
        Http().setPostRequestContent(conn, jsonObject)

        // make POST request to the given URL
        conn.connect()
        return if (conn.responseCode in 200..399) {
            val jsonResponseObject = JSONObject(Http().streamToString(conn.inputStream))
            if (jsonResponseObject["errcode"] == 0) {
                val intentWebserviceSent = Intent(WebserviceEvents.INFO)
                intentWebserviceSent.putExtra("message", jsonObject.toString())
                context.sendBroadcast(intentWebserviceSent)
                WebserviceResponse.OK
            } else {
                val intentWs = Intent(WebserviceEvents.ERROR)
                intentWs.putExtra(
                    "message",
                    jsonResponseObject["errcode"].toString() + " " + jsonResponseObject["message"].toString()
                )
                context.sendBroadcast(intentWs)
                WebserviceResponse.ERROR
            }
        } else if (conn.responseCode == 401) {
            WebserviceResponse.TOKEN_EXPIRED
        } else {
            val intentWs = Intent(WebserviceEvents.ERROR)
            intentWs.putExtra("message", conn.responseCode.toString() + " " + conn.responseMessage)
            context.sendBroadcast(intentWs)
            WebserviceResponse.ERROR
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

