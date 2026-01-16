// app/src/main/java/com/guardianos/shield/ui/CustomFiltersScreen.kt
package com.guardianos.shield.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardianos.shield.data.CustomFilterEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFiltersScreen(
    blacklist: List<CustomFilterEntity>,
    whitelist: List<CustomFilterEntity>,
    onAddToBlacklist: (String) -> Unit,
    onAddToWhitelist: (String) -> Unit,
    onRemoveFilter: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("blacklist") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filtros Personalizados") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    dialogType = if (selectedTab == 0) "blacklist" else "whitelist"
                    showAddDialog = true
                }
            ) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Lista Negra") },
                    icon = { Icon(Icons.Default.Close, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Lista Blanca") },
                    icon = { Icon(Icons.Default.Check, null) }
                )
            }

            when (selectedTab) {
                0 -> FilterList(
                    filters = blacklist,
                    type = "blacklist",
                    onRemove = onRemoveFilter,
                    emptyMessage = "No hay dominios bloqueados personalizados"
                )
                1 -> FilterList(
                    filters = whitelist,
                    type = "whitelist",
                    onRemove = onRemoveFilter,
                    emptyMessage = "No hay dominios permitidos explícitamente"
                )
            }
        }

        if (showAddDialog) {
            AddFilterDialog(
                type = dialogType,
                onDismiss = { showAddDialog = false },
                onConfirm = { domain ->
                    if (dialogType == "blacklist") {
                        onAddToBlacklist(domain)
                    } else {
                        onAddToWhitelist(domain)
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun FilterList(
    filters: List<CustomFilterEntity>,
    type: String,
    onRemove: (String) -> Unit,
    emptyMessage: String
) {
    if (filters.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (type == "blacklist") Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (type == "blacklist")
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (type == "blacklist") Icons.Default.Info else Icons.Default.Lock,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (type == "blacklist")
                                "Estos dominios siempre serán bloqueados"
                            else
                                "Estos dominios siempre serán permitidos, incluso si coinciden con filtros",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(filters) { filter ->
                FilterItem(
                    filter = filter,
                    type = type,
                    onRemove = { onRemove(filter.domain) }
                )
            }
        }
    }
}

@Composable
fun FilterItem(
    filter: CustomFilterEntity,
    type: String,
    onRemove: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (type == "blacklist") Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (type == "blacklist")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        filter.domain,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Agregado: ${android.text.format.DateFormat.format("dd/MM/yyyy", filter.addedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Eliminar")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar filtro") },
            text = { Text("¿Deseas eliminar '${filter.domain}' de la lista?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AddFilterDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == "blacklist") "Bloquear dominio" else "Permitir dominio"
            )
        },
        text = {
            Column {
                Text(
                    if (type == "blacklist")
                        "Introduce el dominio que deseas bloquear siempre"
                    else
                        "Introduce el dominio que deseas permitir siempre",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = domain,
                    onValueChange = {
                        domain = it.lowercase().trim()
                        error = ""
                    },
                    label = { Text("Dominio") },
                    placeholder = { Text("ejemplo.com") },
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Ejemplos válidos:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("• ejemplo.com", style = MaterialTheme.typography.bodySmall)
                        Text("• subdomain.ejemplo.com", style = MaterialTheme.typography.bodySmall)
                        Text("• *.ejemplo.com (todo el dominio)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        domain.isEmpty() -> error = "El dominio no puede estar vacío"
                        !isValidDomain(domain) -> error = "Formato de dominio inválido"
                        else -> onConfirm(domain)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun isValidDomain(domain: String): Boolean {
    // Validación básica de dominio
    val domainPattern = Regex("^(\\*\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
    return domainPattern.matches(domain)
}

// ============ IMPORTADOR DE LISTAS ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportListsScreen(
    onImportBlacklist: (List<String>) -> Unit,
    onImportWhitelist: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Listas") },
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Listas Predefinidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Importa listas de bloqueo reconocidas internacionalmente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PredefineListItem(
                    name = "Steven Black's Unified Hosts",
                    description = "Lista de malware, adware y dominios maliciosos",
                    domains = "~200,000 dominios",
                    onImport = { /* Implementar importación */ }
                )
            }

            item {
                PredefineListItem(
                    name = "OISD Big List",
                    description = "Bloqueo de publicidad y rastreadores",
                    domains = "~1,000,000 dominios",
                    onImport = { /* Implementar importación */ }
                )
            }

            item {
                PredefineListItem(
                    name = "1Hosts (Pro)",
                    description = "Protección contra malware y phishing",
                    domains = "~500,000 dominios",
                    onImport = { /* Implementar importación */ }
                )
            }
        }
    }
}

@Composable
fun PredefineListItem(
    name: String,
    description: String,
    domains: String,
    onImport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        domains,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(onClick = onImport) {
                    Text("Importar")
                }
            }
        }
    }
}
