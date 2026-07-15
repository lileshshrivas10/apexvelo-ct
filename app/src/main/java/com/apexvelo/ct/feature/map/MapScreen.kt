package com.apexvelo.ct.feature.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

private const val MAX_ROUTE_GPS_DISTANCE_METERS = 2_000.0
private const val EARTH_RADIUS_METERS = 6_371_000.0

private val fallbackMapCenter = LatLng(18.5204, 73.8567)

private enum class SearchField { START, DESTINATION }

@Composable
fun MapScreen(
    connectionStatus: String = "Device disconnected"
) {
    val context = LocalContext.current
    val locationProvider = remember { PhoneLocationProvider(context) }
    var phoneLocation by remember { mutableStateOf<PhoneLocation?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(hasLocationPermission, locationProvider) {
        if (hasLocationPermission) {
            locationProvider.start { phoneLocation = it }
        }
        onDispose { locationProvider.stop() }
    }
    val searchClient = remember { RouteSearchClient() }
    var startQuery by remember { mutableStateOf("") }
    var destinationQuery by remember { mutableStateOf("") }
    var selectedStart by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var selectedDestination by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var activeSearchField by remember { mutableStateOf(SearchField.DESTINATION) }
    var searchResults by remember {
        mutableStateOf<List<PlaceSearchResult>>(emptyList())
    }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var activeRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var calculatedRoute by remember { mutableStateOf<CalculatedRoute?>(null) }
    var isNavigationStarted by remember { mutableStateOf(false) }

    fun selectedStartPoint(): GeoPoint? =
        selectedStart?.location ?: phoneLocation?.position?.let { position ->
            GeoPoint(position.latitude, position.longitude)
        }

    fun calculateRoute(start: GeoPoint, destination: PlaceSearchResult) {
        isSearching = true
        searchResults = emptyList()
        searchMessage = "Calculating motorcycle route…"
        searchClient.route(start, destination.location) { result ->
            isSearching = false
            result.onSuccess { routeResult ->
                calculatedRoute = routeResult
                isNavigationStarted = false
                activeRoute = routeResult.points.map { point ->
                    LatLng(point.latitude, point.longitude)
                }
                searchMessage =
                    "${formatDistance(routeResult.distanceMeters)} • " +
                        formatDuration(routeResult.durationSeconds)
            }.onFailure { error ->
                searchMessage = error.message ?: "Route calculation failed"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {
        NavigationMap(
            modifier = Modifier.fillMaxSize(),
            route = activeRoute,
            phoneLocation = phoneLocation,
            navigationStarted = isNavigationStarted,
            followPhoneLocation = activeRoute.isEmpty() || isNavigationStarted
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
        ) {
            if (!isNavigationStarted) {
                RouteSearchPanel(
                startQuery = startQuery,
                destinationQuery = destinationQuery,
                results = searchResults,
                activeSearchField = activeSearchField,
                message = searchMessage,
                isSearching = isSearching,
                onStartQueryChange = {
                    startQuery = it
                    selectedStart = null
                },
                onDestinationQueryChange = {
                    destinationQuery = it
                    selectedDestination = null
                },
                onUseCurrentLocation = {
                    selectedStart = null
                    startQuery = ""
                    searchResults = emptyList()
                    searchMessage = "Using live phone location"
                },
                onSearch = { field ->
                    val query = if (field == SearchField.START) {
                        startQuery
                    } else {
                        destinationQuery
                    }
                    if (query.isNotBlank()) {
                        activeSearchField = field
                        isSearching = true
                        searchMessage = null
                        searchResults = emptyList()
                        val bias = phoneLocation?.position?.let { position ->
                            GeoPoint(position.latitude, position.longitude)
                        }
                        searchClient.search(query, bias) { result ->
                            isSearching = false
                            result.onSuccess { places ->
                                searchResults = places
                                if (places.isEmpty()) {
                                    searchMessage = "No matching places found"
                                }
                            }.onFailure { error ->
                                searchMessage = error.message ?: "Search failed"
                            }
                        }
                    }
                },
                onResultClick = { place ->
                    searchResults = emptyList()
                    if (activeSearchField == SearchField.START) {
                        selectedStart = place
                        startQuery = place.displayName.substringBefore(',')
                        searchMessage = "Starting point selected"
                    } else {
                        selectedDestination = place
                        destinationQuery = place.displayName.substringBefore(',')
                        searchMessage = "Destination selected"
                    }
                },
                canCalculate = selectedDestination != null && selectedStartPoint() != null,
                routeReady = calculatedRoute != null,
                navigationStarted = isNavigationStarted,
                onCalculateRoute = {
                    val start = selectedStartPoint()
                    val destination = selectedDestination
                    if (start != null && destination != null) {
                        calculateRoute(start, destination)
                    } else {
                        searchMessage = "Select both route points first"
                    }
                },
                onStartRoute = {
                    isNavigationStarted = true
                    searchMessage = "Motorcycle navigation started"
                },
                onEndRoute = {
                    isNavigationStarted = false
                    activeRoute = emptyList()
                    calculatedRoute = null
                    selectedDestination = null
                    destinationQuery = ""
                    searchMessage = "Route ended"
                }
                )

                Spacer(Modifier.height(8.dp))

                CompactNavigationHeader(
                    route = calculatedRoute,
                    connectionStatus = connectionStatus
                )
            } else {
                ActiveNavigationHeader(
                    route = calculatedRoute,
                    onEndRoute = {
                        isNavigationStarted = false
                        activeRoute = emptyList()
                        calculatedRoute = null
                        selectedDestination = null
                        destinationQuery = ""
                        searchMessage = "Route ended"
                    }
                )
            }
        }

        CompactManeuverPanel(
            phoneLocation = phoneLocation,
            hasRoute = activeRoute.isNotEmpty(),
            navigationStarted = isNavigationStarted,
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
    modifier: Modifier = Modifier,
    route: List<LatLng>,
    phoneLocation: PhoneLocation?,
    navigationStarted: Boolean,
    followPhoneLocation: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        val mapOptions =
            MapLibreMapOptions.createFromAttributes(context)
                .textureMode(true)

        MapView(context, mapOptions).apply {
            onCreate(Bundle())

            getMapAsync { map ->
                configureNavigationMap(
                    map = map,
                    route = emptyList(),
                    initialLocation = phoneLocation?.position
                )
            }
        }
    }

    LaunchedEffect(mapView, route, navigationStarted) {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                replaceNavigationRoute(style, route)
                if (route.size >= 2) {
                    if (navigationStarted) {
                        updateRiderMarker(style, route.first())
                    }
                    mapView.post {
                        if (navigationStarted) {
                            updateNavigationCamera(
                                map = map,
                                location = route.first(),
                                bearing = 0.0
                            )
                        } else {
                            fitRouteInView(map, route)
                        }
                    }
                }
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

            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            phoneLocation?.takeIf { followPhoneLocation }?.let { location ->
                val canUseLiveGps = !navigationStarted ||
                    route.isEmpty() ||
                    isLocationNearRoute(location.position, route)
                val displayedPosition = if (canUseLiveGps) {
                    location.position
                } else {
                    route.first()
                }
                val displayedBearing = if (canUseLiveGps) {
                    location.bearingDegrees
                } else {
                    0.0
                }

                view.getMapAsync { map ->
                    map.style?.let { style ->
                        updateRiderMarker(style, displayedPosition)
                    }
                    updateNavigationCamera(
                        map = map,
                        location = displayedPosition,
                        bearing = displayedBearing
                    )
                }
            }
        }
    )
}

private fun isLocationNearRoute(
    location: LatLng,
    route: List<LatLng>
): Boolean = route.minOfOrNull { routePoint ->
    distanceMeters(location, routePoint)
}?.let { distance ->
    distance <= MAX_ROUTE_GPS_DISTANCE_METERS
} ?: false

private fun distanceMeters(
    first: LatLng,
    second: LatLng
): Double {
    val latitudeDelta = Math.toRadians(second.latitude - first.latitude)
    val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
    val firstLatitude = Math.toRadians(first.latitude)
    val secondLatitude = Math.toRadians(second.latitude)
    val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(firstLatitude) * cos(secondLatitude) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)

    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
}

private fun replaceNavigationRoute(
    style: Style,
    route: List<LatLng>
) {
    style.removeLayer(ROUTE_LAYER_ID)
    style.removeLayer(ROUTE_OUTLINE_LAYER_ID)
    style.removeSource(ROUTE_SOURCE_ID)

    if (route.size >= 2) {
        addNavigationRoute(style, route)
    }
}

@Composable
private fun ActiveNavigationHeader(
    route: CalculatedRoute?,
    onEndRoute: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "MOTORCYCLE NAVIGATION",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = route?.let {
                        "${formatDistance(it.distanceMeters)} · Arrive ${formatArrivalTime(it.durationSeconds)}"
                    } ?: "Route active",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(onClick = onEndRoute) {
                Text("END ROUTE")
            }
        }
    }
}

private fun configureNavigationMap(
    map: MapLibreMap,
    route: List<LatLng>,
    initialLocation: LatLng?
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

    map.setStyle(
        Style.Builder().fromUri(
            MAP_STYLE_URL
        )
    ) { style ->
        if (route.size >= 2) {
            addNavigationRoute(style, route)
            fitRouteInView(map, route)
        }
        addRiderMarker(
            style,
            route.firstOrNull() ?: initialLocation ?: fallbackMapCenter
        )

        if (route.size < 2) {
            map.cameraPosition =
                CameraPosition.Builder()
                    .target(initialLocation ?: fallbackMapCenter)
                    .zoom(14.5)
                    .bearing(0.0)
                    .tilt(0.0)
                    .build()
        }
    }
}

private fun fitRouteInView(
    map: MapLibreMap,
    route: List<LatLng>
) {
    val bounds = LatLngBounds.Builder()
        .includes(route)
        .build()

    map.animateCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, 140),
        700
    )
}

