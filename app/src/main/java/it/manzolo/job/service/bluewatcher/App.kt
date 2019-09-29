package it.manzolo.job.service.bluewatcher

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * @author Andrea Manzi manzolo@libero.it
 * @since Sep, Sun 29 2019 23.48
 **/
class App : Application() {

    companion object {

        fun scheduleJobService(context: Context) {

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val componentName = ComponentName(context, MainJobService::class.java)

            val jobInfo = JobInfo.Builder(1, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(60))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(3600)) //30
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()

            jobScheduler.schedule(jobInfo)
        }
    }
}