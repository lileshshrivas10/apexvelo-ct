package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.apexvelo.ct.feature.device.model.RoadPolyline
import com.apexvelo.ct.feature.device.model.RoadType

internal fun DrawScope.drawDeviceRoad(
    road: RoadPolyline
) {
    if (road.points.size < 2) {
        return
    }

    val roadPath = createPolylinePath(road.points)

    val outlineWidth = when (road.type) {
        RoadType.PRIMARY -> 15.dp.toPx()
        RoadType.SECONDARY -> 10.dp.toPx()
    }

    val surfaceWidth = when (road.type) {
        RoadType.PRIMARY -> 8.dp.toPx()
        RoadType.SECONDARY -> 5.dp.toPx()
    }

    val surfaceColor = when (road.type) {
        RoadType.PRIMARY -> RendererPalette.PrimaryRoad
        RoadType.SECONDARY -> RendererPalette.SecondaryRoad
    }

    drawPath(
        path = roadPath,
        color = RendererPalette.RoadOutline,
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