private fun addNavigationRoute(
    style: Style,
    route: List<LatLng>
) {
    val routeFeature = Feature.fromGeometry(
        LineString.fromLngLats(
            route.map { point ->
                Point.fromLngLat(point.longitude, point.latitude)
            }
        )
    )

    style.addSource(
        GeoJsonSource(
            ROUTE_SOURCE_ID,
            routeFeature
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
    style: Style,
    initialLocation: LatLng
) {
    style.addSource(
        GeoJsonSource(
            RIDER_SOURCE_ID,
            createRiderGeoJson(
                initialLocation
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
    route: CalculatedRoute?,
    connectionStatus: String,
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
                    text = route?.let { formatDistance(it.distanceMeters) } ?: "No route",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (route == null) "SELECT ROUTE POINTS" else "MOTORCYCLE ROUTE",
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
                    text = route?.let { "Arrive ${formatArrivalTime(it.durationSeconds)}" } ?: "--:--",
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = route?.let {
                        "${formatDuration(it.durationSeconds)} • $connectionStatus"
                    } ?: connectionStatus,
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
    phoneLocation: PhoneLocation?,
    hasRoute: Boolean,
    navigationStarted: Boolean,
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
                text = "\u25B2",
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
                    text = when {
                        navigationStarted -> "Motorcycle navigation active"
                        hasRoute -> "Motorcycle route ready"
                        else -> "Waiting for destination"
                    },
                    color = MaterialTheme
                        .colorScheme
                        .onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (phoneLocation == null) "Waiting for GPS location" else "Live phone GPS",
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
                    text = phoneLocation?.speedKmh?.toInt()?.toString() ?: "--",
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

@Composable
private fun RouteSearchPanel(
    startQuery: String,
    destinationQuery: String,
    results: List<PlaceSearchResult>,
    activeSearchField: SearchField,
    message: String?,
    isSearching: Boolean,
    onStartQueryChange: (String) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearch: (SearchField) -> Unit,
    onResultClick: (PlaceSearchResult) -> Unit,
    canCalculate: Boolean,
    routeReady: Boolean,
    navigationStarted: Boolean,
    onCalculateRoute: () -> Unit,
    onStartRoute: () -> Unit,
    onEndRoute: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startQuery,
                    onValueChange = onStartQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Starting point") },
                    placeholder = { Text("Current location") },
                    singleLine = true
                )
                Button(
                    onClick = { onSearch(SearchField.START) },
                    enabled = startQuery.isNotBlank() && !isSearching,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("FIND")
                }
            }

            Text(
                text = "Use phone GPS",
                modifier = Modifier
                    .clickable(onClick = onUseCurrentLocation)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = destinationQuery,
                    onValueChange = onDestinationQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Destination") },
                    placeholder = { Text("Search destination") },
                    singleLine = true
                )
                Button(
                    onClick = { onSearch(SearchField.DESTINATION) },
                    enabled = destinationQuery.isNotBlank() && !isSearching,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (isSearching) "…" else "FIND")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCalculateRoute,
                    enabled = canCalculate && !isSearching,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CALCULATE ROUTE")
                }

                Button(
                    onClick = if (navigationStarted) onEndRoute else onStartRoute,
                    enabled = routeReady,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (navigationStarted) "END ROUTE" else "START ROUTE")
                }
            }

            message?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }

            if (results.isNotEmpty()) {
                Text(
                    text = if (activeSearchField == SearchField.START) {
                        "Choose starting point"
                    } else {
                        "Choose destination"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            results.take(3).forEach { place ->
                Text(
                    text = place.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(place) }
                        .padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }

            Text(
                text = "Search: Photon/OSM · Routes: Valhalla/OSM",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 9.sp
            )
        }
    }
}

private fun formatDuration(seconds: Int): String =
    "${(seconds / 60).coerceAtLeast(0)} min"

private fun formatArrivalTime(seconds: Int): String =
    java.time.LocalTime.now()
        .plusSeconds(seconds.toLong())
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

private fun formatDistance(meters: Int): String =
    if (meters >= 1_000) {
        String.format("%.1f km remaining", meters / 1_000f)
    } else {
        "$meters m remaining"
    }
