// app/src/main/java/com/guardianos/shield/MainActivity.kt
package com.guardianos.shield

// ============= IMPORTS CORREGIDOS Y OPTIMIZADOS =============
// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*

// Android
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

// AndroidX
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Compose
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// App
import com.guardianos.shield.data.*
import com.guardianos.shield.service.*
import com.guardianos.shield.ui.*
import com.guardianos.shield.ui.theme.GuardianShieldTheme

// Coroutines & Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============= ENUMS =============
enum class ProtectionMode {
    Recommended, Advanced, CustomStats
}

// ============= ACTIVITY PRINCIPAL =============
class MainActivity : ComponentActivity() {

    // Estados
    private var protectionMode by mutableStateOf(ProtectionMode.Recommended)
    private var isServiceRunning by mutableStateOf(false)
    private var blockedCount by mutableStateOf(0)
    private val recentBlocked = mutableStateListOf<BlockedSiteEntity>()
    internal var currentProfile by mutableStateOf<UserProfileEntity?>(null)
    private var showDnsDialog by mutableStateOf(false)
    private val customFiltersBlacklist = mutableStateListOf<CustomFilterEntity>()
    private val customFiltersWhitelist = mutableStateListOf<CustomFilterEntity>()
    
    // Servicios
    internal lateinit var repository: GuardianRepository
    private lateinit var usageMonitor: UsageStatsMonitor
    private lateinit var vpnStateReceiver: BroadcastReceiver
    
    // Estados de monitoreo
    private var isMonitoringActive by mutableStateOf(false)
    private var hasUsageStatsPermission by mutableStateOf(false)

