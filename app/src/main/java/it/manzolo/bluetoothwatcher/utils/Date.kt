package it.manzolo.bluetoothwatcher.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object Date {
    @JvmStatic
    fun now(): String {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat
        dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALIAN)
        return dateFormat.format(date)
    }
}