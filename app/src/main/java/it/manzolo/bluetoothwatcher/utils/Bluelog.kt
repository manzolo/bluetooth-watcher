package it.manzolo.bluetoothwatcher.utils

class Bluelog(val data: String, val message: String, val type: String) {

    interface logEvents {
        companion object {
            const val DEBUG = "D"
            const val ERROR = "E"
            const val WARNING = "W"
            const val INFO = "I"
            const val BROADCAST = "BROADCASTMESSAGE"
        }
    }
}