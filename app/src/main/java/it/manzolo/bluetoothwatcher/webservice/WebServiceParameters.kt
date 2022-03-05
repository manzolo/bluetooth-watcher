package it.manzolo.bluetoothwatcher.webservice

class WebServiceParameters(
    private var webServiceUrl: String,
    private var webServiceUsername: String,
    private var webServicePassword: String
) {

    fun getUrl(): String {
        return this.webServiceUrl
    }

    fun getUsername(): String {
        return this.webServiceUsername
    }

    fun getPassword(): String {
        return this.webServicePassword
    }
}