    // Launchers de permisos
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Permiso VPN requerido", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        }
    }

    // ============= LIFECYCLE =============
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializeApp()
        setupVpnReceiver()
        loadInitialData()
        requestNotificationPermission()
        
        setContent {
            GuardianShieldTheme {
                var showSplash by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }
                
                if (showSplash) {
                    WelcomeSplashScreen()
                } else {
                    GuardianShieldApp(
                        protectionMode = protectionMode,
                        onModeChange = { handleModeChange(it) },
                        isServiceRunning = isServiceRunning,
                        onToggleService = { toggleProtection() },
                        blockedCount = blockedCount,
                        recentBlocked = recentBlocked.toList(),
                        currentProfile = currentProfile,
                        onOpenBrowser = { openSafeBrowser() },
                        isMonitoringActive = isMonitoringActive,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        onRequestUsagePermission = { requestUsageStatsPermission() },
                        onToggleMonitoring = { toggleMonitoring() }
                    )
                }
            }
        }
    }

    // ============= INICIALIZACIÓN =============
    private fun initializeApp() {
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        usageMonitor = UsageStatsMonitor(this, repository)
        hasUsageStatsPermission = usageMonitor.hasPermission()
    }

    private fun setupVpnReceiver() {
        vpnStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    DnsFilterService.ACTION_VPN_STARTED -> {
                        isServiceRunning = true
                        loadStatistics()
                    }
                    DnsFilterService.ACTION_VPN_STOPPED -> {
                        isServiceRunning = false
                    }
                    DnsFilterService.ACTION_VPN_ERROR -> {
                        Toast.makeText(context, "Error en servicio VPN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(DnsFilterService.ACTION_VPN_STARTED)
            addAction(DnsFilterService.ACTION_VPN_STOPPED)
            addAction(DnsFilterService.ACTION_VPN_ERROR)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(vpnStateReceiver, filter)
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            currentProfile = repository.getActiveProfile()
            loadStatistics()
            loadCustomFilters()
        }
    }
    
    private fun loadCustomFilters() {
        lifecycleScope.launch {
            repository.blacklist.collect { list ->
                customFiltersBlacklist.clear()
                customFiltersBlacklist.addAll(list)
            }
        }
        lifecycleScope.launch {
            repository.whitelist.collect { list ->
                customFiltersWhitelist.clear()
                customFiltersWhitelist.addAll(list)
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val sites = repository.getRecentBlockedSites(10)
            recentBlocked.clear()
            recentBlocked.addAll(sites)
            blockedCount = sites.size
        }
    }

    // ============= GESTIÓN DE MODOS =============
    private fun handleModeChange(newMode: ProtectionMode) {
        protectionMode = newMode
        
        when (newMode) {
            ProtectionMode.Recommended -> {
                lifecycleScope.launch {
                    if (isServiceRunning) {
                        stopVpnService()
                        delay(500)
                    }
                    startLightweightMonitoring()
                    Toast.makeText(this@MainActivity, "Modo Recomendado activado", Toast.LENGTH_SHORT).show()
                }
            }
            ProtectionMode.Advanced -> {
                lifecycleScope.launch {
                    stopService(Intent(this@MainActivity, LightweightMonitorService::class.java))
                    delay(300)
                    showDnsDialog = true
                }
            }
            ProtectionMode.CustomStats -> {
                lifecycleScope.launch {
                    if (isServiceRunning) {
                        stopVpnService()
                        delay(300)
                    }
                    stopMonitoring()
                    Toast.makeText(this@MainActivity, "Modo Manual - Sin protección activa", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLightweightMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, LightweightMonitorService::class.java))
        } else {
            startService(Intent(this, LightweightMonitorService::class.java))
        }
    }

    // ============= GESTIÓN VPN =============
    private fun toggleProtection() {
        if (isServiceRunning) {
            stopVpnService()
        } else {
            requestVpnPermission()
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, DnsFilterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
    }

    private fun stopVpnService() {
        stopService(Intent(this, DnsFilterService::class.java))
        isServiceRunning = false
    }

    // ============= GESTIÓN USAGE STATS =============
    private fun toggleMonitoring() {
        if (!hasUsageStatsPermission) {
            requestUsageStatsPermission()
            return
        }

        if (isMonitoringActive) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        AppMonitorService.start(this)
        isMonitoringActive = true
        Toast.makeText(this, "Monitoreo de apps activado", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        AppMonitorService.stop(this)
        isMonitoringActive = false
        Toast.makeText(this, "Monitoreo desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Por favor, activa el permiso para GuardianOS Shield",
            Toast.LENGTH_LONG
        ).show()
    }

    // ============= OTRAS FUNCIONES =============
    private fun openSafeBrowser() {
        startActivity(Intent(this, SafeBrowserActivity::class.java))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasUsageStatsPermission = usageMonitor.hasPermission()
        loadStatistics()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(vpnStateReceiver)
        } catch (e: Exception) {
            // Receiver ya desregistrado
        }
    }
}

// ============= COMPOSABLES - PANTALLA PRINCIPAL =============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianShieldApp(
    protectionMode: ProtectionMode,
    onModeChange: (ProtectionMode) -> Unit,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    blockedCount: Int,
    recentBlocked: List<BlockedSiteEntity>,
    currentProfile: UserProfileEntity?,
    onOpenBrowser: () -> Unit,
    isMonitoringActive: Boolean,
    hasUsageStatsPermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onToggleMonitoring: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                protectionMode = protectionMode,
                onModeChange = onModeChange,
                isServiceRunning = isServiceRunning,
                onToggleService = onToggleService,
                blockedCount = blockedCount,
                recentBlocked = recentBlocked,
                currentProfile = currentProfile,
                onOpenBrowser = onOpenBrowser,
                isMonitoringActive = isMonitoringActive,
                hasUsageStatsPermission = hasUsageStatsPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onToggleMonitoring = onToggleMonitoring,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("parental") { 
            ParentalControlScreen(
                currentProfile = currentProfile ?: UserProfileEntity(),
                onProfileUpdate = { profile -> 
                    (navController.context as MainActivity).lifecycleScope.launch {
                        (navController.context as MainActivity).repository.updateProfile(profile)
                        (navController.context as MainActivity).currentProfile = profile
                    }
                },
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("filters") { 
            val mainActivity = navController.context as MainActivity
            LaunchedEffect(Unit) {
        // Cargar datos iniciales
            }
            CustomFiltersScreen(
                blacklist = emptyList(),  // ✅ Placeholder seguro (reemplazar luego con datos reales)
                whitelist = emptyList(),  // ✅ Placeholder seguro
                onAddToBlacklist = { domain ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.addToBlacklist(domain)
                    }
                },
                onAddToWhitelist = { domain ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.addToWhitelist(domain)
                    }
                },
                onRemoveFilter = { domain ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.removeFilter(domain)
                    }
                },
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("statistics") { 
            StatisticsScreen(
                todayBlocked = blockedCount,          // ✅ Usar contador existente
                weeklyStats = emptyList(),            // ✅ Placeholder seguro
                recentBlocked = recentBlocked,        // ✅ Ya lo tienes
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("settings") { 
            SettingsScreen(
                navController = navController,        // ✅ Pasar explícitamente
                onBack = { navController.popBackStack() }
            ) 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    protectionMode: ProtectionMode,
    onModeChange: (ProtectionMode) -> Unit,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    blockedCount: Int,
    recentBlocked: List<BlockedSiteEntity>,
    currentProfile: UserProfileEntity?,
    onOpenBrowser: () -> Unit,
    isMonitoringActive: Boolean,
    hasUsageStatsPermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "GuardianOS Shield",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Rounded.Settings, "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil activo
            item {
                ProfileIndicator(currentProfile)
            }

            // Selector de modo
            item {
                ProtectionModeSelector(
                    selectedMode = protectionMode,
                    onModeSelected = onModeChange
                )
            }

            // Estado del servicio
            item {
                StatusCard(
                    isActive = isServiceRunning,
                    onToggle = onToggleService,
                    mode = protectionMode
                )
            }

            // Monitoreo de apps
            if (protectionMode == ProtectionMode.Recommended) {
                item {
                    MonitoringCard(
                        isActive = isMonitoringActive,
                        hasPermission = hasUsageStatsPermission,
                        onRequestPermission = onRequestUsagePermission,
                        onToggle = onToggleMonitoring
                    )
                }
            }

            // Estadísticas rápidas
            item {
                QuickStatsCard(
                    count = blockedCount,
                    onClick = { onNavigate("statistics") }
                )
            }

            // Acciones rápidas
            item {
                Text(
                    "Acciones Rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureButton(
                        modifier = Modifier.weight(1f),
                        title = "Control",
                        subtitle = "Parental",
                        icon = Icons.Rounded.FamilyRestroom,
                        color = Color(0xFF4CAF50),
                        onClick = { onNavigate("parental") }
                    )
                    FeatureButton(
                        modifier = Modifier.weight(1f),
                        title = "Filtros",
                        subtitle = "Custom",
                        icon = Icons.Rounded.FilterAlt,
                        color = Color(0xFF2196F3),
                        onClick = { onNavigate("filters") }
                    )
                }
            }

            item {
                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Icon(Icons.Rounded.Language, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir Navegador Seguro")
                }
            }

            // Sitios bloqueados recientes
            if (recentBlocked.isNotEmpty()) {
                item {
                    Text(
                        "Bloqueados Recientemente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(recentBlocked.take(5)) { site ->
                    BlockedSiteRow(site)
                }
            }
        }
    }
}

// ============= COMPONENTES =============
@Composable
fun ProtectionModeSelector(
    selectedMode: ProtectionMode,
    onModeSelected: (ProtectionMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Modo de Protección",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = "Recomendado",
                    isSelected = selectedMode == ProtectionMode.Recommended,
                    onClick = { onModeSelected(ProtectionMode.Recommended) }
                )
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = "Avanzado",
                    isSelected = selectedMode == ProtectionMode.Advanced,
                    onClick = { onModeSelected(ProtectionMode.Advanced) }
                )
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = "Manual",
                    isSelected = selectedMode == ProtectionMode.CustomStats,
                    onClick = { onModeSelected(ProtectionMode.CustomStats) }
                )
            }
        }
    }
}

