package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.apexvelo.ct.feature.device.model.NormalizedPoint

internal fun DrawScope.drawDeviceRider(
    riderPosition: NormalizedPoint
) {
    val center = riderPosition.toCanvasOffset(
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
        color = RendererPalette.RiderOutline,
        style = Stroke(
            width = 6.dp.toPx(),
            join = StrokeJoin.Round
        )
    )

    drawPath(
        path = arrowPath,
        color = RendererPalette.RiderFill
    )
}