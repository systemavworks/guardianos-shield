// app/src/main/java/com/guardianos/shield/ui/theme/Theme.kt
package com.guardianos.shield.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colores personalizados para GuardianOS Shield
private val GuardianBlue = Color(0xFF2196F3)
private val GuardianDarkBlue = Color(0xFF1976D2)
private val GuardianLightBlue = Color(0xFFBBDEFB)
private val SafeGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFF44336)

private val DarkColorScheme = darkColorScheme(
    primary = GuardianBlue,
    secondary = SafeGreen,
    tertiary = GuardianLightBlue,
    error = DangerRed,
    primaryContainer = GuardianDarkBlue,
    secondaryContainer = Color(0xFF2E7D32)
)

private val LightColorScheme = lightColorScheme(
    primary = GuardianBlue,
    secondary = SafeGreen,
    tertiary = GuardianDarkBlue,
    error = DangerRed,
    primaryContainer = GuardianLightBlue,
    secondaryContainer = Color(0xFFC8E6C9),
    
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun GuardianShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// app/src/main/java/com/guardianos/shield/ui/theme/Type.kt
val Typography = Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
)
