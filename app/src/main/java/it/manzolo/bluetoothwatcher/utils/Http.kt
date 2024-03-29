package it.manzolo.bluetoothwatcher.utils

import android.content.Context
import android.util.Log
import it.manzolo.bluetoothwatcher.webservice.WebServiceParameters
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class Http {
    @Throws(IOException::class)
    fun setPostRequestContent(conn: HttpURLConnection, jsonObject: JSONObject) {
        val os = conn.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
        writer.write(jsonObject.toString())
        writer.flush()
        writer.close()
        os.close()
    }

    fun streamToString(`is`: InputStream): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }

    companion object {
        private const val loginUrl = "/api/login_check"
        const val sendVoltUrl = "/api/volt/record.json"
        const val getSettingsUrl = "/api/get/settings/app.json"
        const val connectionTimeout = 15000

        @Throws(Exception::class)
        fun getNewWebserviceToken(
            context: Context,
            webserviceparameter: WebServiceParameters
        ): String {
            val sessionPreferences = Session(context)
            val token: String
            val loginConn = loginWebservice(
                webserviceparameter
            )
            val responseCode = loginConn.responseCode
            val responseMessage = loginConn.responseMessage
            if (responseCode in 200..399) {
                val tokenObject = JSONObject(Http().streamToString(loginConn.inputStream))
                token = tokenObject.getString("token")
                Log.d("TOKEN", token)
                sessionPreferences.webserviceToken = token
            } else {
                Log.e("TOKEN", "Unable to retrieve token")
                throw Exception("$responseCode $responseMessage, unable to retrieve token from ${webserviceparameter.getUrl()}")
            }
            return token
        }

        @Throws(IOException::class, JSONException::class)
        fun loginWebservice(webserviceparameter: WebServiceParameters): HttpURLConnection {
            val loginUrl = URL(webserviceparameter.getUrl() + loginUrl)
            val loginConn = loginUrl.openConnection() as HttpURLConnection
            loginConn.useCaches = false
            loginConn.allowUserInteraction = false
            loginConn.connectTimeout = connectionTimeout
            loginConn.readTimeout = connectionTimeout
            loginConn.requestMethod = "POST"
            loginConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val jsonObject = JSONObject()
            jsonObject.put("username", webserviceparameter.getUsername())
            jsonObject.put("password", webserviceparameter.getPassword())
            // 3. add JSON content to POST request body
            Http().setPostRequestContent(loginConn, jsonObject)
            // 4. make POST request to the given URL
            loginConn.connect()
            return loginConn
        }

        @Throws(Exception::class)
        fun getWebserviceToken(
            context: Context,
            webserviceparameter: WebServiceParameters
        ): String {
            val sessionPreferences = Session(context)
            var lastToken = sessionPreferences.webserviceToken!!
            if (lastToken.isEmpty()) {
                Log.d("TOKEN", "Try to get new token")
                lastToken = getNewWebserviceToken(context, webserviceparameter)
            } else {
                Log.d("TOKEN", "Used last token")
            }
            return lastToken
        }
    }
}