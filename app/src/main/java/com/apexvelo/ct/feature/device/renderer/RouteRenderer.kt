package com.apexvelo.ct.feature.device.renderer

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.apexvelo.ct.feature.device.model.NormalizedPoint

internal fun DrawScope.drawDeviceRoute(
    route: List<NormalizedPoint>
) {
    if (route.size < 2) {
        return
    }

    val routePath = createPolylinePath(route)

    drawPath(
        path = routePath,
        color = RendererPalette.RouteOutline,
        style = Stroke(
            width = 18.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawPath(
        path = routePath,
        color = RendererPalette.ActiveRoute,
        style = Stroke(
            width = 9.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}