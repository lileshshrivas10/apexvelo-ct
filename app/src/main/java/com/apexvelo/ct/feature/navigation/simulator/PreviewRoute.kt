package com.apexvelo.ct.feature.navigation.simulator

import org.maplibre.android.geometry.LatLng

object PreviewRoute {

    val points: List<LatLng> = listOf(
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

    val initialLocation: LatLng
        get() = points.first()
}