package com.guardianos.shield.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.window.Dialog

// ───────────────────────────────────────────────────────────────────────────────
// Tracker de conversión — analytics locales sin dependencias externas
// Todos los eventos son visibles via: adb logcat | grep ConversionTracker
// ───────────────────────────────────────────────────────────────────────────────

object ConversionTracker {

    private const val TAG = "ConversionTracker"

    /** Contadores en memoria (se resetean al cerrar la app, útiles para sesiones de testing) */
    private val featureTapCounts = mutableMapOf<String, Int>()
    private var upgradeClickCount = 0
    private var purchaseScreenOpenCount = 0
    private var trialExpiredCount = 0
    private var trialExpiredUpgradeClickCount = 0
    private var restorePurchasesClickCount = 0

    /**
     * Registra que el usuario ha tocado una función gateada.
     * Evento clave para medir qué features impulsan más el upgrade.
     */
    fun trackFeatureTapped(feature: PremiumFeature) {
        val count = (featureTapCounts[feature.name] ?: 0) + 1
        featureTapCounts[feature.name] = count
        Log.i(TAG, "[TAP] Función gateada tocada: ${feature.titulo} (total sesion: $count)")
    }

    /**
     * Registra que el usuario ha pulsado "Desbloquear" en un PremiumGateDialog.
     * Mide la intención real de compra desde features específicas.
     */
    fun trackUpgradeClicked(feature: PremiumFeature) {
        upgradeClickCount++
        Log.i(TAG, "[UPGRADE_CLICK] Desde función: ${feature.titulo} (total sesion: $upgradeClickCount)")
    }

    /**
     * Registra apertura de la pantalla de compra (banner, botón directo, etc.).
     */
    fun trackPurchaseScreenOpened(origin: String = "unknown") {
        purchaseScreenOpenCount++
        Log.i(TAG, "[PURCHASE_SCREEN] Abierta desde: $origin (total sesion: $purchaseScreenOpenCount)")
    }

    /**
     * Registra que el trial de 48h ha expirado y se muestra el diálogo bloqueante.
     */
    fun trackTrialExpired() {
        trialExpiredCount++
        Log.i(TAG, "[TRIAL_EXPIRED] Diálogo de expiración mostrado (total sesion: $trialExpiredCount)")
    }

    /**
     * Registra que el usuario pulsó "Upgrade" desde el diálogo de trial expirado.
     */
    fun trackTrialExpiredUpgradeClicked() {
        trialExpiredUpgradeClickCount++
        Log.i(TAG, "[TRIAL_EXPIRED_UPGRADE] Usuario pulsó Upgrade desde diálogo de expiración (total sesion: $trialExpiredUpgradeClickCount)")
    }

    /**
     * Registra que el usuario usó "Restaurar compras" desde Ajustes.
     */
    fun trackRestorePurchasesTapped() {
        restorePurchasesClickCount++
        Log.i(TAG, "[RESTORE_PURCHASES] Usuario intentó restaurar compras (total sesion: $restorePurchasesClickCount)")
    }

    /**
     * Registra una compra exitosa. El evento más importante del funnel.
     */
    fun trackPurchaseSuccess() {
        Log.i(TAG, "[⭐ PURCHASE_SUCCESS] Usuario ha completado la compra premium vitalicia")
    }

