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
import kotlin.math.sqrt

class RideSimulator(
    private val route: List<LatLng>,
    private val updateIntervalMillis: Long = 50L,
    private val segmentDurationMillis: Long = 1_500L,
    private val arrivalHoldMillis: Long = 3_000L,
    private val simulatedSpeedKmh: Float = 38f,
    private val onFrame: (NavigationFrame) -> Unit
) {
    private val handler: Handler =
        Handler(Looper.getMainLooper())

    private var routeIndex: Int = 0
    private var segmentStartTimeMillis: Long = 0L
    private var arrivalStartTimeMillis: Long? = null
    private var isRunning: Boolean = false

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isRunning || route.size < 2) {
                return
            }

            val now = SystemClock.uptimeMillis()

            if (routeIndex >= route.lastIndex) {
                routeIndex = route.lastIndex - 1
            }

            val segmentStart = route[routeIndex]
            val segmentEnd = route[routeIndex + 1]

            val elapsedMillis =
                now - segmentStartTimeMillis

            val progress = (
                    elapsedMillis.toFloat() /
                            segmentDurationMillis.toFloat()
                    ).coerceIn(0f, 1f)

            val currentLocation = interpolate(
                start = segmentStart,
                end = segmentEnd,
                progress = progress
            )

            val remainingDistanceMeters =
                calculateRemainingRouteDistanceMeters(
                    currentLocation = currentLocation,
                    currentSegmentEndIndex = routeIndex + 1
                )

            val isArrived =
                remainingDistanceMeters <= ARRIVAL_THRESHOLD_METERS

            val maneuver = determinePreviewManeuver(
                index = routeIndex,
                isArrived = isArrived
            )

            val speedForFrame =
                if (isArrived) 0f else simulatedSpeedKmh

            val remainingDurationSeconds =
                if (
                    isArrived ||
                    speedForFrame <= 0f
                ) {
                    0
                } else {
                    (
                            remainingDistanceMeters /
                                    (speedForFrame / 3.6f)
                            ).toInt()
                }

            val frame = NavigationFrame(
                currentLocation = currentLocation.toGeoPoint(),
                nextLocation = segmentEnd.toGeoPoint(),
                bearingDegrees = calculateBearing(
                    start = segmentStart,
                    end = segmentEnd
                ),
                speedKmh = speedForFrame,
                maneuver = maneuver,
                distanceToTurnMeters = if (isArrived) {
                    remainingDistanceMeters
                        .toInt()
                        .coerceAtLeast(0)
                } else {
                    formatDisplayDistance(
                        remainingDistanceMeters
                    )
                },
                streetName = if (isArrived) {
                    "Destination"
                } else {
                    "FC Road"
                },
                remainingDistanceMeters =
                    remainingDistanceMeters
                        .toInt()
                        .coerceAtLeast(0),
                remainingDurationSeconds =
                    remainingDurationSeconds
                        .coerceAtLeast(0),
                route = route.map { point ->
                    point.toGeoPoint()
                }
            )

            onFrame(frame)

            if (isArrived) {
                handleArrival(now)
            } else {
                arrivalStartTimeMillis = null

                if (progress >= 1f) {
                    moveToNextSegment(now)
                }
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

        if (segmentStartTimeMillis == 0L) {
            segmentStartTimeMillis =
                SystemClock.uptimeMillis()
        }

        handler.post(updateRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    fun restart() {
        stop()
        resetSimulation()
        start()
    }

    private fun handleArrival(
        currentTimeMillis: Long
    ) {
        val arrivalStartedAt =
            arrivalStartTimeMillis

        if (arrivalStartedAt == null) {
            arrivalStartTimeMillis =
                currentTimeMillis
            return
        }

        val arrivalElapsedMillis =
            currentTimeMillis - arrivalStartedAt

        if (arrivalElapsedMillis >= arrivalHoldMillis) {
            resetSimulation()
        }
    }

    private fun moveToNextSegment(
        currentTimeMillis: Long
    ) {
        routeIndex++

        if (routeIndex >= route.lastIndex) {
            routeIndex = route.lastIndex - 1
        }

        segmentStartTimeMillis =
            currentTimeMillis
    }

    private fun resetSimulation() {
        routeIndex = 0
        segmentStartTimeMillis =
            SystemClock.uptimeMillis()
        arrivalStartTimeMillis = null
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
        index: Int,
        isArrived: Boolean
    ): Maneuver {
        if (isArrived) {
            return Maneuver.ARRIVE
        }

        return when {
            index < 3 -> Maneuver.STRAIGHT
            index < 7 -> Maneuver.RIGHT
            index < 11 -> Maneuver.LEFT
            else -> Maneuver.STRAIGHT
        }
    }

    private fun calculateRemainingRouteDistanceMeters(
        currentLocation: LatLng,
        currentSegmentEndIndex: Int
    ): Double {
        if (route.isEmpty()) {
            return 0.0
        }

        if (currentSegmentEndIndex !in route.indices) {
            return 0.0
        }

        var remainingDistance =
            calculateDistanceMeters(
                start = currentLocation,
                end = route[currentSegmentEndIndex]
            )

        for (
        index in currentSegmentEndIndex
                until route.lastIndex
        ) {
            remainingDistance +=
                calculateDistanceMeters(
                    start = route[index],
                    end = route[index + 1]
                )
        }

        return remainingDistance
    }

    private fun calculateDistanceMeters(
        start: LatLng,
        end: LatLng
    ): Double {
        val startLatitude =
            Math.toRadians(start.latitude)

        val endLatitude =
            Math.toRadians(end.latitude)

        val latitudeDifference =
            Math.toRadians(
                end.latitude - start.latitude
            )

        val longitudeDifference =
            Math.toRadians(
                end.longitude - start.longitude
            )

        val latitudeSin =
            sin(latitudeDifference / 2.0)

        val longitudeSin =
            sin(longitudeDifference / 2.0)

        val haversine =
            latitudeSin * latitudeSin +
                    cos(startLatitude) *
                    cos(endLatitude) *
                    longitudeSin *
                    longitudeSin

        val angularDistance =
            2.0 * atan2(
                sqrt(haversine),
                sqrt(1.0 - haversine)
            )

        return EARTH_RADIUS_METERS *
                angularDistance
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

    private fun LatLng.toGeoPoint(): GeoPoint {
        return GeoPoint(
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun formatDisplayDistance(
        distanceMeters: Double
    ): Int {
        val distance = distanceMeters
            .toInt()
            .coerceAtLeast(0)

        return when {
            distance > 200 -> {
                roundToNearest(
                    value = distance,
                    interval = 10
                )
            }

            distance > 50 -> {
                roundToNearest(
                    value = distance,
                    interval = 5
                )
            }

            else -> distance
        }
    }

    private fun roundToNearest(
        value: Int,
        interval: Int
    ): Int {
        return (
                (value + interval / 2) /
                        interval
                ) * interval
    }
    private companion object {
        const val EARTH_RADIUS_METERS =
            6_371_000.0

        const val ARRIVAL_THRESHOLD_METERS =
            8.0
    }
}