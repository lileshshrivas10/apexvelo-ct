package com.apexvelo.ct.feature.device.renderer

import org.junit.Assert.assertEquals
import org.junit.Test

class ContinuousBearingTest {
    @Test
    fun `crossing north clockwise uses the short path`() {
        val bearing = ContinuousBearing(359f)

        assertEquals(361f, bearing.update(1f), 0.001f)
    }

    @Test
    fun `crossing north counterclockwise uses the short path`() {
        val bearing = ContinuousBearing(1f)

        assertEquals(-1f, bearing.update(359f), 0.001f)
    }

    @Test
    fun `successive updates remain continuous across multiple crossings`() {
        val bearing = ContinuousBearing(350f)

        assertEquals(370f, bearing.update(10f), 0.001f)
        assertEquals(380f, bearing.update(20f), 0.001f)
        assertEquals(355f, bearing.update(355f), 0.001f)
    }
}
