package it.manzolo.job.service.bluewatcher

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class MainService : JobService() {

    companion object {
        val TAG: String = MainService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")
    }

    override fun onStartJob(jobParameters: JobParameters?): Boolean {

        Log.d(TAG, "onStartJob : " + jobParameters.toString())

        startWatcherTask()

        App.scheduleJobService(this)

        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {

        Log.d(TAG, "onStopJob")

        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy")
    }

    private fun startWatcherTask() {
        //Log.d(TAG, "startWatcherTask")
        try {

            val btclient1 = Btclient(this.applicationContext, "44:44:09:04:01:CC")
            if (btclient1.openBT()) {
                btclient1.getData()
                Thread.sleep(2000)
                btclient1.closeBT()
            }

            Thread.sleep(3000)

            val btclient2 = Btclient(this.applicationContext, "34:43:0B:07:0F:58")
            if (btclient2.openBT()) {
                btclient2.getData()
                Thread.sleep(2000)
                btclient2.closeBT()
            }


        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}