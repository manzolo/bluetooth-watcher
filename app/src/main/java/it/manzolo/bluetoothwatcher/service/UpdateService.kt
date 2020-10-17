package it.manzolo.bluetoothwatcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.network.GithubUpdater

class UpdateService : Service() {
    companion object {
        val TAG: String = UpdateService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onUpdateCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val autoAppUpdate = preferences.getBoolean("auto_app_update", false)
        if (autoAppUpdate) {
            Log.d(TAG, "checkForUpdate")
            val intent = Intent(WebserviceEvents.APP_CHECK_UPDATE)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            val githubup = GithubUpdater()
            githubup.checkUpdate(applicationContext)
        }
    }
}
