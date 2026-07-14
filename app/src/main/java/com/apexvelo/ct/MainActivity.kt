package com.apexvelo.ct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.apexvelo.ct.core.theme.ApexVeloTheme
import com.apexvelo.ct.feature.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApexVeloTheme {
                HomeScreen()
            }
        }
    }
}