package it.manzolo.job.service.bluewatcher.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.bluewatcher.App
import it.manzolo.job.service.bluewatcher.utils.GithubUpdater
import it.manzolo.job.service.enums.WebserverEvents

class UpdateService : JobService() {
    companion object {
        val TAG: String = UpdateService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onUpdateCreate")
    }

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        Log.d(TAG, "onUpdateStartJob : " + jobParameters.toString())
        startUpdateTask()
        App.scheduleUpdateService(this)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.d(TAG, "onUpdateStopJob")
        return true
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
