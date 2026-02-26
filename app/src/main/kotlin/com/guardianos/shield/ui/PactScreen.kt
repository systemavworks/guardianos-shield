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
import androidx.compose.ui.res.stringResource
import com.guardianos.shield.R
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
                            contentDescription = stringResource(R.string.pact_back),
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
                        stringResource(R.string.pact_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    stringResource(R.string.pact_subtitle),
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
                text = { Text(stringResource(R.string.pact_tab_mailbox)) },
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
                text = { Text(if (padreDesbloqueado) stringResource(R.string.pact_tab_parent_unlocked) else stringResource(R.string.pact_tab_parent_locked)) }
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
                        Text(stringResource(R.string.pact_verifying_pin), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        stringResource(R.string.pact_request_prompt),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.pact_request_prompt_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onNuevaPeticion) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pact_btn_ask))
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
                        stringResource(R.string.pact_no_petitions_sent),
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
                        stringResource(R.string.pact_petition_history),
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
        "APPROVED" -> Triple(Icons.Rounded.CheckCircle, Color(0xFF43A047), stringResource(R.string.pact_status_approved))
        "REJECTED" -> Triple(Icons.Rounded.Cancel, Color(0xFFE53935), stringResource(R.string.pact_status_rejected))
        else -> Triple(Icons.Rounded.HourglassTop, Color(0xFFFF8F00), stringResource(R.string.pact_status_pending))
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
                Text(stringResource(R.string.pact_no_petitions_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    stringResource(R.string.pact_pending_responses, pendientes.size),
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
                    stringResource(R.string.pact_history_responded),
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
                Text(stringResource(R.string.pact_btn_respond))
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
        title = { Text(stringResource(R.string.pact_dialog_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.pact_dialog_what_ask), style = MaterialTheme.typography.bodyMedium)

                // Selector de tipo
                listOf(
                    "TIME_EXTENSION" to stringResource(R.string.pact_option_more_time),
                    "SITE_UNLOCK" to stringResource(R.string.pact_option_unlock_site)
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
                    "TIME_EXTENSION" -> stringResource(R.string.pact_field_minutes_extra)
                    else -> stringResource(R.string.pact_field_what_site)
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
                    label = { Text(stringResource(R.string.pact_field_reason)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text(stringResource(R.string.pact_field_reason_hint)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(tipoSeleccionado, valorTexto.trim(), razonTexto.trim()) },
                enabled = valorTexto.isNotBlank()
            ) { Text(stringResource(R.string.pact_btn_send_petition)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.pact_cancel)) }
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
        title = { Text(stringResource(R.string.pact_dialog_respond_title)) },
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
                        label = { Text(stringResource(R.string.pact_btn_approve)) }
                    )
                    FilterChip(
                        selected = !aprobar,
                        onClick = { aprobar = false },
                        label = { Text(stringResource(R.string.pact_btn_reject)) }
                    )
                }

                if (aprobar && peticion.tipo == "TIME_EXTENSION") {
                    OutlinedTextField(
                        value = minutosExtra,
                        onValueChange = { minutosExtra = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.pact_field_minutes_granted)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text(if (aprobar) stringResource(R.string.pact_field_message_optional) else stringResource(R.string.pact_field_rejection_reason)) },
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
            ) { Text(if (aprobar) stringResource(R.string.pact_btn_approve_confirm) else stringResource(R.string.pact_btn_reject_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.pact_cancel)) }
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
        title = { Text(stringResource(R.string.pact_pin_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pact_pin_dialog_desc))
                OutlinedTextField(
                    value = pinIntroducido,
                    onValueChange = {
                        if (it.length <= 6) {
                            pinIntroducido = it
                            errorPin = false
                        }
                    },
                    label = { Text(stringResource(R.string.pact_pin_label)) },
                    isError = errorPin,
                    supportingText = if (errorPin) ({ Text(stringResource(R.string.pact_pin_wrong), color = MaterialTheme.colorScheme.error) }) else null,
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
            }) { Text(stringResource(R.string.pact_btn_access)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.pact_cancel)) }
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
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
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
    val colorNivel = when (trustLevel) {
        TrustLevel.LOCKED  -> Color(0xFFD32F2F)
        TrustLevel.CAUTION -> Color(0xFFFBC02D)
        TrustLevel.TRUSTED -> Color(0xFF388E3C)
    }
    val descripcion = when (trustLevel) {
        TrustLevel.LOCKED  -> stringResource(R.string.pact_trust_desc_locked)
        TrustLevel.CAUTION -> stringResource(R.string.pact_trust_desc_caution)
        TrustLevel.TRUSTED -> stringResource(R.string.pact_trust_desc_trusted, minutosRestantes)
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
                    val etiquetaLocal = when (trustLevel) {
                        TrustLevel.LOCKED  -> stringResource(R.string.trust_label_locked)
                        TrustLevel.CAUTION -> stringResource(R.string.trust_label_caution)
                        TrustLevel.TRUSTED -> stringResource(R.string.trust_label_trusted)
                    }
                    val nombreLocal = when (trustLevel) {
                        TrustLevel.LOCKED  -> stringResource(R.string.trust_name_locked)
                        TrustLevel.CAUTION -> stringResource(R.string.trust_name_caution)
                        TrustLevel.TRUSTED -> stringResource(R.string.trust_name_trusted)
                    }
                    Text(
                        "$etiquetaLocal — $nombreLocal",
                        fontWeight = FontWeight.Bold,
                        color = colorNivel,
                        fontSize = 15.sp
                    )
                    Text(
                        stringResource(R.string.pact_trust_racha_days, rachaActual),
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
                            if (trustLevel == TrustLevel.LOCKED) stringResource(R.string.pact_trust_next_level_explorer) else stringResource(R.string.pact_trust_next_level_guardian),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.pact_trust_days_remaining, diasSiguienteNivel),
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
                        Text(stringResource(R.string.pact_trust_autonomy_today), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.pact_trust_autonomy_value, minutosRestantes), fontSize = 11.sp, color = colorNivel, fontWeight = FontWeight.SemiBold)
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
                                stringResource(R.string.pact_gaming_bonus_title),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                stringResource(R.string.pact_gaming_bonus_desc, minutosGamingExtra),
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
                stringResource(R.string.pact_parent_actions),
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
                    if (minutosGamingExtra > 0) stringResource(R.string.pact_trust_btn_extend_gaming, minutosGamingExtra)
                    else stringResource(R.string.pact_trust_btn_give_gaming),
                    fontSize = 13.sp
                )
            }

            // Botón: adelantar nivel (solo si no es TRUSTED)
            if (trustLevel != TrustLevel.TRUSTED) {
                val nivelDestino = if (trustLevel == TrustLevel.LOCKED)
                    stringResource(R.string.pact_level_name_explorer)
                else
                    stringResource(R.string.pact_level_name_guardian)
                OutlinedButton(
                    onClick = { mostrarDialogoNivel = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorNivel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorNivel.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pact_trust_btn_level_up, nivelDestino), fontSize = 13.sp)
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
                Text(stringResource(R.string.pact_trust_btn_reset_minutes), fontSize = 13.sp)
            }
        }
    }

    // ── Diálogo: dar tiempo de gaming ───────────────────────────────
    if (mostrarDialogoGaming) {
        var minutosSeleccionados by remember { mutableIntStateOf(30) }
        AlertDialog(
            onDismissRequest = { mostrarDialogoGaming = false },
            icon = { Text("🎮", fontSize = 28.sp) },
            title = { Text(stringResource(R.string.pact_gaming_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.pact_gaming_dialog_desc),
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
                            stringResource(R.string.pact_gaming_dialog_already_has, minutosGamingExtra),
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
                    Text(stringResource(R.string.pact_gaming_dialog_confirm, minutosSeleccionados))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGaming = false }) { Text(stringResource(R.string.pact_cancel)) }
            }
        )
    }

    // ── Diálogo: adelantar nivel ──────────────────────────────────
    if (mostrarDialogoNivel) {
        val nivelDestino = if (trustLevel == TrustLevel.LOCKED)
            stringResource(R.string.trust_dest_explorer)
        else stringResource(R.string.trust_dest_guardian)
        val efectos = stringResource(
            if (trustLevel == TrustLevel.LOCKED) R.string.pact_level_up_effect_locked
            else R.string.pact_level_up_effect_caution
        )
        AlertDialog(
            onDismissRequest = { mostrarDialogoNivel = false },
            icon = { Text("🏅", fontSize = 28.sp) },
            title = { Text(stringResource(R.string.pact_level_up_dialog_title, nivelDestino), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.pact_level_up_dialog_effort, rachaActual),
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
                            stringResource(R.string.pact_level_up_dialog_info),
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
                    Text(stringResource(R.string.pact_level_up_btn_confirm, nivelDestino))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNivel = false }) { Text(stringResource(R.string.pact_cancel)) }
            }
        )
    }
}