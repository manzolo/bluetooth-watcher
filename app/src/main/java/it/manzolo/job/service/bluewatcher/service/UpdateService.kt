package it.manzolo.job.service.bluewatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.bluewatcher.utils.GithubUpdater
import it.manzolo.job.service.enums.WebserverEvents

class UpdateService : Service() {
    companion object {
        val TAG: String = UpdateService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onUpdateCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onUpdateStartJob")
        startUpdateTask()
        //App.scheduleUpdateService(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onUpdateDestroy")
    }

    private fun startUpdateTask() {

        Log.d(TAG, "checkForUpdate")
        val intent = Intent(WebserverEvents.APP_CHECK_UPDATE)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        val githubup = GithubUpdater()
        githubup.checkUpdate(applicationContext)
    }
}
