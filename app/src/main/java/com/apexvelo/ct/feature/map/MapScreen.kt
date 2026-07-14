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
import com.apexvelo.ct.feature.navigation.simulator.PreviewRoute
import com.apexvelo.ct.feature.navigation.simulator.RideSimulator
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
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

private const val MAP_STYLE_URL =
    "https://tiles.openfreemap.org/styles/liberty"

private const val ROUTE_SOURCE_ID =
    "apex-route-source"

private const val ROUTE_OUTLINE_LAYER_ID =
    "apex-route-outline-layer"

private const val ROUTE_LAYER_ID =
    "apex-route-layer"

private const val RIDER_SOURCE_ID =
    "apex-rider-source"

private const val RIDER_LAYER_ID =
    "apex-rider-layer"

private val initialRiderLocation =
    PreviewRoute.initialLocation

@Composable
fun MapScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
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
                    vertical = 10.dp
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

    val simulatorHolder = remember {
        arrayOfNulls<RideSimulator>(1)
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())

            getMapAsync { map ->
                configureNavigationMap(
                    map = map,
                    onSimulatorCreated = { simulator ->
                        simulatorHolder[0]?.stop()
                        simulatorHolder[0] = simulator
                    }
                )
            }
        }
    }

    DisposableEffect(
        lifecycleOwner,
        mapView
    ) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> Unit
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // The screen may be composed after these lifecycle events already happened.
        // Bring MapView immediately into the Activity's current lifecycle state.
        if (
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        ) {
            mapView.onStart()
        }

        if (
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)
        ) {
            mapView.onResume()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)

            simulatorHolder[0]?.stop()
            simulatorHolder[0] = null

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
    map: MapLibreMap,
    onSimulatorCreated: (RideSimulator) -> Unit
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

    map.cameraPosition =
        CameraPosition.Builder()
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
        Style.Builder().fromUri(
            MAP_STYLE_URL
        )
    ) { style ->
        addPreviewRoute(style)
        addRiderMarker(style)

        val simulator =
            createPreviewRideSimulator(
                map = map,
                style = style
            )

        onSimulatorCreated(simulator)
        simulator.start()
    }
}

private fun addPreviewRoute(
    style: Style
) {
    val coordinates =
        PreviewRoute.points.joinToString(
            separator = ",\n"
        ) { point ->
            "[${point.longitude}, ${point.latitude}]"
        }

    val routeGeoJson = """
        {
          "type": "Feature",
          "properties": {},
          "geometry": {
            "type": "LineString",
            "coordinates": [
              $coordinates
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
            lineCap(
                Property.LINE_CAP_ROUND
            ),
            lineJoin(
                Property.LINE_JOIN_ROUND
            )
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
            lineCap(
                Property.LINE_CAP_ROUND
            ),
            lineJoin(
                Property.LINE_JOIN_ROUND
            )
        )
    )
}

private fun addRiderMarker(
    style: Style
) {
    style.addSource(
        GeoJsonSource(
            RIDER_SOURCE_ID,
            createRiderGeoJson(
                initialRiderLocation
            )
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

private fun createPreviewRideSimulator(
    map: MapLibreMap,
    style: Style
): RideSimulator {
    return RideSimulator(
        route = PreviewRoute.points,
        updateIntervalMillis = 1_500L,
        simulatedSpeedKmh = 38f,
        onFrame = { frame ->
            updateRiderMarker(
                style = style,
                location = LatLng(
                    frame.currentLocation.latitude,
                    frame.currentLocation.longitude
                )
            )
            updateNavigationCamera(
                map = map,
                location = LatLng(
                    frame.currentLocation.latitude,
                    frame.currentLocation.longitude
                ),
                bearing = frame.bearingDegrees
            )
        }
    )
}

private fun updateRiderMarker(
    style: Style,
    location: LatLng
) {
    style.getSourceAs<GeoJsonSource>(
        RIDER_SOURCE_ID
    )?.setGeoJson(
        createRiderGeoJson(location)
    )
}

private fun createRiderGeoJson(
    location: LatLng
): String {
    return """
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
}

private fun updateNavigationCamera(
    map: MapLibreMap,
    location: LatLng,
    bearing: Double
) {
    val cameraPosition =
        CameraPosition.Builder()
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
        CameraUpdateFactory
            .newCameraPosition(
                cameraPosition
            ),
        1_200
    )
}

@Composable
private fun CompactNavigationHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme
            .colorScheme
            .surface
            .copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 9.dp
            ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "250 m",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "TURN LEFT",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(alpha = 0.56f),
                    fontSize = 9.sp
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    text = "18 min",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = "8.2 km remaining",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(alpha = 0.56f),
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
        color = MaterialTheme
            .colorScheme
            .surface
            .copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 12.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "↰",
                color = MaterialTheme
                    .colorScheme
                    .secondary,
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
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "FC Road",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(alpha = 0.60f),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    text = "38",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "km/h",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(alpha = 0.56f),
                    fontSize = 9.sp
                )
            }
        }
    }
}