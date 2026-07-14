package com.apexvelo.ct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.apexvelo.ct.core.theme.ApexVeloTheme
import com.apexvelo.ct.feature.home.HomeScreen
import com.apexvelo.ct.feature.map.MapScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApexVeloTheme {
                var showMap by rememberSaveable {
                    mutableStateOf(false)
                }

                if (showMap) {
                    MapScreen()
                } else {
                    HomeScreen(
                        onStartClick = {
                            showMap = true
                        }
                    )
                }
            }
        }
    }
}