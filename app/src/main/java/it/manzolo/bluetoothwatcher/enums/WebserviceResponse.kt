package it.manzolo.bluetoothwatcher.enums

interface WebserviceResponse {
    companion object {
        const val ERROR = "ERROR"
        const val OK = "OK"
        const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    }
}