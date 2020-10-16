package it.manzolo.bluetoothwatcher.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import java.io.File

class GithubUpdater {
    fun checkUpdate(context: Context) {
        val fileupdate = File(context.cacheDir, "app.ava")
        if (!fileupdate.exists()) {
            val appUpdaterUtils = AppUpdaterUtils(context)
                    .setUpdateFrom(UpdateFrom.GITHUB)
                    .setGitHubUserAndRepo("manzolo", "bluetooth-watcher")
                    .withListener(object : AppUpdaterUtils.UpdateListener {
                        override fun onSuccess(update: Update, isUpdateAvailable: Boolean?) {
                            Log.d("Latest Version", update.latestVersion)
                            Log.d("URL", update.urlToDownload.toString() + "/download/app-release.apk")
                            Log.d("Ava", isUpdateAvailable.toString())

                            if (isUpdateAvailable!!) {
                                val updateFile = File(context.cacheDir, "app.ava")
                                updateFile.createNewFile()

                                val intent = Intent(WebserviceEvents.APP_AVAILABLE)
                                intent.putExtra("message", update.urlToDownload.toString() + "/download/app-release.apk")
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            } else {
                                val intent = Intent(WebserviceEvents.APP_NO_AVAILABLE_UPDATE)
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
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