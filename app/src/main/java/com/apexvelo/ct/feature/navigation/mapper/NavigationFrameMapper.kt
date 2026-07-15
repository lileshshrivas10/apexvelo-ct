package com.apexvelo.ct.feature.navigation.mapper

import com.apexvelo.ct.feature.device.model.BuildingPolygon
import com.apexvelo.ct.feature.device.model.DeviceMapFrame
import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.device.model.RoadPolyline
import com.apexvelo.ct.feature.device.model.RoadType
import com.apexvelo.ct.feature.navigation.model.Maneuver
import com.apexvelo.ct.feature.navigation.model.NavigationFrame
import com.apexvelo.ct.feature.navigation.camera.DeviceCameraTransform

class NavigationFrameMapper(
    private val cameraTransform: DeviceCameraTransform =
        DeviceCameraTransform()
) {

    fun map(frame: NavigationFrame): DeviceMapFrame {
        val riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.80f
        )

        val visibleRadiusMeters = when {
            frame.speedKmh < 25f -> 220.0
            frame.speedKmh < 50f -> 350.0
            frame.speedKmh < 80f -> 500.0
            else -> 700.0
        }

        val transformedRoute =
            cameraTransform.transformRoute(
                route = frame.route,
                currentLocation = frame.currentLocation,
                riderPosition = riderPosition,
                visibleRadiusMeters = visibleRadiusMeters
            )

        return DeviceMapFrame(
            surroundingRoads = generateNearbyRoads(
                route = transformedRoute
            ),
            buildings = generateBuildings(
                route = transformedRoute
            ),
            activeRoute = transformedRoute,
            riderPosition = riderPosition,
            riderBearingDegrees =
                frame.bearingDegrees.toFloat(),
            maneuverSymbol =
                frame.maneuver.toSymbol(),
            distanceToTurnMeters =
                frame.distanceToTurnMeters,
            streetName = frame.streetName,
            speedKmh = frame.speedKmh.toInt()
        )
    }

    private fun generateNearbyRoads(
        route: List<NormalizedPoint>
    ): List<RoadPolyline> {
        if (route.size < 2) {
            return emptyList()
        }

        val roads = mutableListOf<RoadPolyline>()

        roads += RoadPolyline(
            points = route,
            type = RoadType.PRIMARY
        )

        route.forEachIndexed { index, point ->
            if (
                index == 0 ||
                index == route.lastIndex ||
                index % 2 != 0
            ) {
                return@forEachIndexed
            }

            val branchLength =
                if (index % 4 == 0) 0.22f else 0.16f

            roads += RoadPolyline(
                type = RoadType.SECONDARY,
                points = listOf(
                    point,
                    NormalizedPoint(
                        x = point.x - branchLength,
                        y = point.y - 0.04f
                    )
                )
            )

            roads += RoadPolyline(
                type = RoadType.SECONDARY,
                points = listOf(
                    point,
                    NormalizedPoint(
                        x = point.x + branchLength,
                        y = point.y + 0.03f
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
            .filterIndexed { index, _ ->
                index % 2 == 0
            }
            .flatMap { point ->
                listOf(
                    createBuilding(
                        centerX = point.x - 0.13f,
                        centerY = point.y,
                        width = 0.09f,
                        height = 0.055f
                    ),
                    createBuilding(
                        centerX = point.x + 0.13f,
                        centerY = point.y + 0.015f,
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
                    centerX - halfWidth,
                    centerY - halfHeight
                ),
                NormalizedPoint(
                    centerX + halfWidth,
                    centerY - halfHeight
                ),
                NormalizedPoint(
                    centerX + halfWidth,
                    centerY + halfHeight
                ),
                NormalizedPoint(
                    centerX - halfWidth,
                    centerY + halfHeight
                )
            )
        )
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
}