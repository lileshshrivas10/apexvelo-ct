package com.apexvelo.ct.feature.device.model

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

enum class RoadType {
    PRIMARY,
    SECONDARY
}

data class RoadPolyline(
    val points: List<NormalizedPoint>,
    val type: RoadType = RoadType.SECONDARY
)

data class DeviceMapFrame(
    val surroundingRoads: List<RoadPolyline>,
    val activeRoute: List<NormalizedPoint>,
    val riderPosition: NormalizedPoint,
    val riderBearingDegrees: Float,
    val maneuverSymbol: String,
    val distanceToTurnMeters: Int,
    val streetName: String,
    val speedKmh: Int
)