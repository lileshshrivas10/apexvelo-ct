package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawDeviceBuildings() {
    previewBuildings.forEach { building ->
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
                color = RendererPalette.BuildingShadow,
                topLeft = Offset(
                    x = left + 3.dp.toPx(),
                    y = top + 3.dp.toPx()
                ),
                size = Size(width, height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            drawRoundRect(
                color = RendererPalette.BuildingFill,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

private data class BuildingBlock(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float
)

private val previewBuildings = listOf(
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