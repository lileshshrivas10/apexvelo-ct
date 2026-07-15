package com.apexvelo.ct.feature.device.renderer

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.rotate
import com.apexvelo.ct.feature.device.model.DeviceMapFrame

@Composable
fun DeviceMiniMapRenderer(
    frame: DeviceMapFrame,
    modifier: Modifier = Modifier
) {
    val animatedBearing by animateFloatAsState(
        targetValue = frame.riderBearingDegrees,
        animationSpec = tween(
            durationMillis = 600,
            easing = LinearOutSlowInEasing
        ),
        label = "device-map-bearing"
    )

    Canvas(modifier = modifier) {
        drawRect(RendererPalette.MapBackground)

        val riderCenter =
            frame.riderPosition.toCanvasOffset(
                canvasWidth = size.width,
                canvasHeight = size.height
            )

        rotate(
            degrees = -animatedBearing,
            pivot = riderCenter
        ) {
            frame.buildings.forEach { building ->
                drawDeviceBuilding(building)
            }

            frame.surroundingRoads.forEach { road ->
                drawDeviceRoad(road)
            }

            drawDeviceRoute(
                route = frame.activeRoute
            )

            drawDestinationFlag(
                destinationPosition = frame.destinationPosition
            )
        }

        drawDeviceRider(
            riderPosition = frame.riderPosition
        )
    }
}