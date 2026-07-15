package com.apexvelo.ct.feature.device.model

object PreviewDeviceMap {

    private val previewRoute: List<NormalizedPoint> = listOf(
        NormalizedPoint(0.50f, 0.80f),
        NormalizedPoint(0.49f, 0.68f),
        NormalizedPoint(0.48f, 0.56f),
        NormalizedPoint(0.55f, 0.45f),
        NormalizedPoint(0.68f, 0.38f),
        NormalizedPoint(0.76f, 0.29f),
        NormalizedPoint(0.79f, 0.17f)
    )

    private val previewRoads: List<RoadPolyline> = listOf(
        RoadPolyline(
            type = RoadType.PRIMARY,
            points = previewRoute
        ),
        RoadPolyline(
            type = RoadType.SECONDARY,
            points = listOf(
                NormalizedPoint(0.48f, 0.56f),
                NormalizedPoint(0.28f, 0.52f),
                NormalizedPoint(0.10f, 0.45f)
            )
        ),
        RoadPolyline(
            type = RoadType.SECONDARY,
            points = listOf(
                NormalizedPoint(0.55f, 0.45f),
                NormalizedPoint(0.72f, 0.49f),
                NormalizedPoint(0.91f, 0.46f)
            )
        )
    )

    private val previewBuildings: List<BuildingPolygon> = listOf(
        createBuilding(
            centerX = 0.26f,
            centerY = 0.38f,
            width = 0.14f,
            height = 0.08f
        ),
        createBuilding(
            centerX = 0.72f,
            centerY = 0.55f,
            width = 0.15f,
            height = 0.09f
        ),
        createBuilding(
            centerX = 0.28f,
            centerY = 0.69f,
            width = 0.12f,
            height = 0.07f
        )
    )

    val frame: DeviceMapFrame = DeviceMapFrame(
        surroundingRoads = previewRoads,
        buildings = previewBuildings,
        activeRoute = previewRoute,
        riderPosition = NormalizedPoint(
            x = 0.50f,
            y = 0.80f
        ),
        destinationPosition = previewRoute.last(),
        riderBearingDegrees = 0f,
        maneuverSymbol = "↰",
        distanceToTurnMeters = 250,
        streetName = "FC Road",
        speedKmh = 38,
        isArrived = false
    )

    private fun createBuilding(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float
    ): BuildingPolygon {
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        return BuildingPolygon(
            points = listOf(
                NormalizedPoint(
                    x = centerX - halfWidth,
                    y = centerY - halfHeight
                ),
                NormalizedPoint(
                    x = centerX + halfWidth,
                    y = centerY - halfHeight
                ),
                NormalizedPoint(
                    x = centerX + halfWidth,
                    y = centerY + halfHeight
                ),
                NormalizedPoint(
                    x = centerX - halfWidth,
                    y = centerY + halfHeight
                )
            )
        )
    }
}