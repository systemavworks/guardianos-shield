package com.guardianos.shield.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.ui.theme.GuardianShieldTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de bloqueo de apps — se lanza encima de la app bloqueada.
 * No puede cerrarse sin PIN parental o hasta que se solicite permiso (Pacto Digital).
 */
class AppBlockedActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: GuardianRepository

    companion object {
        const val EXTRA_PAQUETE = "paquete_bloqueado"
        const val EXTRA_START_HORA = "hora_inicio"
        const val EXTRA_END_HORA = "hora_fin"
        const val EXTRA_MODO = "modo_bloqueo"
        const val EXTRA_RACHA = "racha_actual"
        const val MODO_LOCKED  = "LOCKED"   // Bloqueo duro (0-6 días)
        const val MODO_CAUTION = "CAUTION"  // Friction mode (7-29 días)

        fun createIntent(context: Context, paquete: String, startMin: Int, endMin: Int,
                         modo: String = MODO_LOCKED, racha: Int = 0): Intent {
            return Intent(context, AppBlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_PAQUETE, paquete)
                putExtra(EXTRA_START_HORA, startMin)
                putExtra(EXTRA_END_HORA, endMin)
                putExtra(EXTRA_MODO, modo)
                putExtra(EXTRA_RACHA, racha)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))

        val paquete = intent.getStringExtra(EXTRA_PAQUETE) ?: ""
        val horaInicio = intent.getIntExtra(EXTRA_START_HORA, 480)
        val horaFin = intent.getIntExtra(EXTRA_END_HORA, 1260)
        val modo = intent.getStringExtra(EXTRA_MODO) ?: MODO_LOCKED
        val racha = intent.getIntExtra(EXTRA_RACHA, 0)

        val nombreApp = obtenerNombreApp(paquete)

        setContent {
            GuardianShieldTheme {
                if (modo == MODO_CAUTION) {
                    PantallaModoPrevencion(
                        nombreApp = nombreApp,
                        paquete = paquete,
                        rachaActual = racha,
                        onContinuar = {
                            // El menor decide continuar — registrar y dejar pasar
                            scope.launch {
                                repository.logTrustedAccess(paquete)
                            }
                            finish()
                        },
                        onCerrar = {
                            val launcher = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(launcher)
                            finish()
                        }
                    )
                } else {
                    PantallaAppBloqueada(
                        nombreApp = nombreApp,
                        paquete = paquete,
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        onSolicitarPermiso = { razon ->
                            scope.launch {
                                repository.crearPeticion(
                                    tipo = "APP_UNLOCK",
                                    valorSolicitado = paquete,
                                    razonHijo = razon
                                )
                            }
                            finish()
                        },
                        onCerrar = {
                            val launcher = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(launcher)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun obtenerNombreApp(paquete: String): String {        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            paquete.split(".").lastOrNull()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: paquete
        }
    }
}

@Composable
private fun PantallaAppBloqueada(
    nombreApp: String,
    paquete: String,
    horaInicio: Int,
    horaFin: Int,
    onSolicitarPermiso: (razon: String) -> Unit,
    onCerrar: () -> Unit
) {
    // Interceptar botón Atrás para que no vuelva a la app bloqueada
    BackHandler { onCerrar() }

    var mostrarDialogoPeticion by remember { mutableStateOf(false) }
    var razonTexto by remember { mutableStateOf("") }

    val horaInicioStr = String.format("%02d:%02d", horaInicio / 60, horaInicio % 60)
    val horaFinStr = String.format("%02d:%02d", horaFin / 60, horaFin % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icono de escudo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(56.dp)
                )
            }

            Text(
                text = "App Bloqueada",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = nombreApp,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF9A9A)
            )

            // Tarjeta de explicación
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Fuera del horario permitido",
                            color = Color(0xFF90CAF9),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Puedes usar esta app de $horaInicioStr a $horaFinStr",
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Botón principal: pedir permiso al padre
            Button(
                onClick = { mostrarDialogoPeticion = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pedir permiso al padre/madre", fontWeight = FontWeight.Bold)
            }

            // Botón secundario: volver al inicio
            OutlinedButton(
                onClick = onCerrar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver al inicio")
            }

            Text(
                text = "GuardianOS Shield — protección local",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }

        // Diálogo para escribir la razón de la petición
        if (mostrarDialogoPeticion) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoPeticion = false },
                icon = { Icon(Icons.Rounded.Send, contentDescription = null) },
                title = { Text("Pedir permiso") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Escribe al padre/madre por qué quieres usar $nombreApp ahora:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = razonTexto,
                            onValueChange = { razonTexto = it },
                            placeholder = { Text("Ej: He terminado los deberes") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (razonTexto.isNotBlank()) {
                                onSolicitarPermiso(razonTexto.trim())
                                mostrarDialogoPeticion = false
                            }
                        },
                        enabled = razonTexto.isNotBlank()
                    ) { Text("Enviar petición") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoPeticion = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

/**
 * PantallaModoPrevencion — Friction Mode (racha 7-29 días).
 *
 * Fondo amarillo cálido. Muestra la racha en riesgo y una cuenta atrás de 15 s.
 * Pasada la cuenta, el menor puede continuar — el acceso queda registrado.
 */
@Composable
private fun PantallaModoPrevencion(
    nombreApp: String,
    paquete: String,
    rachaActual: Int,
    onContinuar: () -> Unit,
    onCerrar: () -> Unit
) {
    BackHandler { onCerrar() }

    var segundosRestantes by remember { mutableStateOf(15) }
    val cuentaAtrasFinalizada = segundosRestantes <= 0

    LaunchedEffect(Unit) {
        while (segundosRestantes > 0) {
            delay(1000)
            segundosRestantes--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF5D4037))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icono amarillo de advertencia
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC107).copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚠️", fontSize = 50.sp)
            }

            Text(
                text = "Modo Precaución",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )

            Text(
                text = nombreApp,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            // Tarjeta: racha en juego
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFC107).copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔥 Llevas $rachaActual días de racha",
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (!cuentaAtrasFinalizada)
                            "¿Seguro que quieres romperla? Reflexiona $segundosRestantes s..."
                        else
                            "Puedes continuar — el acceso quedará registrado para tu tutor/a.",
                        color = Color.White.copy(alpha = 0.80f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    // Barra de progreso de la cuenta atrás
                    if (!cuentaAtrasFinalizada) {
                        LinearProgressIndicator(
                            progress = segundosRestantes / 15f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = Color(0xFFFFC107),
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Botón "Continuar" — solo activo tras la cuenta atrás
            Button(
                onClick = onContinuar,
                enabled = cuentaAtrasFinalizada,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color(0xFF3E2723),
                    disabledContainerColor = Color(0xFFFFC107).copy(alpha = 0.30f),
                    disabledContentColor = Color.White.copy(alpha = 0.50f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (cuentaAtrasFinalizada)
                        "Continuar (se registrará en el informe)"
                    else
                        "Espera $segundosRestantes s...",
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón secundario: volver (la racha se mantiene)
            OutlinedButton(
                onClick = onCerrar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC107))
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver (mantener racha 🔥)")
            }

            Text(
                text = "GuardianOS Shield — TrustFlow Engine",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.30f)
            )
        }
    }
}
