package com.apexvelo.ct.feature.device.renderer

/** Keeps bearing updates continuous when the normalized value crosses 0/360 degrees. */
internal class ContinuousBearing(initialBearing: Float) {
    private var lastBearing = initialBearing

    var value: Float = initialBearing
        private set

    fun update(bearing: Float): Float {
        val delta = shortestAngularDelta(
            from = lastBearing,
            to = bearing
        )

        value += delta
        lastBearing = bearing
        return value
    }
}

internal fun shortestAngularDelta(
    from: Float,
    to: Float
): Float {
    var delta = (to - from) % 360f

    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f

    return delta
}
