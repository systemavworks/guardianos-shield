package com.guardianos.shield.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.data.PetitionEntity
import com.guardianos.shield.data.TrustLevel
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.security.SecurityHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────────────────────
//  PactScreen — Pantalla principal del Pacto Digital Familiar
//  Dos pestañas: "Mi buzón" (hijo) y "Responder" (padre con PIN)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PactScreen(
    isPremium: Boolean = false,
    isFreeTrialActive: Boolean = true,
    onShowPremium: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { GuardianRepository(context, GuardianDatabase.getDatabase(context)) }
    val scope = rememberCoroutineScope()

    // Gate premium: accesible durante trial (48h) o con plan premium
    val tieneAcceso = isPremium || isFreeTrialActive

    if (!tieneAcceso) {
        FreePremiumGateScreen(
            feature = PremiumFeature.PACTO_DIGITAL,
            onUpgrade = onShowPremium,
            onBack = onBack
        )
        return
    }

    val peticiones by repository.peticionesFlow.collectAsState(initial = emptyList())
    val pendientesCount by repository.contadorPendientesFlow.collectAsState(initial = 0)
    val perfilActivo by repository.activeProfile.collectAsState(initial = null)

    var tabSeleccionado by remember { mutableIntStateOf(0) }
    var padreDesbloqueado by remember { mutableStateOf(false) }

    // Estado para nueva petición
    var mostrarDialogoNueva by remember { mutableStateOf(false) }

    // Estado para responder petición
    var peticionAResponder by remember { mutableStateOf<PetitionEntity?>(null) }

    // Estado para desbloquear vista padre
    var mostrarPinPadre by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Pacto Digital Familiar",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    "Comunícate con tu padre/madre sin salir del dispositivo",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Pestañas
        TabRow(selectedTabIndex = tabSeleccionado) {
            Tab(
                selected = tabSeleccionado == 0,
                onClick = { tabSeleccionado = 0 },
                text = { Text("Mi buzón") },
                icon = { Icon(Icons.Rounded.Inbox, contentDescription = null) }
            )
            Tab(
                selected = tabSeleccionado == 1,
                onClick = {
                    if (padreDesbloqueado) {
                        tabSeleccionado = 1
                    } else {
                        mostrarPinPadre = true
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (pendientesCount > 0) Badge { Text("$pendientesCount") }
                        }
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                    }
                },
                text = { Text(if (padreDesbloqueado) "Panel padre ✓" else "Panel padre 🔒") }
            )
        }

        AnimatedContent(targetState = tabSeleccionado, label = "pact_tabs") { tab ->
            when (tab) {
                0 -> BuzonHijo(
                    peticiones = peticiones,
                    onNuevaPeticion = { mostrarDialogoNueva = true }
                )
                1 -> if (padreDesbloqueado) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tarjeta de nivel de confianza con control manual (solo para el padre)
                        perfilActivo?.let { perfil ->
                            TrustLevelCard(
                                trustLevel = perfil.trustLevel,
                                rachaActual = perfil.rachaActual,
                                minutosRestantes = perfil.minutosAutonomiaDiarios,
                                minutosGamingExtra = perfil.minutosGamingExtra,
                                onForzarReset = {
                                    scope.launch { repository.resetearMinutosAutonomia() }
                                },
                                onOtorgarGaming = { minutos ->
                                    scope.launch { repository.otorgarTiempoGaming(minutos) }
                                },
                                onAdelantar = {
                                    scope.launch { repository.adelantarNivel() }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        PanelPadre(
                            peticiones = peticiones,
                            onResponder = { peticion -> peticionAResponder = peticion }
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Verificando PIN...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ── Diálogo nueva petición (hijo) ──────────────────────────────────────────
    if (mostrarDialogoNueva) {
        DialogoNuevaPeticion(
            onConfirmar = { tipo, valor, razon ->
                scope.launch {
                    repository.crearPeticion(tipo, valor, razon)
                }
                mostrarDialogoNueva = false
            },
            onCancelar = { mostrarDialogoNueva = false }
        )
    }

    // ── Diálogo responder (padre) ──────────────────────────────────────────────
    peticionAResponder?.let { peticion ->
        DialogoResponder(
            peticion = peticion,
            onResponder = { aprobar, nota, minutos ->
                scope.launch {
                    repository.responderPeticion(peticion.id, aprobar, nota, minutos)
                }
                peticionAResponder = null
            },
            onCancelar = { peticionAResponder = null }
        )
    }

    // ── Diálogo PIN para panel padre ──────────────────────────────────────────
    if (mostrarPinPadre) {
        DialogoPinPadre(
            profileId = perfilActivo?.id ?: 1,
            onVerificado = {
                padreDesbloqueado = true
                mostrarPinPadre = false
                tabSeleccionado = 1
            },
            onCancelar = { mostrarPinPadre = false }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Buzón del hijo — historial de peticiones con estado
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun BuzonHijo(
    peticiones: List<PetitionEntity>,
    onNuevaPeticion: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Botón nueva petición
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "¿Quieres pedir algo?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Más tiempo, desbloquear una app o un sitio web",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onNuevaPeticion) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pedir")
                }
            }
        }

        if (peticiones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "Aún no has enviado ninguna petición",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Historial de peticiones",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(peticiones) { peticion ->
                    TarjetaPeticionHijo(peticion)
                }
            }
        }
    }
}

@Composable
private fun TarjetaPeticionHijo(peticion: PetitionEntity) {
    val (iconoEstado, colorEstado, textoEstado) = when (peticion.estado) {
        "APPROVED" -> Triple(Icons.Rounded.CheckCircle, Color(0xFF43A047), "✅ Aprobada")
        "REJECTED" -> Triple(Icons.Rounded.Cancel, Color(0xFFE53935), "❌ Rechazada")
        else -> Triple(Icons.Rounded.HourglassTop, Color(0xFFFF8F00), "⏳ Pendiente")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorEstado.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconoEstado, contentDescription = null, tint = colorEstado, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(textoEstado, fontWeight = FontWeight.SemiBold, color = colorEstado, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    formatearFecha(peticion.creadoEn),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                etiquetaTipo(peticion.tipo) + " — " + peticion.valorSolicitado,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            if (peticion.razonHijo.isNotBlank()) {
                Text(
                    "\"${peticion.razonHijo}\"",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            if (peticion.notaPadre.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        peticion.notaPadre,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Panel del padre — responde peticiones pendientes
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PanelPadre(
    peticiones: List<PetitionEntity>,
    onResponder: (PetitionEntity) -> Unit
) {
    val pendientes = peticiones.filter { it.estado == "PENDING" }
    val respondidas = peticiones.filter { it.estado != "PENDING" }

    if (peticiones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Rounded.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text("No hay peticiones de momento", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pendientes.isNotEmpty()) {
            item {
                Text(
                    "Pendientes de respuesta (${pendientes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFFF8F00),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(pendientes) { peticion ->
                TarjetaPeticionPadre(peticion, onResponder = { onResponder(peticion) })
            }
        }

        if (respondidas.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Historial respondido",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(respondidas) { peticion ->
                TarjetaPeticionHijo(peticion)
            }
        }
    }
}

@Composable
private fun TarjetaPeticionPadre(
    peticion: PetitionEntity,
    onResponder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF8F00).copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = Color(0xFFFF8F00), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    etiquetaTipo(peticion.tipo),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF8F00)
                )
                Spacer(Modifier.weight(1f))
                Text(formatearFecha(peticion.creadoEn), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(peticion.valorSolicitado, fontWeight = FontWeight.Medium)
            if (peticion.razonHijo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "\"${peticion.razonHijo}\"",
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            Button(
                onClick = onResponder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Responder")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Diálogos
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogoNuevaPeticion(
    onConfirmar: (tipo: String, valor: String, razon: String) -> Unit,
    onCancelar: () -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf("TIME_EXTENSION") }
    var valorTexto by remember { mutableStateOf("") }
    var razonTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Rounded.Send, contentDescription = null) },
        title = { Text("Nueva petición") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("¿Qué quieres pedir?", style = MaterialTheme.typography.bodyMedium)

                // Selector de tipo
                listOf(
                    "TIME_EXTENSION" to "⏱️ Más tiempo de pantalla",
                    "SITE_UNLOCK" to "🌐 Desbloquear un sitio web"
                ).forEach { (tipo, etiqueta) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == tipo,
                            onClick = { tipoSeleccionado = tipo }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                val labelValor = when (tipoSeleccionado) {
                    "TIME_EXTENSION" -> "¿Cuántos minutos extra?"
                    else -> "¿Qué sitio web?"
                }

                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text(labelValor) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = razonTexto,
                    onValueChange = { razonTexto = it },
                    label = { Text("¿Por qué lo necesitas? (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("Ej: He terminado los deberes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(tipoSeleccionado, valorTexto.trim(), razonTexto.trim()) },
                enabled = valorTexto.isNotBlank()
            ) { Text("Enviar petición") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoResponder(
    peticion: PetitionEntity,
    onResponder: (aprobar: Boolean, nota: String, minutos: Int) -> Unit,
    onCancelar: () -> Unit
) {
    var nota by remember { mutableStateOf("") }
    var minutosExtra by remember { mutableStateOf("30") }
    var aprobar by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                if (aprobar) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = null,
                tint = if (aprobar) Color(0xFF43A047) else Color(0xFFE53935)
            )
        },
        title = { Text("Responder a la petición") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${etiquetaTipo(peticion.tipo)}: ${peticion.valorSolicitado}",
                    fontWeight = FontWeight.Medium
                )

                if (peticion.razonHijo.isNotBlank()) {
                    Text(
                        "Razón: \"${peticion.razonHijo}\"",
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = aprobar,
                        onClick = { aprobar = true },
                        label = { Text("✅ Aprobar") }
                    )
                    FilterChip(
                        selected = !aprobar,
                        onClick = { aprobar = false },
                        label = { Text("❌ Rechazar") }
                    )
                }

                if (aprobar && peticion.tipo == "TIME_EXTENSION") {
                    OutlinedTextField(
                        value = minutosExtra,
                        onValueChange = { minutosExtra = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutos extra concedidos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text(if (aprobar) "Mensaje (opcional)" else "Motivo del rechazo") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onResponder(
                        aprobar,
                        nota.trim(),
                        if (aprobar && peticion.tipo == "TIME_EXTENSION") minutosExtra.toIntOrNull() ?: 30 else 0
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aprobar) Color(0xFF43A047) else Color(0xFFE53935)
                )
            ) { Text(if (aprobar) "Aprobar" else "Rechazar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoPinPadre(
    profileId: Int,
    onVerificado: () -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var pinIntroducido by remember { mutableStateOf("") }
    var errorPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
        title = { Text("Acceso del padre/madre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Introduce el PIN parental para ver y responder peticiones.")
                OutlinedTextField(
                    value = pinIntroducido,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinIntroducido = it
                            errorPin = false
                        }
                    },
                    label = { Text("PIN parental") },
                    isError = errorPin,
                    supportingText = if (errorPin) ({ Text("PIN incorrecto", color = MaterialTheme.colorScheme.error) }) else null,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val verificado = if (SecurityHelper.hasPin(context, profileId)) {
                    SecurityHelper.verifyPin(context, profileId, pinIntroducido)
                } else {
                    // Sin PIN configurado: acceso libre al panel padre
                    true
                }
                if (verificado) {
                    onVerificado()
                } else {
                    errorPin = true
                    pinIntroducido = ""
                }
            }) { Text("Acceder") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Helpers de formato
// ──────────────────────────────────────────────────────────────────────────────

private fun etiquetaTipo(tipo: String): String = when (tipo) {
    "TIME_EXTENSION" -> "⏱️ Más tiempo"
    "APP_UNLOCK" -> "📱 App"
    "SITE_UNLOCK" -> "🌐 Sitio web"
    "APP_BLOQUEADA" -> "📱 App bloqueada"
    else -> tipo
}

private fun formatearFecha(timestamp: Long): String {
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
    return fmt.format(Date(timestamp))
}

// ──────────────────────────────────────────────────────────────────────────────
//  TrustLevelCard — Tarjeta de nivel de confianza para el panel del padre
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Tarjeta visible exclusivamente en el Panel Padre (tras verificar PIN).
 * Muestra el nivel de confianza actual del menor, los minutos de autonomía
 * restantes hoy, y permite al padre forzar un reset manual si lo necesita.
 */
@Composable
internal fun TrustLevelCard(
    trustLevel: TrustLevel,
    rachaActual: Int,
    minutosRestantes: Int,
    minutosGamingExtra: Int = 0,
    onForzarReset: () -> Unit,
    onOtorgarGaming: (Int) -> Unit,
    onAdelantar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorNivel, descripcion) = when (trustLevel) {
        TrustLevel.LOCKED  -> Color(0xFFD32F2F) to "Bloqueo total activo — el menor debe pedir permiso para cualquier app sensible."
        TrustLevel.CAUTION -> Color(0xFFFBC02D) to "Reflexión de 15 s antes de abrir apps. El menor ve sus días en riesgo."
        TrustLevel.TRUSTED -> Color(0xFF388E3C) to "Acceso libre con $minutosRestantes min restantes hoy. El sistema registra el uso."
    }

    val diasSiguienteNivel = when (trustLevel) {
        TrustLevel.LOCKED  -> 7 - rachaActual
        TrustLevel.CAUTION -> 30 - rachaActual
        TrustLevel.TRUSTED -> 0
    }
    val progresoNivel = when (trustLevel) {
        TrustLevel.LOCKED  -> rachaActual / 7f
        TrustLevel.CAUTION -> (rachaActual - 7) / 23f
        TrustLevel.TRUSTED -> 1f
    }.coerceIn(0f, 1f)

    // Estados de diálogos
    var mostrarDialogoGaming by remember { mutableStateOf(false) }
    var mostrarDialogoNivel   by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorNivel.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Cabecera: emoji + nombre + racha ───────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(trustLevel.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${trustLevel.etiqueta} — ${trustLevel.nombreMostrar}",
                        fontWeight = FontWeight.Bold,
                        color = colorNivel,
                        fontSize = 15.sp
                    )
                    Text(
                        "Racha actual: $rachaActual días",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = descripcion,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Barra de progreso al siguiente nivel ───────────────────────
            if (trustLevel != TrustLevel.TRUSTED) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Próximo nivel: ${if (trustLevel == TrustLevel.LOCKED) "Explorador" else "Guardián"}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$diasSiguienteNivel días restantes",
                            fontSize = 11.sp, color = colorNivel, fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = progresoNivel,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = colorNivel,
                        trackColor = colorNivel.copy(alpha = 0.15f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Minutos de autonomía hoy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$minutosRestantes / 60 min", fontSize = 11.sp, color = colorNivel, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = (minutosRestantes / 60f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = colorNivel,
                        trackColor = colorNivel.copy(alpha = 0.15f)
                    )
                }
            }

            // ── Indicador bonus gaming (si hay minutos activos) ─────────────
            if (minutosGamingExtra > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1565C0).copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎮", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bonus gaming activo hoy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                "$minutosGamingExtra min disponibles — se gastan minuto a minuto",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colorNivel.copy(alpha = 0.15f))

            // ── Botones de acción del padre ─────────────────────────────
            Text(
                "Acciones del padre",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Botón: dar tiempo de gaming
            OutlinedButton(
                onClick = { mostrarDialogoGaming = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (minutosGamingExtra > 0) "🎮 Ampliar tiempo de gaming ($minutosGamingExtra min activos)"
                    else "🎮 Dar tiempo de gaming hoy",
                    fontSize = 13.sp
                )
            }

            // Botón: adelantar nivel (solo si no es TRUSTED)
            if (trustLevel != TrustLevel.TRUSTED) {
                val nivelDestino = if (trustLevel == TrustLevel.LOCKED) "Explorador 🟡" else "Guardián 🟢"
                OutlinedButton(
                    onClick = { mostrarDialogoNivel = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorNivel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorNivel.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("🏅 Ascender a $nivelDestino (adelantar nivel)", fontSize = 13.sp)
                }
            }

            // Botón: resetear minutos
            OutlinedButton(
                onClick = onForzarReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Resetear minutos de hoy", fontSize = 13.sp)
            }
        }
    }

    // ── Diálogo: dar tiempo de gaming ───────────────────────────────
    if (mostrarDialogoGaming) {
        var minutosSeleccionados by remember { mutableIntStateOf(30) }
        AlertDialog(
            onDismissRequest = { mostrarDialogoGaming = false },
            icon = { Text("🎮", fontSize = 28.sp) },
            title = { Text("Dar tiempo de gaming", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Selecciona cuántos minutos de gaming extra concedes hoy. ¿Cuánto tiempo quieres dar?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Presets de tiempo
                    val opciones = listOf(15, 30, 60, 90)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opciones.forEach { min ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = minutosSeleccionados == min,
                                onClick = { minutosSeleccionados = min },
                                label = {
                                    Text(
                                        "${min}m",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                    if (minutosGamingExtra > 0) {
                        Text(
                            "Ya tiene $minutosGamingExtra min activos. Se sumarán (máx. 120 min/día).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onOtorgarGaming(minutosSeleccionados)
                    mostrarDialogoGaming = false
                }) {
                    Text("Conceder $minutosSeleccionados min")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGaming = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: adelantar nivel ──────────────────────────────────
    if (mostrarDialogoNivel) {
        val nivelActual = trustLevel.etiqueta
        val nivelDestino = if (trustLevel == TrustLevel.LOCKED) "Explorador" else "Guardián"
        val efectos = if (trustLevel == TrustLevel.LOCKED)
            "El menor pasará a Modo Precaución: verá una cuenta atrás de 15 s antes de abrir apps. Ya no es bloqueo duro."
        else
            "El menor pasará a Zona de Confianza: acceso libre con registro (60 min/día). El sistema sigue monitorizando."
        AlertDialog(
            onDismissRequest = { mostrarDialogoNivel = false },
            icon = { Text("🏅", fontSize = 28.sp) },
            title = { Text("Ascender a $nivelDestino", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Estás reconociendo el esfuerzo de $rachaActual días bien hechos.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        efectos,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                    ) {
                        Text(
                            "ℹ️ Esto sólo sube UN nivel. La racha seguirá creciendo con normalidad.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdelantar()
                        mostrarDialogoNivel = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (trustLevel == TrustLevel.LOCKED) Color(0xFFFBC02D) else Color(0xFF388E3C)
                    )
                ) {
                    Text("⬆️ Ascender a $nivelDestino")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNivel = false }) { Text("Cancelar") }
            }
        )
    }
}