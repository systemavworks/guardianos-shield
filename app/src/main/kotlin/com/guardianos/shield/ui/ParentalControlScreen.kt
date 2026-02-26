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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.guardianos.shield.R
import com.guardianos.shield.data.UserProfileEntity
import java.util.*

@Composable
fun ParentalControlScreen(
    currentProfile: UserProfileEntity?,
    onProfileUpdate: (UserProfileEntity) -> Unit,
    onBack: () -> Unit,
    navController: NavController? = null,
    isPremium: Boolean = false,
    onShowPremium: () -> Unit = {}
) {
    // Plan FREE: pantalla completamente bloqueada
    if (!isPremium) {
        FreePremiumGateScreen(
            feature = PremiumFeature.MULTIPLES_PERFILES,
            onUpgrade = onShowPremium,
            onBack = onBack
        )
        return
    }

    var profile by remember { 
        mutableStateOf(currentProfile ?: UserProfileEntity(
            id = 0,
            name = "Niño/a",
            age = 10,
            parentalPin = "",
            restrictionLevel = "MEDIUM",
            isActive = true,
            startTimeMinutes = 900,  // 15:00 — llegan del colegio
            endTimeMinutes = 1260,   // 21:00 — hora de dormir
            createdAt = System.currentTimeMillis()
        )) 
    }
    
    var startTime by remember { mutableStateOf(profile.startTimeMinutes) }
    var endTime by remember { mutableStateOf(profile.endTimeMinutes) }
    var weekendStartTime by remember { mutableStateOf(profile.weekendStartTimeMinutes) }
    var weekendEndTime by remember { mutableStateOf(profile.weekendEndTimeMinutes) }
    var schoolStartTime by remember { mutableStateOf(profile.schoolStartTimeMinutes) }
    var schoolEndTime by remember { mutableStateOf(profile.schoolEndTimeMinutes) }

    // ── Dialogs premium gate ───────────────────────────────────────────────
    var showScheduleGate by remember { mutableStateOf(false) }
    var showPinGate by remember { mutableStateOf(false) }

    if (showScheduleGate) {
        PremiumGateDialog(
            feature = PremiumFeature.HORARIOS_AVANZADOS,
            onUpgrade = { showScheduleGate = false; onShowPremium() },
            onDismiss = { showScheduleGate = false }
        )
    }
    if (showPinGate) {
        PremiumGateDialog(
            feature = PremiumFeature.PIN_PARENTAL,
            onUpgrade = { showPinGate = false; onShowPremium() },
            onDismiss = { showPinGate = false }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parental_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                    text = stringResource(R.string.parental_section_profile),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ProfileForm(
                    profile = profile,
                    isPremium = isPremium,
                    onShowPremiumGate = { showPinGate = true }
                ) { updatedProfile ->
                    profile = updatedProfile
                    onProfileUpdate(updatedProfile)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.parental_schedule_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isPremium) {
                                    Spacer(Modifier.width(8.dp))
                                    PremiumLockBadge()
                                }
                            }
                            Text(
                                text = if (profile.scheduleEnabled) stringResource(R.string.parental_schedule_enabled) else stringResource(R.string.parental_schedule_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = profile.scheduleEnabled,
                            onCheckedChange = { enabled ->
                                if (!isPremium && enabled) {
                                    showScheduleGate = true
                                } else {
                                    profile = profile.copy(scheduleEnabled = enabled)
                                    onProfileUpdate(profile)
                                }
                            }
                        )
                    }
                }
            }

            item {
                if (profile.scheduleEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeSelector(
                            label = stringResource(R.string.parental_schedule_from),
                            currentTime = startTime,
                            onTimeSelected = { time ->
                                startTime = time
                                profile = profile.copy(startTimeMinutes = time)
                                onProfileUpdate(profile)
                            }
                        )
                        
                        TimeSelector(
                            label = stringResource(R.string.parental_schedule_until),
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

                        // ── Horario fin de semana ────────────────────────────────────
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.parental_weekend_schedule),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (profile.weekendScheduleEnabled)
                                        "Sáb y Dom: ${weekendStartTime/60}:%02d – ${weekendEndTime/60}:%02d".format(
                                            weekendStartTime%60, weekendEndTime%60)
                                    else stringResource(R.string.parental_weekend_same),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = profile.weekendScheduleEnabled,
                                onCheckedChange = { enabled ->
                                    profile = profile.copy(weekendScheduleEnabled = enabled)
                                    onProfileUpdate(profile)
                                }
                            )
                        }

                        if (profile.weekendScheduleEnabled) {
                            TimeSelector(
                                label = stringResource(R.string.parental_weekend_from),
                                currentTime = weekendStartTime,
                                onTimeSelected = { time ->
                                    weekendStartTime = time
                                    profile = profile.copy(weekendStartTimeMinutes = time)
                                    onProfileUpdate(profile)
                                }
                            )
                            TimeSelector(
                                label = stringResource(R.string.parental_weekend_until),
                                currentTime = weekendEndTime,
                                onTimeSelected = { time ->
                                    weekendEndTime = time
                                    profile = profile.copy(weekendEndTimeMinutes = time)
                                    onProfileUpdate(profile)
                                }
                            )
                        }

                        // ── Bloqueo durante el horario del cole (L-V) ────────────
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.parental_school_block),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (profile.schoolScheduleEnabled)
                                        "Bloqueado de ${schoolStartTime/60}:%02d a ${schoolEndTime/60}:%02d".format(
                                            schoolStartTime%60, schoolEndTime%60)
                                    else stringResource(R.string.parental_school_no_block),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (profile.schoolScheduleEnabled)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = profile.schoolScheduleEnabled,
                                onCheckedChange = { enabled ->
                                    profile = profile.copy(schoolScheduleEnabled = enabled)
                                    onProfileUpdate(profile)
                                }
                            )
                        }

                        if (profile.schoolScheduleEnabled) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.parental_school_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            TimeSelector(
                                label = stringResource(R.string.parental_school_entry),
                                currentTime = schoolStartTime,
                                onTimeSelected = { time ->
                                    schoolStartTime = time
                                    profile = profile.copy(schoolStartTimeMinutes = time)
                                    onProfileUpdate(profile)
                                }
                            )
                            TimeSelector(
                                label = stringResource(R.string.parental_school_exit),
                                currentTime = schoolEndTime,
                                onTimeSelected = { time ->
                                    schoolEndTime = time
                                    profile = profile.copy(schoolEndTimeMinutes = time)
                                    onProfileUpdate(profile)
                                }
                            )
                        }
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
                                text = stringResource(R.string.parental_schedule_inactive_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6C757D)
                            )
                        }
                    }
                }
            }

            // ── Bloqueo por categoría ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.parental_category_block_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.parental_category_block_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Contenido adulto
                        CategoryBlockRow(
                            emoji = "🔞",
                            titulo = stringResource(R.string.parental_cat_adult_title),
                            subtitulo = stringResource(R.string.parental_cat_adult_desc),
                            checked = profile.blockAdultContent,
                            onCheckedChange = {
                                profile = profile.copy(blockAdultContent = it)
                                onProfileUpdate(profile)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Apuestas y casino
                        CategoryBlockRow(
                            emoji = "🎰",
                            titulo = stringResource(R.string.parental_cat_gambling_title),
                            subtitulo = stringResource(R.string.parental_cat_gambling_desc),
                            checked = profile.blockGambling,
                            onCheckedChange = {
                                profile = profile.copy(blockGambling = it)
                                onProfileUpdate(profile)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Redes sociales
                        CategoryBlockRow(
                            emoji = "📱",
                            titulo = stringResource(R.string.parental_cat_social_title),
                            subtitulo = stringResource(R.string.parental_cat_social_desc),
                            checked = profile.blockSocialMedia,
                            onCheckedChange = {
                                profile = profile.copy(blockSocialMedia = it)
                                onProfileUpdate(profile)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Videojuegos
                        CategoryBlockRow(
                            emoji = "🎮",
                            titulo = stringResource(R.string.parental_cat_gaming_title),
                            subtitulo = stringResource(R.string.parental_cat_gaming_desc),
                            checked = profile.blockGaming,
                            onCheckedChange = {
                                profile = profile.copy(blockGaming = it)
                                onProfileUpdate(profile)
                            }
                        )

                        if (profile.blockGaming) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.parental_gaming_edu_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
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
                                text = stringResource(R.string.parental_important_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.parental_important_body),
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
    isPremium: Boolean = false,
    onShowPremiumGate: () -> Unit = {},
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
                label = { Text(stringResource(R.string.parental_profile_name)) },
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
                label = { Text(stringResource(R.string.parental_profile_age)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Campo PIN — gateado en plan gratuito
            Box {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (isPremium) {
                            if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                pin = it
                                onProfileUpdate(profile.copy(parentalPin = it))
                            }
                        }
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.parental_profile_pin))
                            if (!isPremium) {
                                Spacer(Modifier.width(6.dp))
                                PremiumLockBadge()
                            }
                        }
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isPremium,
                    colors = if (!isPremium) OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = androidx.compose.ui.graphics.Color(0xFFFFC107).copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ) else OutlinedTextFieldDefaults.colors()
                )
                // Área invisible para capturar tap y mostrar gate
                if (!isPremium) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { onShowPremiumGate() }
                    )
                }
            }
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
                contentDescription = stringResource(R.string.parental_select_hour_icon),
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
                    Text(stringResource(R.string.parental_select_time), style = MaterialTheme.typography.bodyMedium)
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
                            Text(stringResource(R.string.parental_time_hour), style = MaterialTheme.typography.labelSmall)
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
                            Text(stringResource(R.string.parental_time_min), style = MaterialTheme.typography.labelSmall)
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
                    Text(stringResource(R.string.action_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                    text = if (isWithinSchedule) stringResource(R.string.parental_access_allowed) else stringResource(R.string.parental_access_blocked),
                    fontWeight = FontWeight.Bold,
                    color = if (isWithinSchedule) Color(0xFF155724) else Color(0xFF842029)
                )
                val startText = String.format("%02d:%02d", startTime / 60, startTime % 60)
                val endText = String.format("%02d:%02d", endTime / 60, endTime % 60)
                Text(
                    text = stringResource(R.string.parental_allowed_schedule, startText, endText),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
@Composable
private fun CategoryBlockRow(
    emoji: String,
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitulo,
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