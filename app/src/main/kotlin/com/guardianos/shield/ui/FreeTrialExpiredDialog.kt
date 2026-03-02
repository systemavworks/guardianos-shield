package com.guardianos.shield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Diálogo de pantalla completa que aparece cuando el trial gratuito de 48h ha expirado.
 * El usuario solo puede actualizar a Premium o salir de la app.
 * La VPN se detiene antes de mostrarlo (desde MainActivity).
 */
@Composable
fun FreeTrialExpiredDialog(
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit // "Salir de la aplicación"
) {
    Dialog(
        onDismissRequest = { /* No se puede cerrar tocando fuera */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(24.dp))

                // Icono central
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = Color(0xFFFFC107).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏰", fontSize = 44.sp)
                }

                Spacer(Modifier.height(20.dp))

                // Título
                Text(
                    text = "¡Tu prueba gratuita ha terminado!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Has disfrutado 48 horas de protección gratuita. Mejora a Premium para seguir protegiendo a tu familia.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // Tarjeta de limitaciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estado actual (plan gratuito expirado):",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        _StatusRow(icon = "❌", text = "Protección VPN desactivada")
                        _StatusRow(icon = "❌", text = "Control Parental no disponible")
                        _StatusRow(icon = "❌", text = "Horarios avanzados no disponibles")
                        _StatusRow(icon = "❌", text = "Filtros de dominios no disponibles")
                        _StatusRow(icon = "❌", text = "Historial — sin acceso")
                        _StatusRow(icon = "❌", text = "Monitorización de apps — sin acceso")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tarjeta de lo que incluye Premium
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.12f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Con Premium obtienes:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF5D4100)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        _StatusRow(icon = "✅", text = "Protección VPN y DNS 24/7")
                        _StatusRow(icon = "✅", text = "Control Parental completo")
                        _StatusRow(icon = "✅", text = "Horarios avanzados por día")
                        _StatusRow(icon = "✅", text = "Filtros personalizados ilimitados")
                        _StatusRow(icon = "✅", text = "Historial y estadísticas de 30 días")
                        _StatusRow(icon = "✅", text = "Monitorización de apps en tiempo real")
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Botón principal — Upgrade
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color(0xFF1A1200)
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Desbloquear Premium — 14,99 €",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Salir de la aplicación",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun _StatusRow(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
