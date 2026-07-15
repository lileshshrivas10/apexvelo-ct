package com.apexvelo.ct.feature.navigation.camera

import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import kotlin.math.cos
import kotlin.math.hypot

class DeviceCameraTransform {

    fun transformRoute(
        route: List<GeoPoint>,
        currentLocation: GeoPoint,
        riderPosition: NormalizedPoint,
        visibleDistanceMeters: Double
    ): List<NormalizedPoint> {
        if (route.isEmpty()) {
            return listOf(riderPosition)
        }

        /*
         * Most of the display is reserved for the road ahead.
         * 0.72 means the selected visible distance occupies
         * roughly 72% of the circular display.
         */
        val scale = 0.72 / visibleDistanceMeters

        return route
            .map { point ->
                val eastMeters = longitudeDistanceMeters(
                    from = currentLocation,
                    to = point
                )

                val northMeters = latitudeDistanceMeters(
                    from = currentLocation,
                    to = point
                )

                WorldPoint(
                    eastMeters = eastMeters,
                    northMeters = northMeters,
                    distanceMeters = hypot(
                        eastMeters,
                        northMeters
                    )
                )
            }
            .filter { point ->
                point.distanceMeters <=
                        visibleDistanceMeters * ROUTE_BUFFER_MULTIPLIER
            }
            .map { point ->
                NormalizedPoint(
                    x = (
                            riderPosition.x +
                                    point.eastMeters * scale
                            ).toFloat(),
                    y = (
                            riderPosition.y -
                                    point.northMeters * scale
                            ).toFloat()
                )
            }
            .let { transformedPoints ->
                if (transformedPoints.isEmpty()) {
                    listOf(riderPosition)
                } else {
                    transformedPoints
                }
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

    private data class WorldPoint(
        val eastMeters: Double,
        val northMeters: Double,
        val distanceMeters: Double
    )

    private companion object {
        const val METERS_PER_DEGREE = 111_320.0
        const val ROUTE_BUFFER_MULTIPLIER = 1.25
    }
}