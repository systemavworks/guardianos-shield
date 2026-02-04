package com.guardianos.shield.service

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.ui.SafeBrowserActivity
import kotlinx.coroutines.*
import java.util.Calendar

class RealisticAppBlocker : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var repository: GuardianRepository
    private var isMonitoring = false
    
    private val blockedApps = mutableSetOf<String>()
    
    private val browserApps = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.UCMobile.intl",
        "com.mi.globalbrowser"
    )
    
    private val socialMediaApps = setOf(
        "com.facebook.katana",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.snapchat.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.whatsapp",
        "com.telegram.messenger",
        "com.discord"
    )
    
    // El filtrado parental se gestiona solo desde GuardianRepository (persistente)
    
    companion object {
        private const val TAG = "RealisticAppBlocker"
        private const val NOTIFICATION_ID = 100
        private const val CHANNEL_ID = "app_blocker_channel"
        private const val CHECK_INTERVAL = 2000L
        
        fun start(context: Context) {
            val intent = Intent(context, RealisticAppBlocker::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, RealisticAppBlocker::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        createNotificationChannel()
        loadBlockedApps()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "⚠️ Permiso UsageStats no concedido")
            requestUsageStatsPermission()
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.d(TAG, "✅ Monitoreo iniciado")
        
        scope.launch {
            while (isMonitoring) {
                try {
                    checkForegroundApp()
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en monitoreo: ${e.message}", e)
                }
            }
        }
    }

    private fun checkForegroundApp() {
        val foregroundApp = getForegroundApp()
        if (foregroundApp != null) {
            val profile = runBlocking { repository.getActiveProfile() }
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val start = profile?.startTimeMinutes ?: 0
            val end = profile?.endTimeMinutes ?: 1439
            val horarioPermitido = profile?.scheduleEnabled != true || (currentMinutes in start..end)
            when {
                foregroundApp in browserApps -> {
                    handleBrowserApp(foregroundApp)
                }
                repository.isAppSensitive(foregroundApp) || foregroundApp in blockedApps -> {
                    if (!horarioPermitido) {
                        handleBlockedApp(foregroundApp)
                    }
                }
                foregroundApp in socialMediaApps -> {
                    if (!horarioPermitido) {
                        handleSocialMediaApp(foregroundApp)
                    }
                }
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - CHECK_INTERVAL
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun handleBrowserApp(packageName: String) {
        Log.d(TAG, "🌐 Navegador detectado: $packageName → Redirigiendo a SafeBrowser")
        
        // Redirigir a SafeBrowser
        val intent = Intent(this, SafeBrowserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        
        showNotification(
            "Navegador Protegido",
            "Usa el navegador seguro de GuardianOS Shield"
        )
        
        tryKillApp(packageName)
    }

    private fun handleBlockedApp(packageName: String) {
        val appName = getAppName(packageName)
        Log.w(TAG, "⛔ App bloqueada detectada: $appName")
        
        scope.launch {
            repository.logBlockedAccess(packageName)
        }
        
        showHighPriorityNotification(
            "App Bloqueada",
            "$appName está restringida por control parental"
        )
        
        goToHome()
        tryKillApp(packageName)
    }

    private fun handleSocialMediaApp(packageName: String) {
        scope.launch {
            val profile = repository.getActiveProfileSync()
            // Verificar si existe el campo blockSocialMedia en tu UserProfileEntity
            // Si no existe, comenta esta línea o ajusta según tu modelo
            // if (profile?.blockSocialMedia == true) {
            //     handleBlockedApp(packageName)
            // }
        }
    }

    private fun goToHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun tryKillApp(packageName: String) {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            Log.d(TAG, "🔪 Proceso cerrado: $packageName")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cerrar $packageName: ${e.message}")
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun loadBlockedApps() {
        scope.launch {
            try {
                val profile = repository.getActiveProfileSync()
                blockedApps.clear()
                // Ajustar según tu modelo de datos
                Log.d(TAG, "📋 Apps bloqueadas cargadas: ${blockedApps.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando apps bloqueadas: ${e.message}")
            }
        }
    }

    fun blockApp(packageName: String) {
        blockedApps.add(packageName)
        Log.d(TAG, "➕ App añadida a bloqueo: $packageName")
    }

    fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        Log.d(TAG, "➖ App eliminada de bloqueo: $packageName")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de Aplicaciones",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo de apps bloqueadas"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("🛡️ GuardianOS Shield")
        .setContentText("Protección de apps activa")
        .setSmallIcon(R.drawable.ic_shield)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun showNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showHighPriorityNotification(title: String, text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300))
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        isMonitoring = false
        scope.cancel()
        Log.d(TAG, "🛑 Servicio detenido")
        super.onDestroy()
    }
}
