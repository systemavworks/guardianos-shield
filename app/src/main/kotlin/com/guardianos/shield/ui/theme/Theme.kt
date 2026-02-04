// app/src/main/java/com/guardianos/shield/ui/theme/Theme.kt
package com.guardianos.shield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta ciberseguridad: fondo negro profundo + acento técnico azul
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1976D2),      // Azul oscuro Material (más legible)
    onPrimary = Color(0xFFFFFFFF),    // Blanco sobre azul oscuro → mejor contraste
    primaryContainer = Color(0xFF001F2B),
    onPrimaryContainer = Color(0xFFA6E8FF),

    secondary = Color(0xFF9E9E9E),    // Gris neutro
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFC7C7C7),

    tertiary = Color(0xFFCE93D8),     // Púrpura para diversidad visual
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF210029),
    onTertiaryContainer = Color(0xFFF0B8FF),

    background = Color(0xFF0A0A0A),   // Fondo global (negro profundo)
    onBackground = Color(0xFFE0E0E0), // Texto principal claro

    surface = Color(0xFF121212),      // Superficies (tarjetas)
    onSurface = Color(0xFFE0E0E0),    // Texto en superficies

    surfaceVariant = Color(0xFF1E1E1E), // Variantes de superficie (campos de entrada)
    onSurfaceVariant = Color(0xFFB0B0B0), // Texto secundario

    outline = Color(0xFF606060),      // Bordes sutiles

    error = Color(0xFFD32F2F),        // Rojo profundo para errores
    onError = Color(0xFFFFFFFF),      // Blanco sobre rojo
    errorContainer = Color(0xFF3A0000),
    onErrorContainer = Color(0xFFFFB4AB)
)

// Tipografía accesible: tamaños y colores optimizados
val Typography = Typography(
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Color(0xFFE0E0E0) // Blanco suave
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
        color = Color(0xFFE0E0E0)
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 28.sp,
        color = Color(0xFFE0E0E0)
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFB0B0B0) // Gris claro para etiquetas
    )
)

@Composable
fun GuardianShieldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
