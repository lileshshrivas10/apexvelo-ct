package com.apexvelo.ct.feature.map

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import android.os.Handler
import android.os.Looper
import org.maplibre.android.camera.CameraUpdateFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val MAP_STYLE_URL =
    "https://tiles.openfreemap.org/styles/liberty"

private const val ROUTE_SOURCE_ID = "apex-route-source"
private const val ROUTE_OUTLINE_LAYER_ID = "apex-route-outline-layer"
private const val ROUTE_LAYER_ID = "apex-route-layer"

private const val RIDER_SOURCE_ID = "apex-rider-source"
private const val RIDER_LAYER_ID = "apex-rider-layer"

private val previewRoute = listOf(
    LatLng(18.52040, 73.85670),
    LatLng(18.52083, 73.85677),
    LatLng(18.52123, 73.85688),
    LatLng(18.52158, 73.85708),
    LatLng(18.52184, 73.85740),
    LatLng(18.52198, 73.85782),
    LatLng(18.52202, 73.85830),
    LatLng(18.52210, 73.85882),
    LatLng(18.52230, 73.85927),
    LatLng(18.52264, 73.85960),
    LatLng(18.52307, 73.85979),
    LatLng(18.52353, 73.85984),
    LatLng(18.52400, 73.85991),
    LatLng(18.52442, 73.86012),
    LatLng(18.52472, 73.86049)
)

private val initialRiderLocation = previewRoute.first()

@Composable
fun MapScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavigationMap(
            modifier = Modifier.fillMaxSize()
        )

        CompactNavigationHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
        )

        CompactManeuverPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
        )
    }
}

@Composable
private fun NavigationMap(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())

            getMapAsync { map ->
                configureNavigationMap(map)
            }
        }
    }

    DisposableEffect(
        lifecycleOwner,
        mapView
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
}

private fun configureNavigationMap(
    map: MapLibreMap
) {
    map.uiSettings.apply {
        isCompassEnabled = false
        isLogoEnabled = false
        isAttributionEnabled = false

        isRotateGesturesEnabled = true
        isTiltGesturesEnabled = true
        isZoomGesturesEnabled = true
        isScrollGesturesEnabled = true
    }

    /*
     * Camera padding is ordered:
     * left, top, right, bottom.
     *
     * A large bottom padding shifts the logical map centre upward,
     * leaving the rider visually closer to the bottom of the screen.
     */
    map.cameraPosition = CameraPosition.Builder()
        .target(initialRiderLocation)
        .zoom(16.6)
        .bearing(18.0)
        .tilt(52.0)
        .padding(
            0.0,
            160.0,
            0.0,
            620.0
        )
        .build()

    map.setStyle(
        Style.Builder().fromUri(MAP_STYLE_URL)
    ) { style ->
        addPreviewRoute(style)
        addRiderMarker(style)
        startPreviewRide(map, style)
    }
}

