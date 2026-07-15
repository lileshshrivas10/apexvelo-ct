# ApexVelo CT BLE Navigation Protocol

This document defines the Bluetooth Low Energy interface between the ApexVelo CT Android app and the Raspberry Pi display device. It reflects protocol version 1 in the current source code.

## Current implementation status

The Android project currently contains:

- BLE scanning, connection, MTU negotiation, service discovery, and characteristic writes.
- A versioned 34-byte navigation packet encoder and decoder.
- A separate Android BLE peripheral simulator that receives and decodes the same packets.

The live phone-navigation screen is **not yet connected to the BLE packet sender**. Therefore, calculating or starting a real route does not currently transmit navigation frames. Wiring live route/GPS state into `NavigationPacketCodec` and `ApexVeloBleClient.sendNavigationFrame()` is the next integration step.

## BLE roles and GATT configuration

| Item | Configuration |
|---|---|
| Android phone | BLE central and GATT client |
| Raspberry Pi | BLE peripheral and GATT server |
| Advertised service | `8f4e0001-6f3b-4d4b-9a5c-415045585645` |
| Navigation characteristic | `8f4e0002-6f3b-4d4b-9a5c-415045585645` |
| Service type | Primary |
| Characteristic properties | Write and Write Without Response |
| Characteristic permission | Write |
| Android write type | Write Without Response |
| Required ATT MTU | 37 bytes or greater |
| Navigation payload | Exactly 34 bytes |
| Byte order | Big-endian/network order |
| Scan timeout | 10 seconds |

The phone scans by service UUID, not by device name. `ApexVelo` is the intended device-name prefix, but advertising the navigation service UUID is the discovery requirement.

ATT uses three bytes of the negotiated MTU for its write header. An MTU of 37 is therefore required to carry one complete 34-byte frame without application-level fragmentation. The Android client disconnects if MTU negotiation succeeds with a smaller value.

## Version 1 packet layout

All multi-byte values use big-endian byte order. Offsets are zero-based.

| Offset | Size | Type | Field | Encoding and unit |
|---:|---:|---|---|---|
| 0 | 2 | bytes | Signature | ASCII `AV`, bytes `0x41 0x56` |
| 2 | 1 | unsigned byte | Protocol version | `1` |
| 3 | 1 | bit flags | Flags | Bit 0: arrived; bits 1–7 reserved |
| 4 | 4 | signed int32 | Sequence number | Increases for each frame |
| 8 | 4 | signed int32 | Latitude | Degrees multiplied by `10,000,000` |
| 12 | 4 | signed int32 | Longitude | Degrees multiplied by `10,000,000` |
| 16 | 2 | unsigned int16 | Bearing | Degrees multiplied by `10`; range `0–3599` |
| 18 | 2 | unsigned int16 | Speed | km/h multiplied by `10` |
| 20 | 1 | unsigned byte | Maneuver | Code from the maneuver table below |
| 21 | 1 | byte | Reserved | Must be written as `0`; receiver should ignore |
| 22 | 4 | signed int32 | Distance to turn | Metres, non-negative |
| 26 | 4 | signed int32 | Remaining distance | Metres, non-negative |
| 30 | 4 | signed int32 | Remaining duration | Seconds, non-negative |

Packet size is fixed at 34 bytes. A receiver must reject frames with an incorrect size, signature, or unsupported protocol version.

### Maneuver codes

| Code | Meaning |
|---:|---|
| 0 | Straight |
| 1 | Slight left |
| 2 | Left |
| 3 | Sharp left |
| 4 | Slight right |
| 5 | Right |
| 6 | Sharp right |
| 7 | U-turn |
| 8 | Roundabout |
| 9 | Arrive |

The arrived flag is set when the maneuver is `ARRIVE`.

## Data transmitted and omitted

Each navigation frame transmits the rider's current coordinate, bearing, speed, next maneuver, distance to that maneuver, remaining trip distance, remaining trip time, arrival state, and a sequence number.

Version 1 does **not** transmit:

- The complete route polyline or map tiles.
- Starting-point or destination names.
- Street names, despite `streetName` existing in the internal navigation model.
- The next route coordinate, despite `nextLocation` existing internally.
- User identity, phone identifiers, or authentication credentials.
- Checksums, acknowledgements, timestamps, or retransmission requests.

