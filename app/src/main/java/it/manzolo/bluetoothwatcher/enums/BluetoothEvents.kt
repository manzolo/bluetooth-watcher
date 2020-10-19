package it.manzolo.bluetoothwatcher.enums

interface BluetoothEvents {
    companion object {
        const val ERROR = "Connection error"
        const val DATA_RETRIEVED = "Data retrieved"
        const val CLOSECONNECTION = "Close connection"
    }
}