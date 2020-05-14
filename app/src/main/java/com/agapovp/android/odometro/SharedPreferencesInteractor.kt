package com.agapovp.android.odometro

import android.content.Context
import android.content.ContextWrapper
import io.reactivex.rxjava3.core.Single

private const val PREFERENCE_FILE_KEY =
    "com.agapovp.android.odometro.odometer_service_preference_file_key"

class SharedPreferencesInteractor {

    fun getFromSharedPreference(key: String, context: ContextWrapper): Single<Double> = Single.just(
        context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE).getFloat(
            key, 0f
        ).toDouble()
    )

    fun saveToSharedPreference(key: String, value: Float, context: ContextWrapper): Single<Unit> =
        Single.just(
            with(context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE).edit()) {
                putFloat(key, value)
                apply()
            }
        )
}
