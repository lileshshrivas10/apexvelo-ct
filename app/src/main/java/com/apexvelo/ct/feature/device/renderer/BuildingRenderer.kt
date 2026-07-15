package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apexvelo.ct.feature.device.model.BuildingPolygon

internal fun DrawScope.drawDeviceBuilding(
    building: BuildingPolygon
) {
    if (building.points.size < 3) {
        return
    }

    val path = Path().apply {
        val firstPoint =
            building.points.first()

        val firstOffset =
            firstPoint.toCanvasOffset(
                canvasWidth = size.width,
                canvasHeight = size.height
            )

        moveTo(
            firstOffset.x,
            firstOffset.y
        )

        building.points
            .drop(1)
            .forEach { point ->
                val offset =
                    point.toCanvasOffset(
                        canvasWidth = size.width,
                        canvasHeight = size.height
                    )

                lineTo(
                    offset.x,
                    offset.y
                )
            }

        close()
    }

    drawPath(
        path = path,
        color = RendererPalette.BuildingFill
    )
}