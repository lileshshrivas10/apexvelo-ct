package com.apexvelo.ct.feature.navigation.simulator

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame
import org.maplibre.android.geometry.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RideSimulator(
    private val route: List<LatLng>,
    private val updateIntervalMillis: Long = 50L,
    private val segmentDurationMillis: Long = 1_500L,
    private val simulatedSpeedKmh: Float = 38f,
    private val onFrame: (NavigationFrame) -> Unit
) {
    private val handler: Handler =
        Handler(Looper.getMainLooper())

    private var routeIndex: Int = 0
    private var segmentStartTimeMillis: Long = 0L
    private var isRunning: Boolean = false

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isRunning || route.size < 2) {
                return
            }

            if (routeIndex >= route.lastIndex) {
                routeIndex = 0
                segmentStartTimeMillis = SystemClock.uptimeMillis()
            }

            val segmentStart = route[routeIndex]
            val segmentEnd = route[routeIndex + 1]

            val elapsedMillis =
                SystemClock.uptimeMillis() - segmentStartTimeMillis

            val progress = (
                    elapsedMillis.toFloat() /
                            segmentDurationMillis.toFloat()
                    ).coerceIn(0f, 1f)

            val currentLocation = interpolate(
                start = segmentStart,
                end = segmentEnd,
                progress = progress
            )

            val remainingSegmentCount =
                route.lastIndex - routeIndex

            val frame = NavigationFrame(
                currentLocation = currentLocation.toGeoPoint(),
                nextLocation = segmentEnd.toGeoPoint(),
                bearingDegrees = calculateBearing(
                    start = segmentStart,
                    end = segmentEnd
                ),
                speedKmh = simulatedSpeedKmh,
                maneuver = determinePreviewManeuver(routeIndex),
                distanceToTurnMeters =
                    (remainingSegmentCount * 45).coerceAtLeast(0),
                streetName = "FC Road",
                remainingDistanceMeters =
                    (remainingSegmentCount * 70).coerceAtLeast(0),
                remainingDurationSeconds =
                    (remainingSegmentCount * 12).coerceAtLeast(0),
                route = route.map { point ->
                    point.toGeoPoint()
                }
            )

            onFrame(frame)

            if (progress >= 1f) {
                routeIndex++

                if (routeIndex >= route.lastIndex) {
                    routeIndex = 0
                }

                segmentStartTimeMillis =
                    SystemClock.uptimeMillis()
            }

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
        segmentStartTimeMillis =
            SystemClock.uptimeMillis()

        handler.post(updateRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    fun restart() {
        stop()

        routeIndex = 0
        segmentStartTimeMillis = 0L

        start()
    }

    private fun interpolate(
        start: LatLng,
        end: LatLng,
        progress: Float
    ): LatLng {
        val latitude =
            start.latitude +
                    (end.latitude - start.latitude) * progress

        val longitude =
            start.longitude +
                    (end.longitude - start.longitude) * progress

        return LatLng(
            latitude,
            longitude
        )
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
                Math.toDegrees(
                    atan2(y, x)
                ) + 360.0
                ) % 360.0
    }
}