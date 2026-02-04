package com.guardianos.shield.service

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.ui.SafeBrowserActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UsageStatsMonitor(
    private val context: Context,
    private val repository: GuardianRepository? = null
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    private var lastBlockedApp: String? = null
    private var lastBlockedTime: Long = 0
    private val mutex = Mutex()

    private val browserPackages = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
        "org.mozilla.firefox", "org.mozilla.firefox_beta", "com.opera.browser",
        "com.brave.browser", "com.microsoft.emmx", "com.sec.android.app.sbrowser",
        "com.android.browser", "com.duckduckgo.mobile.android"
    )
    
    private val socialMediaPackages = setOf(
        "com.instagram.android",           // Instagram
        "com.instagram.lite",              // Instagram Lite
        "com.zhiliaoapp.musically",        // TikTok
        "com.ss.android.ugc.trill",        // TikTok Lite
        "com.facebook.katana",             // Facebook
        "com.facebook.lite",               // Facebook Lite
        "com.whatsapp",                    // WhatsApp
        "com.whatsapp.w4b",                // WhatsApp Business
        "com.snapchat.android",            // Snapchat
        "com.twitter.android",             // Twitter/X
        "com.google.android.youtube",      // YouTube
        "com.google.android.apps.youtube.music",  // YouTube Music
        "com.facebook.orca",               // Messenger
        "com.facebook.mlite",              // Messenger Lite
        "com.telegram.messenger",          // Telegram
        "org.telegram.messenger",          // Telegram X
        "com.discord",                     // Discord
        "com.reddit.frontpage",            // Reddit
        "com.pinterest",                   // Pinterest
        "com.linkedin.android",            // LinkedIn
        "com.spotify.music",               // Spotify
        "com.netflix.mediaclient",         // Netflix
        "com.amazon.avod.thirdpartyclient", // Prime Video
        "tv.twitch.android.app"            // Twitch
    )
    
    companion object {
        private const val CHANNEL_ID = "GuardianShield_AppBlocked"
        private const val NOTIFICATION_ID_BASE = 3000
    }

    fun startMonitoring() {
        if (isMonitoring || !hasPermission()) return
        
        isMonitoring = true
        scope.launch {
            while (isActive) {
                try {
                    monitorForegroundApp()
                    delay(if (isScreenOn()) 2000L else 10000L)
                } catch (e: Exception) {
                    Log.e("UsageStatsMonitor", "Error monitoring apps", e)
                    delay(5000)
                }
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        scope.cancel()
    }

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private suspend fun monitorForegroundApp() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val statsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000

            val stats = statsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: return@withLock

            val foregroundApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return@withLock

            if (foregroundApp.startsWith("android.") || 
                foregroundApp.startsWith("com.android.") || 
                foregroundApp == context.packageName) return@withLock

            if (foregroundApp in browserPackages) {
                handleBrowserAttempt(foregroundApp)
            } else if (foregroundApp in socialMediaPackages) {
                handleSocialMediaAttempt(foregroundApp)
            }
        }
    }

    private fun handleSocialMediaAttempt(packageName: String) {
        if (lastBlockedApp == packageName && System.currentTimeMillis() - lastBlockedTime < 30000) return
        
        lastBlockedApp = packageName
        lastBlockedTime = System.currentTimeMillis()
        
        val appLabel = getAppLabel(packageName)

        // Verificar horario para apps de redes sociales
        repository?.let { repo ->
            scope.launch(Dispatchers.IO) {
                try {
                    val profile = repo.getActiveProfile()
                    
                    if (profile != null && !profile.isWithinAllowedTime()) {
                        // ⛔ FUERA DE HORARIO → Notificar + intentar cerrar
                        Log.i("UsageStatsMonitor", "⏰ FUERA DE HORARIO - Bloqueando app: $appLabel")
                        showAppBlockedByScheduleNotification(appLabel)
                        
                        // Registrar intento de uso bloqueado
                        repo.addBlockedSite(
                            domain = "$appLabel ($packageName)",
                            category = "APP_BLOQUEADA",
                            threatLevel = 3
                        )
                        Log.i("UsageStatsMonitor", "💾 Registrado bloqueo: $appLabel")
                        
                        // Intentar cerrar la app (funciona en Android 10-, limitado en 11+)
                        try {
                            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                            activityManager.killBackgroundProcesses(packageName)
                            Log.i("UsageStatsMonitor", "✅ Intento de cierre enviado: $appLabel (puede no funcionar en Android 11+)")
                        } catch (e: Exception) {
                            Log.w("UsageStatsMonitor", "⚠️ No se pudo cerrar $appLabel: ${e.message}")
                        }
                    } else {
                        // ✅ DENTRO DEL HORARIO → Solo notificar + registrar uso
                        Log.i("UsageStatsMonitor", "✓ Dentro del horario - Detectada: $appLabel")
                        showSocialMediaDetectedNotification(appLabel)
                        
                        // Registrar uso permitido (para reportes)
                        repo.addBlockedSite(
                            domain = "$appLabel ($packageName)",
                            category = "APP_PERMITIDA",
                            threatLevel = 0
                        )
                        Log.i("UsageStatsMonitor", "💾 Registrado uso permitido: $appLabel")
                    }
                } catch (e: Exception) {
                    Log.e("UsageStatsMonitor", "Error checking schedule for social media", e)
                    // En caso de error, solo notificar (fail-safe no intrusivo)
                    showSocialMediaDetectedNotification(appLabel)
                }
            }
        } ?: run {
            // Sin repository, solo notificar
            showSocialMediaDetectedNotification(appLabel)
        }
    }
    
    private fun handleBrowserAttempt(packageName: String) {
        if (lastBlockedApp == packageName && System.currentTimeMillis() - lastBlockedTime < 30000) return
        
        lastBlockedApp = packageName
        lastBlockedTime = System.currentTimeMillis()
        
        val appLabel = getAppLabel(packageName)

        // ✅ OPCIÓN 2: Redirigir SOLO si está fuera del horario permitido
        repository?.let { repo ->
            scope.launch(Dispatchers.IO) {
                try {
                    val profile = repo.getActiveProfile()
                    
                    if (profile != null && !profile.isWithinAllowedTime()) {
                        // ⛔ FUERA DE HORARIO → Redirigir obligatoriamente
                        Log.i("UsageStatsMonitor", "⏰ FUERA DE HORARIO - Redirigiendo: $appLabel")
                        showBlockedByScheduleNotification(appLabel)
                        redirectToSafeBrowser(packageName, appLabel)
                    } else {
                        // ✅ DENTRO DEL HORARIO → Solo notificar (sin redirección)
                        Log.i("UsageStatsMonitor", "✓ Dentro del horario - Solo notificando: $appLabel")
                        showBrowserDetectedNotification(appLabel)
                    }
                } catch (e: Exception) {
                    Log.e("UsageStatsMonitor", "Error checking schedule", e)
                    // Si hay error, solo notificar (fail-safe no intrusivo)
                    showBrowserDetectedNotification(appLabel)
                }
            }
        } ?: run {
            // Sin repository, solo notificar (no forzar redirección)
            showBrowserDetectedNotification(appLabel)
        }
    }
    
    private fun redirectToSafeBrowser(packageName: String, appLabel: String) {
        // 🔔 NOTIFICACIÓN DE APP BLOQUEADA
        showAppBlockedNotification(appLabel)

        if (packageName != "com.guardianos.shield") {
            try {
                val intent = Intent(context, SafeBrowserActivity::class.java).apply {
                    // ✅ FLAGS OPPO-COMPATIBLE: incluye CLEAR_TASK para forzar redirección
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra("redirected_from", packageName)
                    putExtra("app_label", appLabel)
                }
                
                // ✅ OPPO FIX: Usar ApplicationContext para evitar restricciones
                context.applicationContext.startActivity(intent)
                Log.d("UsageStatsMonitor", "✅ Redirigido a SafeBrowser desde: $appLabel")
            } catch (e: Exception) {
                Log.e("UsageStatsMonitor", "❌ Error redirigiendo a SafeBrowser: ${e.message}")
                // Fallback: mostrar notificación más prominente
                showCriticalBlockNotification(appLabel)
            }
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.split(".").lastOrNull()?.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            } ?: packageName
        } catch (e: Exception) {
            packageName
        }
    }

    private fun isScreenOn(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= 20) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }
        } catch (e: Exception) {
            Log.w("UsageStatsMonitor", "Error checking screen state: ${e.message}")
            true // Asumir pantalla encendida si falla
        }
    }
    
    // 🔔 SISTEMA DE NOTIFICACIONES
    
    // Notificación para apps de redes sociales (dentro del horario)
    private fun showSocialMediaDetectedNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("📱 App de red social detectada")
            .setContentText("Usando: $appName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Estás usando la app: $appName\n\nEl monitoreo de GuardianOS Shield está activo."))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + 4000 + appName.hashCode(), notification)
    }
    
    // Notificación de bloqueo (app fuera de horario)
    private fun showAppBlockedByScheduleNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🚫 App bloqueada por horario")
            .setContentText("$appName no está permitida ahora")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("La app $appName está bloqueada fuera del horario permitido.\n\nComprueba la configuración de horarios en Control Parental."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + 5000 + appName.hashCode(), notification)
    }
    
    // Notificación informativa (navegador detectado pero permitido)
    private fun showBrowserDetectedNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("ℹ️ Navegador externo detectado")
            .setContentText("Navegando con $appName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Estás usando el navegador externo: $appName\n\nRecuerda que puedes usar el Navegador Seguro desde la app."))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + 3000 + appName.hashCode(), notification)
    }
    
    private fun showAppBlockedNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🚫 App bloqueada")
            .setContentText("Navegador externo bloqueado: $appName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Se ha bloqueado el acceso al navegador externo:\n$appName\n\nRedirigido al Navegador Seguro"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + appName.hashCode(), notification)
    }
    
    private fun showBlockedByScheduleNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("⏰ Fuera de horario")
            .setContentText("$appName bloqueado fuera del horario permitido")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("No puedes usar $appName en este momento.\\n\\nComprueba el horario permitido en la configuración."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + 1000 + appName.hashCode(), notification)
    }
    
    private fun showCriticalBlockNotification(appName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🛡️ Acceso bloqueado")
            .setContentText("$appName no está permitido")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("El navegador externo $appName está bloqueado por GuardianOS Shield.\\n\\nUsa el navegador integrado desde la app principal."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + 2000 + appName.hashCode(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Apps Bloqueadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se bloquea una app"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
