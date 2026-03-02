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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.guardianos.shield.BuildConfig
import com.guardianos.shield.R
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.action_back))
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
                    Text(stringResource(R.string.settings_restore_snackbar))
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
            item { Spacer(modifier = Modifier.height(8.dp)); SectionHeader(stringResource(R.string.settings_section_notifications)) }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notif_blocking),
                    description = stringResource(R.string.settings_notif_blocking_desc),
                    checked = currentSettings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(notificationsEnabled = enabled)
                        scope.launch { settingsRepo.updateNotifications(enabled) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader(stringResource(R.string.settings_section_protection)) }
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
                                text = stringResource(R.string.settings_free_data_limit, FreeTierLimits.MAX_HISTORY_HOURS),
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
                    title = stringResource(R.string.settings_malware_block),
                    description = if (isPremium) stringResource(R.string.settings_malware_block_desc_premium) else stringResource(R.string.settings_free_data_desc),
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
                    title = stringResource(R.string.settings_adult_block),
                    description = if (isPremium) stringResource(R.string.settings_adult_block_desc_premium) else stringResource(R.string.settings_free_data_desc),
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
                    title = stringResource(R.string.settings_social_block),
                    description = if (isPremium) stringResource(R.string.settings_social_block_desc_premium) else stringResource(R.string.settings_free_data_desc),
                    checked = currentSettings.blockSocialMedia,
                    onCheckedChange = { enabled ->
                        currentSettings = currentSettings.copy(blockSocialMedia = enabled)
                        scope.launch { settingsRepo.updateBlockSocial(enabled) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader(stringResource(R.string.settings_section_data)) }
            item {
                SettingsSliderItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_data_retention),
                    description = stringResource(R.string.settings_data_retention_desc, currentSettings.dataRetentionDays),
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
                    title = stringResource(R.string.settings_export),
                    description = stringResource(R.string.settings_export_desc),
                    onClick = { showExportDialog = true }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_clear_history),
                    description = stringResource(R.string.settings_clear_history_desc),
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

            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader(stringResource(R.string.settings_section_info)) }
            item {
                SettingsActionItem(
                    icon = Icons.Default.HelpOutline,
                    title = stringResource(R.string.settings_usage_guide),
                    description = stringResource(R.string.settings_usage_guide_desc),
                    onClick = { navController.navigate("help") }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about),
                    description = stringResource(R.string.settings_about_desc),
                    onClick = { showAboutDialog = true }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.PrivacyTip,
                    title = stringResource(R.string.settings_privacy),
                    description = stringResource(R.string.settings_privacy_desc),
                    onClick = { openUrl(context, "https://guardianos.es/shield") }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.settings_source_code),
                    description = stringResource(R.string.settings_source_code_desc),
                    onClick = { openUrl(context, "https://github.com/systemavworks/guardianos-shield") }
                )
            }

            // ── Sección Seguridad Avanzada (nuevas funciones PREMIUM) ──────────
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader(stringResource(R.string.settings_section_security_adv)) }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.AppBlocking,
                    title = stringResource(R.string.settings_app_blocking),
                    description = if (FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive))
                        if (accessibilityActivo) stringResource(R.string.settings_app_blocking_acc_active)
                        else stringResource(R.string.settings_app_blocking_acc_inactive)
                    else
                        stringResource(R.string.settings_app_blocking_premium),
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
                    title = stringResource(R.string.settings_anti_uninstall),
                    description = if (FreeTierLimits.canAccessPremiumFeature(isPremium, isFreeTrialActive))
                        if (deviceAdminActivo) stringResource(R.string.settings_anti_uninstall_active)
                        else stringResource(R.string.settings_anti_uninstall_inactive)
                    else
                        stringResource(R.string.settings_anti_uninstall_premium),
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
                                text = if (isPremium) stringResource(R.string.settings_plan_premium_active) else stringResource(R.string.settings_plan_free),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPremium)
                                    stringResource(R.string.settings_plan_premium_benefits)
                                else
                                    stringResource(R.string.settings_plan_free_cta),
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
                    title = stringResource(R.string.settings_restore_purchases_title),
                    description = stringResource(R.string.settings_restore_purchases_desc),
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
                                Text("Reset trial (fresh install)")
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
                            Toast.makeText(context, context.getString(R.string.settings_history_cleared), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.settings_history_error), Toast.LENGTH_SHORT).show()
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
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // ── Funciones principales ─────────────────────────────────
                Text(
                    text = stringResource(R.string.about_section_features),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                AboutFeatureRow(
                    icon = Icons.Default.Dns,
                    title = stringResource(R.string.about_feat1_title),
                    subtitle = stringResource(R.string.about_feat1_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.about_feat2_title),
                    subtitle = stringResource(R.string.about_feat2_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.AppBlocking,
                    title = stringResource(R.string.about_feat3_title),
                    subtitle = stringResource(R.string.about_feat3_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.Category,
                    title = stringResource(R.string.about_feat4_title),
                    subtitle = stringResource(R.string.about_feat4_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.Store,
                    title = stringResource(R.string.about_feat5_title),
                    subtitle = stringResource(R.string.about_feat5_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.VpnLock,
                    title = stringResource(R.string.about_feat6_title),
                    subtitle = stringResource(R.string.about_feat6_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.about_feat7_title),
                    subtitle = stringResource(R.string.about_feat7_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.BarChart,
                    title = stringResource(R.string.about_feat8_title),
                    subtitle = stringResource(R.string.about_feat8_subtitle)
                )
                AboutFeatureRow(
                    icon = Icons.Default.NotificationsActive,
                    title = stringResource(R.string.about_feat9_title),
                    subtitle = stringResource(R.string.about_feat9_subtitle)
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
                        text = stringResource(R.string.about_footer_thanks),
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
                            text = stringResource(R.string.about_footer_no_tracking),
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
                        text = stringResource(R.string.about_footer_origin),
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
                Text(stringResource(R.string.stats_close), color = MaterialTheme.colorScheme.primary)
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
        title = { Text(stringResource(R.string.clear_history_dialog_title)) },
        text = { Text(stringResource(R.string.clear_history_dialog_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.clear_history_btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
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
        title = { Text(stringResource(R.string.export_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.export_dialog_select_format))
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
                Text(stringResource(R.string.export_dialog_btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
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
