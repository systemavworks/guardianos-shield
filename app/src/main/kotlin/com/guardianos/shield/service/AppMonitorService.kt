// app/src/main/java/com/guardianos/shield/service/AppMonitorService.kt
package com.guardianos.shield.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository

class AppMonitorService : Service() {

    private lateinit var usageMonitor: UsageStatsMonitor
    private lateinit var repository: GuardianRepository

    override fun onCreate() {
        super.onCreate()
        Log.d("AppMonitorService", "🚀 onCreate() - Inicializando servicio de monitoreo")
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        usageMonitor = UsageStatsMonitor(this, repository)
        Log.d("AppMonitorService", "✅ UsageStatsMonitor creado con repository")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppMonitorService", "📡 onStartCommand() - Iniciando monitoreo foreground...")
        // ✅ Notificación persistente para evitar que OPPO/Motorola maten el servicio
        // minSdk=31 >= Q, siempre usamos la forma de 3 args con tipo specialUse (coherente con Manifest)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createForegroundNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
        }
        usageMonitor.startMonitoring()
        Log.i("AppMonitorService", "✅ Monitoreo de apps ACTIVO")
        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GuardianOS Shield - Monitoreo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el monitoreo de apps activo"
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ GuardianOS Shield")
            .setContentText("Monitoreando uso de apps")
            .setSmallIcon(R.drawable.guardianos_shield_logo) // Logo del proyecto
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        usageMonitor.stopMonitoring()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "guardianos_monitor_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }
}
