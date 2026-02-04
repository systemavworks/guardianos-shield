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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    requiredPin: String?,
    onPinVerified: () -> Unit,
    onBack: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificación Parental") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Cancelar")
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
                "Control Parental Activo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Ingresa el PIN parental para continuar",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = enteredPin,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() } && it.length <= 4) {
                        enteredPin = it
                        errorMessage = ""
                    }
                },
                label = { Text("PIN (4 dígitos)") },
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
                        requiredPin.isNullOrEmpty() -> {
                            // Sin PIN configurado, permitir acceso pero advertir
                            onPinVerified()
                        }
                        enteredPin == requiredPin -> {
                            onPinVerified()
                        }
                        else -> {
                            errorMessage = "PIN incorrecto"
                            enteredPin = ""
                        }
                    }
                },
                enabled = enteredPin.length == 4 || requiredPin.isNullOrEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (requiredPin.isNullOrEmpty()) "Continuar sin PIN" else "Verificar")
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (requiredPin.isNullOrEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ PIN no configurado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Configura un PIN en Control Parental para proteger la app del acceso no autorizado.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
