package it.manzolo.bluetoothwatcher.enums

interface MainEvents {
    companion object {
        const val BROADCAST = "BROADCAST"
        const val ERROR = "E"
        const val INFO = "I"
        const val WARNING = "W"
        const val DEBUG = "D"
    }
}