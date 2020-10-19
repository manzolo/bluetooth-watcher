package it.manzolo.bluetoothwatcher.network

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import it.manzolo.bluetoothwatcher.enums.MainEvents
import it.manzolo.bluetoothwatcher.enums.WebserviceEvents
import it.manzolo.bluetoothwatcher.utils.Network
import it.manzolo.bluetoothwatcher.utils.Session

class GithubUpdater {
    fun checkUpdate(context: Context) {
        if (Network().isNetworkAvailable(context)) {
            val session = Session(context)
            if (!session.isAvailableUpdate) {
                val appUpdaterUtils = AppUpdaterUtils(context)
                        .setUpdateFrom(UpdateFrom.GITHUB)
                        .setGitHubUserAndRepo("manzolo", "bluetooth-watcher")
                        .withListener(object : AppUpdaterUtils.UpdateListener {
                            override fun onSuccess(update: Update, isUpdateAvailable: Boolean?) {
                                Log.d("Latest Version", update.latestVersion)
                                Log.d("URL", update.urlToDownload.toString() + "/download/app-release.apk")
                                Log.d("isUpdateAvailable", isUpdateAvailable.toString())

                                if (isUpdateAvailable!!) {
                                    session.isAvailableUpdate = true

                                    val intent = Intent(WebserviceEvents.APP_AVAILABLE)
                                    intent.putExtra("message", update.urlToDownload.toString() + "/download/app-release.apk")
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                                } else {
                                    val intent = Intent(WebserviceEvents.APP_NO_AVAILABLE_UPDATE)
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                                }
                            }

                            override fun onFailed(error: AppUpdaterError) {
                                val intent = Intent(MainEvents.ERROR)
                                intent.putExtra("message", error)
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            }
                        })
                appUpdaterUtils.start()
            }
        } else {
            val intent = Intent(MainEvents.ERROR)
            intent.putExtra("message", "No internet connection")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}