package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.apexvelo.ct.feature.device.model.NormalizedPoint

internal fun DrawScope.drawDestinationFlag(
    destinationPosition: NormalizedPoint?
) {
    if (destinationPosition == null) {
        return
    }

    val base = destinationPosition.toCanvasOffset(
        canvasWidth = size.width,
        canvasHeight = size.height
    )

    val poleHeight = 26.dp.toPx()
    val poleWidth = 2.dp.toPx()
    val flagWidth = 22.dp.toPx()
    val flagHeight = 16.dp.toPx()

    val poleTop = base.y - poleHeight

    drawRect(
        color = Color.White,
        topLeft = Offset(
            x = base.x - poleWidth / 2f,
            y = poleTop
        ),
        size = androidx.compose.ui.geometry.Size(
            width = poleWidth,
            height = poleHeight
        )
    )

    val cellWidth = flagWidth / 4f
    val cellHeight = flagHeight / 4f

    repeat(4) { row ->
        repeat(4) { column ->
            val isWhite =
                (row + column) % 2 == 0

            drawRect(
                color = if (isWhite) {
                    Color.White
                } else {
                    Color(0xFF15171C)
                },
                topLeft = Offset(
                    x = base.x + column * cellWidth,
                    y = poleTop + row * cellHeight
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = cellWidth,
                    height = cellHeight
                )
            )
        }
    }

    drawCircle(
        color = Color.White,
        radius = 4.dp.toPx(),
        center = base
    )

    drawCircle(
        color = Color(0xFF111318),
        radius = 2.dp.toPx(),
        center = base
    )
}