private fun addPreviewRoute(
    style: Style
) {
    val routeGeoJson = """
        {
          "type": "Feature",
          "properties": {},
          "geometry": {
            "type": "LineString",
            "coordinates": [
              [73.85670, 18.52040],
              [73.85677, 18.52083],
              [73.85688, 18.52123],
              [73.85708, 18.52158],
              [73.85740, 18.52184],
              [73.85782, 18.52198],
              [73.85830, 18.52202],
              [73.85882, 18.52210],
              [73.85927, 18.52230],
              [73.85960, 18.52264],
              [73.85979, 18.52307],
              [73.85984, 18.52353],
              [73.85991, 18.52400],
              [73.86012, 18.52442],
              [73.86049, 18.52472]
            ]
          }
        }
    """.trimIndent()

    style.addSource(
        GeoJsonSource(
            ROUTE_SOURCE_ID,
            routeGeoJson
        )
    )

    style.addLayer(
        LineLayer(
            ROUTE_OUTLINE_LAYER_ID,
            ROUTE_SOURCE_ID
        ).withProperties(
            lineColor("#101218"),
            lineWidth(13f),
            lineOpacity(0.90f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        )
    )

    style.addLayer(
        LineLayer(
            ROUTE_LAYER_ID,
            ROUTE_SOURCE_ID
        ).withProperties(
            lineColor("#00D4FF"),
            lineWidth(8f),
            lineOpacity(1f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
}

private fun addRiderMarker(
    style: Style
) {
    val riderGeoJson = """
        {
          "type": "Feature",
          "properties": {},
          "geometry": {
            "type": "Point",
            "coordinates": [
              ${'$'}{initialRiderLocation.longitude},
              ${'$'}{initialRiderLocation.latitude}
            ]
          }
        }
    """.trimIndent()

    style.addSource(
        GeoJsonSource(
            RIDER_SOURCE_ID,
            riderGeoJson
        )
    )

    style.addLayer(
        CircleLayer(
            RIDER_LAYER_ID,
            RIDER_SOURCE_ID
        ).withProperties(
            circleRadius(8f),
            circleColor("#FFFFFF"),
            circleStrokeColor("#00D4FF"),
            circleStrokeWidth(4f)
        )
    )
}

private fun startPreviewRide(
    map: MapLibreMap,
    style: Style
) {
    val handler = Handler(Looper.getMainLooper())
    var routeIndex = 0

    val updateRide = object : Runnable {
        override fun run() {
            if (routeIndex >= previewRoute.lastIndex) {
                routeIndex = 0
            }

            val currentPoint = previewRoute[routeIndex]
            val nextPoint = previewRoute[routeIndex + 1]
            val bearing = calculateBearing(currentPoint, nextPoint)

            updateRiderMarker(
                style = style,
                location = currentPoint
            )

            updateNavigationCamera(
                map = map,
                location = currentPoint,
                bearing = bearing
            )

            routeIndex++

            handler.postDelayed(
                this,
                1_500L
            )
        }
    }

    handler.post(updateRide)
}

private fun updateRiderMarker(
    style: Style,
    location: LatLng
) {
    val riderGeoJson = """
        {
          "type": "Feature",
          "properties": {},
          "geometry": {
            "type": "Point",
            "coordinates": [
              ${location.longitude},
              ${location.latitude}
            ]
          }
        }
    """.trimIndent()

    style.getSourceAs<GeoJsonSource>(
        RIDER_SOURCE_ID
    )?.setGeoJson(riderGeoJson)
}

private fun updateNavigationCamera(
    map: MapLibreMap,
    location: LatLng,
    bearing: Double
) {
    val cameraPosition = CameraPosition.Builder()
        .target(location)
        .zoom(16.6)
        .bearing(bearing)
        .tilt(52.0)
        .padding(
            0.0,
            160.0,
            0.0,
            620.0
        )
        .build()

    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(cameraPosition),
        1_200
    )
}

private fun calculateBearing(
    start: LatLng,
    end: LatLng
): Double {
    val startLatitude = Math.toRadians(start.latitude)
    val startLongitude = Math.toRadians(start.longitude)
    val endLatitude = Math.toRadians(end.latitude)
    val endLongitude = Math.toRadians(end.longitude)

    val longitudeDifference = endLongitude - startLongitude

    val y = sin(longitudeDifference) * cos(endLatitude)
    val x =
        cos(startLatitude) * sin(endLatitude) -
                sin(startLatitude) *
                cos(endLatitude) *
                cos(longitudeDifference)

    return (
            Math.toDegrees(
                atan2(y, x)
            ) + 360.0
            ) % 360.0
}
@Composable
private fun CompactNavigationHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 9.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "250 m",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "TURN LEFT",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    fontSize = 9.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "18 min",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "8.2 km remaining",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun CompactManeuverPanel(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↰",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "Turn left",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "FC Road",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "38",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "km/h",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    fontSize = 9.sp
                )
            }
        }
    }
}