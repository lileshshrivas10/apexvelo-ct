package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apexvelo.ct.feature.device.model.NormalizedPoint

internal fun NormalizedPoint.toCanvasOffset(
    canvasWidth: Float,
    canvasHeight: Float
): Offset {
    return Offset(
        x = x * canvasWidth,
        y = y * canvasHeight
    )
}

internal fun DrawScope.createPolylinePath(
    points: List<NormalizedPoint>
): Path {
    return Path().apply {
        val firstPoint = points.firstOrNull() ?: return@apply

        val firstOffset = firstPoint.toCanvasOffset(
            canvasWidth = size.width,
            canvasHeight = size.height
        )

        moveTo(firstOffset.x, firstOffset.y)

        points.drop(1).forEach { point ->
            val offset = point.toCanvasOffset(
                canvasWidth = size.width,
                canvasHeight = size.height
            )

            lineTo(offset.x, offset.y)
        }
    }
}