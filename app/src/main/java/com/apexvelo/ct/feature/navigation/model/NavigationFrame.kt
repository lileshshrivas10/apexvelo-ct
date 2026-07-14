package com.apexvelo.ct.feature.navigation.model

data class NavigationFrame(
    val currentLocation: GeoPoint,
    val nextLocation: GeoPoint,
    val bearingDegrees: Double,
    val speedKmh: Float,

    val maneuver: Maneuver,
    val distanceToTurnMeters: Int,
    val streetName: String,

    val remainingDistanceMeters: Int,
    val remainingDurationSeconds: Int,

    val route: List<GeoPoint>
)