The Raspberry Pi is expected to render navigation from the latest compact state frame. If it needs a full local route/map, that requires a future packet type or a separate transfer channel.

## Raspberry Pi receiver behavior

The Pi should:

1. Advertise the primary navigation service UUID.
2. Expose the navigation characteristic with Write and Write Without Response support.
3. Accept an ATT MTU of at least 37.
4. Require offset `0` and exactly 34 payload bytes.
5. Validate signature and version before decoding.
6. Decode unsigned 16-bit bearing and speed values correctly.
7. Retain the last valid frame and ignore malformed frames.
8. Use the sequence number to detect duplicates, stale packets, or dropped frames.
9. Show an arrival state when flag bit 0 is set.
10. Mark navigation data stale if frames stop arriving. The timeout policy is not yet defined by version 1.

### Python decoding example

This example decodes a received characteristic value. It does not create the BlueZ GATT service itself.

```python
import struct

PACKET_FORMAT = ">2sBBiiiHHBBiii"
PACKET_SIZE = struct.calcsize(PACKET_FORMAT)  # 34

MANEUVERS = (
    "straight", "slight_left", "left", "sharp_left",
    "slight_right", "right", "sharp_right", "u_turn",
    "roundabout", "arrive",
)


def decode_navigation_frame(payload: bytes) -> dict:
    if len(payload) != PACKET_SIZE:
        raise ValueError(f"expected 34 bytes, received {len(payload)}")

    (signature, version, flags, sequence, latitude_e7, longitude_e7,
     bearing_x10, speed_x10, maneuver_code, _reserved,
     distance_to_turn_m, remaining_distance_m,
     remaining_duration_s) = struct.unpack(PACKET_FORMAT, payload)

    if signature != b"AV":
        raise ValueError("invalid ApexVelo signature")
    if version != 1:
        raise ValueError(f"unsupported protocol version: {version}")
    if maneuver_code >= len(MANEUVERS):
        raise ValueError(f"unknown maneuver code: {maneuver_code}")

    return {
        "sequence": sequence,
        "latitude": latitude_e7 / 10_000_000.0,
        "longitude": longitude_e7 / 10_000_000.0,
        "bearing_degrees": bearing_x10 / 10.0,
        "speed_kmh": speed_x10 / 10.0,
        "maneuver": MANEUVERS[maneuver_code],
        "distance_to_turn_m": max(0, distance_to_turn_m),
        "remaining_distance_m": max(0, remaining_distance_m),
        "remaining_duration_s": max(0, remaining_duration_s),
        "arrived": bool(flags & 0x01),
    }
```

## Update frequency and flow control

Version 1 does not yet define a production transmission interval. The in-app ride simulator generates UI frames every 50 ms, but that is not a BLE transport guarantee and should not be treated as the required rate.

Because the characteristic uses Write Without Response, the phone receives no application acknowledgement and must avoid writing faster than the BLE stack can accept. A practical initial target for live navigation is 5–10 frames per second, with coalescing so only the newest state is sent. This is a configuration recommendation, not part of the version 1 wire contract.

## Security and privacy

The current GATT contract does not require pairing, bonding, encryption, or application authentication. Location, speed, and trip progress are sensitive data. Before production use, configure BlueZ and Android to require an encrypted/bonded connection or add authenticated application-level protection and replay handling.

## Source-of-truth files

- Android BLE contract: `app/src/main/java/com/apexvelo/ct/feature/device/connection/ApexVeloBleContract.kt`
- Android BLE client: `app/src/main/java/com/apexvelo/ct/feature/device/connection/ApexVeloBleClient.kt`
- Packet definition: `app/src/main/java/com/apexvelo/ct/feature/navigation/protocol/NavigationPacket.kt`
- Packet codec: `app/src/main/java/com/apexvelo/ct/feature/navigation/protocol/NavigationPacketCodec.kt`
- Receiver reference: `device-simulator/src/main/java/com/apexvelo/ct/simulator/BlePeripheralServer.kt`
- Decoder reference: `device-simulator/src/main/java/com/apexvelo/ct/simulator/SimulatorPacketCodec.kt`

If code and this document disagree, the codec and its unit tests are authoritative until the discrepancy is resolved.
