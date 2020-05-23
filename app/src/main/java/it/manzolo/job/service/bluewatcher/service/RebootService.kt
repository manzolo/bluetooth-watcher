package it.manzolo.job.service.bluewatcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log


class RebootService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onLocationStartJob")
        starRebootTask()
        return START_NOT_STICKY
    }

    fun starRebootTask() {
        Log.d(TAG, "onStartCommand")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.reboot(null)
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

    }


    companion object {
        val TAG: String = RebootService::class.java.simpleName

    }
}