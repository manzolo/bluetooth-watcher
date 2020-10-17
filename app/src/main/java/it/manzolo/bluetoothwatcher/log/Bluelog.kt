package it.manzolo.bluetoothwatcher.log

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