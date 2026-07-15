package com.apexvelo.ct.feature.navigation.protocol

import com.apexvelo.ct.feature.navigation.model.Maneuver
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object NavigationPacketCodec {

    const val PACKET_SIZE_BYTES: Int = 34

    private const val MAGIC_BYTE_1: Byte = 0x41
    private const val MAGIC_BYTE_2: Byte = 0x56
    private const val PROTOCOL_VERSION: Byte = 1

    private const val ARRIVED_FLAG: Int = 1

    private const val COORDINATE_SCALE: Double =
        10_000_000.0

    private const val BEARING_SCALE: Double =
        10.0

    private const val SPEED_SCALE: Float =
        10.0f

    fun encode(
        packet: NavigationPacket
    ): ByteArray {
        val buffer: ByteBuffer = ByteBuffer
            .allocate(PACKET_SIZE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)

        val flags: Int =
            if (packet.isArrived) {
                ARRIVED_FLAG
            } else {
                0
            }

        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(PROTOCOL_VERSION)
        buffer.put(flags.toByte())

        buffer.putInt(packet.sequenceNumber)

        buffer.putInt(
            (packet.latitude * COORDINATE_SCALE)
                .roundToInt()
        )

        buffer.putInt(
            (packet.longitude * COORDINATE_SCALE)
                .roundToInt()
        )

        val encodedBearing: Int = (
                normalizeBearing(packet.bearingDegrees) *
                        BEARING_SCALE
                )
            .roundToInt()
            .coerceIn(0, 3_599)

        buffer.putShort(
            encodedBearing.toShort()
        )

        val encodedSpeed: Int = (
                packet.speedKmh * SPEED_SCALE
                )
            .roundToInt()
            .coerceIn(0, 65_535)

        buffer.putShort(
            encodedSpeed.toShort()
        )

        buffer.put(
            maneuverToCode(packet.maneuver).toByte()
        )

        // Reserved byte for future protocol changes.
        buffer.put(0.toByte())

        buffer.putInt(
            packet.distanceToTurnMeters
                .coerceAtLeast(0)
        )

        buffer.putInt(
            packet.remainingDistanceMeters
                .coerceAtLeast(0)
        )

        buffer.putInt(
            packet.remainingDurationSeconds
                .coerceAtLeast(0)
        )

        return buffer.array()
    }

    fun decode(
        bytes: ByteArray
    ): NavigationPacket {
        require(bytes.size == PACKET_SIZE_BYTES) {
            "Expected $PACKET_SIZE_BYTES bytes, received ${bytes.size}"
        }

        val buffer: ByteBuffer = ByteBuffer
            .wrap(bytes)
            .order(ByteOrder.BIG_ENDIAN)

        val magicByte1: Byte = buffer.get()
        val magicByte2: Byte = buffer.get()

        require(
            magicByte1 == MAGIC_BYTE_1 &&
                    magicByte2 == MAGIC_BYTE_2
        ) {
            "Invalid ApexVelo CT packet signature"
        }

        val protocolVersion: Byte =
            buffer.get()

        require(
            protocolVersion == PROTOCOL_VERSION
        ) {
            "Unsupported protocol version: $protocolVersion"
        }

        val flags: Int =
            buffer.get().toInt() and 0xFF

        val sequenceNumber: Int =
            buffer.int

        val latitude: Double =
            buffer.int.toDouble() /
                    COORDINATE_SCALE

        val longitude: Double =
            buffer.int.toDouble() /
                    COORDINATE_SCALE

        val bearingDegrees: Double =
            unsignedShort(buffer.short)
                .toDouble() /
                    BEARING_SCALE

        val speedKmh: Float =
            unsignedShort(buffer.short)
                .toFloat() /
                    SPEED_SCALE

        val maneuverCode: Int =
            buffer.get().toInt() and 0xFF

        val maneuver: Maneuver =
            codeToManeuver(maneuverCode)

        // Skip reserved byte.
        buffer.get()

        val distanceToTurnMeters: Int =
            buffer.int.coerceAtLeast(0)

        val remainingDistanceMeters: Int =
            buffer.int.coerceAtLeast(0)

        val remainingDurationSeconds: Int =
            buffer.int.coerceAtLeast(0)

        return NavigationPacket(
            sequenceNumber = sequenceNumber,
            latitude = latitude,
            longitude = longitude,
            bearingDegrees = bearingDegrees,
            speedKmh = speedKmh,
            maneuver = maneuver,
            distanceToTurnMeters =
                distanceToTurnMeters,
            remainingDistanceMeters =
                remainingDistanceMeters,
            remainingDurationSeconds =
                remainingDurationSeconds,
            isArrived =
                flags and ARRIVED_FLAG != 0
        )
    }

    private fun normalizeBearing(
        bearingDegrees: Double
    ): Double {
        return (
                bearingDegrees % 360.0 + 360.0
                ) % 360.0
    }

    private fun unsignedShort(
        value: Short
    ): Int {
        return value.toInt() and 0xFFFF
    }

    private fun maneuverToCode(
        maneuver: Maneuver
    ): Int {
        return when (maneuver) {
            Maneuver.STRAIGHT -> 0
            Maneuver.SLIGHT_LEFT -> 1
            Maneuver.LEFT -> 2
            Maneuver.SHARP_LEFT -> 3
            Maneuver.SLIGHT_RIGHT -> 4
            Maneuver.RIGHT -> 5
            Maneuver.SHARP_RIGHT -> 6
            Maneuver.U_TURN -> 7
            Maneuver.ROUNDABOUT -> 8
            Maneuver.ARRIVE -> 9
        }
    }

    private fun codeToManeuver(
        code: Int
    ): Maneuver {
        return when (code) {
            0 -> Maneuver.STRAIGHT
            1 -> Maneuver.SLIGHT_LEFT
            2 -> Maneuver.LEFT
            3 -> Maneuver.SHARP_LEFT
            4 -> Maneuver.SLIGHT_RIGHT
            5 -> Maneuver.RIGHT
            6 -> Maneuver.SHARP_RIGHT
            7 -> Maneuver.U_TURN
            8 -> Maneuver.ROUNDABOUT
            9 -> Maneuver.ARRIVE

            else -> throw IllegalArgumentException(
                "Unknown maneuver code: $code"
            )
        }
    }
}
