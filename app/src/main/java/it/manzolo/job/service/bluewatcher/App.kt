package it.manzolo.job.service.bluewatcher

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.widget.Toast
import java.util.concurrent.TimeUnit


class App : Application() {

    companion object {

        fun scheduleJobService(context: Context) {

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val componentName = ComponentName(context, MainService::class.java)

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("seconds", "15") //"" is the default String to return if the preference isn't found
            val enabled = preferences.getBoolean("enabled", true) //"" is the default String to return if the preference isn't found
            val address = preferences.getString("devices", "")
            if (address.replace("\\s".toRegex(), "").length === 0) {
                Toast.makeText(context, "No devices in preferences", Toast.LENGTH_LONG).show()
            } else {
                //Log.i("Manzolo",seconds);
                if (enabled) {
                    Toast.makeText(context, "Start service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()
                    val jobInfo = JobInfo.Builder(1, componentName)
                            .setMinimumLatency(TimeUnit.SECONDS.toMillis(60))
                            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong() * 1000))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPersisted(true)
                            .build()

                    jobScheduler.schedule(jobInfo)
                } else {
                    Toast.makeText(context, "Service disabled in preferences", Toast.LENGTH_LONG).show()
                }
            }

        }
    }
}