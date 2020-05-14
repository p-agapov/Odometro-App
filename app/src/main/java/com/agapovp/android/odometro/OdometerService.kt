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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class OdometerService : Service() {

    private val binder: IBinder = OdometerBinder()
    private val compositeDisposable = CompositeDisposable()

    private var sharedPreferencesInteractor: SharedPreferencesInteractor? =
        SharedPreferencesInteractor()

    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()

        sharedPreferencesInteractor?.also {
            compositeDisposable.add(
                it.getFromSharedPreference(SAVED_DISTANCE_METERS_KEY, this)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { t -> distanceInMeters = t }
            )
        }

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

        sharedPreferencesInteractor?.also {
            compositeDisposable.add(
                it.saveToSharedPreference(
                    SAVED_DISTANCE_METERS_KEY,
                    distanceInMeters.toFloat(),
                    this
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
            )
        }

        sharedPreferencesInteractor = null
        compositeDisposable.clear()
    }

    fun getDistance() = distanceInMeters / resources.getFloat(R.dimen.divider)

    fun resetDistance() {
        distanceInMeters = 0.0
        sharedPreferencesInteractor?.let {
            compositeDisposable.add(
                it.saveToSharedPreference(SAVED_DISTANCE_METERS_KEY, 0f, this)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
            )
        }
    }

    inner class OdometerBinder : Binder() {
        fun getOdometer() = this@OdometerService
    }

    companion object {
        const val PERMISSION_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION

        private const val SAVED_DISTANCE_METERS_KEY = "saved_distance_meters_key"

        private var lastLocation: Location? = null
        private var distanceInMeters = 0.0
    }
}
