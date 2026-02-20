package com.guardianos.shield.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * Banner de cuenta atrás del trial gratuito de 48h.
 * Visible en HomeScreen mientras el trial está activo y el usuario no es premium.
 */
@Composable
fun FreeTrialBanner(
    remainingHours: Float,
    onUpgrade: () -> Unit
) {
    val hoursInt = floor(remainingHours).toInt()
    val minutesInt = ((remainingHours - hoursInt) * 60).toInt()
    val tiempoTexto = when {
        hoursInt > 0 -> "${hoursInt}h ${minutesInt}min restantes"
        else         -> "${minutesInt}min restantes"
    }

    // Color progresivo: verde→naranja→rojo según tiempo restante
    val containerColor = when {
        remainingHours > 24 -> Color(0xFFF9FBE7) // amarillo suave
        remainingHours > 8  -> Color(0xFFFFF3E0) // naranja suave
        else                -> Color(0xFFFFEBEE) // rojo suave
    }
    val accentColor = when {
        remainingHours > 24 -> Color(0xFF558B2F)
        remainingHours > 8  -> Color(0xFFE65100)
        else                -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Modo Free — $tiempoTexto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentColor
                    )
                    Text(
                        text = "Actualiza para protección ilimitada",
                        fontSize = 11.sp,
                        color = accentColor.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color(0xFF1A1200)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