    /**
     * Imprime un resumen de eventos de conversión de la sesión actual.
     * Útil para llamar en onDestroy() durante testing.
     */
    fun printSessionSummary() {
        Log.i(TAG, "─────── RESUMEN CONVERSIÓN SESIÓN ───────")
        Log.i(TAG, "Aperturas pantalla de compra: $purchaseScreenOpenCount")
        Log.i(TAG, "Clics en Desbloquear: $upgradeClickCount")
        Log.i(TAG, "Trial expirado mostrado: $trialExpiredCount")
        Log.i(TAG, "Upgrade desde trial expirado: $trialExpiredUpgradeClickCount")
        Log.i(TAG, "Restaurar compras: $restorePurchasesClickCount")
        if (featureTapCounts.isNotEmpty()) {
            Log.i(TAG, "Features gateadas tocadas:")
            featureTapCounts.entries.sortedByDescending { it.value }
                .forEach { (name, count) -> Log.i(TAG, "  • $name: $count veces") }
        }
        Log.i(TAG, "───────────────────────────────────")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Premium feature model — used in dialogs and upgrade screen
// ─────────────────────────────────────────────────────────────────────────────

/** Individual features locked in the free plan */
enum class PremiumFeature(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector
) {
    // ── New FREE restrictions ────────────────────────────────────────────
    DNS_ILIMITADO(
        titulo = "DNS ilimitado 24/7",
        descripcion = "En el plan gratuito el filtrado DNS se detiene tras 2 horas diarias. Con Premium funciona sin límite de tiempo.",
        icono = Icons.Rounded.Security
    ),
    HISTORIAL_COMPLETO(
        titulo = "Historial completo (30 días)",
        descripcion = "En el plan gratuito solo puedes ver el historial de las últimas 48 horas. Premium te da acceso a 30 días.",
        icono = Icons.Rounded.History
    ),
    EXPORTAR_HISTORIAL(
        titulo = "Exportar historial",
        descripcion = "Exporta tu historial de bloqueos en formato CSV para análisis detallado. Disponible solo en Premium.",
        icono = Icons.Rounded.Share
    ),
    FILTROS_ILIMITADOS(
        titulo = "Filtros personalizados ilimitados",
        descripcion = "Los filtros personalizados son una función exclusiva de Premium. Desbloquea para añadir dominios bloqueados/permitidos ilimitados.",
        icono = Icons.Rounded.FilterList
    ),
    // ── Funciones avanzadas existentes ───────────────────────────────────────
    MULTIPLES_PERFILES(
        titulo = "Perfiles múltiples",
        descripcion = "Gestiona hasta 3 perfiles independientes de menores, cada uno con sus propias restricciones y horarios.",
        icono = Icons.Rounded.People
    ),
    HORARIOS_AVANZADOS(
        titulo = "Horarios avanzados",
        descripcion = "Configura horarios por día de la semana, con excepciones de fin de semana y períodos festivos.",
        icono = Icons.Rounded.Schedule
    ),
    PIN_PARENTAL(
        titulo = "PIN parental",
        descripcion = "Protege los ajustes con un PIN para que los menores no puedan desactivar la protección.",
        icono = Icons.Rounded.Lock
    ),
    ESTADISTICAS_DETALLADAS(
        titulo = "Estadísticas detalladas (30 días)",
        descripcion = "Informes semanales y mensuales, uso por app y por hora, gráficas de tendencia de 30 días.",
        icono = Icons.Rounded.BarChart
    ),
    MONITOREO_TIEMPO_REAL(
        titulo = "Monitorización en tiempo real",
        descripcion = "Detecta las apps abiertas al instante y redirige automáticamente cuando se intenta saltarse la protección.",
        icono = Icons.Rounded.Visibility
    ),
    ALERTAS_PUSH(
        titulo = "Alertas parentales push",
        descripcion = "Recibe notificaciones inmediatas cuando se detecta un intento de acceder a contenido bloqueado.",
        icono = Icons.Rounded.NotificationsActive
    ),
    BLOQUEO_GRANULAR(
        titulo = "Bloqueo granular de apps",
        descripcion = "Bloquea redes sociales específicas (Instagram, TikTok…) por horario o de forma permanente.",
        icono = Icons.Rounded.Block
    ),

    // ── Nuevas funciones ──────────────────────────────────────────────────────────
    PACTO_DIGITAL(
        titulo = "Pacto Digital Familiar",
        descripcion = "El menor puede enviar peticiones al padre/madre (más tiempo, desbloquear una app…) y el padre responde desde el mismo dispositivo con PIN. 100% local, sin servidores.",
        icono = Icons.Rounded.Handshake
    ),
    BLOQUEO_APPS_REAL(
        titulo = "Bloqueo real de apps",
        descripcion = "Bloquea apps instaladas (Instagram, TikTok, juegos…) fuera del horario permitido usando el servicio de accesibilidad. Sin root, sin VPN adicional.",
        icono = Icons.Rounded.AppBlocking
    ),
    ANTI_TAMPERING(
        titulo = "Protección anti-desinstalación",
        descripcion = "Registra GuardianOS Shield como Administrador del dispositivo para que el menor no pueda desinstalarlo sin el PIN parental.",
        icono = Icons.Rounded.AdminPanelSettings
    ),
    TRUST_FLOW(
        titulo = "Motor TrustFlow",
        descripcion = "El nivel de restricción se adapta automáticamente según la racha del menor: Cadete (bloqueo total), Explorador (pantalla de reflexión 15 s), Guardián (acceso libre con registro). Tu hijo/a gana autonomía siendo responsable.",
        icono = Icons.Rounded.TrendingUp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog contextual — se muestra al tocar una función bloqueada
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dialog informativo que aparece al intentar usar una función premium estando
 * en el plan gratuito. Ofrece botón directo de upgrade y opción de cerrar.
 */
@Composable
fun PremiumGateDialog(
    feature: PremiumFeature,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    // Registrar que el usuario vio el gate de esta función
    LaunchedEffect(feature) {
        ConversionTracker.trackFeatureTapped(feature)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono de función
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFC107), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icono,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Función Premium",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = feature.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = feature.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        ConversionTracker.trackUpgradeClicked(feature)
                        onUpgrade()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Desbloquear por 14,99 € (pago único)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Continuar con el plan gratuito",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes reutilizables de UI para indicar estado premium
// ─────────────────────────────────────────────────────────────────────────────

/** Badge dorado "PRO" + candado para superponer en botones/cards de funciones premium */
@Composable
fun PremiumLockBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFFFFC107), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = "Premium",
            tint = Color(0xFF4A3800),
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "PRO",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4A3800)
        )
    }
}

