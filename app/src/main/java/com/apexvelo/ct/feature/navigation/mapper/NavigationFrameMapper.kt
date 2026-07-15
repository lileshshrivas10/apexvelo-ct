package com.apexvelo.ct.feature.navigation.mapper

import com.apexvelo.ct.feature.device.model.DeviceMapFrame
import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.device.model.PreviewDeviceMap
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame
import kotlin.math.cos

class NavigationFrameMapper {

    fun map(frame: NavigationFrame): DeviceMapFrame {
        val riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.80f
        )

        return DeviceMapFrame(
            surroundingRoads = PreviewDeviceMap.frame.surroundingRoads,
            activeRoute = normalizeRoute(
                route = frame.route,
                currentLocation = frame.currentLocation,
                riderPosition = riderPosition,
                speedKmh = frame.speedKmh
            ),
            riderPosition = riderPosition,
            riderBearingDegrees = frame.bearingDegrees.toFloat(),
            maneuverSymbol = frame.maneuver.toSymbol(),
            distanceToTurnMeters = frame.distanceToTurnMeters,
            streetName = frame.streetName,
            speedKmh = frame.speedKmh.toInt()
        )
    }

    private fun normalizeRoute(
        route: List<GeoPoint>,
        currentLocation: GeoPoint,
        riderPosition: NormalizedPoint,
        speedKmh: Float
    ): List<NormalizedPoint> {
        if (route.isEmpty()) {
            return listOf(riderPosition)
        }

        val visibleRadiusMeters = when {
            speedKmh < 25f -> 220.0
            speedKmh < 50f -> 350.0
            speedKmh < 80f -> 500.0
            else -> 700.0
        }

        val normalizedScale = 0.65 / visibleRadiusMeters

        return route.map { point ->
            val eastMeters = longitudeDistanceMeters(
                from = currentLocation,
                to = point
            )

            val northMeters = latitudeDistanceMeters(
                from = currentLocation,
                to = point
            )

            NormalizedPoint(
                x = (
                        riderPosition.x +
                                eastMeters * normalizedScale
                        ).toFloat(),
                y = (
                        riderPosition.y -
                                northMeters * normalizedScale
                        ).toFloat()
            )
        }
    }

    private fun latitudeDistanceMeters(
        from: GeoPoint,
        to: GeoPoint
    ): Double {
        return (to.latitude - from.latitude) * METERS_PER_LATITUDE_DEGREE
    }

    private fun longitudeDistanceMeters(
        from: GeoPoint,
        to: GeoPoint
    ): Double {
        val latitudeRadians = Math.toRadians(from.latitude)

        return (
                to.longitude - from.longitude
                ) * METERS_PER_LATITUDE_DEGREE * cos(latitudeRadians)
    }

    private fun Maneuver.toSymbol(): String {
        return when (this) {
            Maneuver.STRAIGHT -> "↑"
            Maneuver.SLIGHT_LEFT -> "↖"
            Maneuver.LEFT -> "↰"
            Maneuver.SHARP_LEFT -> "↶"
            Maneuver.SLIGHT_RIGHT -> "↗"
            Maneuver.RIGHT -> "↱"
            Maneuver.SHARP_RIGHT -> "↷"
            Maneuver.U_TURN -> "↶"
            Maneuver.ROUNDABOUT -> "⟳"
            Maneuver.ARRIVE -> "●"
        }
    }

    private companion object {
        const val METERS_PER_LATITUDE_DEGREE = 111_320.0
    }
}