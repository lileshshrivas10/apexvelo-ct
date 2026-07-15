package com.apexvelo.ct.feature.navigation.protocol

import com.apexvelo.ct.feature.navigation.model.Maneuver
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPacketCodecTest {
    @Test
    fun `encoded packet has documented size and round trips`() {
        val packet = NavigationPacket(
            sequenceNumber = 42,
            latitude = 18.52043,
            longitude = 73.85674,
            bearingDegrees = 359.94,
            speedKmh = 38.5f,
            maneuver = Maneuver.RIGHT,
            distanceToTurnMeters = 125,
            remainingDistanceMeters = 8_240,
            remainingDurationSeconds = 1_080,
            isArrived = false
        )

        val encoded = NavigationPacketCodec.encode(packet)
        val decoded = NavigationPacketCodec.decode(encoded)

        assertEquals(34, encoded.size)
        assertEquals(packet.sequenceNumber, decoded.sequenceNumber)
        assertEquals(packet.latitude, decoded.latitude, 0.0000001)
        assertEquals(packet.longitude, decoded.longitude, 0.0000001)
        assertEquals(359.9, decoded.bearingDegrees, 0.01)
        assertEquals(packet.speedKmh, decoded.speedKmh, 0.01f)
        assertEquals(packet.maneuver, decoded.maneuver)
        assertEquals(packet.distanceToTurnMeters, decoded.distanceToTurnMeters)
        assertEquals(packet.remainingDistanceMeters, decoded.remainingDistanceMeters)
        assertEquals(packet.remainingDurationSeconds, decoded.remainingDurationSeconds)
        assertEquals(packet.isArrived, decoded.isArrived)
    }
}
