// app/src/main/java/com/guardianos/shield/ui/ParentalControlScreen.kt
package com.guardianos.shield.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.guardianos.shield.data.UserProfileEntity
import kotlinx.coroutines.delay
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    currentProfile: UserProfileEntity?,
    onProfileUpdate: (UserProfileEntity) -> Unit,
    onBack: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var isPinVerified by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control Parental") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (!isPinVerified) {
            PinVerificationScreen(
                currentPin = currentProfile?.parentalPin,
                onPinVerified = { isPinVerified = true },
                onCreatePin = { newPin ->
                    currentProfile?.let {
                        onProfileUpdate(it.copy(parentalPin = hashPin(newPin)))
                    }
                    isPinVerified = true
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Perfil") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Restricciones") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Horarios") }
                    )
                }

                when (selectedTab) {
                    0 -> ProfileTab(currentProfile, onProfileUpdate)
                    1 -> RestrictionsTab(currentProfile, onProfileUpdate)
                    2 -> ScheduleTab(currentProfile, onProfileUpdate)
                }
            }
        }
    }
}

@Composable
fun PinVerificationScreen(
    currentPin: String?,
    onPinVerified: () -> Unit,
    onCreatePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val isCreatingPin = currentPin == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isCreatingPin) "Crear PIN Parental" else "Verificar PIN Parental",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        PinInput(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            label = if (isCreatingPin) "Nuevo PIN (4 dígitos)" else "PIN Parental"
        )

        if (isCreatingPin) {
            Spacer(modifier = Modifier.height(16.dp))
            PinInput(
                value = confirmPin,
                onValueChange = { if (it.length <= 4) confirmPin = it },
                label = "Confirmar PIN"
            )
        }

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isCreatingPin) {
                    when {
                        pin.length != 4 -> error = "El PIN debe tener 4 dígitos"
                        pin != confirmPin -> error = "Los PINs no coinciden"
                        else -> {
                            onCreatePin(pin)
                            error = ""
                        }
                    }
                } else {
                    if (hashPin(pin) == currentPin) {
                        onPinVerified()
                        error = ""
                    } else {
                        error = "PIN incorrecto"
                        pin = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length == 4 && (!isCreatingPin || confirmPin.length == 4)
        ) {
            Text(if (isCreatingPin) "Crear PIN" else "Verificar")
        }
    }
}

@Composable
fun PinInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun ProfileTab(
    profile: UserProfileEntity?,
    onUpdate: (UserProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var age by remember { mutableStateOf(profile?.age?.toString() ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Información del Perfil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del menor") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = age,
                        onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                        label = { Text("Edad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            profile?.let {
                                onUpdate(
                                    it.copy(
                                        name = name,
                                        age = age.toIntOrNull() ?: it.age
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Cambios")
                    }
                }
            }
        }
    }
}

@Composable
fun RestrictionsTab(
    profile: UserProfileEntity?,
    onUpdate: (UserProfileEntity) -> Unit
) {
    var restrictionLevel by remember { mutableStateOf(profile?.restrictionLevel ?: "MODERATE") }
    var allowSocialMedia by remember { mutableStateOf(profile?.allowSocialMedia ?: true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nivel de Restricción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RestrictionLevelSelector(
                        selectedLevel = restrictionLevel,
                        onLevelSelected = { restrictionLevel = it }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configuración Adicional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permitir redes sociales")
                            Text(
                                "Instagram, TikTok, Snapchat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = allowSocialMedia,
                            onCheckedChange = { allowSocialMedia = it }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    profile?.let {
                        onUpdate(
                            it.copy(
                                restrictionLevel = restrictionLevel,
                                allowSocialMedia = allowSocialMedia
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Configuración")
            }
        }
    }
}

@Composable
fun RestrictionLevelSelector(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit
) {
    val levels = listOf(
        "STRICT" to "Estricto - Solo sitios educativos",
        "MODERATE" to "Moderado - Bloqueo estándar",
        "MILD" to "Suave - Supervisión básica"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (level, description) ->
            Card(
                onClick = { onLevelSelected(level) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedLevel == level)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            description.split(" - ")[0],
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            description.split(" - ")[1],
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (selectedLevel == level) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleTab(
    profile: UserProfileEntity?,
    onUpdate: (UserProfileEntity) -> Unit
) {
    var startHour by remember { mutableStateOf(profile?.allowedHoursStart ?: 8) }
    var endHour by remember { mutableStateOf(profile?.allowedHoursEnd ?: 21) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Horario de Uso Permitido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Hora de inicio: ${formatHour(startHour)}")
                    Slider(
                        value = startHour.toFloat(),
                        onValueChange = { startHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Hora de fin: ${formatHour(endHour)}")
                    Slider(
                        value = endHour.toFloat(),
                        onValueChange = { endHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Internet disponible de ${formatHour(startHour)} a ${formatHour(endHour)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    profile?.let {
                        onUpdate(
                            it.copy(
                                allowedHoursStart = startHour,
                                allowedHoursEnd = endHour
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Horario")
            }
        }
    }
}

fun formatHour(hour: Int): String {
    return String.format("%02d:00", hour)
}

fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
