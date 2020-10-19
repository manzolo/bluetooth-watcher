package it.manzolo.bluetoothwatcher.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log

class DatabaseVoltwatcher(private val context: Context) {
    private var database: SQLiteDatabase? = null
    private var dbHelper: DatabaseHelper? = null

    @Throws(SQLException::class)
    fun open(): DatabaseVoltwatcher {
        dbHelper = DatabaseHelper(context)
        database = dbHelper!!.writableDatabase
        return this
    }

    fun close() {
        dbHelper!!.close()
    }

    private fun createContentValues(device: String, volt: String, temp: String, data: String, longitude: String, latitude: String, detectorbattery: String, sent: Int): ContentValues {
        val values = ContentValues()
        values.put(KEY_DEVICE, device)
        values.put(KEY_VOLT, volt)
        values.put(KEY_TEMP, temp)
        values.put(KEY_DATA, data)
        values.put(KEY_LONGITUDE, longitude)
        values.put(KEY_LATITUDE, latitude)
        values.put(KEY_DETECTOR_BATTERY, detectorbattery)
        values.put(KEY_SENT, sent)
        return values
    }

    // create a contact
    fun createRow(device: String, volt: String, temp: String, data: String, longitude: String, latitude: String, detectorbattery: String): Long {
        val sent = 0
        val initialValues = createContentValues(device, volt, temp, data, longitude, latitude, detectorbattery, sent)
        return database!!.insertOrThrow(DATABASE_TABLE, null, initialValues)
    }

    // update a contact
    fun updateRow(id: Long, device: String, volt: String, temp: String, data: String, longitude: String, latitude: String, detectorbattery: String, sent: Int): Boolean {
        val updateValues = createContentValues(device, volt, temp, data, longitude, latitude, detectorbattery, sent)
        ContentValues()
        return database!!.update(DATABASE_TABLE, updateValues, KEY_ID
                + "=" + id, null) > 0
    }

    // update a contact
    fun updateSingleSent(id: Int): Boolean {
        val updateValues = ContentValues()
        updateValues.put(KEY_SENT, 1)
        return database!!.update(DATABASE_TABLE, updateValues, KEY_ID
                + "=" + id, null) > 0
    }

    // update
    fun updateSent(device: String, data: String): Boolean {
        val updateQuery = "update voltwatcher " +
                "set sent = 1 " +
                "where substr(data,1,15)||'0' = '" + data + "' and device = '" + device + "'"
        Log.d("TAG", updateQuery)
        val c = database!!.rawQuery(updateQuery, null)
        c.moveToFirst()
        c.close()
        return true
    }

    // delete
    fun deleteSent(): Boolean {
        val deleteQuery = "delete from voltwatcher where sent = 1"
        Log.d("TAG", deleteQuery)
        val c = database!!.rawQuery(deleteQuery, null)
        c.moveToFirst()
        c.close()
        return true
    }

    // delete
    fun deleteOldSent(): Boolean {
        val deleteQuery = "delete from voltwatcher where sent = 1 and data < datetime('now','-1 day','localtime')"
        Log.d("TAG", deleteQuery)
        val c = database!!.rawQuery(deleteQuery, null)
        c.moveToFirst()
        c.close()
        return true
    }

    // delete a contact
    fun deleteRow(id: Long): Boolean {
        return database!!.delete(DATABASE_TABLE, KEY_ID + "=" + id, null) > 0
    }

    // fetch all rows
    fun fetchAllRows(): Cursor {
        return database!!.query(DATABASE_TABLE, arrayOf(KEY_ID, KEY_DEVICE,
                KEY_VOLT, KEY_TEMP, KEY_DATA, KEY_LONGITUDE, KEY_LATITUDE, KEY_DETECTOR_BATTERY, KEY_SENT), null, null, null, null, null)
    }

    fun fetchRowsNotSent(): Cursor {
        val query = ("select device as device ,substr(data,1,15)||'0' as grData, round(avg(volts),2) as volts, round(avg(temps),2) as temps, round(avg(detectorbattery),2) as detectorbattery, avg(longitude) as longitude, avg(latitude) as latitude "
                + "from voltwatcher "
                + "where sent=0 "
                + "and substr(data,1,15)||'0' <= DATETIME('now', '-10 minutes', 'localtime') "
                + "group by device,grData")
        Log.d("TAG", query)
        return database!!.rawQuery(query, null)
    }

    companion object {
        const val KEY_ID = "_id"
        const val KEY_DEVICE = "device"
        const val KEY_VOLT = "volts"
        const val KEY_TEMP = "temps"
        const val KEY_DATA = "data"
        const val KEY_DETECTOR_BATTERY = "detectorbattery"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_LATITUDE = "latitude"
        const val KEY_SENT = "sent"
        private val LOG_TAG = DatabaseVoltwatcher::class.java.simpleName

        // Database fields
        private const val DATABASE_TABLE = "voltwatcher"
    }
}