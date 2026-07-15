package com.apexvelo.ct.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartClick: () -> Unit,
    onDevicePreviewClick: () -> Unit,
    connectionStatus: String,
    isDeviceConnected: Boolean,
    onConnectDeviceClick: () -> Unit,
    onDisconnectDeviceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ApexVelo CT",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Compact Navigation.\nMaximum Focus.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onStartClick
        ) {
            Text("PHONE NAVIGATION")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDevicePreviewClick
        ) {
            Text("DEVICE PREVIEW")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = connectionStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = if (isDeviceConnected) {
                onDisconnectDeviceClick
            } else {
                onConnectDeviceClick
            }
        ) {
            Text(
                if (isDeviceConnected) "DISCONNECT DEVICE"
                else "CONNECT DEVICE"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Companion Technology",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
