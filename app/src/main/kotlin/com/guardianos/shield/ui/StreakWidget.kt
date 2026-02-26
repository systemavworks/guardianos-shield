package com.guardianos.shield.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.data.UserProfileEntity
import androidx.compose.ui.res.stringResource
import com.guardianos.shield.R

/**
 * StreakWidget — Widget de racha diaria para el dashboard.
 *
 * Muestra:
 * - Racha actual (días consecutivos "limpios")
 * - Récord personal
 * - Badges según hitos alcanzados
 * - Animación de pulso cuando la racha > 0
 */
@Composable
fun StreakWidget(
    perfil: UserProfileEntity?,
    modifier: Modifier = Modifier
) {
    if (perfil == null) return

    val racha = perfil.rachaActual
    val rachaMax = perfil.rachaMaxima

    // Animación de pulso para rachas activas
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
    val escala by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (racha > 0) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (colorFondo, colorTexto, emojiBadge) = when {
        racha >= 30 -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))),
            Color.White,
            "🏆"
        )
        racha >= 14 -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF7E57C2), Color(0xFF512DA8))),
            Color.White,
            "💜"
        )
        racha >= 7 -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0))),
            Color.White,
            "🔵"
        )
        racha >= 3 -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))),
            Color.White,
            "🟢"
        )
        racha > 0 -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF78909C), Color(0xFF37474F))),
            Color.White,
            "⚡"
        )
        else -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF546E7A), Color(0xFF263238))),
            Color.White.copy(alpha = 0.6f),
            "😴"
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (racha > 0) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorFondo)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lado izquierdo: número de racha
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Círculo con número animado
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(if (racha > 0) escala else 1f)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (racha > 0) "$racha" else "0",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = colorTexto,
                                lineHeight = 28.sp
                            )
                            Text(
                                text = if (racha == 1) stringResource(R.string.streak_day_singular) else stringResource(R.string.streak_day_plural),
                                fontSize = 9.sp,
                                color = colorTexto.copy(alpha = 0.8f),
                                lineHeight = 10.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$emojiBadge ${stringResource(R.string.streak_title_active)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTexto
                        )
                        Text(
                            text = when {
                                racha == 0 -> stringResource(R.string.streak_msg_zero)
                                racha == 1 -> stringResource(R.string.streak_msg_day1)
                                racha < 7  -> stringResource(R.string.streak_msg_few, racha)
                                racha < 14 -> stringResource(R.string.streak_msg_week)
                                racha < 30 -> stringResource(R.string.streak_msg_ongoing, racha)
                                else       -> stringResource(R.string.streak_msg_legendary)
                            },
                            fontSize = 12.sp,
                            color = colorTexto.copy(alpha = 0.85f)
                        )
                        if (rachaMax > 0) {
                            Text(
                                text = stringResource(R.string.streak_record, rachaMax),
                                fontSize = 11.sp,
                                color = colorTexto.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Lado derecho: badges de hitos
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BadgeHito(racha = racha, umbral = 3, emoji = "🌱", label = "3d")
                    BadgeHito(racha = racha, umbral = 7, emoji = "🔥", label = "7d")
                    BadgeHito(racha = racha, umbral = 30, emoji = "🏆", label = "30d")
                }
            }
        }
    }
}

@Composable
private fun BadgeHito(racha: Int, umbral: Int, emoji: String, label: String) {
    val conseguido = racha >= umbral
    Surface(
        shape = CircleShape,
        color = if (conseguido) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (conseguido) {
                Text(emoji, fontSize = 16.sp)
            } else {
                Text(
                    label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
