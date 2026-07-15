package com.apexvelo.ct.simulator

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class SimulatorManeuver {
    STRAIGHT, SLIGHT_LEFT, LEFT, SHARP_LEFT, SLIGHT_RIGHT,
    RIGHT, SHARP_RIGHT, U_TURN, ROUNDABOUT, ARRIVE
}

data class SimulatorNavigationPacket(
    val sequenceNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Double,
    val speedKmh: Float,
    val maneuver: SimulatorManeuver,
    val distanceToTurnMeters: Int,
    val remainingDistanceMeters: Int,
    val remainingDurationSeconds: Int,
    val isArrived: Boolean
)

object SimulatorPacketCodec {
    private const val PACKET_SIZE = 34
    private const val MAGIC_1: Byte = 0x41
    private const val MAGIC_2: Byte = 0x56
    private const val VERSION: Byte = 1

    fun decode(bytes: ByteArray): SimulatorNavigationPacket {
        require(bytes.size == PACKET_SIZE) { "Expected a 34-byte frame" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        require(buffer.get() == MAGIC_1 && buffer.get() == MAGIC_2) {
            "Invalid packet signature"
        }
        require(buffer.get() == VERSION) { "Unsupported protocol version" }

        val flags = buffer.get().toInt() and 0xFF
        val sequence = buffer.int
        val latitude = buffer.int / 10_000_000.0
        val longitude = buffer.int / 10_000_000.0
        val bearing = (buffer.short.toInt() and 0xFFFF) / 10.0
        val speed = (buffer.short.toInt() and 0xFFFF) / 10.0f
        val maneuverCode = buffer.get().toInt() and 0xFF
        buffer.get() // Reserved.

        require(maneuverCode in SimulatorManeuver.entries.indices) {
            "Unknown maneuver code"
        }

        return SimulatorNavigationPacket(
            sequenceNumber = sequence,
            latitude = latitude,
            longitude = longitude,
            bearingDegrees = bearing,
            speedKmh = speed,
            maneuver = SimulatorManeuver.entries[maneuverCode],
            distanceToTurnMeters = buffer.int,
            remainingDistanceMeters = buffer.int,
            remainingDurationSeconds = buffer.int,
            isArrived = flags and 1 != 0
        )
    }
}
