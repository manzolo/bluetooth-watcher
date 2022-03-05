package it.manzolo.bluetoothwatcher.webservice

class WebServiceParameters(
    private var webserviceUrl: String,
    private var webserviceUsername: String,
    private var webservicePassword: String
) {

    fun getUrl(): String {
        return this.webserviceUrl
    }

    fun getUsername(): String {
        return this.webserviceUsername
    }

    fun getPassword(): String {
        return this.webservicePassword
    }
}