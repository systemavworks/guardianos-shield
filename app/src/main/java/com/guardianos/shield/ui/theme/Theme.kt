// app/src/main/java/com/guardianos/shield/ui/theme/Theme.kt
package com.guardianos.shield.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta Cybersecurity Professional
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D9FF),      // Cyan brillante
    secondary = Color(0xFF00FF88),     // Verde neón
    tertiary = Color(0xFFFFAA00),      // Naranja
    background = Color(0xFF0A0E27),    // Navy oscuro
    surface = Color(0xFF0F1629),       // Azul profundo
    error = Color(0xFFFF0044),         // Rojo amenaza
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),
    secondary = Color(0xFF00AA55),
    tertiary = Color(0xFFCC8800),
    background = Color(0xFFF0F4FF),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFCC0033),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun GuardianShieldTheme(
    darkTheme: Boolean = true, // Forzamos modo oscuro por defecto
    // dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
