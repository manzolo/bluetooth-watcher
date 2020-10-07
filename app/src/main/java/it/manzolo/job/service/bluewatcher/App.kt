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
            val seconds = preferences.getString("bluetooth_service_every_seconds", "90")
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start bluetooth service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val serviceIntent = Intent(context, BluetoothService::class.java)
            if (seconds != null) {
                cron(context, serviceIntent, seconds)
            }
        }

        fun scheduleWebsendService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("webservice_service_every_seconds", "120")
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start websend service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val serviceIntent = Intent(context, WebsendService::class.java)
            if (seconds != null) {
                cron(context, serviceIntent, seconds)
            }
        }

        fun scheduleLocationService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("location_service_every_seconds", "600")
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start location service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val serviceIntent = Intent(context, LocationService::class.java)
            if (seconds != null) {
                cron(context, serviceIntent, seconds)
            }

        }

        fun scheduleUpdateService(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("update_service_every_seconds", "25200")
            val debug = preferences.getBoolean("debug", false)
            if (debug) {
                Toast.makeText(context, "Start update service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            val serviceIntent = Intent(context, UpdateService::class.java)
            if (seconds != null) {
                cron(context, serviceIntent, seconds)
            }
        }

        fun scheduleRebootService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("restart_app_service_every_seconds", "21600")
            val serviceIntent = Intent(context, RebootService::class.java)
            if (seconds != null) {
                cron(context, serviceIntent, seconds)
            }
        }

        private fun cron(context: Context, intent: Intent, seconds: String) {

            val pendingIntent = PendingIntent.getService(context, 0, intent, 0)
            val time: Calendar = Calendar.getInstance()
            time.timeInMillis = System.currentTimeMillis()
            time.add(Calendar.SECOND, seconds.toInt()) // first time
            val frequency = seconds.toInt() * 1000.toLong() // in ms

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time.timeInMillis, frequency, pendingIntent)


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
    }
}