/**
 * Banner no intrusivo que se muestra en la pantalla principal cuando el usuario
 * está en el plan gratuito. Comunica el valor del upgrade sin bloquear la UI.
 */
@Composable
fun FreePlanBanner(onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onUpgrade
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "Plan gratuito activo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = "Perfiles múltiples, horarios y alertas — 14,99 € vitalicio",
                    fontSize = 12.sp,
                    color = Color(0xFF795548),
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Ver →",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
                fontSize = 13.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fila comparativa Free vs Premium — usada dentro de PremiumPurchaseScreen
// ─────────────────────────────────────────────────────────────────────────────

/** Una fila de comparativa para la pantalla de upgrade */
@Composable
fun FeatureComparisonRow(
    icono: ImageVector,
    titulo: String,
    enFree: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (enFree) Color(0xFF4CAF50) else Color(0xFFFFC107),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = if (enFree) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = if (enFree) "Incluido en Free" else "Solo Premium",
            tint = if (enFree) Color(0xFF4CAF50) else Color(0xFFFFC107),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pantalla completa de bloqueo — reemplaza la pantalla gateada en FREE
// Uso: si (!isPremium) { FreePremiumGateScreen(...); return }
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pantalla completa que reemplaza una sección completamente bloqueada en FREE.
 * Muestra el ícono de la función, descripción y botón de upgrade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreePremiumGateScreen(
    feature: PremiumFeature,
    onUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(feature) {
        ConversionTracker.trackFeatureTapped(feature)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(feature.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícono con fondo dorado
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFFFFC107), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icono,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Función Premium",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFC107),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = feature.titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = feature.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                    color = Color(0xFF0D47A1)
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Esta función no está disponible en el plan gratuito.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    ConversionTracker.trackUpgradeClicked(feature)
                    onUpgrade()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                )
            ) {
                Icon(Icons.Rounded.Star, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Desbloquear Premium — 14,99 €",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBack) {
                Text(
                    "Volver",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
