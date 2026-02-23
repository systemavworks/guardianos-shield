package com.guardianos.shield.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.billing.BillingManager

@Composable
fun PremiumPurchaseScreen(
    billingManager: BillingManager,
    isPremium: Boolean,
    onPurchaseSuccess: () -> Unit,
    activity: Activity,
    onDismiss: (() -> Unit)? = null
) {
    var loading by remember { mutableStateOf(false) }
    var billingError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val buttonText = if (isPremium) "¡Ya eres premium!" else "Desbloquear por 14,99 €"

    val version = "v1.1.0 Build 20260220"
    val year = "2026"
    val context = LocalContext.current
    if (billingError) {
        LaunchedEffect(billingError) {
            snackbarHostState.showSnackbar(
                message = "El pago estará disponible próximamente en Google Play.",
                duration = SnackbarDuration.Long
            )
            billingError = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .navigationBarsPadding()
            .padding(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberVectorPainter(Icons.Rounded.Star),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Versión Premium",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sin anuncios · Sin suscripción · Pago único vitalicio",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // ── Comparativa Free vs Premium ──────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Cabecera
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Función",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Free",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFFFFC107)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Premium",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        // label, inFree, freeIsLimited (naranja ⚠️ si está pero limitado)
                        data class FeatureRow(val label: String, val inFree: Boolean, val freeIsLimited: Boolean = false)
                        val features = listOf(
                            FeatureRow("DNS activo — datos registrados 48h", inFree = true, freeIsLimited = true),
                            FeatureRow("DNS — historial completo 30 días", inFree = false),
                            FeatureRow("Historial últimas 48h", inFree = true, freeIsLimited = true),
                            FeatureRow("Historial 30 días completo", inFree = false),
                            FeatureRow("Sin exportar historial", inFree = true, freeIsLimited = true),
                            FeatureRow("Exportar historial CSV", inFree = false),
                            FeatureRow("Monitoreo apps — datos últimas 48h", inFree = true, freeIsLimited = true),
                            FeatureRow("Monitoreo apps — datos 30 días", inFree = false),
                            FeatureRow("Filtros personalizados no disponibles", inFree = false),
                            FeatureRow("Filtros personalizados ilimitados", inFree = false),
                            FeatureRow("Protección adulto/malware/redes sociales (48h)", inFree = true, freeIsLimited = true),
                            FeatureRow("Protección completa con estadísticas 30d", inFree = false),
                            FeatureRow("Modo parental no disponible", inFree = false),
                            FeatureRow("Modo parental completo", inFree = false),
                            FeatureRow("Horarios por día no disponibles", inFree = false),
                            FeatureRow("Horarios avanzados por día", inFree = false),
                            FeatureRow("Múltiples perfiles (hasta 10)", inFree = false),
                            FeatureRow("PIN parental anti-bypass", inFree = false),
                            FeatureRow("Alertas push parentales", inFree = false)
                        )
                        features.forEach { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = f.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = if (f.freeIsLimited)
                                        androidx.compose.ui.graphics.Color(0xFFE65100)
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                // Columna FREE
                                Icon(
                                    imageVector = if (f.inFree)
                                        androidx.compose.material.icons.Icons.Rounded.CheckCircle
                                    else
                                        androidx.compose.material.icons.Icons.Rounded.Cancel,
                                    contentDescription = null,
                                    tint = when {
                                        f.inFree && f.freeIsLimited -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                        f.inFree -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                // Columna PREMIUM
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Naranja = disponible con limitaciones FREE · Gris = no disponible en FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!isPremium) {
                            loading = true
                            billingManager.launchPurchaseFlow(
                                activity = activity,
                                onError = {
                                    loading = false
                                    billingError = true
                                }
                            )
                        }
                    },
                    enabled = !isPremium && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (loading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                if (isPremium) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "¡Gracias por apoyar Guardianos Shield!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Operación 100% segura y gestionada por Google Play. Guardianos Shield cumple con la legislación vigente y protege tu privacidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                val urlWeb      = "https://guardianos.es"
                val urlPrivacy  = "https://guardianos.es/politica-privacidad"
                val urlTerminos = "https://guardianos.es/shield/terminos"
                val email       = "info@guardianos.es"
                val linkColor   = MaterialTheme.colorScheme.primary
                // Fila de enlaces: web · T&C · privacidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌐 guardianos.es",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWeb)))
                        }
                    )
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Privacidad",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlPrivacy)))
                        }
                    )
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "T&C",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTerminos)))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Enlace de email
                Text(
                    text = "📧 $email",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center,
                        color = linkColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                            }
                            context.startActivity(intent)
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Versión $version  •  Guardianos Shield  •  © $year",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                // Botón para continuar sin comprar (modelo freemium)
                if (onDismiss != null && !isPremium) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Continuar con la versión gratuita",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    } // Column
    } // Scaffold
}
