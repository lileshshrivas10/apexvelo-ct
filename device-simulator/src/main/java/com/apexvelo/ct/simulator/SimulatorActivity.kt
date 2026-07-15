package com.apexvelo.ct.simulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SimulatorActivity : ComponentActivity() {
    private var status by mutableStateOf("Simulator stopped")
    private var packet by mutableStateOf<SimulatorNavigationPacket?>(null)

    private val server by lazy {
        BlePeripheralServer(
            context = applicationContext,
            onStatus = { runOnUiThread { status = it } },
            onPacket = { runOnUiThread { packet = it } }
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) server.start()
        else status = "Bluetooth permissions are required"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D4FF),
                    background = Color(0xFF08090B),
                    surface = Color(0xFF18181F)
                )
            ) {
                SimulatorScreen(
                    status = status,
                    packet = packet,
                    onStart = ::startSimulator
                )
            }
        }
    }

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }

    private fun startSimulator() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            server.start()
            return
        }
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) server.start()
        else permissionLauncher.launch(missing.toTypedArray())
    }
}

@androidx.compose.runtime.Composable
private fun SimulatorScreen(
    status: String,
    packet: SimulatorNavigationPacket?,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF08090B)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("APEXVELO DEVICE SIMULATOR", color = Color(0xFF00D4FF), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(Color(0xFF111318)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 70.dp, horizontal = 38.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(packet?.maneuver?.symbol() ?: "\u2191", fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text(
                    packet?.let { "${it.distanceToTurnMeters} m" } ?: "Waiting",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(30.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(packet?.let { "${it.speedKmh.toInt()} km/h" } ?: "-- km/h")
                    Text(packet?.let { "${it.remainingDistanceMeters} m left" } ?: "-- m left")
                }
                packet?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Frame #${it.sequenceNumber}", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(status, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStart) { Text("START BLE SIMULATOR") }
    }
}

private fun SimulatorManeuver.symbol(): String = when (this) {
    SimulatorManeuver.STRAIGHT -> "\u2191"
    SimulatorManeuver.SLIGHT_LEFT -> "\u2196"
    SimulatorManeuver.LEFT, SimulatorManeuver.SHARP_LEFT -> "\u21B0"
    SimulatorManeuver.SLIGHT_RIGHT -> "\u2197"
    SimulatorManeuver.RIGHT, SimulatorManeuver.SHARP_RIGHT -> "\u21B1"
    SimulatorManeuver.U_TURN -> "\u21B6"
    SimulatorManeuver.ROUNDABOUT -> "\u27F3"
    SimulatorManeuver.ARRIVE -> "\u2691"
}
