// app/src/main/java/com/guardianos/shield/MainActivity.kt
package com.guardianos.shield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guardianos.shield.data.* // ✅ IMPORTA TODAS LAS ENTIDADES Y REPOSITORIO
import com.guardianos.shield.service.TunelLocal
import com.guardianos.shield.ui.*
import com.guardianos.shield.ui.theme.GuardianShieldTheme // ✅ IMPORTA EL TEMA
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isServiceRunning by mutableStateOf(false)
    private var blockedCount by mutableStateOf(0)
    private val recentBlocked = mutableStateListOf<BlockedSiteEntity>()
    
    private lateinit var repository: GuardianRepository
    private var currentProfile by mutableStateOf<UserProfileEntity?>(null)
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startProtectionService()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestVpnPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar repositorio
        repository = GuardianRepository(GuardianDatabase.getDatabase(this))
        
        // Observar cambios en la base de datos
        lifecycleScope.launch {
            repository.todayBlockedCount.collect { count ->
                blockedCount = count
            }
        }
        
        lifecycleScope.launch {
            repository.recentBlocked.collect { blocked ->
                recentBlocked.clear()
                recentBlocked.addAll(blocked)
            }
        }
        
        lifecycleScope.launch {
            repository.activeProfile.collect { profile ->
                currentProfile = profile
            }
        }
        
        setContent {
            GuardianShieldTheme { // ✅ USA TU TEMA PERSONALIZADO
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        GuardianShieldApp(
                            isServiceRunning = isServiceRunning,
                            blockedCount = blockedCount,
                            recentBlocked = recentBlocked,
                            onToggleProtection = { toggleProtection() },
                            onViewStats = { navController.navigate("statistics") },
                            onSettings = { navController.navigate("settings") },
                            onParentalControl = { navController.navigate("parental") },
                            onCustomFilters = { navController.navigate("filters") }
                        )
                    }
                    
                    composable("statistics") {
                        val weeklyStats = remember { mutableStateListOf<StatisticEntity>() }
                        
                        LaunchedEffect(Unit) {
                            repository.last30DaysStats.collect { stats ->
                                weeklyStats.clear()
                                weeklyStats.addAll(stats.take(7))
                            }
                        }
                        
                        StatisticsScreen(
                            todayBlocked = blockedCount,
                            weeklyStats = weeklyStats,
                            recentBlocked = recentBlocked,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("parental") {
                        ParentalControlScreen(
                            currentProfile = currentProfile,
                            onProfileUpdate = { profile ->
                                lifecycleScope.launch {
                                    repository.userProfileDao.update(profile)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("filters") {
                        val blacklist = remember { mutableStateListOf<CustomFilterEntity>() }
                        val whitelist = remember { mutableStateListOf<CustomFilterEntity>() }
                        
                        LaunchedEffect(Unit) {
                            launch {
                                repository.blacklist.collect { list ->
                                    blacklist.clear()
                                    blacklist.addAll(list)
                                }
                            }
                            launch {
                                repository.whitelist.collect { list ->
                                    whitelist.clear()
                                    whitelist.addAll(list)
                                }
                            }
                        }
                        
                        CustomFiltersScreen(
                            blacklist = blacklist,
                            whitelist = whitelist,
                            onAddToBlacklist = { domain ->
                                lifecycleScope.launch {
                                    repository.addToBlacklist(domain)
                                }
                            },
                            onAddToWhitelist = { domain ->
                                lifecycleScope.launch {
                                    repository.addToWhitelist(domain)
                                }
                            },
                            onRemoveFilter = { domain ->
                                lifecycleScope.launch {
                                    repository.removeFilter(domain)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("settings") {
                        SettingsScreen(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
    
    private fun toggleProtection() {
        if (isServiceRunning) {
            stopProtectionService()
        } else {
            checkAndRequestPermissions()
        }
    }
    
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    requestVpnPermission()
                }
                else -> {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        } else {
            requestVpnPermission()
        }
    }
    
    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startProtectionService()
        }
    }
    
    private fun startProtectionService() {
        val serviceIntent = Intent(this, TunelLocal::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isServiceRunning = true
    }
    
    private fun stopProtectionService() {
        stopService(Intent(this, TunelLocal::class.java))
        isServiceRunning = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianShieldApp(
    isServiceRunning: Boolean,
    blockedCount: Int,
    recentBlocked: List<BlockedSiteEntity>,
    onToggleProtection: () -> Unit,
    onViewStats: () -> Unit,
    onSettings: () -> Unit,
    onParentalControl: () -> Unit,
    onCustomFilters: () -> Unit
) {
    var showWelcome by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2000)
        showWelcome = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️ GuardianOS Shield", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProtectionStatusCard(
                        isRunning = isServiceRunning,
                        onToggle = onToggleProtection
                    )
                }
                
                item {
                    StatisticsCard(
                        blockedCount = blockedCount,
                        onViewDetails = onViewStats
                    )
                }
                
                item {
                    QuickActionsCard(
                        onParentalControl = onParentalControl,
                        onCustomFilters = onCustomFilters,
                        onViewStats = onViewStats
                    )
                }
                
                item {
                    FilterCategoriesCard()
                }
                
                if (recentBlocked.isNotEmpty()) {
                    item {
                        Text(
                            "Bloqueados recientemente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(recentBlocked.take(5)) { blocked ->
                        BlockedSiteItemNew(blocked)
                    }
                }
            }
            
            AnimatedVisibility(
                visible = showWelcome,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WelcomeScreen()
            }
        }
    }
}

@Composable
fun ProtectionStatusCard(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isRunning) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isRunning) "Protección ACTIVA" else "Protección INACTIVA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = if (isRunning) 
                    "Navegación segura activada" 
                else 
                    "Toca para activar la protección",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRunning) "Desactivar" else "Activar Protección")
            }
        }
    }
}

@Composable
fun StatisticsCard(
    blockedCount: Int,
    onViewDetails: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Amenazas bloqueadas hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onViewDetails) {
                    Text("Ver más")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = blockedCount.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FilterCategoriesCard() {
    val categories = listOf(
        FilterCategory("Contenido adulto", Icons.Default.Close, Color(0xFFE57373)),
        FilterCategory("Violencia", Icons.Default.Warning, Color(0xFFFFB74D)),
        FilterCategory("Malware", Icons.Default.Build, Color(0xFF64B5F6)),
        FilterCategory("Phishing", Icons.Default.Email, Color(0xFF81C784)),
        FilterCategory("Redes sociales", Icons.Default.Person, Color(0xFFBA68C8))
    )
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Categorías de filtrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            categories.forEach { category ->
                FilterCategoryItem(category)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FilterCategoryItem(category: FilterCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = category.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = category.color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Activo",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun QuickActionsCard(
    onParentalControl: () -> Unit,
    onCustomFilters: () -> Unit,
    onViewStats: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Acciones rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.Face,
                    label = "Control Parental",
                    onClick = onParentalControl
                )
                QuickActionButton(
                    icon = Icons.Default.List,
                    label = "Filtros",
                    onClick = onCustomFilters
                )
                QuickActionButton(
                    icon = Icons.Default.Info,
                    label = "Estadísticas",
                    onClick = onViewStats
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun BlockedSiteItemNew(blocked: BlockedSiteEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blocked.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = blocked.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "🛡️",
                style = MaterialTheme.typography.displayLarge,
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "GuardianOS Shield",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Protección web local para menores\nSin rastreo • Privacidad total",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CircularProgressIndicator(color = Color.White)
        }
    }
}

data class FilterCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color
)
