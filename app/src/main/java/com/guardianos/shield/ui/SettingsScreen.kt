// app/src/main/java/com/guardianos/shield/ui/SettingsScreen.kt
package com.guardianos.shield.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sección: General
            item {
                SectionHeader("General")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = "Configura las alertas de bloqueo",
                    onClick = { /* Abrir configuración de notificaciones */ }
                )
            }
            
            item {
                var notifyOnBlock by remember { mutableStateOf(true) }
                SettingsSwitch(
                    icon = Icons.Default.Warning,
                    title = "Notificar bloqueos",
                    subtitle = "Recibe una notificación cada vez que se bloquea un sitio",
                    checked = notifyOnBlock,
                    onCheckedChange = { notifyOnBlock = it }
                )
            }
            
            item {
                var autoUpdate by remember { mutableStateOf(true) }
                SettingsSwitch(
                    icon = Icons.Default.Refresh,
                    title = "Actualización automática",
                    subtitle = "Actualizar listas de bloqueo diariamente",
                    checked = autoUpdate,
                    onCheckedChange = { autoUpdate = it }
                )
            }
            
            // Sección: Seguridad
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Seguridad")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Cambiar PIN parental",
                    subtitle = "Modifica tu PIN de acceso",
                    onClick = { /* Abrir cambio de PIN */ }
                )
            }
            
            item {
                var requirePin by remember { mutableStateOf(true) }
                SettingsSwitch(
                    icon = Icons.Default.Face,
                    title = "Bloqueo de configuración",
                    subtitle = "Requiere PIN para cambiar ajustes",
                    checked = requirePin,
                    onCheckedChange = { requirePin = it }
                )
            }
            
            // Sección: Privacidad
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Privacidad")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Protección de privacidad",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GuardianOS Shield NO recopila, almacena ni transmite ninguna información de navegación. Todo el filtrado se realiza localmente en tu dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Limpiar historial",
                    subtitle = "Elimina el registro de sitios bloqueados",
                    onClick = { showResetDialog = true }
                )
            }
            
            // Sección: Datos y almacenamiento
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Datos y Almacenamiento")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = "Retención de datos",
                    subtitle = "Mantener historial por 30 días",
                    onClick = { /* Configurar retención */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Send,
                    title = "Exportar estadísticas",
                    subtitle = "Generar reporte de actividad",
                    onClick = { exportStatistics(context) }
                )
            }
            
            // Sección: Acerca de
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Acerca de")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de GuardianOS Shield",
                    subtitle = "Versión 1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = "Calificar app",
                    subtitle = "Ayúdanos con tu opinión",
                    onClick = { openPlayStore(context) }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Contacto y soporte",
                    subtitle = "support@guardianos.com",
                    onClick = { sendSupportEmail(context) }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Home,
                    title = "Política de privacidad",
                    subtitle = "Conoce cómo protegemos tus datos",
                    onClick = { openPrivacyPolicy(context) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "GuardianOS Shield v1.0.0\nFiltrado web local para menores\nSin rastreo • Privacidad total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    if (showResetDialog) {
        ResetDataDialog(
            onConfirm = {
                // Implementar limpieza de datos
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Divider()
}

@Composable
fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
    Divider()
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("🛡️", style = MaterialTheme.typography.displaySmall) },
        title = { Text("GuardianOS Shield") },
        text = {
            Column {
                Text(
                    "Versión 1.0.0",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Filtrado web local para la protección de menores. " +
                    "Sin rastreo, sin servidores externos, privacidad total.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Características:",
                    fontWeight = FontWeight.Bold
                )
                Text("• Filtrado en tiempo real", style = MaterialTheme.typography.bodySmall)
                Text("• Sin conexión a internet requerida", style = MaterialTheme.typography.bodySmall)
                Text("• Control parental completo", style = MaterialTheme.typography.bodySmall)
                Text("• Estadísticas detalladas", style = MaterialTheme.typography.bodySmall)
                Text("• 100% privado y local", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "© 2025 GuardianOS Team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ResetDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Limpiar historial") },
        text = {
            Text("¿Estás seguro de que deseas eliminar todo el historial de sitios bloqueados? Esta acción no se puede deshacer.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
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

// Funciones auxiliares
private fun openPlayStore(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=${context.packageName}")
    }
    context.startActivity(intent)
}

private fun sendSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@guardianos.com")
        putExtra(Intent.EXTRA_SUBJECT, "GuardianOS Shield - Soporte")
    }
    context.startActivity(intent)
}

private fun openPrivacyPolicy(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://guardianos.com/privacy")
    }
    context.startActivity(intent)
}

private fun exportStatistics(context: Context) {
    // Implementar exportación de estadísticas
    // Por ejemplo, generar un archivo CSV o PDF
}
