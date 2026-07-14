package com.apexvelo.ct.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ApexDarkColorScheme = darkColorScheme(
    primary = ApexPrimary,
    secondary = ApexSecondary,

    background = Background,
    surface = Surface,

    onPrimary = TextPrimary,
    onSecondary = TextPrimary,

    onBackground = TextPrimary,
    onSurface = TextPrimary,

    error = Error
)

@Composable
fun ApexVeloTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = ApexTypography,
        content = content
    )
}