package it.manzolo.bluetoothwatcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import it.manzolo.bluetoothwatcher.enums.MainEvents
import it.manzolo.bluetoothwatcher.service.*
import it.manzolo.bluetoothwatcher.utils.HandlerList

class App : Application() {
    companion object {
        fun getHandlers(): ArrayList<HandlerList> {
            return handlers
        }

        private val handlers: ArrayList<HandlerList> = ArrayList()
        fun scheduleSentinelService(context: Context) {
            val intent = Intent(MainEvents.BROADCAST)
            intent.putExtra("message", "Start sentinel service every 60 seconds")
            intent.putExtra("type", MainEvents.INFO)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            cron(context, SentinelService::class.java, "60")
        }

        fun scheduleBluetoothService(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("bluetoothServiceEverySeconds", "90")
            val debug = preferences.getBoolean("debugApp", false)
            if (debug) {
                Toast.makeText(context, "Start bluetooth service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            if (seconds != null) {
                val intent = Intent(MainEvents.BROADCAST)
                intent.putExtra("message", "Start bluetooth service every $seconds seconds")
                intent.putExtra("type", MainEvents.INFO)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                cron(context, BluetoothService::class.java, seconds)
            }
        }

        fun scheduleWebserviceSendService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("webserviceServiceEverySeconds", "120")
            val debug = preferences.getBoolean("debugApp", false)
            if (debug) {
                Toast.makeText(context, "Start websend service every $seconds seconds", Toast.LENGTH_SHORT).show()
            }

            if (seconds != null) {
                val intent = Intent(MainEvents.BROADCAST)
                intent.putExtra("message", "Start webserviceSend service every $seconds seconds")
                intent.putExtra("type", MainEvents.INFO)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                cron(context, WebserviceSendService::class.java, seconds)
            }
        }

        fun scheduleLocationService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("locationServiceEverySeconds", "600")

            if (seconds != null) {
                val intent = Intent(MainEvents.BROADCAST)
                intent.putExtra("message", "Start location service every $seconds seconds")
                intent.putExtra("type", MainEvents.INFO)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                cron(context, LocationService::class.java, seconds)
            }

        }

        fun scheduleUpdateService(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("updateServiceEverySeconds", "25200")

            if (seconds != null) {
                val intent = Intent(MainEvents.BROADCAST)
                intent.putExtra("message", "Start checking for update app service every $seconds seconds")
                intent.putExtra("type", MainEvents.INFO)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                cron(context, UpdateService::class.java, seconds)
            }
        }

        fun scheduleRestartAppService(context: Context) {

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seconds = preferences.getString("restartAppServiceEverySeconds", "43200")

            if (seconds != null) {
                val intent = Intent(MainEvents.BROADCAST)
                intent.putExtra("message", "Start restart app service every $seconds seconds")
                intent.putExtra("type", MainEvents.INFO)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                cron(context, RestartAppService::class.java, seconds)
            }
        }

        private fun cron(context: Context, serviceClass: Class<*>, seconds: String) {
            val handler = Handler()
            val frequency = seconds.toInt() * 1000.toLong() // in ms
            val runnable = object : Runnable {
                override fun run() {
                    val intent = Intent(context, serviceClass)
                    context.startService(intent)
                    handler.postDelayed(this, frequency) //now is every 2 minutes
                }
            }
            handler.postDelayed(runnable, frequency) //Every 120000 ms (2 minutes)
            handlers.add(0, HandlerList(serviceClass, handler, runnable, frequency))


            /*val serviceIntent = Intent(context, serviceClass)
            val pendingIntent = PendingIntent.getService(context, 0, serviceIntent, 0)
            val time: Calendar = Calendar.getInstance()
            time.timeInMillis = System.currentTimeMillis()
            time.add(Calendar.SECOND, seconds.toInt()) // first time
            val frequency = seconds.toInt() * 1000.toLong() // in ms

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time.timeInMillis, frequency, pendingIntent)*/


            /*val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, WebsendService::class.java)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debugApp", false)
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

        fun findHandler(
                name: Class<*>, handlers: List<HandlerList>): HandlerList? {
            val iterator: Iterator<HandlerList> = handlers.iterator()
            while (iterator.hasNext()) {
                val currentHandler: HandlerList = iterator.next()
                if (currentHandler.classname == name) {
                    return currentHandler
                }
            }
            return null
        }
    }
}