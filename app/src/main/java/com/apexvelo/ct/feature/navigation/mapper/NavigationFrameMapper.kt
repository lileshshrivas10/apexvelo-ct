package com.apexvelo.ct.feature.navigation.mapper

import com.apexvelo.ct.feature.device.model.BuildingPolygon
import com.apexvelo.ct.feature.device.model.DeviceMapFrame
import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.device.model.RoadPolyline
import com.apexvelo.ct.feature.device.model.RoadType
import com.apexvelo.ct.feature.navigation.camera.DeviceCameraTransform
import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame

class NavigationFrameMapper(
    private val cameraTransform: DeviceCameraTransform =
        DeviceCameraTransform()
) {

    fun map(
        frame: NavigationFrame
    ): DeviceMapFrame {
        val riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.82f
        )

        val visibleDistanceMeters = when {
            frame.speedKmh < 20f -> 110.0
            frame.speedKmh < 40f -> 160.0
            frame.speedKmh < 70f -> 230.0
            frame.speedKmh < 100f -> 320.0
            else -> 420.0
        }

        val transformedRoute: List<NormalizedPoint> =
            cameraTransform.transformRoute(
                route = frame.route,
                currentLocation = frame.currentLocation,
                riderPosition = riderPosition,
                visibleDistanceMeters = visibleDistanceMeters
            )

        val isArrived: Boolean =
            frame.maneuver == Maneuver.ARRIVE

        val destination: NormalizedPoint? =
            transformedRoute
                .lastOrNull()
                ?.takeIf { point ->
                    isArrived ||
                            point.isInsideViewport()
                }

        return DeviceMapFrame(
            surroundingRoads = generateNearbyRoads(
                route = transformedRoute
            ),
            buildings = generateBuildings(
                route = transformedRoute
            ),
            activeRoute = transformedRoute,
            riderPosition = riderPosition,
            destinationPosition = destination,
            riderBearingDegrees =
                frame.bearingDegrees.toFloat(),
            maneuverSymbol =
                frame.maneuver.toSymbol(),
            distanceToTurnMeters =
                frame.distanceToTurnMeters
                    .coerceAtLeast(0),
            streetName = frame.streetName,
            speedKmh = if (isArrived) {
                0
            } else {
                frame.speedKmh
                    .toInt()
                    .coerceAtLeast(0)
            },
            isArrived = isArrived
        )
    }

    private fun generateNearbyRoads(
        route: List<NormalizedPoint>
    ): List<RoadPolyline> {
        if (route.size < 2) {
            return emptyList()
        }

        val roads = mutableListOf<RoadPolyline>()

        // Main road under the active route.
        roads += RoadPolyline(
            points = route,
            type = RoadType.PRIMARY
        )

        route.forEachIndexed { index, point ->
            val shouldSkip =
                index == 0 ||
                        index == route.lastIndex ||
                        index % 2 != 0 ||
                        !point.isInsideExtendedViewport()

            if (shouldSkip) {
                return@forEachIndexed
            }

            val branchLength =
                if (index % 4 == 0) {
                    0.20f
                } else {
                    0.14f
                }

            val verticalOffset =
                if (index % 4 == 0) {
                    -0.045f
                } else {
                    0.035f
                }

            roads += RoadPolyline(
                type = RoadType.SECONDARY,
                points = listOf(
                    point,
                    NormalizedPoint(
                        x = point.x - branchLength,
                        y = point.y + verticalOffset
                    )
                )
            )

            roads += RoadPolyline(
                type = RoadType.SECONDARY,
                points = listOf(
                    point,
                    NormalizedPoint(
                        x = point.x + branchLength,
                        y = point.y - verticalOffset
                    )
                )
            )
        }

        return roads
    }

    private fun generateBuildings(
        route: List<NormalizedPoint>
    ): List<BuildingPolygon> {
        if (route.size < 3) {
            return emptyList()
        }

        return route
            .drop(1)
            .dropLast(1)
            .filter { point ->
                point.isInsideExtendedViewport()
            }
            .filterIndexed { index, _ ->
                index % 2 == 0
            }
            .flatMapIndexed { index, point ->
                val verticalOffset =
                    if (index % 2 == 0) {
                        0.01f
                    } else {
                        -0.015f
                    }

                listOf(
                    createBuilding(
                        centerX = point.x - 0.13f,
                        centerY = point.y + verticalOffset,
                        width = 0.09f,
                        height = 0.055f
                    ),
                    createBuilding(
                        centerX = point.x + 0.13f,
                        centerY = point.y - verticalOffset,
                        width = 0.08f,
                        height = 0.05f
                    )
                )
            }
    }

    private fun createBuilding(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float
    ): BuildingPolygon {
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        return BuildingPolygon(
            points = listOf(
                NormalizedPoint(
                    x = centerX - halfWidth,
                    y = centerY - halfHeight
                ),
                NormalizedPoint(
                    x = centerX + halfWidth,
                    y = centerY - halfHeight
                ),
                NormalizedPoint(
                    x = centerX + halfWidth,
                    y = centerY + halfHeight
                ),
                NormalizedPoint(
                    x = centerX - halfWidth,
                    y = centerY + halfHeight
                )
            )
        )
    }

    private fun NormalizedPoint.isInsideViewport(): Boolean {
        return x in 0f..1f &&
                y in 0f..1f
    }

    private fun NormalizedPoint.isInsideExtendedViewport(): Boolean {
        return x in -0.15f..1.15f &&
                y in -0.15f..1.15f
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
            Maneuver.ARRIVE -> "🏁"
        }
    }
}