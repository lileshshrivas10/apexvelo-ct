package com.apexvelo.ct.feature.device.connection

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data object Scanning : BleConnectionState
    data class Connecting(val deviceName: String) : BleConnectionState
    data class Connected(val deviceName: String) : BleConnectionState
    data class Error(val message: String) : BleConnectionState
}
