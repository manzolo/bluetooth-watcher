package it.manzolo.job.service.bluewatcher

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.preference.PreferenceManager
import it.manzolo.job.service.bluewatcher.service.*
import java.util.*


class App : Application() {
    companion object {

        fun scheduleWatcherService(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = "60"
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start main service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val myIntent = Intent(context, MainService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, seconds.toInt()) // first time

            val frequency = seconds.toInt() * 1000.toLong() // in ms

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, frequency, pendingIntent)
            /*val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val componentName = ComponentName(context, MainService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = "60"
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(1, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds.toLong() / 2))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
            //.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            jobScheduler.schedule(jobInfo.build())*/

        }

        fun scheduleUpdateService(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = "25200" //25200
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start update service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val myIntent = Intent(context, UpdateService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, seconds.toInt()) // first time

            val frequency = seconds.toInt() * 1000.toLong() // in ms

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, frequency, pendingIntent)

            /*val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, UpdateService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = "25200"
            if (debug) {
                Toast.makeText(context, "Start update service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(2, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds.toLong() / 2))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            jobScheduler.schedule(jobInfo.build())*/
        }

        fun scheduleWebsendService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("seconds", "60")
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start websend service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val myIntent = Intent(context, WebsendService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, seconds!!.toInt()) // first time

            val frequency = seconds.toInt() * 1000.toLong() // in ms

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, frequency, pendingIntent)

            /*val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, WebsendService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = preferences.getString("seconds", "300")
            if (debug) {
                Toast.makeText(context, "Start websend service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(3, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds!!.toLong() / 2))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            jobScheduler.schedule(jobInfo.build())*/
        }


        fun scheduleLocationService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = "600" //600
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start location service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val myIntent = Intent(context, LocationService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, seconds.toInt()) // first time

            val frequency = seconds.toInt() * 1000.toLong() // in ms

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, frequency, pendingIntent)
            /*
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, LocationService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = preferences.getString("seconds", "600")
            if (debug) {
                Toast.makeText(context, "Start location service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(4, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds!!.toLong() / 2))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
            //.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            jobScheduler.schedule(jobInfo.build())*/
        }

        fun scheduleRebootService(context: Context) {

            val seconds = "3600"
            val myIntent = Intent(context, RebootService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, myIntent, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, seconds.toInt()) // first time

            val frequency = seconds.toInt() * 1000.toLong() // in ms

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, frequency, pendingIntent)
            /*
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, LocationService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val seconds = preferences.getString("seconds", "600")
            if (debug) {
                Toast.makeText(context, "Start location service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val jobInfo = JobInfo.Builder(4, componentName)
                    .setMinimumLatency(TimeUnit.SECONDS.toMillis(seconds!!.toLong() / 2))
                    .setOverrideDeadline(TimeUnit.SECONDS.toMillis(seconds.toLong()))
            //.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            jobScheduler.schedule(jobInfo.build())*/
        }

    }

}