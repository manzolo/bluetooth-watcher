package it.manzolo.job.service.bluewatcher

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import it.manzolo.job.service.bluewatcher.service.LocationService
import it.manzolo.job.service.bluewatcher.service.MainService
import it.manzolo.job.service.bluewatcher.service.UpdateService
import it.manzolo.job.service.bluewatcher.service.WebsendService
import java.util.concurrent.TimeUnit


class App : Application() {
    companion object {

        fun scheduleWatcherService(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val componentName = ComponentName(context, MainService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = "60"
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(1, componentName)
                            .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPersisted(true)
                            .build()

                    jobScheduler.schedule(jobInfo)

        }

        fun scheduleUpdateService(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, UpdateService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = "25200"
            if (debug) {
                Toast.makeText(context, "Start update service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(2, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
            jobScheduler.schedule(jobInfo)
        }

        fun scheduleWebsendService(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, WebsendService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = preferences.getString("seconds", "300")
            if (debug) {
                Toast.makeText(context, "Start websend service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(3, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds!!.toLong()))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
            jobScheduler.schedule(jobInfo)
        }


        fun scheduleLocationService(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, LocationService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = preferences.getString("seconds", "600")
            if (debug) {
                Toast.makeText(context, "Start location service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(4, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds!!.toLong()))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
            jobScheduler.schedule(jobInfo)
        }

    }

}