package com.guardianos.shield.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guardianos.shield.R
import com.guardianos.shield.security.SecurityHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    requiredPin: String?,
    profileId: Int? = null,
    onPinVerified: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Determina si realmente existe un PIN configurado en alguno de los dos sistemas
    val hasPinConfigured = remember(requiredPin, profileId) {
        val hasLegacy = !requiredPin.isNullOrEmpty()
        val hasSecure = profileId != null && profileId > 0 && SecurityHelper.hasPin(context, profileId)
        hasLegacy || hasSecure
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pin_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                stringResource(R.string.pin_screen_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                if (hasPinConfigured)
                    stringResource(R.string.pin_screen_enter_prompt)
                else
                    stringResource(R.string.pin_screen_no_pin_prompt),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))

            if (hasPinConfigured) {
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            enteredPin = it
                            errorMessage = ""
                        }
                    },
                    label = { Text(stringResource(R.string.pin_field_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = if (errorMessage.isNotEmpty()) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        when {
                            // Modo legacy: verificar contra requiredPin (para migración)
                            !requiredPin.isNullOrEmpty() -> {
                                if (enteredPin == requiredPin) {
                                    // Migrar PIN a almacenamiento seguro si hay profileId
                                    profileId?.let { SecurityHelper.migrateLegacyPin(context, it, requiredPin) }
                                    onPinVerified()
                                } else {
                                    errorMessage = context.getString(R.string.pin_error_wrong)
                                }
                            }
                            // Modo nuevo: usar SecurityHelper
                            profileId != null -> {
                                if (SecurityHelper.verifyPin(context, profileId, enteredPin)) {
                                    onPinVerified()
                                } else {
                                    errorMessage = context.getString(R.string.pin_error_wrong)
                                }
                            }
                            else -> onPinVerified()
                        }
                    },
                    enabled = enteredPin.length == 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pin_verify_button))
                }
            } else {
                // Sin PIN configurado: permite acceso directo con aviso
                Button(
                    onClick = { onPinVerified() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pin_continue_no_pin))
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.pin_no_pin_warning_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            stringResource(R.string.pin_no_pin_warning_body),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
