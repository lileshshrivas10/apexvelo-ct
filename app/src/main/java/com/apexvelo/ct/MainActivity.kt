package com.apexvelo.ct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.apexvelo.ct.core.theme.ApexVeloTheme
import com.apexvelo.ct.feature.device.ui.DevicePreviewScreen
import com.apexvelo.ct.feature.home.HomeScreen
import com.apexvelo.ct.feature.map.MapScreen

private enum class AppScreen {
    HOME,
    PHONE_NAVIGATION,
    DEVICE_PREVIEW
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            }
                        )
                    }

                    AppScreen.PHONE_NAVIGATION -> {
                        MapScreen()
                    }

                    AppScreen.DEVICE_PREVIEW -> {
                        DevicePreviewScreen()
                    }
                }
            }
        }
    }
}