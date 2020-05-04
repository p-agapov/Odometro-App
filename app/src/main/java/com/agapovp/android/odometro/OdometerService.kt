package com.agapovp.android.odometro

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat

class OdometerService : Service() {

    private val binder: IBinder = OdometerBinder()

    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()

        distanceInMeters = getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE).getFloat(
            SAVED_DISTANCE_METERS_KEY, 0f
        ).toDouble()

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (lastLocation == null) {
                    lastLocation = location
                }
                distanceInMeters += location.distanceTo(lastLocation)
                lastLocation = location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String?) = Unit

            override fun onProviderDisabled(provider: String?) = Unit
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(this, PERMISSION_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getBestProvider(Criteria(), true)?.let {
                locationManager.requestLocationUpdates(it, 1000, 1f, locationListener)
            }
        }
    }

    override fun onBind(intent: Intent) = binder

    override fun onDestroy() {
        super.onDestroy()

        if (ContextCompat.checkSelfPermission(this, PERMISSION_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.removeUpdates(locationListener)
        }

        with(getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE).edit()) {
            putFloat(SAVED_DISTANCE_METERS_KEY, distanceInMeters.toFloat())
            apply()
        }
    }

    fun getDistance() = distanceInMeters / resources.getFloat(R.dimen.divider)

    inner class OdometerBinder : Binder() {
        fun getOdometer() = this@OdometerService
    }

    companion object {
        const val PERMISSION_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION

        private const val PREFERENCE_FILE_KEY =
            "com.agapovp.android.odometro.odometer_service_preference_file_key"
        private const val SAVED_DISTANCE_METERS_KEY = "saved_distance_meters_key"

        private var lastLocation: Location? = null
        private var distanceInMeters = 0.0
    }
}
