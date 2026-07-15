package com.apexvelo.ct.feature.navigation.camera

import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import kotlin.math.cos

class DeviceCameraTransform {

    fun transformRoute(
        route: List<GeoPoint>,
        currentLocation: GeoPoint,
        riderPosition: NormalizedPoint,
        visibleRadiusMeters: Double
    ): List<NormalizedPoint> {
        if (route.isEmpty()) {
            return listOf(riderPosition)
        }

        val scale = 0.62 / visibleRadiusMeters

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
                                eastMeters * scale
                        ).toFloat(),
                y = (
                        riderPosition.y -
                                northMeters * scale
                        ).toFloat()
            )
        }
    }

    private fun latitudeDistanceMeters(
        from: GeoPoint,
        to: GeoPoint
    ): Double {
        return (
                to.latitude - from.latitude
                ) * METERS_PER_DEGREE
    }

    private fun longitudeDistanceMeters(
        from: GeoPoint,
        to: GeoPoint
    ): Double {
        val latitudeRadians =
            Math.toRadians(from.latitude)

        return (
                to.longitude - from.longitude
                ) * METERS_PER_DEGREE * cos(latitudeRadians)
    }

    private companion object {
        const val METERS_PER_DEGREE = 111_320.0
    }
}