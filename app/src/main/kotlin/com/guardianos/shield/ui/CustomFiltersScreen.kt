// app/src/main/java/com/guardianos/shield/ui/CustomFiltersScreen.kt
package com.guardianos.shield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var newDomain by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.BLACKLIST) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filtros Personalizados") },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Agregar nuevo dominio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newDomain,
                                onValueChange = { 
                                    newDomain = it.trim().lowercase()
                                        .removePrefix("http://")
                                        .removePrefix("https://")
                                        .removePrefix("www.")
                                        .removeSuffix("/")
                                },
                                label = { Text("Dominio (ej: instagram.com)") },
                                placeholder = { Text("instagram.com, tiktok.com...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    if (newDomain.isNotEmpty()) {
                                        when (filterType) {
                                            FilterType.BLACKLIST -> onAddToBlacklist(newDomain)
                                            FilterType.WHITELIST -> onAddToWhitelist(newDomain)
                                        }
                                        newDomain = ""
                                    }
                                },
                                enabled = newDomain.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Agregar",
                                    tint = if (newDomain.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row {
                            FilterTypeToggle(
                                currentType = filterType,
                                onTypeChange = { filterType = it }
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            text = "💡 Consejo: Al bloquear 'instagram.com' se bloquearán automáticamente todos los subdominios (www.instagram.com, api.instagram.com, etc.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6C757D)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Dominios bloqueados (${blacklist.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(blacklist) { filter ->
                FilterItem(
                    filter = filter,
                    isBlacklist = true,
                    onRemove = { onRemoveFilter(filter.domain) }
                )
            }
            
            if (blacklist.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Block,
                        title = "Sin dominios bloqueados",
                        message = "Agrega dominios para bloquear contenido no deseado"
                    )
                }
            }

            item {
                Text(
                    text = "Dominios permitidos (${whitelist.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(whitelist) { filter ->
                FilterItem(
                    filter = filter,
                    isBlacklist = false,
                    onRemove = { onRemoveFilter(filter.domain) }
                )
            }
            
            if (whitelist.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Sin dominios permitidos",
                        message = "Agrega dominios que siempre deben estar accesibles"
                    )
                }
            }
        }
    }
}

enum class FilterType {
    BLACKLIST,
    WHITELIST
}

@Composable
private fun FilterTypeToggle(
    currentType: FilterType,
    onTypeChange: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = currentType == FilterType.BLACKLIST,
            onClick = { onTypeChange(FilterType.BLACKLIST) },
            label = "Bloquear",
            icon = Icons.Rounded.Block,
            color = Color(0xFFE57373)
        )
        
        Spacer(Modifier.width(8.dp))
        
        FilterChip(
            selected = currentType == FilterType.WHITELIST,
            onClick = { onTypeChange(FilterType.WHITELIST) },
            label = "Permitir",
            icon = Icons.Rounded.CheckCircle,
            color = Color(0xFF81C784)
        )
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (selected) color else Color(0xFFF5F5F5),
        contentColor = if (selected) Color.White else Color(0xFF616161)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun FilterItem(
    filter: CustomFilterEntity,
    isBlacklist: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRemove() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlacklist) Color(0xFFFBE9E9) else Color(0xFFE8F5E9)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isBlacklist) Color(0xFFEF9A9A) else Color(0xFFA5D6A7),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isBlacklist) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = filter.domain,
                    fontWeight = FontWeight.Bold,
                    color = if (isBlacklist) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
                Text(
                    text = if (isBlacklist) "Bloqueado" else "Siempre permitido",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBlacklist) Color(0xFFEF5350) else Color(0xFF4CAF50)
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE9ECEF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C757D), modifier = Modifier.size(32.dp))
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6C757D),
            textAlign = TextAlign.Center
        )
    }
}
