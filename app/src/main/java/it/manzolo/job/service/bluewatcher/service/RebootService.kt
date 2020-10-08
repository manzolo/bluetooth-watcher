package it.manzolo.job.service.bluewatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.jakewharton.processphoenix.ProcessPhoenix


class RebootService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onRebootStartJob")
        starRebootTask()
        return START_NOT_STICKY
    }

    fun starRebootTask() {
        Log.d(TAG, "onRebootStartCommand")
        //val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        //pm.reboot(null)
        //Runtime.getRuntime().exit(0)
        ProcessPhoenix.triggerRebirth(this.applicationContext)
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