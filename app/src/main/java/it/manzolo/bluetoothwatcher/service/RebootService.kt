package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import com.jakewharton.processphoenix.ProcessPhoenix
import it.manzolo.bluetoothwatcher.database.DatabaseLog
import it.manzolo.bluetoothwatcher.log.Bluelog
import it.manzolo.bluetoothwatcher.utils.Date


class RebootService : Service() {

    companion object {
        val TAG: String = RebootService::class.java.simpleName

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onRebootStartJob")
        starRebootTask()
        return START_NOT_STICKY
    }

    fun starRebootTask() {
        Log.d(TAG, "onRebootStartCommand")
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val autoAppRestart = preferences.getBoolean("auto_app_restart", false)
        if (autoAppRestart) {
            val dbLog = DatabaseLog(this.applicationContext)
            dbLog.open()
            dbLog.createRow(Date.now(), "Restart App", Bluelog.logEvents.INFO)
            dbLog.close()
            //Only for system app
            //val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            //pm.reboot(null)
            ProcessPhoenix.triggerRebirth(this.applicationContext)
        } else {
            Log.w(TAG, "auto_app_restart set to false")
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}