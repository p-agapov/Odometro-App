package com.agapovp.android.odometro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.agapovp.android.odometro.OdometerService.Companion.PERMISSION_FINE_LOCATION
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), ResetDialogFragment.ResetDialogListener {

    private var odometer: OdometerService? = null
    private var bound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayDistance()
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(this, PERMISSION_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(PERMISSION_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            bindService(
                Intent(this, OdometerService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()

        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bindService(
                        Intent(this, OdometerService::class.java),
                        connection,
                        Context.BIND_AUTO_CREATE
                    )
                } else {
                    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_compass)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.permission_denied))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(longArrayOf(1000, 1000))
                        .setAutoCancel(true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .createNotificationChannel(
                                NotificationChannel(
                                    CHANNEL_ID,
                                    getString(R.string.channel_name),
                                    NotificationManager.IMPORTANCE_HIGH
                                ).apply { description = getString(R.string.channel_description) }
                            )
                    }

                    val actionPendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    builder.setContentIntent(actionPendingIntent)

                    with(NotificationManagerCompat.from(this)) {
                        notify(NOTIFICATION_ID, builder.build())
                    }
                }
            }
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        tv_distance.text = getDistanceString(0.0)
        odometer?.resetDistance()
        Snackbar.make(
            findViewById(R.id.coordinator_layout),
            getString(R.string.snackbar_positive_text),
            Snackbar.LENGTH_LONG
        )
            .show()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        Snackbar.make(
            findViewById(R.id.coordinator_layout),
            getString(R.string.snackbar_negative_text),
            Snackbar.LENGTH_SHORT
        )
            .show()
    }

    fun onResetButtonClicked(view: View) {
        ResetDialogFragment().show(
            supportFragmentManager,
            ResetDialogFragment::class.java.canonicalName
        )
    }

    private fun displayDistance() {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                var distance = 0.0
                odometer?.let {
                    if (bound) {
                        distance = it.getDistance()
                    }
                }
                tv_distance.text = getDistanceString(distance)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun getDistanceString(distance: Double) = String.format(
        Locale.getDefault(),
        "%1$,.3f ${getString(R.string.tv_distance_measure)}",
        distance
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            odometer = (binder as OdometerService.OdometerBinder).getOdometer()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 698
        private const val CHANNEL_ID = "channel_id_location"
        private const val NOTIFICATION_ID = 423
    }
}
