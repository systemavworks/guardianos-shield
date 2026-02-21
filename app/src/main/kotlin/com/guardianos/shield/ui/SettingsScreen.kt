// app/src/main/kotlin/com/guardianos/shield/ui/SettingsScreen.kt
package com.guardianos.shield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.guardianos.shield.BuildConfig
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.data.SettingsRepository
import com.guardianos.shield.data.ShieldSettings
import com.guardianos.shield.billing.FreeTierLimits
import com.guardianos.shield.security.DeviceAdminHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onBack: () -> Unit,
    currentPin: String? = null,
    profileId: Int? = null,
    onRestorePurchases: (() -> Unit)? = null,
    isPremium: Boolean = false,
    isFreeTrialActive: Boolean = false,
    onShowPremium: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { GuardianRepository(context, GuardianDatabase.getDatabase(context)) }
    val settingsRepo = remember { SettingsRepository(context) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRestoreSnackbar by remember { mutableStateOf(false) }
    var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }

    // Estados para gates de nuevas funciones premium
    var showAboutDialog by remember { mutableStateOf(false) }
    var showGateDialog by remember { mutableStateOf(false) }
    var gateFeature by remember { mutableStateOf<PremiumFeature?>(null) }
    var accessibilityActivo by remember {
        mutableStateOf(
            (android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: "").contains("AppBlockerAccessibilityService", ignoreCase = true)
        )
    }
    var deviceAdminActivo by remember { mutableStateOf(DeviceAdminHelper.estaActivo(context)) }

    // Launcher para el diálogo de sistema de Device Admin — captura el resultado
    // para actualizar el estado inmediatamente al volver (evita crash por startActivity directo)
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        deviceAdminActivo = DeviceAdminHelper.estaActivo(context)
    }

    if (showRestoreSnackbar) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            showRestoreSnackbar = false
        }
    }

    var currentSettings by remember { mutableStateOf(ShieldSettings()) }

    LaunchedEffect(Unit) {
        settingsRepo.settings.collect { settings ->
            currentSettings = settings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            if (showRestoreSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("Compras consultadas — si tienes una compra activa se restaurará automáticamente")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)); SectionHeader("Notificaciones") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones de bloqueo",
                    description = "Alertas cuando se bloquea un sitio",
                    checked = currentSettings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(notificationsEnabled = enabled)
                        scope.launch { settingsRepo.updateNotifications(enabled) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("Protección") }
            // Badge 48h FREE para las 3 opciones de protección
            if (!isPremium) {
                item {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFF3E0)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Plan FREE — datos de protección registrados las últimas ${FreeTierLimits.MAX_HISTORY_HOURS}h. Actualiza a Premium para acceso completo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Shield,
                    title = "Bloqueo automático de malware",
                    description = if (isPremium) "Usar Google Safe Browsing API" else "Activo — datos últimas 48h (FREE)",
                    checked = currentSettings.autoBlockMalware,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(autoBlockMalware = enabled)
                        scope.launch { settingsRepo.updateAutoBlockMalware(enabled) }
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Block,
                    title = "Bloquear contenido adulto",
                    description = if (isPremium) "Filtrado de sitios +18" else "Activo — datos últimas 48h (FREE)",
                    checked = currentSettings.blockAdultContent,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(blockAdultContent = enabled)
                        scope.launch { settingsRepo.updateBlockAdult(enabled) }
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.People,
                    title = "Bloquear redes sociales",
                    description = if (isPremium) "Facebook, Instagram, TikTok, etc." else "Activo — datos últimas 48h (FREE)",
                    checked = currentSettings.blockSocialMedia,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(blockSocialMedia = enabled)
                        scope.launch { settingsRepo.updateBlockSocial(enabled) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("Datos y Privacidad") }
            item {
                SettingsSliderItem(
                    icon = Icons.Default.Timer,
                    title = "Retención de datos",
                    description = "Días de historial: ${currentSettings.dataRetentionDays}",
                    value = currentSettings.dataRetentionDays.toFloat(),
                    onValueChange = { value ->
                        val days = value.toInt()
                        currentSettings = currentSettings.copy(dataRetentionDays = days)
                        scope.launch { settingsRepo.updateRetentionDays(days) }
                    },
                    valueRange = 7f..90f,
                    steps = 0
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.FileDownload,
                    title = "Exportar estadísticas",
                    description = "Guardar datos en CSV o JSON",
                    onClick = { showExportDialog = true }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Delete,
                    title = "Limpiar historial",
                    description = "Eliminar todos los registros",
                    onClick = {
                        // Si hay PIN configurado, pedirlo antes de borrar
                        if (!currentPin.isNullOrEmpty()) {
                            pendingAction = { showClearDialog = true }
                            showPinDialog = true
                        } else {
                            showClearDialog = true
                        }
                    },
                    destructive = true
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("Información") }
            item {
                SettingsActionItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Guía de uso",
                    description = "Primeros pasos y preguntas frecuentes para padres",
                    onClick = { navController.navigate("help") }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de GuardianOS Shield",
                    description = "Versión 1.1.0 • Build 20260220",
                    onClick = { showAboutDialog = true }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Política de privacidad",
                    description = "Cómo protegemos tus datos",
                    onClick = { openUrl(context, "https://guardianos.es/shield") }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Code,
                    title = "Código fuente",
                    description = "Proyecto open source en GitHub",
                    onClick = { openUrl(context, "https://github.com/systemavworks/guardianos-shield") }
                )
            }

            // ── Sección Seguridad Avanzada (nuevas funciones PREMIUM) ──────────
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("Seguridad Avanzada") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.AppBlocking,
                    title = "Bloqueo real de apps 🔒",
                    description = if (FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive))
                        if (accessibilityActivo) "Servicio de accesibilidad activo"
                        else "Toca para activar en Ajustes del sistema"
                    else
                        "PREMIUM — bloquea apps sensibles en tiempo real",
                    checked = accessibilityActivo,
                    onCheckedChange = { activar ->
                        if (!FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive)) {
                            gateFeature = PremiumFeature.BLOQUEO_APPS_REAL
                            showGateDialog = true
                        } else {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            // Estado se actualizará al volver - update optimista visual
                            accessibilityActivo = activar
                        }
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Anti-desinstalación 🛡️",
                    description = if (FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive))
                        if (deviceAdminActivo) "Administrador de dispositivo activo"
                        else "Activa para impedir que el menor desinstale la app"
                    else
                        "PREMIUM — impide la desinstalación no autorizada",
                    checked = deviceAdminActivo,
                    onCheckedChange = { activar ->
                        if (!FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive)) {
                            gateFeature = PremiumFeature.ANTI_TAMPERING
                            showGateDialog = true
                        } else {
                            if (activar) {
                                // Usar launcher para capturar el resultado y actualizar estado
                                deviceAdminLauncher.launch(
                                    DeviceAdminHelper.crearIntentActivacion(context)
                                )
                            } else {
                                DeviceAdminHelper.desactivar(context)
                                deviceAdminActivo = false
                            }
                        }
                    }
                )
            }

            // ── Sección Premium ──────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("Premium") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPremium)
                            androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.12f)
                        else
                            androidx.compose.ui.graphics.Color(0xFFFFC107).copy(alpha = 0.12f)
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
                            imageVector = if (isPremium) Icons.Default.Star else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPremium)
                                androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            else
                                androidx.compose.ui.graphics.Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isPremium) "Plan Premium activo" else "Plan Gratuito",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPremium)
                                    "Acceso vitalicio a todas las funciones"
                                else
                                    "Desbloquea todo por 14,99 € (pago único)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Refresh,
                    title = "Restaurar compras",
                    description = "Recupera el acceso premium tras reinstalar o cambiar de dispositivo",
                    onClick = {
                        ConversionTracker.trackRestorePurchasesTapped()
                        onRestorePurchases?.invoke()
                        showRestoreSnackbar = true
                    }
                )
            }

            // ── Sección DEBUG (solo visible en builds debug) ────────────────────────────────
            if (BuildConfig.DEBUG) {
                item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("🛠️ Testing (DEBUG)") }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFB71C1C).copy(alpha = 0.08f)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Herramientas de prueba del trial",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color(0xFFB71C1C),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        settingsRepo.simulateTrialExpired()
                                        Toast.makeText(
                                            context,
                                            "⌛ Trial simulado como expirado (reinicia la app)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFB71C1C)
                                )
                            ) {
                                Text("Simular trial expirado (49h)", color = androidx.compose.ui.graphics.Color.White)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        settingsRepo.resetTrialForTesting()
                                        Toast.makeText(
                                            context,
                                            "✅ Trial reseteado (reinicia la app)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Resetear trial (nueva instalación)")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Diálogo Acerca de
    if (showAboutDialog) {
        AboutGuardianDialog(onDismiss = { showAboutDialog = false })
    }

    // Diálogo de PIN antes de acciones críticas
    if (showPinDialog) {
        PinLockScreen(
            requiredPin = currentPin,
            profileId = profileId,
            onPinVerified = {
                showPinDialog = false
                pendingAction?.invoke()
                pendingAction = null
            },
            onBack = {
                showPinDialog = false
                pendingAction = null
            }
        )
    }

    // Diálogo de gate premium para funciones Seguridad Avanzada
    if (showGateDialog && gateFeature != null) {
        PremiumGateDialog(
            feature = gateFeature!!,
            onUpgrade = {
                showGateDialog = false
                gateFeature = null
                onShowPremium?.invoke()
            },
            onDismiss = {
                showGateDialog = false
                gateFeature = null
            }
        )
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                scope.launch {
                    try {
                        repository.clearAllData()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Historial eliminado correctamente", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al limpiar historial", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDataDialog(
            onConfirm = { format ->
                scope.launch {
                    exportData(context, repository, format)
                }
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) { { Icon(Icons.Default.Check, null) } } else null
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (destructive)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
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
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (destructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutGuardianDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Cabecera ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GuardianOS Shield",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v1.2.0 · Build 20260221",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Control parental inteligente para Android. Filtra contenido, bloquea apps y protege la navegación sin necesidad de root.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // ── Funciones principales ─────────────────────────────────
                Text(
                    text = "FUNCIONES PRINCIPALES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                AboutFeatureRow(
                    icon = Icons.Default.Dns,
                    title = "Filtro DNS (CleanBrowsing)",
                    subtitle = "Bloquea contenido adulto, redes sociales, juegos y malware a nivel de red automáticamente."
                )
                AboutFeatureRow(
                    icon = Icons.Default.Language,
                    title = "Navegador seguro + SafeSearch triple",
                    subtitle = "WebView con validación de URLs. SafeSearch activado automáticamente en Google, Bing y YouTube."
                )
                AboutFeatureRow(
                    icon = Icons.Default.AppBlocking,
                    title = "Bloqueo de apps · Premium",
                    subtitle = "Impide abrir Instagram, TikTok, etc. fuera del horario permitido mediante accesibilidad."
                )
                AboutFeatureRow(
                    icon = Icons.Default.Category,
                    title = "Bloqueo por categorías",
                    subtitle = "Controla por categoría: contenido adulto, apuestas, redes sociales y videojuegos. Las apps educativas (Duolingo, Khan Academy, etc.) nunca se bloquean."
                )
                AboutFeatureRow(
                    icon = Icons.Default.Store,
                    title = "Play Store fuera de horario",
                    subtitle = "Bloquea el acceso a Google Play Store fuera del horario permitido para evitar instalaciones no autorizadas."
                )
                AboutFeatureRow(
                    icon = Icons.Default.VpnLock,
                    title = "Anti-evasión VPN automática",
                    subtitle = "Detecta y bloquea al instante apps de VPN y proxies (Psiphon, Turbo VPN, Orbot, Hotspot Shield…) y alerta al padre."
                )
                AboutFeatureRow(
                    icon = Icons.Default.Schedule,
                    title = "Horarios y rachas",
                    subtitle = "Define franjas horarias de uso. El sistema TrustFlow premia el cumplimiento con autonomía progresiva."
                )
                AboutFeatureRow(
                    icon = Icons.Default.BarChart,
                    title = "Estadísticas e historial",
                    subtitle = "Consulta qué dominios y apps se han bloqueado, con registro completo de actividad."
                )
                AboutFeatureRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Resumen semanal para padres",
                    subtitle = "Cada domingo a las 20:00 recibes un informe con el desglose de bloqueos de la semana: webs, apps, intentos de evasión."
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // ── Footer ───────────────────────────────────────────────
                val ctx = LocalContext.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Gracias por usar GuardianOS Shield de forma responsable.\nTu confianza es nuestra mayor responsabilidad.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Sin rastreadores · Sin publicidad · Sin venta de datos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🌐 guardianos.es/shield",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { openUrl(ctx, "https://guardianos.es/shield") }
                            .padding(vertical = 2.dp)
                    )
                    Text(
                        text = "✉ info@guardianos.es",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { openUrl(ctx, "mailto:info@guardianos.es") }
                            .padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Desarrollado en Andalucía, España 🇪🇸 · Proyecto open source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun AboutFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Limpiar historial") },
        text = { Text("¿Estás seguro de que deseas eliminar todo el historial de sitios bloqueados? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar datos") },
        text = {
            Column {
                Text("Selecciona el formato de exportación:")
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    FilterChip(
                        selected = selectedFormat == "CSV",
                        onClick = { selectedFormat = "CSV" },
                        label = { Text("CSV") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedFormat == "JSON",
                        onClick = { selectedFormat = "JSON" },
                        label = { Text("JSON") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFormat) }) {
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se puede abrir el enlace", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun exportData(
    context: Context,
    repository: GuardianRepository,
    format: String
) = withContext(Dispatchers.IO) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "guardian_export_$timestamp.$format"
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        exportDir.mkdirs()
        val file = File(exportDir, filename)

        val blockedList = repository.getBlockedSitesByDateRange(0, System.currentTimeMillis())
        val content = if (format == "JSON") {
            Gson().toJson(blockedList)
        } else {
            val csv = StringBuilder("Domain,Category,Timestamp,ThreatLevel\n")
            blockedList.forEach { site ->
                csv.append("\"${site.domain}\",\"${site.category}\",${site.timestamp},${site.threatLevel}\n")
            }
            csv.toString()
        }
        file.writeText(content)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Datos exportados a: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
