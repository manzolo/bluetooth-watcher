package it.manzolo.job.service.bluewatcher

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import java.util.concurrent.TimeUnit


class AppUpdate : Application() {
    companion object {

        fun scheduleUpdateService(context: Context) {
            val jobUpdateScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, UpdateService::class.java)
            val seconds = "14400"
            //val seconds = "60"
            Toast.makeText(context, "Start update service every " + seconds + " seconds", Toast.LENGTH_SHORT).show()

            val jobInfo = JobInfo.Builder(1, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
            jobUpdateScheduler.schedule(jobInfo)
        }
    }
}