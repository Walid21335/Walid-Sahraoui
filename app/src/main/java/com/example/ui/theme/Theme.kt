package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AcademicBluePrimary,
    secondary = AcademicTealSecondary,
    tertiary = LightAccentBlue,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = OnDarkText,
    onSurface = OnDarkText,
    primaryContainer = CardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicBluePrimary,
    secondary = AcademicTealSecondary,
    tertiary = LightAccentBlue,
    background = LightBg,
    surface = LightSurface,
    onBackground = OnLightText,
    onSurface = OnLightText,
    primaryContainer = CardBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors so our custom academic theme takes full precedence
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
