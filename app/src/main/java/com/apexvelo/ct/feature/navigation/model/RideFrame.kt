package com.apexvelo.ct.feature.navigation.model

import org.maplibre.android.geometry.LatLng

data class RideFrame(
    val currentLocation: LatLng,
    val nextLocation: LatLng,
    val bearingDegrees: Double,
    val speedKmh: Float
)