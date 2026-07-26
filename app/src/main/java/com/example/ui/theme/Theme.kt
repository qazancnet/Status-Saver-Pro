package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.Black,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = SecondaryColor,
    onSecondary = Color.Black,
    background = VibrantDarkBg,
    onBackground = VibrantDarkOnSurface,
    surface = VibrantDarkSurface,
    onSurface = VibrantDarkOnSurface,
    surfaceVariant = VibrantDarkContainer,
    onSurfaceVariant = VibrantDarkMutedText,
    error = Color(0xFFEA0038)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = VibrantLightAccentBg,
    onPrimaryContainer = VibrantLightAccentText,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    background = VibrantLightBg,
    onBackground = VibrantLightOnSurface,
    surface = VibrantLightSurface,
    onSurface = VibrantLightOnSurface,
    surfaceVariant = VibrantLightContainer,
    onSurfaceVariant = VibrantLightMutedText,
    error = Color(0xFFEA0038)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Allow dynamic theme overrides (Material You on Android 12+) or stick to styled branding
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
