// app/src/main/java/com/guardianos/shield/service/LightweightMonitorService.kt
package com.guardianos.shield.service

import android.app.*
import android.content.pm.ServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.guardianos.shield.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LightweightMonitorService : LifecycleService() {

    private val TAG = "LightweightMonitor"
    private var isMonitoring = false
    private var packageReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isMonitoring) return START_STICKY

        // startForeground puede fallar en Android 34 si el manifiesto declara un tipo FGS restringido
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No se pudo iniciar startForeground en LightweightMonitorService (permiso faltante)", e)
            // Si no podemos poner el servicio en foreground, no tiene sentido seguir
            stopSelf()
            return START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando foreground en LightweightMonitorService", e)
            stopSelf()
            return START_NOT_STICKY
        }

        isMonitoring = true

        // Monitoreo ligero: solo detectar apps nuevas (sin verificar DNS)
        lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
            }
        }

        // Detectar instalación de apps nuevas
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    val packageName = intent.data?.schemeSpecificPart ?: return
                    lifecycleScope.launch {
                        checkIfRiskApp(packageName)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(packageReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(packageReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar packageReceiver", e)
        }

        return START_STICKY
    }

    // ✅ Corregido: sin getBlockCategory (usar lógica simple de keywords)
    private suspend fun checkIfRiskApp(packageName: String) {
        // Inferir categoría desde keywords del package name
        val category = when {
            packageName.contains("porn", ignoreCase = true) || 
            packageName.contains("sex", ignoreCase = true) || 
            packageName.contains("xxx", ignoreCase = true) -> "ADULT"
            packageName.contains("casino", ignoreCase = true) || 
            packageName.contains("gambling", ignoreCase = true) || 
            packageName.contains("bet", ignoreCase = true) -> "GAMBLING"
            else -> null
        }
        
        if (category != null) {
            Log.i(TAG, "App de riesgo instalada: $packageName [$category]")
            showRiskAppNotification(packageName, category)
        }
    }

    private fun showRiskAppNotification(appName: String, category: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ App de riesgo detectada")
            .setContentText("Se ha instalado $appName (categoría: $category)")
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(appName.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo Ligero",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianOS Shield - Monitoreo Activo")
            .setContentText("Detectando apps de riesgo")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isMonitoring = false
        try {
            packageReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistrando receiver en onDestroy", e)
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "GuardianShield_Monitor"
        const val NOTIFICATION_ID = 2
    }
}
