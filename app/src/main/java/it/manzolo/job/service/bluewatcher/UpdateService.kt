package it.manzolo.job.service.bluewatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import it.manzolo.job.service.enums.WebserverEvents
import java.io.File


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

        val fileupdate = File(applicationContext.cacheDir, "app.ava")
        if (!fileupdate.exists()) {
            val appUpdaterUtils = AppUpdaterUtils(applicationContext)
                    .setUpdateFrom(UpdateFrom.GITHUB)
                    .setGitHubUserAndRepo("manzolo", "bluetooth-watcher")
                    .withListener(object : AppUpdaterUtils.UpdateListener {
                        override fun onSuccess(update: Update, isUpdateAvailable: Boolean?) {
                            Log.d("Latest Version", update.latestVersion)
                            Log.d("URL", update.urlToDownload.toString() + "/download/app-release.apk")
                            Log.d("Ava", isUpdateAvailable.toString())

                            if (isUpdateAvailable!!) {
                                val fileupdate = File(applicationContext.cacheDir, "app.ava")
                                fileupdate.createNewFile()

                                val intent = Intent(WebserverEvents.APP_AVAILABLE)
                                intent.putExtra("message", update.urlToDownload.toString() + "/download/app-release.apk")
                                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                            }
                        }

                        override fun onFailed(error: AppUpdaterError) {
                            //Log.d("AppUpdater Error", "Something went wrong")
                        }
                    })
            appUpdaterUtils.start()
        }


    }
}
