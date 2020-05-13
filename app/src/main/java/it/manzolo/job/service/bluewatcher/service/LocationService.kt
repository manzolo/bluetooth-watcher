package it.manzolo.job.service.bluewatcher.service

import android.Manifest
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import it.manzolo.job.service.bluewatcher.utils.Session

class LocationService : JobService() {
    var mLocationListeners = arrayOf(
            LocationListener(LocationManager.GPS_PROVIDER)
    )
    private var mLocationManager: LocationManager? = null

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        Log.d(TAG, "onLocationStartJob : " + jobParameters.toString())
        startWebsendTask()
        //App.scheduleWebsendService(this)
        return true
    }

    fun startWebsendTask() {
        Log.d(TAG, "onStartCommand")
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.d(WebsendService.TAG, "onLocationStopJob")
        return true
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        initializeLocationManager()
        try {
            mLocationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL.toLong(),
                    LOCATION_DISTANCE,
                    mLocationListeners[0]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, " + ex.message)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        if (mLocationManager != null) {
            for (i in mLocationListeners.indices) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                    mLocationManager!!.removeUpdates(mLocationListeners[i])
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex)
                }
            }
        }
    }

    private fun initializeLocationManager() {
        Log.d(TAG, "initializeLocationManager - LOCATION_INTERVAL: $LOCATION_INTERVAL LOCATION_DISTANCE: $LOCATION_DISTANCE")
        if (mLocationManager == null) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    inner class LocationListener(provider: String) : android.location.LocationListener {
        var mLastLocation: Location
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "onLocationChanged: $location")
            mLastLocation.set(location)
            val session = Session(applicationContext)

            //GPS
            Log.d(TAG, location.longitude.toString())
            Log.d(TAG, location.latitude.toString())
            session.setlongitude(location.longitude.toString())
            session.setlatitude(location.latitude.toString())

        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d(TAG, "onStatusChanged: $provider")
        }

        init {
            Log.d(TAG, "LocationListener $provider")
            mLastLocation = Location(provider)
        }
    }

    companion object {
        val TAG: String = LocationService::class.java.simpleName
        private const val LOCATION_INTERVAL = 1000
        private const val LOCATION_DISTANCE = 10f
    }
}