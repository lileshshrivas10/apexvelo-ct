package com.apexvelo.ct.feature.device.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.apexvelo.ct.feature.device.model.DeviceMapFrame
import com.apexvelo.ct.feature.device.model.NormalizedPoint
import com.apexvelo.ct.feature.device.model.RoadPolyline
import com.apexvelo.ct.feature.device.model.RoadType

private val MapBackground = Color(0xFF111318)

private val BuildingFill = Color(0xFF1B1E24)
private val BuildingShadow = Color(0xFF090A0D)

private val RoadOutline = Color(0xFF15181E)
private val PrimaryRoad = Color(0xFF50555E)
private val SecondaryRoad = Color(0xFF343841)

private val RouteOutline = Color(0xFF090A0D)
private val ActiveRoute = Color(0xFFF6F6F6)

private val RiderFill = Color(0xFFF8F8F8)
private val RiderOutline = Color(0xFF050608)

@Composable
fun DeviceMiniMapRenderer(
    frame: DeviceMapFrame,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(MapBackground)

        val riderCenter = frame.riderPosition.toOffset(
            canvasWidth = size.width,
            canvasHeight = size.height
        )

        /*
         * Heading-up navigation:
         *
         * The rider remains pointing upward.
         * The map rotates in the opposite direction of travel.
         */
        rotate(
            degrees = -frame.riderBearingDegrees,
            pivot = riderCenter
        ) {
            drawPreviewBuildings()

            frame.surroundingRoads.forEach { road ->
                drawRoad(road)
            }

            drawActiveRoute(frame.activeRoute)
        }

        // Draw rider after rotating the world so the rider stays upright.
        drawRiderArrow(
            riderPosition = frame.riderPosition
        )
    }
}

private fun DrawScope.drawPreviewBuildings() {
    val buildings = listOf(
        BuildingBlock(0.08f, 0.29f, 0.16f, 0.07f, -12f),
        BuildingBlock(0.25f, 0.22f, 0.18f, 0.09f, 8f),
        BuildingBlock(0.51f, 0.19f, 0.13f, 0.07f, -5f),
        BuildingBlock(0.73f, 0.27f, 0.18f, 0.09f, 12f),

        BuildingBlock(0.08f, 0.45f, 0.20f, 0.10f, -8f),
        BuildingBlock(0.29f, 0.43f, 0.11f, 0.07f, 7f),
        BuildingBlock(0.65f, 0.45f, 0.21f, 0.10f, 6f),

        BuildingBlock(0.13f, 0.64f, 0.17f, 0.09f, 10f),
        BuildingBlock(0.34f, 0.69f, 0.13f, 0.08f, -7f),
        BuildingBlock(0.64f, 0.65f, 0.17f, 0.09f, 8f),
        BuildingBlock(0.80f, 0.76f, 0.11f, 0.07f, -11f)
    )

    buildings.forEach { building ->
        val left = building.x * size.width
        val top = building.y * size.height
        val width = building.width * size.width
        val height = building.height * size.height

        val pivot = Offset(
            x = left + width / 2f,
            y = top + height / 2f
        )

        rotate(
            degrees = building.rotationDegrees,
            pivot = pivot
        ) {
            drawRoundRect(
                color = BuildingShadow,
                topLeft = Offset(
                    x = left + 3.dp.toPx(),
                    y = top + 3.dp.toPx()
                ),
                size = Size(width, height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            drawRoundRect(
                color = BuildingFill,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawRoad(
    road: RoadPolyline
) {
    if (road.points.size < 2) {
        return
    }

    val roadPath = createPath(road.points)

    val outlineWidth = when (road.type) {
        RoadType.PRIMARY -> 15.dp.toPx()
        RoadType.SECONDARY -> 10.dp.toPx()
    }

    val surfaceWidth = when (road.type) {
        RoadType.PRIMARY -> 8.dp.toPx()
        RoadType.SECONDARY -> 5.dp.toPx()
    }

    val surfaceColor = when (road.type) {
        RoadType.PRIMARY -> PrimaryRoad
        RoadType.SECONDARY -> SecondaryRoad
    }

    drawPath(
        path = roadPath,
        color = RoadOutline,
        style = Stroke(
            width = outlineWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawPath(
        path = roadPath,
        color = surfaceColor,
        style = Stroke(
            width = surfaceWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawActiveRoute(
    route: List<NormalizedPoint>
) {
    if (route.size < 2) {
        return
    }

    val routePath = createPath(route)

    drawPath(
        path = routePath,
        color = RouteOutline,
        style = Stroke(
            width = 18.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawPath(
        path = routePath,
        color = ActiveRoute,
        style = Stroke(
            width = 9.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawRiderArrow(
    riderPosition: NormalizedPoint
) {
    val center = riderPosition.toOffset(
        canvasWidth = size.width,
        canvasHeight = size.height
    )

    val halfWidth = 15.dp.toPx()
    val arrowHeight = 27.dp.toPx()

    val arrowPath = Path().apply {
        moveTo(
            center.x,
            center.y - arrowHeight
        )

        lineTo(
            center.x - halfWidth,
            center.y + arrowHeight * 0.65f
        )

        lineTo(
            center.x,
            center.y + arrowHeight * 0.20f
        )

        lineTo(
            center.x + halfWidth,
            center.y + arrowHeight * 0.65f
        )

        close()
    }

    drawPath(
        path = arrowPath,
        color = RiderOutline,
        style = Stroke(
            width = 6.dp.toPx(),
            join = StrokeJoin.Round
        )
    )

    drawPath(
        path = arrowPath,
        color = RiderFill
    )
}

private fun DrawScope.createPath(
    points: List<NormalizedPoint>
): Path {
    return Path().apply {
        val firstPoint = points.firstOrNull() ?: return@apply

        val firstOffset = firstPoint.toOffset(
            canvasWidth = size.width,
            canvasHeight = size.height
        )

        moveTo(
            firstOffset.x,
            firstOffset.y
        )

        points.drop(1).forEach { point ->
            val offset = point.toOffset(
                canvasWidth = size.width,
                canvasHeight = size.height
            )

            lineTo(
                offset.x,
                offset.y
            )
        }
    }
}

private fun NormalizedPoint.toOffset(
    canvasWidth: Float,
    canvasHeight: Float
): Offset {
    return Offset(
        x = x * canvasWidth,
        y = y * canvasHeight
    )
}

private data class BuildingBlock(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float
)