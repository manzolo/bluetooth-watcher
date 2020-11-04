package it.manzolo.bluetoothwatcher.device

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.utils.Date
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class DebugData {
    fun insertDebugData(context: Context?) {
// Dummy reading volt for emulator
        val dummyTemperatureC = "30"
        val dummyTemperatureF = "100"
        val dummyAmpere = "1"
        val dummyDate = Date.now()//getDummyDate()


        for (x in 0..6) {
            val dummyDevice = "00:00:00:00:00:0$x"
            val dummyVolt = getNextRandomDummyVolt()
            val intentBt = Intent(BluetoothEvents.DATA_RETRIEVED)

            intentBt.putExtra("device", dummyDevice)
            intentBt.putExtra("volt", dummyVolt)
            intentBt.putExtra("data", dummyDate)
            intentBt.putExtra("tempC", dummyTemperatureC)
            intentBt.putExtra("tempF", dummyTemperatureF)
            intentBt.putExtra("amp", dummyAmpere)

            intentBt.putExtra("message", dummyDevice + " " + dummyVolt + "v " + dummyTemperatureC + "°")
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intentBt)

            val intentBluetoothCloseConnection = Intent(BluetoothEvents.CLOSECONNECTION)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBluetoothCloseConnection)
        }
    }

    private fun getNextRandomDummyVolt(): String {
        val min = 11.60
        val max = 13.01
        val df = DecimalFormat("#.##")
        val random: Double = (min + Random().nextDouble() * (max - min))
        df.roundingMode = RoundingMode.CEILING
        return df.format(random).toString()
    }
    /*private fun getDummyDate(): String {
        val ONE_MINUTE_IN_MILLIS: Long = 60000 //millisecs

        val date = Calendar.getInstance()
        val t = date.timeInMillis
        val afterAddingTenMins = Date(t - 10 * ONE_MINUTE_IN_MILLIS)

        val dateFormat: DateFormat
        dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALIAN)
        return dateFormat.format(afterAddingTenMins)


    }*/
}