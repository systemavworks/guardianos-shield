
package com.guardianos.shield

import com.guardianos.shield.billing.BillingManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

// ============= IMPORTS CORREGIDOS Y OPTIMIZADOS =============
// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*

// Android
import android.Manifest
import android.annotation.SuppressLint
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
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

// App
import com.guardianos.shield.BuildConfig
import com.guardianos.shield.R
import com.guardianos.shield.data.*
import com.guardianos.shield.security.SecurityHelper
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

    // Billing
    internal lateinit var billingManager: BillingManager
    private var isPremium by mutableStateOf(false)
    private var isPremiumLoaded by mutableStateOf(false)  // evita flash de pantalla premium
    private var showPremiumScreen by mutableStateOf(false)

    // Trial gratuito de 48 horas
    private var isFreeTrialActive by mutableStateOf(true)
    private var freeTrialRemainingHours by mutableStateOf(48f)
    private var showFreeTrialExpiredDialog by mutableStateOf(false)

    // Estados
    private var protectionMode by mutableStateOf(ProtectionMode.Recommended)
    private var isServiceRunning by mutableStateOf(false)
    private var blockedCount by mutableStateOf(0)
    private val recentBlocked = mutableStateListOf<BlockedSiteEntity>()
    internal var currentProfile by mutableStateOf<UserProfileEntity?>(null)
    private val customFiltersBlacklist = mutableStateListOf<CustomFilterEntity>()
    private val customFiltersWhitelist = mutableStateListOf<CustomFilterEntity>()
    private var showPinForVpnToggle by mutableStateOf(false)
    private var pendingModeChange: ProtectionMode? by mutableStateOf(null)
    private var showPinForModeChange by mutableStateOf(false)

    // Servicios
    internal lateinit var repository: GuardianRepository
    internal lateinit var settingsRepository: SettingsRepository
    private lateinit var usageMonitor: UsageStatsMonitor
    private lateinit var vpnStateReceiver: BroadcastReceiver
    
    // Estados de monitoreo
    private var isMonitoringActive by mutableStateOf(false)
    private var hasUsageStatsPermission by mutableStateOf(false)

    // Launchers de permisos
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Se requiere permiso VPN", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
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
        
        billingManager = BillingManager(this)

        initializeApp()
        setupVpnReceiver()
        loadInitialData()
        requestNotificationPermission()

        // Inicializar y monitorear trial de 48 horas
        lifecycleScope.launch {
            settingsRepository.setFreeTrialStartTimeIfNotSet()
            updateFreeTrialStatus()
        }
        lifecycleScope.launch {
            while (true) {
                delay(60_000L) // verificar cada minuto
                if (!isPremium) updateFreeTrialStatus()
            }
        }

        // Iniciar billing DESPUÉS de initializeApp() para que settingsRepository esté listo
        billingManager.startConnection {
            lifecycleScope.launch {
                billingManager.isPremium.collect { premium ->
                    isPremium = premium
                    if (premium) settingsRepository.setPremium(true)
                }
            }
        }

        // Sincronizar premium con DataStore (settingsRepository ya inicializado)
        lifecycleScope.launch {
            settingsRepository.isPremium.collect { premium ->
                isPremium = premium
                // En builds debug se fuerza premium para testing sin compra real.
                // BuildConfig.FORCE_PREMIUM = false en release → R8 elimina este bloque.
                if (BuildConfig.FORCE_PREMIUM) isPremium = true
                isPremiumLoaded = true  // primer valor recibido → ya podemos mostrar la UI correcta
            }
        }
        
        setContent {
            GuardianShieldTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }
                if (showSplash || !isPremiumLoaded) {
                    WelcomeSplashScreen()
                } else {
                    // ── Modelo freemium: todos los usuarios acceden a la app ──
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
                        onToggleMonitoring = { toggleMonitoring() },
                        onShowPremium = {
                            ConversionTracker.trackPurchaseScreenOpened("main_button")
                            showPremiumScreen = true
                        },
                        isPremium = isPremium,
                        isFreeTrialActive = isFreeTrialActive,
                        freeTrialRemainingHours = freeTrialRemainingHours
                    )
                    // Pantalla de upgrade como overlay (no paywall)
                    if (showPremiumScreen) {
                        PremiumPurchaseScreen(
                            billingManager = billingManager,
                            isPremium = isPremium,
                            onPurchaseSuccess = {
                                isPremium = true
                                showPremiumScreen = false
                                showFreeTrialExpiredDialog = false
                                ConversionTracker.trackPurchaseSuccess()
                                lifecycleScope.launch { settingsRepository.setPremium(true) }
                            },
                            activity = this@MainActivity,
                            onDismiss = {
                                showPremiumScreen = false
                                // Si el trial ha expirado y no se compró, restaurar el bloqueo
                                if (!isPremium && !isFreeTrialActive) {
                                    showFreeTrialExpiredDialog = true
                                }
                            }
                        )
                    }
                    // Diálogo de trial expirado — bloquea la app (sin dismiss libre)
                    if (showFreeTrialExpiredDialog && !isPremium) {
                        FreeTrialExpiredDialog(
                            onUpgrade = {
                                ConversionTracker.trackTrialExpiredUpgradeClicked()
                                ConversionTracker.trackPurchaseScreenOpened("trial_expired_dialog")
                                showFreeTrialExpiredDialog = false
                                showPremiumScreen = true
                            },
                            onDismiss = { finish() } // "Salir de la app"
                        )
                    }
                    if (showPinForVpnToggle) {
                        PinLockScreen(
                            requiredPin = currentProfile?.parentalPin,
                            profileId = currentProfile?.id,
                            onPinVerified = {
                                showPinForVpnToggle = false
                                stopVpnService()
                            },
                            onBack = {
                                showPinForVpnToggle = false
                            }
                        )
                    }
                    if (showPinForModeChange) {
                        PinLockScreen(
                            requiredPin = currentProfile?.parentalPin,
                            profileId = currentProfile?.id,
                            onPinVerified = {
                                showPinForModeChange = false
                                pendingModeChange?.let { applyModeChange(it) }
                                pendingModeChange = null
                            },
                            onBack = {
                                showPinForModeChange = false
                                pendingModeChange = null
                            }
                        )
                    }
                }
            }
        }
    }

    // ============= INICIALIZACIÓN =============
    private fun initializeApp() {
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        settingsRepository = SettingsRepository(this)
        usageMonitor = UsageStatsMonitor(this, repository)
        hasUsageStatsPermission = usageMonitor.hasPermission()
        
        // Log información del dispositivo para debugging
        logDeviceInfo()
        
        // Restaurar estado guardado
        lifecycleScope.launch {
            settingsRepository.settings.collect { settings ->
                protectionMode = when(settings.protectionMode) {
                    "Advanced" -> ProtectionMode.Advanced
                    "CustomStats" -> ProtectionMode.CustomStats
                    else -> ProtectionMode.Recommended
                }
                isServiceRunning = settings.isVpnActive
                isMonitoringActive = settings.isMonitoringActive
            }
        }
        
        // ✅ INICIAR MONITOREO AUTOMÁTICAMENTE si tiene permisos
        if (hasUsageStatsPermission) {
            Log.d("MainActivity", "✅ Permiso UsageStats detectado - Iniciando monitoreo automático...")
            AppMonitorService.start(this)
            isMonitoringActive = true
        } else {
            Log.w("MainActivity", "⚠️ Permiso UsageStats NO concedido - Monitoreo desactivado")
        }

        // ✅ Programar resumen semanal (domingos ~20:00) con WorkManager
        programarResumenSemanal()
    }

    /**
     * Programa el WorkManager para enviar un resumen semanal de bloqueos cada domingo.
     * Usa enqueueUniquePeriodicWork con política KEEP para no reprogramar si ya existe.
     */
    private fun programarResumenSemanal() {
        try {
            val ahora = java.util.Calendar.getInstance()
            val proximoDomingo = java.util.Calendar.getInstance().apply {
                // Avanzar hasta el próximo domingo a las 20:00
                while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                set(java.util.Calendar.HOUR_OF_DAY, 20)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                // Si hoy es domingo pero ya pasaron las 20:00, ir al siguiente domingo
                if (timeInMillis <= ahora.timeInMillis) {
                    add(java.util.Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val retardoInicial = proximoDomingo.timeInMillis - ahora.timeInMillis

            val peticion = androidx.work.PeriodicWorkRequestBuilder<com.guardianos.shield.service.WeeklySummaryWorker>(
                7, java.util.concurrent.TimeUnit.DAYS
            )
                .setInitialDelay(retardoInicial, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                com.guardianos.shield.service.WeeklySummaryWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                peticion
            )
            Log.i("MainActivity", "📅 Resumen semanal programado para el próximo domingo a las 20:00")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error programando resumen semanal: ${e.message}")
        }
    }
    
    private fun logDeviceInfo() {
        val androidVersion = when (Build.VERSION.SDK_INT) {
            31 -> "Android 12 (API 31)"
            32 -> "Android 12L (API 32)"
            33 -> "Android 13 (API 33)"
            34 -> "Android 14 (API 34)"
            35 -> "Android 15 (API 35)"
            else -> "Android ${Build.VERSION.SDK_INT}"
        }
        
        Log.i("MainActivity", "╔════════════════════════════════════════════════╗")
        Log.i("MainActivity", "║  GuardianOS Shield - Información del Sistema  ║")
        Log.i("MainActivity", "╠════════════════════════════════════════════════╣")
        Log.i("MainActivity", "║  Versión Android: $androidVersion")
        Log.i("MainActivity", "║  Fabricante: ${Build.MANUFACTURER}")
        Log.i("MainActivity", "║  Modelo: ${Build.MODEL}")
        Log.i("MainActivity", "║  Marca: ${Build.BRAND}")
        Log.i("MainActivity", "╚════════════════════════════════════════════════╝")
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
                        // Sincronizar DataStore para que el Flow no revierta el estado
                        lifecycleScope.launch {
                            settingsRepository.updateVpnActive(false)
                        }
                    }
                    DnsFilterService.ACTION_VPN_ERROR -> {
                        isServiceRunning = false
                        lifecycleScope.launch {
                            settingsRepository.updateVpnActive(false)
                        }
                        val errorMsg = when {
                            Build.VERSION.SDK_INT >= 35 -> 
                                "Error VPN en Android 15+. Revoca y vuelve a conceder permiso VPN en Ajustes"
                            Build.VERSION.SDK_INT >= 33 -> 
                                "Error VPN en Android 13+. Verifica permisos de notificaciones y VPN"
                            else -> 
                                "Error en servicio VPN. Verifica que no haya otra VPN activa"
                        }
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("MainActivity", "VPN ERROR - Android ${Build.VERSION.SDK_INT} (API ${Build.VERSION.SDK_INT})")
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
        Log.d("MainActivity", "🚀 loadInitialData() INICIADO")
        lifecycleScope.launch {
            Log.d("MainActivity", "📡 Coroutine iniciada - consultando perfil...")
            try {
                currentProfile = repository.getActiveProfile()
                Log.d("MainActivity", "✅ getActiveProfile() completado - Resultado: ${currentProfile?.name ?: "null"}")
                
                // 🔍 DEBUG: Mostrar estado del perfil y horarios
                currentProfile?.let { profile ->
                    if (BuildConfig.DEBUG) {
                        Log.d("MainActivity", "═══════════════════════════════════════")
                        Log.d("MainActivity", "📋 PERFIL ACTIVO: ${profile.name}")
                        Log.d("MainActivity", "🔒 PIN configurado: ${SecurityHelper.hasPin(this@MainActivity, profile.id)}")
                        Log.d("MainActivity", "⏰ Horario habilitado: ${profile.scheduleEnabled}")
                    }
                    if (profile.scheduleEnabled && BuildConfig.DEBUG) {
                        val startHour = profile.startTimeMinutes / 60
                        val startMin = profile.startTimeMinutes % 60
                        val endHour = profile.endTimeMinutes / 60
                        val endMin = profile.endTimeMinutes % 60
                        Log.d("MainActivity", "⏰ Desde: ${String.format("%02d:%02d", startHour, startMin)}")
                        Log.d("MainActivity", "⏰ Hasta: ${String.format("%02d:%02d", endHour, endMin)}")
                        Log.d("MainActivity", "⏰ Ahora dentro del horario: ${profile.isWithinAllowedTime()}")
                        Log.d("MainActivity", "═══════════════════════════════════════")
                    }
                } ?: run {
                    Log.w("MainActivity", "⚠️ No hay perfil activo - Creando perfil por defecto...")
                    // Crear perfil por defecto si no existe
                    repository.createProfile(
                        name = getString(R.string.default_profile_name),
                        age = null,
                        restrictionLevel = "MEDIUM",
                        parentalPin = null
                    )
                    currentProfile = repository.getActiveProfile()
                    Log.i("MainActivity", "✅ Perfil por defecto creado: ${currentProfile?.name ?: "Desconocido"}")
                }
                
                loadStatistics()
                loadCustomFilters()
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ ERROR en loadInitialData(): ${e.message}", e)
            }
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
        // Observar historial completo en tiempo real (sin límite artificial de 20)
        lifecycleScope.launch {
            repository.recentBlocked.collect { sites ->
                recentBlocked.clear()
                recentBlocked.addAll(sites)
                Log.d("MainActivity", "📊 Historial actualizado: ${sites.size} registros")
            }
        }
        // Contador de HOY separado y preciso (usa todayStartTimestamp interno del Repository)
        lifecycleScope.launch {
            repository.todayBlockedCount.collect { count ->
                blockedCount = count
            }
        }
    }

    // ============= GESTIÓN DE MODOS =============
    private fun handleModeChange(newMode: ProtectionMode) {
        // Si VPN está activo y hay PIN configurado, pedir verificación antes de cambiar modo
        if (isServiceRunning && !currentProfile?.parentalPin.isNullOrEmpty()) {
            pendingModeChange = newMode
            showPinForModeChange = true
            return
        }

        applyModeChange(newMode)
    }

    private fun applyModeChange(newMode: ProtectionMode) {
        protectionMode = newMode
        
        // Guardar modo en DataStore
        lifecycleScope.launch {
            settingsRepository.updateProtectionMode(newMode.name)
        }
        
        when (newMode) {
            ProtectionMode.Recommended -> {
                // Recomendado: activa VPN + DNS CleanBrowsing automáticamente (sin configuración manual)
                lifecycleScope.launch {
                    stopService(Intent(this@MainActivity, LightweightMonitorService::class.java))
                    delay(300)
                    if (!isServiceRunning) {
                        requestVpnPermission()
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Modo Recomendado — Filtrado DNS activado automáticamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ProtectionMode.Advanced -> {
                // Avanzado: VPN + DNS CleanBrowsing + monitoreo de apps activo
                lifecycleScope.launch {
                    stopService(Intent(this@MainActivity, LightweightMonitorService::class.java))
                    delay(300)
                    if (!isServiceRunning) {
                        requestVpnPermission()
                    }
                    // Activar también el monitoreo de apps si tiene permiso
                    if (hasUsageStatsPermission && !isMonitoringActive) {
                        startMonitoring()
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Modo Avanzado — VPN + monitoreo de apps activo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ProtectionMode.CustomStats -> {
                lifecycleScope.launch {
                    if (isServiceRunning) {
                        stopVpnService()
                        delay(300)
                    }
                    stopMonitoring()
                    Toast.makeText(
                        this@MainActivity,
                        "Modo Manual — Sin protección DNS activa",
                        Toast.LENGTH_SHORT
                    ).show()
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
            // Si VPN está activo y hay PIN configurado, pedir verificación antes de desactivar
            if (!currentProfile?.parentalPin.isNullOrEmpty()) {
                showPinForVpnToggle = true
            } else {
                stopVpnService()
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
        
        // Guardar estado en DataStore
        lifecycleScope.launch {
            settingsRepository.updateVpnActive(true)
        }
    }

    private fun stopVpnService() {
        stopService(Intent(this, DnsFilterService::class.java))
        isServiceRunning = false
        
        // Guardar estado en DataStore
        lifecycleScope.launch {
            settingsRepository.updateVpnActive(false)
        }
    }

    /** Refresca el estado del trial y gestiona la expiración (detiene VPN si corresponde). */
    private suspend fun updateFreeTrialStatus() {
        if (isPremium) return
        isFreeTrialActive = settingsRepository.isFreeTrialActive()
        freeTrialRemainingHours = settingsRepository.getFreeTrialRemainingHours()
        if (!isFreeTrialActive) {
            // Detener VPN desde la UI si sigue activa
            if (isServiceRunning) stopVpnService()
            // Mostrar diálogo bloqueante solo si la pantalla Premium no está abierta
            if (!showPremiumScreen) {
                ConversionTracker.trackTrialExpired()
                showFreeTrialExpiredDialog = true
            }
        }
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
        
        // Guardar estado en DataStore
        lifecycleScope.launch {
            settingsRepository.updateMonitoringActive(true)
        }
        
        if (!isPremium) {
            Toast.makeText(
                this,
                "⏰ Monitoreo activo — Modo FREE: datos de las últimas 48h.\nActualiza a Premium para historial de 30 días.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Monitoreo de apps activado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMonitoring() {
        AppMonitorService.stop(this)
        isMonitoringActive = false
        
        // Guardar estado en DataStore
        lifecycleScope.launch {
            settingsRepository.updateMonitoringActive(false)
        }
        
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
        startActivity(SafeBrowserActivity.createIntent(this, isPremium))
    }

    private fun requestNotificationPermission() {
        // Android 13+ requiere permiso explícito de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 y anteriores: notificaciones habilitadas por defecto
            Log.d("MainActivity", "Android ${Build.VERSION.SDK_INT}: Notificaciones por defecto")
        }
    }

    override fun onResume() {
        super.onResume()
        hasUsageStatsPermission = usageMonitor.hasPermission()
        // Sincronizar estado real de VPN al volver a primer plano
        // (evita desincronía cuando Android revoca la VPN externamente)
        val realVpnRunning = isDnsFilterServiceRunning()
        if (isServiceRunning != realVpnRunning) {
            isServiceRunning = realVpnRunning
            lifecycleScope.launch {
                settingsRepository.updateVpnActive(realVpnRunning)
            }
        }
        loadStatistics()
        // Reconectar Billing si se desconectó en background y re-verificar compras existentes
        billingManager.startConnection()
        // Re-verificar estado del trial por si la app estuvo en background un largo periodo
        if (!isPremium) {
            lifecycleScope.launch { updateFreeTrialStatus() }
        }
    }

    /** Comprueba si DnsFilterService está en ejecución consultando los servicios del proceso propio. */
    private fun isDnsFilterServiceRunning(): Boolean {
        return try {
            val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == DnsFilterService::class.java.name }
        } catch (e: Exception) {
            Log.w("MainActivity", "No se pudo verificar estado real de VPN: ${e.message}")
            isServiceRunning // Conservar estado actual si falla la comprobación
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
        ConversionTracker.printSessionSummary()
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
    onToggleMonitoring: () -> Unit,
    onShowPremium: () -> Unit,
    isPremium: Boolean = false,
    isFreeTrialActive: Boolean = true,
    freeTrialRemainingHours: Float = 48f
) {
    val navController = rememberNavController()

    // ── Protección PIN para secciones sensibles ────────────────────────────
    // "parental" y "filters" requieren PIN cuando hay uno configurado.
    // "pacto" nunca requiere PIN (acceso libre para el menor).
    var pendingProtectedRoute by remember { mutableStateOf<String?>(null) }
    var showPinForRoute by remember { mutableStateOf(false) }

    /** Determina si la ruta requiere verificación PIN antes de navegar */
    val protectedRoutes = setOf("parental", "filters")

    val onNavigateGuarded: (String) -> Unit = { route ->
        if (route in protectedRoutes) {
            // Siempre se muestra el PinLockScreen para rutas protegidas:
            // - Con PIN configurado → requiere verificación real
            // - Sin PIN → muestra aviso "PIN no configurado" con acceso directo
            //   (para que el tutor sepa que debe configurar uno)
            pendingProtectedRoute = route
            showPinForRoute = true
        } else {
            navController.navigate(route)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                onNavigate = onNavigateGuarded,
                onShowPremium = onShowPremium,
                isPremium = isPremium,
                isFreeTrialActive = isFreeTrialActive,
                freeTrialRemainingHours = freeTrialRemainingHours
            )
        }
        composable("parental") {
            val mainActivity = navController.context as MainActivity

            // Observar todos los perfiles en tiempo real
            val allProfiles by mainActivity.repository.allProfilesFlow
                .collectAsState(initial = emptyList())

            ParentalControlScreen(
                allProfiles = allProfiles,
                isFreeTrialActive = isFreeTrialActive,
                onProfileUpdate = { profile ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.updateProfile(profile)
                        mainActivity.currentProfile = mainActivity.repository.getActiveProfile()
                    }
                },
                onCreateProfile = { name, age ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.createProfile(name = name, age = age)
                        // El nuevo perfil queda activo tras la creación
                        mainActivity.currentProfile = mainActivity.repository.getActiveProfile()
                    }
                },
                onDeleteProfile = { profileId ->
                    mainActivity.lifecycleScope.launch {
                        // Si se borra el perfil activo, activar el primero restante
                        val wasActive = mainActivity.currentProfile?.id == profileId
                        mainActivity.repository.deleteProfile(profileId)
                        if (wasActive) {
                            val remaining = mainActivity.repository.getAllProfiles()
                            remaining.firstOrNull()?.let { first ->
                                mainActivity.repository.setActiveProfile(first.id)
                            }
                        }
                        mainActivity.currentProfile = mainActivity.repository.getActiveProfile()
                    }
                },
                onSetActiveProfile = { profileId ->
                    mainActivity.lifecycleScope.launch {
                        mainActivity.repository.setActiveProfile(profileId)
                        mainActivity.currentProfile = mainActivity.repository.getActiveProfile()
                    }
                },
                onBack = { navController.popBackStack() },
                isPremium = isPremium,
                onShowPremium = onShowPremium
            )
        }
        composable("filters") { 
            val mainActivity = navController.context as MainActivity
            
            // Cargar listas de filtros personalizados desde el Repository
            val blacklist by mainActivity.repository.blacklist.collectAsState(initial = emptyList())
            val whitelist by mainActivity.repository.whitelist.collectAsState(initial = emptyList())
            
            CustomFiltersScreen(
                blacklist = blacklist,
                whitelist = whitelist,
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
                onBack = { navController.popBackStack() },
                isPremium = isPremium,
                isFreeTrialActive = isFreeTrialActive,
                onShowPremium = onShowPremium
            ) 
        }
        composable("statistics") {
            val mainActivity = navController.context as MainActivity
            // Conectar weeklyStats desde el Flow real del repositorio
            val weeklyStats by mainActivity.repository.last30DaysStats
                .collectAsState(initial = emptyList())
            StatisticsScreen(
                todayBlocked = blockedCount,
                weeklyStats = weeklyStats,
                recentBlocked = recentBlocked,
                onBack = { navController.popBackStack() },
                isPremium = isPremium,
                onShowPremium = onShowPremium
            )
        }
        composable("pacto") {
            PactScreen(
                isPremium = isPremium,
                isFreeTrialActive = isFreeTrialActive,
                onShowPremium = onShowPremium,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") { 
            SettingsScreen(
                navController = navController,
                onBack = { navController.popBackStack() },
                currentPin = currentProfile?.parentalPin,
                profileId = currentProfile?.id,
                isPremium = isPremium,
                isFreeTrialActive = isFreeTrialActive,
                onShowPremium = onShowPremium,
                onRestorePurchases = {
                    (navController.context as MainActivity).let { activity ->
                        activity.billingManager.restorePurchases()
                        // Sincronizar el estado con DataStore si la restauración lo activa
                        activity.lifecycleScope.launch {
                            activity.billingManager.isPremium.collect { premium ->
                                if (premium) activity.settingsRepository.setPremium(true)
                            }
                        }
                    }
                }
            ) 
        }
        composable("help") {
            val ctx = LocalContext.current
            HelpScreen(
                onBack = { navController.popBackStack() },
                isVpnActive = isServiceRunning,
                isMonitoringActive = isMonitoringActive,
                hasAccessibilityService = AppBlockerAccessibilityService.estaActivado(ctx),
                hasPin = !currentProfile?.parentalPin.isNullOrEmpty(),
                hasProfileConfigured = currentProfile != null &&
                    currentProfile.name.isNotBlank() &&
                    currentProfile.name !in listOf("Niño/a", "Perfil sin nombre")
            )
        }
    } // end NavHost

        // ── Overlay de verificación PIN ─────────────────────────────────────
        // Se muestra sobre el NavHost cuando el usuario intenta acceder a
        // "Control Parental" o "Filtros" y hay un PIN configurado.
        if (showPinForRoute) {
            PinLockScreen(
                requiredPin = currentProfile?.parentalPin,
                profileId = currentProfile?.id,
                onPinVerified = {
                    showPinForRoute = false
                    pendingProtectedRoute?.let { navController.navigate(it) }
                    pendingProtectedRoute = null
                },
                onBack = {
                    showPinForRoute = false
                    pendingProtectedRoute = null
                }
            )
        }
    } // end Box
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
    onNavigate: (String) -> Unit,
    onShowPremium: () -> Unit,
    isPremium: Boolean = false,
    isFreeTrialActive: Boolean = true,
    freeTrialRemainingHours: Float = 48f
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.guardianos_shield_logo),
                            contentDescription = "Logo Guardianos Shield",
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "GuardianOS Shield",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Badge de modo: FREE TRIAL / FREE / ✓ PREMIUM
                    val (badgeText, badgeColor, badgeTextColor) = when {
                        isPremium -> Triple("✓ PREMIUM", Color(0xFF4CAF50), Color.White)
                        isFreeTrialActive -> Triple("FREE TRIAL", Color(0xFFFFC107), Color(0xFF4A3800))
                        else -> Triple("FREE", Color(0xFF9E9E9E), Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(badgeColor, RoundedCornerShape(20.dp))
                            .clickable(enabled = !isPremium) { onShowPremium() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }
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
            // Banner de estado free — trial activo (cuenta atrás) o plan básico
            if (!isPremium) {
                item {
                    if (isFreeTrialActive) {
                        FreeTrialBanner(
                            remainingHours = freeTrialRemainingHours,
                            onUpgrade = onShowPremium
                        )
                    } else {
                        FreePlanBanner(onUpgrade = onShowPremium)
                    }
                }
            }

            // Perfil activo
            item {
                ProfileIndicator(currentProfile)
            }

            // Widget de racha diaria (gamificación)
            item {
                StreakWidget(
                    perfil = currentProfile,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // TrustFlow Engine — indicador de nivel de confianza actual
            currentProfile?.let { perfil ->
                item {
                    TrustLevelBadge(
                        rachaActual = perfil.rachaActual,
                        isPremium = isPremium,
                        isFreeTrialActive = isFreeTrialActive,
                        onShowPremium = onShowPremium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

            // Monitoreo de apps (Recomendado y Avanzado)
            if (protectionMode == ProtectionMode.Recommended || protectionMode == ProtectionMode.Advanced) {
                item {
                    MonitoringCard(
                        isActive = isMonitoringActive,
                        hasPermission = hasUsageStatsPermission,
                        onRequestPermission = onRequestUsagePermission,
                        onToggle = onToggleMonitoring,
                        isPremium = isPremium
                    )
                }
            }

            // Estadísticas rápidas
            item {
                QuickStatsCard(
                    count = blockedCount,
                    onClick = { onNavigate("statistics") },
                    isPremium = isPremium
                )
            }

            // Acciones rápidas
            item {
                Text(
                    stringResource(R.string.home_quick_actions),
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
                        title = stringResource(R.string.home_btn_parental_title),
                        subtitle = stringResource(R.string.home_btn_parental_subtitle),
                        icon = Icons.Rounded.FamilyRestroom,
                        color = Color(0xFF4CAF50),
                        onClick = { onNavigate("parental") }
                    )
                    FeatureButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_btn_filters_title),
                        subtitle = stringResource(R.string.home_btn_filters_subtitle),
                        icon = Icons.Rounded.FilterAlt,
                        color = Color(0xFF2196F3),
                        onClick = { onNavigate("filters") }
                    )
                    FeatureButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_btn_pact_title),
                        subtitle = stringResource(R.string.home_btn_pact_subtitle),
                        icon = Icons.Rounded.Handshake,
                        color = Color(0xFF00ACC1),
                        onClick = { onNavigate("pacto") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isPremium) {
                        FeatureButton(
                            modifier = Modifier.weight(1f),
                            title = "Premium",
                            subtitle = stringResource(R.string.home_btn_premium_subtitle_free),
                            icon = Icons.Rounded.Star,
                            color = Color(0xFFFFC107),
                            onClick = { onShowPremium() }
                        )
                    } else {
                        FeatureButton(
                            modifier = Modifier.weight(1f),
                            title = "Premium",
                            subtitle = stringResource(R.string.home_btn_premium_subtitle_active),
                            icon = Icons.Rounded.Star,
                            color = Color(0xFF4CAF50),
                            onClick = { }
                        )
                    }
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
                    Text(stringResource(R.string.home_open_safe_browser))
                }
            }

            // Sitios bloqueados recientes
            if (recentBlocked.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.home_recent_blocked),
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
                stringResource(R.string.home_mode_protection_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_mode_recommended),
                    isSelected = selectedMode == ProtectionMode.Recommended,
                    onClick = { onModeSelected(ProtectionMode.Recommended) }
                )
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_mode_advanced),
                    isSelected = selectedMode == ProtectionMode.Advanced,
                    onClick = { onModeSelected(ProtectionMode.Advanced) }
                )
                ModeChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_mode_manual),
                    isSelected = selectedMode == ProtectionMode.CustomStats,
                    onClick = { onModeSelected(ProtectionMode.CustomStats) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Descripción del modo seleccionado
            val modoIcono = when (selectedMode) {
                ProtectionMode.Recommended -> "🛡️"
                ProtectionMode.Advanced -> "🔒"
                ProtectionMode.CustomStats -> "⚙️"
            }
            val modoDescripcion = when (selectedMode) {
                ProtectionMode.Recommended -> stringResource(R.string.home_mode_recommended_desc)
                ProtectionMode.Advanced -> stringResource(R.string.home_mode_advanced_desc)
                ProtectionMode.CustomStats -> stringResource(R.string.home_mode_manual_desc)
            }
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = modoIcono,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = modoDescripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        stringResource(R.string.home_profile_label, profile.ageGroup),
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
                        if (isActive) stringResource(R.string.home_status_active) else stringResource(R.string.home_status_inactive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (mode) {
                            ProtectionMode.Recommended -> if (isActive) stringResource(R.string.home_status_dns_active) else stringResource(R.string.home_status_dns_inactive)
                            ProtectionMode.Advanced -> if (isActive) stringResource(R.string.home_status_vpn_active) else stringResource(R.string.home_status_vpn_inactive)
                            ProtectionMode.CustomStats -> stringResource(R.string.home_status_no_dns)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (mode == ProtectionMode.Recommended || mode == ProtectionMode.Advanced) {
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
    onToggle: () -> Unit,
    isPremium: Boolean = false
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
                            stringResource(R.string.home_monitoring_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isActive) stringResource(R.string.home_monitoring_active) else stringResource(R.string.home_monitoring_inactive),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isPremium) {
                            Text(
                                stringResource(R.string.home_monitoring_free_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800)
                            )
                        }
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
                    Text(stringResource(R.string.home_monitoring_grant_permission))
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(count: Int, onClick: () -> Unit, isPremium: Boolean = false) {
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
                    stringResource(R.string.home_threats_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_threats_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isPremium) {
                    Text(
                        stringResource(R.string.home_threats_free_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }
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

// ─────────────────────────────────────────────────────────────────────────────
// TrustFlow Engine — Indicador de nivel de confianza en el dashboard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Badge visual que muestra el nivel de confianza actual del perfil.
 * 🔴 Locked (0-6 d)  |  🟡 Caution (7-29 d)  |  🟢 Trusted (30+ d)
 * En modo FREE sin trial activo muestra una preview bloqueada con opción de upgrade.
 */
@Composable
fun TrustLevelBadge(
    rachaActual: Int,
    isPremium: Boolean,
    isFreeTrialActive: Boolean,
    onShowPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Gate billing: FREE sin trial ve solo un teaser con candado
    if (!com.guardianos.shield.billing.FreeTierLimits.canUseTrustFlow(isPremium, isFreeTrialActive)) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowPremium() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔒", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TrustFlow Engine",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(R.string.trust_free_preview, rachaActual,
                            when {
                                rachaActual >= 30 -> stringResource(R.string.trust_zone_trusted)
                                rachaActual >= 7  -> stringResource(R.string.trust_zone_caution)
                                else              -> stringResource(R.string.trust_zone_locked_path)
                            }),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.trust_free_upgrade),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        return
    }
    val nivel = com.guardianos.shield.data.getTrustLevel(rachaActual)

    val (colorFondo, colorTexto, icono, titulo, descripcion) = when (nivel) {
        com.guardianos.shield.data.TrustLevel.TRUSTED -> TrustLevelStyle(
            fondo       = Color(0xFF1B5E20).copy(alpha = 0.20f),
            texto       = Color(0xFF66BB6A),
            icono       = "🟢",
            titulo      = stringResource(R.string.trust_trusted_title),
            descripcion = stringResource(R.string.trust_trusted_desc, rachaActual)
        )
        com.guardianos.shield.data.TrustLevel.CAUTION -> TrustLevelStyle(
            fondo       = Color(0xFFF57F17).copy(alpha = 0.18f),
            texto       = Color(0xFFFFC107),
            icono       = "🟡",
            titulo      = stringResource(R.string.trust_caution_title),
            descripcion = stringResource(R.string.trust_caution_desc, rachaActual)
        )
        com.guardianos.shield.data.TrustLevel.LOCKED -> TrustLevelStyle(
            fondo       = Color(0xFFB71C1C).copy(alpha = 0.15f),
            texto       = Color(0xFFEF5350),
            icono       = "🔴",
            titulo      = stringResource(R.string.trust_locked_title),
            descripcion = stringResource(R.string.trust_locked_desc, 7 - rachaActual)
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icono, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorTexto
                    )
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        // Barra de progreso hacia el siguiente nivel
        if (nivel != com.guardianos.shield.data.TrustLevel.TRUSTED) {
            val progreso = if (nivel == com.guardianos.shield.data.TrustLevel.CAUTION)
                (rachaActual - 7) / 23f   // 7-29 días → hacia 30
            else
                rachaActual / 7f           // 0-6 días → hacia 7
            LinearProgressIndicator(
                progress = progreso.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                color = colorTexto,
                trackColor = colorTexto.copy(alpha = 0.15f)
            )
            Text(
                text = if (nivel == com.guardianos.shield.data.TrustLevel.CAUTION)
                    stringResource(R.string.trust_days_to_trusted, 30 - rachaActual)
                else
                    stringResource(R.string.trust_days_to_caution, 7 - rachaActual),
                style = MaterialTheme.typography.labelSmall,
                color = colorTexto.copy(alpha = 0.70f),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

private data class TrustLevelStyle(
    val fondo: Color,
    val texto: Color,
    val icono: String,
    val titulo: String,
    val descripcion: String
)
