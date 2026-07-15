package com.apexvelo.ct.feature.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import org.maplibre.android.geometry.LatLng

data class PhoneLocation(
    val position: LatLng,
    val bearingDegrees: Double,
    val speedKmh: Float
)

class PhoneLocationProvider(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun start(onLocation: (PhoneLocation) -> Unit) {
        stop()
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocation(location.toPhoneLocation())
            }

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = locationListener

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { manager?.isProviderEnabled(it) == true }

        providers.forEach { provider ->
            manager?.getLastKnownLocation(provider)?.let {
                onLocation(it.toPhoneLocation())
            }
            manager?.requestLocationUpdates(
                provider,
                500L,
                1f,
                locationListener
            )
        }
    }

    fun stop() {
        listener?.let { manager?.removeUpdates(it) }
        listener = null
    }

    private fun Location.toPhoneLocation() = PhoneLocation(
        position = LatLng(latitude, longitude),
        bearingDegrees = if (hasBearing()) bearing.toDouble() else 0.0,
        speedKmh = if (hasSpeed()) speed * 3.6f else 0f
    )
}
