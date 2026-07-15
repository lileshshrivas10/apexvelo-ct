package com.apexvelo.ct.feature.device.model

object PreviewDeviceMap {

    private val previewRoute = listOf(
        NormalizedPoint(0.50f, 0.80f),
        NormalizedPoint(0.49f, 0.68f),
        NormalizedPoint(0.48f, 0.56f),
        NormalizedPoint(0.55f, 0.45f),
        NormalizedPoint(0.68f, 0.38f),
        NormalizedPoint(0.76f, 0.29f),
        NormalizedPoint(0.79f, 0.17f)
    )

    val frame = DeviceMapFrame(
        surroundingRoads = emptyList(),
        buildings = emptyList(),
        activeRoute = previewRoute,
        riderPosition = NormalizedPoint(0.50f, 0.80f),
        destinationPosition = previewRoute.last(),
        riderBearingDegrees = 0f,
        maneuverSymbol = "↰",
        distanceToTurnMeters = 250,
        streetName = "FC Road",
        speedKmh = 38,
        isArrived = false
    )
}