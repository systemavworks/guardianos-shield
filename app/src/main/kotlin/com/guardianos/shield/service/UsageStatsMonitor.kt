package com.guardianos.shield.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
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
            }
        }
    }

    private fun handleBrowserAttempt(packageName: String) {
        if (lastBlockedApp == packageName && System.currentTimeMillis() - lastBlockedTime < 30000) return
        
        lastBlockedApp = packageName
        lastBlockedTime = System.currentTimeMillis()

        repository?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    // Guardar log del intento bloqueado
                    // Si tu repository tiene este método, descoméntalo:
                    // it.logBlockedAppAccess(packageName, getAppLabel(packageName), System.currentTimeMillis())
                } catch (e: Exception) {
                    Log.e("UsageStatsMonitor", "Error logging blocked app", e)
                }
            }
        }

        if (packageName != "com.guardianos.shield") {
            val intent = Intent(context, SafeBrowserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("redirected_from", packageName)
            }
            context.startActivity(intent)
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
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }
}
