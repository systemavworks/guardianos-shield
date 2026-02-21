package com.guardianos.shield.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import java.util.concurrent.TimeUnit

/**
 * Worker que se ejecuta una vez a la semana (domingos ~20:00) y muestra al padre
 * un resumen de los bloqueos realizados durante los últimos 7 días.
 *
 * Se programa con WorkManager desde MainActivity usando enqueueUniquePeriodicWork,
 * lo que garantiza que solo hay una instancia activa aunque la app se reinicie.
 */
class WeeklySummaryWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "GuardianOS_WeeklySummary"
        private const val CHANNEL_ID = "GuardianShield_WeeklySummary"
        private const val NOTIFICATION_ID = 8888
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i("WeeklySummary", "📊 Iniciando resumen semanal...")

            val database = GuardianDatabase.getDatabase(appContext)
            val repository = GuardianRepository(appContext, database)

            val now = System.currentTimeMillis()
            val weekStart = now - TimeUnit.DAYS.toMillis(7)

            val blockedSites = repository.getBlockedSitesByDateRange(weekStart, now)
            val total = blockedSites.size

            // Agrupar por categoría (excluir APP_PERMITIDA de los bloqueos reales)
            val desglose = blockedSites
                .filter { it.category != "APP_PERMITIDA" }
                .groupBy { it.category }
                .mapValues { it.value.size }
                .filter { it.value > 0 }

            val realBlocked = desglose.values.sum()

            Log.i("WeeklySummary", "📊 Total esta semana: $total registros, $realBlocked bloqueados")

            mostrarNotificacionResumen(realBlocked, desglose)
            Result.success()
        } catch (e: Exception) {
            Log.e("WeeklySummary", "Error en resumen semanal: ${e.message}", e)
            Result.retry()
        }
    }

    private fun mostrarNotificacionResumen(
        totalBloqueados: Int,
        desglose: Map<String, Int>
    ) {
        crearCanalNotificacion()

        val desgloseTexto = if (desglose.isEmpty()) {
            "Ningún contenido bloqueado esta semana. ¡Todo tranquilo!"
        } else {
            desglose.entries.joinToString("\n") { (categoria, cantidad) ->
                val etiqueta = when (categoria) {
                    "ADULT"        -> "🔞 Contenido adulto"
                    "GAMBLING"     -> "🎰 Apuestas/Casino"
                    "SOCIAL_MEDIA", "APP_BLOQUEADA" -> "📱 Redes sociales"
                    "GAMING"       -> "🎮 Videojuegos"
                    "BYPASS_TOOL"  -> "🚨 Intentos de evasión VPN"
                    "APP_STORE"    -> "🛒 Tienda de apps (fuera de horario)"
                    else           -> "🛡️ Otros"
                }
                "  • $etiqueta: $cantidad"
            }
        }

        val titulo = if (totalBloqueados == 0) {
            "🛡️ Resumen semanal GuardianOS"
        } else {
            "🛡️ Esta semana: $totalBloqueados bloqueos"
        }

        val cuerpo = if (totalBloqueados == 0) {
            "Todo ha ido bien esta semana. GuardianOS no ha bloqueado nada."
        } else {
            "GuardianOS bloqueó $totalBloqueados intentos esta semana:\n$desgloseTexto"
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(titulo)
            .setContentText(
                if (totalBloqueados == 0) cuerpo
                else "GuardianOS bloqueó $totalBloqueados intentos esta semana"
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.i("WeeklySummary", "✅ Notificación resumen enviada")
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Resumen Semanal",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Informe semanal de actividad y bloqueos de GuardianOS"
            }
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
