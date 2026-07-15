package com.apexvelo.ct.feature.device.model

object PreviewDeviceMap {

    val frame = DeviceMapFrame(
        surroundingRoads = listOf(
            // existing roads
        ),
        buildings = emptyList(),
        activeRoute = listOf(
            // existing route
        ),
        riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.80f
        ),
        riderBearingDegrees = 0f,
        maneuverSymbol = "↰",
        distanceToTurnMeters = 250,
        streetName = "FC Road",
        speedKmh = 38
    )
}