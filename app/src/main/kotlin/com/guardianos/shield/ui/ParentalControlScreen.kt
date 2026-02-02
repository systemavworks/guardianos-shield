// app/src/main/java/com/guardianos/shield/ui/ParentalControlScreen.kt
package com.guardianos.shield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.guardianos.shield.data.UserProfileEntity
import java.util.*

@Composable
fun ParentalControlScreen(
    currentProfile: UserProfileEntity?,
    onProfileUpdate: (UserProfileEntity) -> Unit,
    onBack: () -> Unit,
    navController: NavController? = null
) {
    var profile by remember { 
        mutableStateOf(currentProfile ?: UserProfileEntity(
            id = 0,
            name = "Niño/a",
            age = 10,
            parentalPin = "",
            restrictionLevel = "MEDIUM",
            isActive = true,
            startTimeMinutes = 480,
            endTimeMinutes = 1260,
            createdAt = System.currentTimeMillis()
        )) 
    }
    
    var startTime by remember { mutableStateOf(profile.startTimeMinutes) }
    var endTime by remember { mutableStateOf(profile.endTimeMinutes) }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo Padres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Perfil del menor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ProfileForm(profile) { updatedProfile ->
                    profile = updatedProfile
                    onProfileUpdate(updatedProfile)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Horario de uso permitido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = profile.scheduleEnabled,
                        onCheckedChange = { enabled ->
                            profile = profile.copy(scheduleEnabled = enabled)
                            onProfileUpdate(profile)
                        }
                    )
                }
            }

            item {
                if (profile.scheduleEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeSelector(
                            label = "Desde las",
                            currentTime = startTime,
                            onTimeSelected = { time ->
                                startTime = time
                                profile = profile.copy(startTimeMinutes = time)
                                onProfileUpdate(profile)
                            }
                        )
                        
                        TimeSelector(
                            label = "Hasta las",
                            currentTime = endTime,
                            onTimeSelected = { time ->
                                endTime = time
                                profile = profile.copy(endTimeMinutes = time)
                                onProfileUpdate(profile)
                            }
                        )
                        
                        CurrentScheduleStatus(
                            scheduleEnabled = profile.scheduleEnabled,
                            startTime = startTime,
                            endTime = endTime
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFF6C757D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Los horarios están desactivados (rango menor a 1 hora).\nAjusta el horario para activar restricciones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6C757D)
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Importante",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "• Los horarios se aplican al reiniciar la app\n" +
                                   "• Para bloqueo en tiempo real, usa el \"Modo Recomendado\" (DNS Cloudflare Family)\n" +
                                   "• El PIN parental protege estos ajustes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileForm(
    profile: UserProfileEntity,
    onProfileUpdate: (UserProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age?.toString() ?: "") }
    var pin by remember { mutableStateOf(profile.parentalPin ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onProfileUpdate(profile.copy(name = it))
                },
                label = { Text("Nombre del menor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = age,
                onValueChange = {
                    if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num <= 99 } == true)) {
                        age = it
                        onProfileUpdate(profile.copy(age = it.toIntOrNull()))
                    }
                },
                label = { Text("Edad (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.all { char -> char.isDigit() } && it.length <= 4) {
                        pin = it
                        onProfileUpdate(profile.copy(parentalPin = it))
                    }
                },
                label = { Text("PIN parental (4 dígitos)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimeSelector(
    label: String,
    currentTime: Int,
    onTimeSelected: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val hours = currentTime / 60
    val minutes = currentTime % 60
    val timeText = String.format("%02d:%02d", hours, minutes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.Schedule,
                contentDescription = "Seleccionar hora",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog) {
        var selectedHour by remember { mutableStateOf(hours) }
        var selectedMinute by remember { mutableStateOf(minutes) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Selecciona la hora:", style = MaterialTheme.typography.bodyMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d".format(selectedHour),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFFF8F9FA), CircleShape)
                                    .padding(12.dp)
                            )
                            Text("Hora", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Spacer(Modifier.width(24.dp))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d".format(selectedMinute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFFF8F9FA), CircleShape)
                                    .padding(12.dp)
                            )
                            Text("Min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = {
                                selectedHour = (selectedHour - 1 + 24) % 24
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("← Hora")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedHour = (selectedHour + 1) % 24
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hora →")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = {
                                selectedMinute = (selectedMinute - 1 + 60) % 60
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("← Min")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedMinute = (selectedMinute + 1) % 60
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Min →")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val totalMinutes = selectedHour * 60 + selectedMinute
                    onTimeSelected(totalMinutes)
                    showDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CurrentScheduleStatus(
    scheduleEnabled: Boolean,
    startTime: Int,
    endTime: Int
) {
    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val isWithinSchedule = currentMinutes in startTime..endTime

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWithinSchedule) Color(0xFFD4EDDA) else Color(0xFFF8D7DA)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isWithinSchedule) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                contentDescription = null,
                tint = if (isWithinSchedule) Color(0xFF155724) else Color(0xFF842029),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isWithinSchedule) "✅ Acceso permitido ahora" else "❌ Acceso bloqueado ahora",
                    fontWeight = FontWeight.Bold,
                    color = if (isWithinSchedule) Color(0xFF155724) else Color(0xFF842029)
                )
                val startText = String.format("%02d:%02d", startTime / 60, startTime % 60)
                val endText = String.format("%02d:%02d", endTime / 60, endTime % 60)
                Text(
                    text = "Horario permitido: $startText - $endText",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
