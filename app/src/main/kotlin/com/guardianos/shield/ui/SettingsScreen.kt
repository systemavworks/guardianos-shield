// app/src/main/java/com/guardianos/shield/ui/SettingsScreen.kt
package com.guardianos.shield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.data.SettingsRepository
import com.guardianos.shield.data.ShieldSettings
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
    currentPin: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { GuardianRepository(context, GuardianDatabase.getDatabase(context)) }
    val settingsRepo = remember { SettingsRepository(context) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }

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
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Shield,
                    title = "Bloqueo automático de malware",
                    description = "Usar Google Safe Browsing API",
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
                    description = "Filtrado de sitios +18",
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
                    description = "Facebook, Instagram, TikTok, etc.",
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
                    icon = Icons.Default.Info,
                    title = "Acerca de GuardianOS Shield",
                    description = "Versión 1.0.0 • Build 20260123",
                    onClick = { /* Mostrar info */ }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Política de privacidad",
                    description = "Cómo protegemos tus datos",
                    onClick = { openUrl(context, "https://guardianos.es/politica-privacidad") }
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
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Diálogo de PIN antes de acciones críticas
    if (showPinDialog) {
        PinLockScreen(
            requiredPin = currentPin,
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
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
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
