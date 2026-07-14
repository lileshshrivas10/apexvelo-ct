package com.apexvelo.ct.domain.model

data class NavigationState(
    val currentLocation: GeoPoint,
    val bearingDegrees: Float,
    val speedKmh: Float,

    val nextManeuver: Maneuver,
    val distanceToManeuverMeters: Int,
    val instruction: String,
    val streetName: String?,

    val remainingDistanceMeters: Int,
    val remainingDurationSeconds: Int,

    val route: List<GeoPoint>,
    val isNavigating: Boolean
) {
    companion object {
        fun preview(): NavigationState {
            return NavigationState(
                currentLocation = GeoPoint(
                    latitude = 18.5204,
                    longitude = 73.8567
                ),
                bearingDegrees = 45f,
                speedKmh = 38f,
                nextManeuver = Maneuver.LEFT,
                distanceToManeuverMeters = 250,
                instruction = "Turn left",
                streetName = "FC Road",
                remainingDistanceMeters = 8_200,
                remainingDurationSeconds = 1_080,
                route = listOf(
                    GeoPoint(18.5204, 73.8567),
                    GeoPoint(18.5210, 73.8575),
                    GeoPoint(18.5220, 73.8585),
                    GeoPoint(18.5230, 73.8590)
                ),
                isNavigating = true
            )
        }
    }
}