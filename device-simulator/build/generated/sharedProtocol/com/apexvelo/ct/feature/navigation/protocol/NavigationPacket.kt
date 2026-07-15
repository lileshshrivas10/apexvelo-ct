package com.apexvelo.ct.feature.navigation.protocol

import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame

data class NavigationPacket(
    val sequenceNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Double,
    val speedKmh: Float,
    val maneuver: Maneuver,
    val distanceToTurnMeters: Int,
    val remainingDistanceMeters: Int,
    val remainingDurationSeconds: Int,
    val isArrived: Boolean
) {
    companion object {
        fun fromFrame(
            frame: NavigationFrame,
            sequenceNumber: Int
        ): NavigationPacket {
            return NavigationPacket(
                sequenceNumber = sequenceNumber,
                latitude = frame.currentLocation.latitude,
                longitude = frame.currentLocation.longitude,
                bearingDegrees = frame.bearingDegrees,
                speedKmh = frame.speedKmh,
                maneuver = frame.maneuver,
                distanceToTurnMeters =
                    frame.distanceToTurnMeters.coerceAtLeast(0),
                remainingDistanceMeters =
                    frame.remainingDistanceMeters.coerceAtLeast(0),
                remainingDurationSeconds =
                    frame.remainingDurationSeconds.coerceAtLeast(0),
                isArrived =
                    frame.maneuver == Maneuver.ARRIVE
            )
        }
    }
}