@Composable
fun ModeChip(
    modifier: Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        label = { 
            Text(
                label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            ) 
        }
    )
}

@Composable
fun ProfileIndicator(profile: UserProfileEntity?) {
    if (profile != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1976D2).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Perfil: ${profile.ageGroup}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    isActive: Boolean,
    onToggle: () -> Unit,
    mode: ProtectionMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else 
                Color(0xFFEF5350).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isActive) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isActive) Icons.Rounded.Shield else Icons.Rounded.ShieldMoon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (isActive) "Protección Activa" else "Protección Inactiva",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (mode) {
                            ProtectionMode.Recommended -> "Modo ligero"
                            ProtectionMode.Advanced -> "VPN activa"
                            ProtectionMode.CustomStats -> "Manual"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (mode == ProtectionMode.Advanced) {
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
fun MonitoringCard(
    isActive: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Monitoreo de Apps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isActive) "Activo" else "Inactivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (hasPermission) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { onToggle() }
                    )
                }
            }

            if (!hasPermission) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conceder Permiso")
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Amenazas Bloqueadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun FeatureButton(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BlockedSiteRow(site: BlockedSiteEntity) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = site.domain, 
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            supportingContent = { 
                Text(
                    "Categoría: ${site.category}",
                    style = MaterialTheme.typography.bodySmall
                ) 
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Text(
                    text = sdf.format(Date(site.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
fun WelcomeSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "GuardianOS Shield",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Protección web inteligente para tu familia",
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
