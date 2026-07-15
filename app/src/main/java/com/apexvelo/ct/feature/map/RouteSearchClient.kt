package com.apexvelo.ct.feature.map

import android.os.Handler
import android.os.Looper
import com.apexvelo.ct.feature.navigation.model.GeoPoint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

data class PlaceSearchResult(
    val displayName: String,
    val location: GeoPoint
)

data class CalculatedRoute(
    val points: List<GeoPoint>,
    val distanceMeters: Int,
    val durationSeconds: Int
)

class RouteSearchClient(
    private val geocodingBaseUrl: String = "https://photon.komoot.io",
    private val routingBaseUrl: String = "https://valhalla1.openstreetmap.de"
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun search(
        query: String,
        locationBias: GeoPoint? = null,
        onResult: (Result<List<PlaceSearchResult>>) -> Unit
    ) {
        executor.execute {
            val result = runCatching {
                val encodedQuery = URLEncoder.encode(
                    query.trim(),
                    StandardCharsets.UTF_8.name()
                )
                val bias = locationBias?.let {
                    "&lat=${it.latitude}&lon=${it.longitude}"
                }.orEmpty()
                val json = request("$geocodingBaseUrl/api/?q=$encodedQuery&limit=5$bias")
                val items = JSONObject(json).getJSONArray("features")
                List(items.length()) { index ->
                    val item = items.getJSONObject(index)
                    val properties = item.getJSONObject("properties")
                    val coordinate = item
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")
                    val displayParts = listOfNotNull(
                        properties.optString("name").takeIf(String::isNotBlank),
                        properties.optString("street").takeIf(String::isNotBlank),
                        properties.optString("city").takeIf(String::isNotBlank),
                        properties.optString("state").takeIf(String::isNotBlank),
                        properties.optString("country").takeIf(String::isNotBlank)
                    ).distinct()
                    PlaceSearchResult(
                        displayName = displayParts.joinToString(", "),
                        location = GeoPoint(
                            latitude = coordinate.getDouble(1),
                            longitude = coordinate.getDouble(0)
                        )
                    )
                }
            }
            mainHandler.post { onResult(result) }
        }
    }

    fun route(
        origin: GeoPoint,
        destination: GeoPoint,
        onResult: (Result<CalculatedRoute>) -> Unit
    ) {
        executor.execute {
            val result = runCatching {
                val requestJson = JSONObject()
                    .put(
                        "locations",
                        JSONArray()
                            .put(JSONObject().put("lat", origin.latitude).put("lon", origin.longitude))
                            .put(JSONObject().put("lat", destination.latitude).put("lon", destination.longitude))
                    )
                    // Standard motor-vehicle road timing is more realistic for
                    // street motorcycles than Valhalla's conservative beta profile.
                    .put("costing", "auto")
                    .put("costing_options", JSONObject().put(
                        "auto",
                        JSONObject().put("use_highways", 0.6)
                    ))
                    .put("format", "osrm")
                    .put("shape_format", "geojson")
                    .put("units", "kilometers")
                val encodedRequest = URLEncoder.encode(
                    requestJson.toString(),
                    StandardCharsets.UTF_8.name()
                )
                val json = request(
                    "$routingBaseUrl/route?json=$encodedRequest"
                )
                val root = JSONObject(json)
                check(root.optString("code", "Ok") == "Ok") {
                    root.optString("message", "No route was found")
                }
                val route = root.getJSONArray("routes").getJSONObject(0)
                val coordinatesJson = route
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")

                CalculatedRoute(
                    points = List(coordinatesJson.length()) { index ->
                        val coordinate = coordinatesJson.getJSONArray(index)
                        GeoPoint(
                            latitude = coordinate.getDouble(1),
                            longitude = coordinate.getDouble(0)
                        )
                    },
                    distanceMeters = route.getDouble("distance").toInt(),
                    durationSeconds = route.getDouble("duration").toInt()
                )
            }
            mainHandler.post { onResult(result) }
        }
    }

    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty(
                "User-Agent",
                "ApexVeloCT/1.0 (Android navigation prototype)"
            )
            check(connection.responseCode in 200..299) {
                "Map service returned HTTP ${connection.responseCode}"
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
