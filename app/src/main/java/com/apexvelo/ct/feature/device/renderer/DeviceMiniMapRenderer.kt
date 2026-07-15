package com.apexvelo.ct.feature.device.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.rotate
import com.apexvelo.ct.feature.device.model.DeviceMapFrame

@Composable
fun DeviceMiniMapRenderer(
    frame: DeviceMapFrame,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(RendererPalette.MapBackground)

        val riderCenter = frame.riderPosition.toCanvasOffset(
            canvasWidth = size.width,
            canvasHeight = size.height
        )

        /*
         * Heading-up mode:
         * The map rotates while the rider remains pointing upward.
         */
        rotate(
            degrees = -frame.riderBearingDegrees,
            pivot = riderCenter
        ) {
            drawDeviceBuildings()

            frame.surroundingRoads.forEach { road ->
                drawDeviceRoad(road)
            }

            drawDeviceRoute(frame.activeRoute)
        }

        drawDeviceRider(
            riderPosition = frame.riderPosition
        )
    }
}