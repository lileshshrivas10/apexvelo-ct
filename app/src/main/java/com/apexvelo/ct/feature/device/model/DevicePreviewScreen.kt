package com.apexvelo.ct.feature.device.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexvelo.ct.feature.device.model.DeviceMapFrame
import com.apexvelo.ct.feature.device.model.PreviewDeviceMap
import com.apexvelo.ct.feature.device.renderer.DeviceMiniMapRenderer

private val SimulatorBackground = Color(0xFFE4E5E8)
private val OuterBezel = Color(0xFF30333A)
private val InnerBezel = Color(0xFF08090B)
private val HeaderBackground = Color(0xF707080A)

@Composable
fun DevicePreviewScreen(
    frame: DeviceMapFrame = PreviewDeviceMap.frame
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimulatorBackground)
            .padding(top = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(OuterBezel)
                .padding(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(InnerBezel)
            ) {
                DeviceMiniMapRenderer(
                    frame = frame,
                    modifier = Modifier.fillMaxSize()
                )

                NavigationHeader(
                    frame = frame,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.90f)
                )

                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp),
                    text = "${frame.speedKmh} km/h",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NavigationHeader(
    frame: DeviceMapFrame,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    bottomStart = 70.dp,
                    bottomEnd = 70.dp
                )
            )
            .background(HeaderBackground)
            .padding(
                top = 17.dp,
                bottom = 20.dp,
                start = 20.dp,
                end = 20.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = frame.maneuverSymbol,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "${frame.distanceToTurnMeters} m",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = frame.streetName,
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}