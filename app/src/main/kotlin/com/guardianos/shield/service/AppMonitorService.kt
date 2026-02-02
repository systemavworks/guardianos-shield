// app/src/main/java/com/guardianos/shield/service/AppMonitorService.kt
package com.guardianos.shield.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R

class AppMonitorService : Service() {

    private lateinit var usageMonitor: UsageStatsMonitor

    override fun onCreate() {
        super.onCreate()
        usageMonitor = UsageStatsMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ✅ Notificación persistente para evitar que OPPO/Motorola maten el servicio
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        usageMonitor.startMonitoring()
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
            .setSmallIcon(R.drawable.guardianos_shield_logo) // Usa tu logo existente
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
