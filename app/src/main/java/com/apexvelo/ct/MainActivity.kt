package com.apexvelo.ct

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.apexvelo.ct.core.theme.ApexVeloTheme
import com.apexvelo.ct.feature.device.ui.DevicePreviewScreen
import com.apexvelo.ct.feature.device.connection.BleConnectionState
import com.apexvelo.ct.feature.home.HomeScreen
import com.apexvelo.ct.feature.map.MapScreen

private enum class AppScreen {
    HOME,
    PHONE_NAVIGATION,
    DEVICE_PREVIEW
}

class MainActivity : ComponentActivity() {

    private var connectionState by mutableStateOf<BleConnectionState>(
        BleConnectionState.Disconnected
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            navigationBleClient.connect()
        } else {
            connectionState = BleConnectionState.Error(
                "Bluetooth permission is required"
            )
        }
    }

    private val navigationBleClient
        get() = (application as ApexVeloApplication).navigationBleClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationBleClient.onStateChanged = { state ->
            connectionState = state
        }

        setContent {
            ApexVeloTheme {
                var currentScreen by rememberSaveable {
                    mutableStateOf(AppScreen.HOME)
                }

                when (currentScreen) {
                    AppScreen.HOME -> {
                        HomeScreen(
                            onStartClick = {
                                currentScreen = AppScreen.PHONE_NAVIGATION
                            },
                            onDevicePreviewClick = {
                                currentScreen = AppScreen.DEVICE_PREVIEW
                            },
                            connectionStatus = connectionState.displayText(),
                            isDeviceConnected =
                                connectionState is BleConnectionState.Connected,
                            onConnectDeviceClick = ::connectDevice,
                            onDisconnectDeviceClick = navigationBleClient::disconnect
                        )
                    }

                    AppScreen.PHONE_NAVIGATION -> {
                        MapScreen(
                            connectionStatus = connectionState.displayText()
                        )
                    }

                    AppScreen.DEVICE_PREVIEW -> {
                        DevicePreviewScreen()
                    }
                }
            }
        }
    }

    private fun connectDevice() {
        val requiredPermissions = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            navigationBleClient.connect()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}

private fun BleConnectionState.displayText(): String = when (this) {
    BleConnectionState.Disconnected -> "Device disconnected"
    BleConnectionState.Scanning -> "Searching for ApexVelo device…"
    is BleConnectionState.Connecting -> "Connecting to $deviceName…"
    is BleConnectionState.Connected -> "Connected to $deviceName"
    is BleConnectionState.Error -> message
}
