package com.apexvelo.ct.feature.navigation.simulator

import android.os.Handler
import android.os.Looper
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame
import org.maplibre.android.geometry.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RideSimulator(
    private val route: List<LatLng>,
    private val updateIntervalMillis: Long = 1_500L,
    private val simulatedSpeedKmh: Float = 38f,
    private val onFrame: (NavigationFrame) -> Unit
) {
    private val handler: Handler =
        Handler(Looper.getMainLooper())

    private var routeIndex: Int = 0
    private var isRunning: Boolean = false

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isRunning || route.size < 2) {
                return
            }

            if (routeIndex >= route.lastIndex) {
                routeIndex = 0
            }

            val currentLocation = route[routeIndex]
            val nextLocation = route[routeIndex + 1]

            val remainingPointCount =
                route.lastIndex - routeIndex

            val frame = NavigationFrame(
                currentLocation = currentLocation.toGeoPoint(),
                nextLocation = nextLocation.toGeoPoint(),
                bearingDegrees = calculateBearing(
                    start = currentLocation,
                    end = nextLocation
                ),
                speedKmh = simulatedSpeedKmh,
                maneuver = determinePreviewManeuver(routeIndex),
                distanceToTurnMeters =
                    remainingPointCount * 45,
                streetName = "FC Road",
                remainingDistanceMeters =
                    remainingPointCount * 70,
                remainingDurationSeconds =
                    remainingPointCount * 12,
                route = route.map { it.toGeoPoint() }
            )

            onFrame(frame)

            routeIndex++

            handler.postDelayed(
                this,
                updateIntervalMillis
            )
        }
    }

    fun start() {
        if (isRunning || route.size < 2) {
            return
        }

        isRunning = true
        handler.post(updateRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    fun restart() {
        stop()
        routeIndex = 0
        start()
    }

    private fun determinePreviewManeuver(
        index: Int
    ): Maneuver {
        return when {
            index < 3 -> Maneuver.STRAIGHT
            index < 7 -> Maneuver.RIGHT
            index < 11 -> Maneuver.LEFT
            else -> Maneuver.ARRIVE
        }
    }

    private fun LatLng.toGeoPoint(): GeoPoint {
        return GeoPoint(
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun calculateBearing(
        start: LatLng,
        end: LatLng
    ): Double {
        val startLatitude =
            Math.toRadians(start.latitude)

        val startLongitude =
            Math.toRadians(start.longitude)

        val endLatitude =
            Math.toRadians(end.latitude)

        val endLongitude =
            Math.toRadians(end.longitude)

        val longitudeDifference =
            endLongitude - startLongitude

        val y =
            sin(longitudeDifference) *
                    cos(endLatitude)

        val x =
            cos(startLatitude) *
                    sin(endLatitude) -
                    sin(startLatitude) *
                    cos(endLatitude) *
                    cos(longitudeDifference)

        return (
                Math.toDegrees(atan2(y, x)) + 360.0
                ) % 360.0
    }
}