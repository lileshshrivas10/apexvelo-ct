package com.apexvelo.ct.feature.device.model

object PreviewDeviceMap {

    val frame = DeviceMapFrame(
        surroundingRoads = listOf(
            RoadPolyline(
                type = RoadType.PRIMARY,
                points = listOf(
                    NormalizedPoint(0.10f, 0.31f),
                    NormalizedPoint(0.23f, 0.36f),
                    NormalizedPoint(0.36f, 0.42f),
                    NormalizedPoint(0.50f, 0.48f),
                    NormalizedPoint(0.65f, 0.50f),
                    NormalizedPoint(0.88f, 0.47f)
                )
            ),
            RoadPolyline(
                type = RoadType.PRIMARY,
                points = listOf(
                    NormalizedPoint(0.49f, 0.91f),
                    NormalizedPoint(0.48f, 0.78f),
                    NormalizedPoint(0.43f, 0.64f),
                    NormalizedPoint(0.44f, 0.52f),
                    NormalizedPoint(0.50f, 0.43f),
                    NormalizedPoint(0.63f, 0.37f),
                    NormalizedPoint(0.72f, 0.28f),
                    NormalizedPoint(0.76f, 0.14f)
                )
            ),
            RoadPolyline(
                points = listOf(
                    NormalizedPoint(0.44f, 0.52f),
                    NormalizedPoint(0.32f, 0.51f),
                    NormalizedPoint(0.19f, 0.47f),
                    NormalizedPoint(0.07f, 0.41f)
                )
            ),
            RoadPolyline(
                points = listOf(
                    NormalizedPoint(0.63f, 0.37f),
                    NormalizedPoint(0.72f, 0.42f),
                    NormalizedPoint(0.83f, 0.45f),
                    NormalizedPoint(0.96f, 0.43f)
                )
            ),
            RoadPolyline(
                points = listOf(
                    NormalizedPoint(0.50f, 0.48f),
                    NormalizedPoint(0.55f, 0.58f),
                    NormalizedPoint(0.65f, 0.66f),
                    NormalizedPoint(0.80f, 0.72f)
                )
            ),
            RoadPolyline(
                points = listOf(
                    NormalizedPoint(0.43f, 0.64f),
                    NormalizedPoint(0.31f, 0.67f),
                    NormalizedPoint(0.18f, 0.75f),
                    NormalizedPoint(0.08f, 0.85f)
                )
            )
        ),
        activeRoute = listOf(
            NormalizedPoint(0.50f, 0.80f),
            NormalizedPoint(0.49f, 0.68f),
            NormalizedPoint(0.48f, 0.56f),
            NormalizedPoint(0.55f, 0.45f),
            NormalizedPoint(0.68f, 0.38f),
            NormalizedPoint(0.76f, 0.29f),
            NormalizedPoint(0.79f, 0.17f)
        ),
        riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.80f
        ),
        riderBearingDegrees = 35f,
        maneuverSymbol = "↰",
        distanceToTurnMeters = 250,
        streetName = "FC Road",
        speedKmh = 38
    )
}