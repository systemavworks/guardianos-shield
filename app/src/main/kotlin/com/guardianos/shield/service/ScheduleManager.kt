// app/src/main/java/com/guardianos/shield/service/ScheduleManager.kt
package com.guardianos.shield.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Control de Horarios Realista
 * 
 * FUNCIONALIDAD:
 * - Permite internet solo en horarios específicos
 * - Fuera de horario: bloquea navegadores y apps
 * - Compatible con battery optimization
 * - Usa AlarmManager para precisión
 */
class ScheduleManager : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var repository: GuardianRepository
    private lateinit var alarmManager: AlarmManager
    
    data class TimeSlot(
        val dayOfWeek: Int, // 1=Domingo, 2=Lunes, ..., 7=Sábado (Calendar)
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    ) {
        fun isNowInSlot(): Boolean {
            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_WEEK)
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            
            if (currentDay != dayOfWeek) return false
            
            val currentMinutes = currentHour * 60 + currentMinute
            val startMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute
            
            return currentMinutes in startMinutes..endMinutes
        }
    }
    
    // Horarios permitidos por defecto
    private val defaultSchedule = listOf(
        // Lunes a Viernes: 16:00 - 20:00
        TimeSlot(Calendar.MONDAY, 16, 0, 20, 0),
        TimeSlot(Calendar.TUESDAY, 16, 0, 20, 0),
        TimeSlot(Calendar.WEDNESDAY, 16, 0, 20, 0),
        TimeSlot(Calendar.THURSDAY, 16, 0, 20, 0),
        TimeSlot(Calendar.FRIDAY, 16, 0, 20, 0),
        // Sábado y Domingo: 10:00 - 22:00
        TimeSlot(Calendar.SATURDAY, 10, 0, 22, 0),
        TimeSlot(Calendar.SUNDAY, 10, 0, 22, 0)
    )
    
    private var currentSchedule = mutableListOf<TimeSlot>()
    private var isTimeAllowed = true
    
    companion object {
        private const val TAG = "ScheduleManager"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "schedule_channel"
        private const val CHECK_INTERVAL = 60000L // 1 minuto
        
        private const val ACTION_CHECK_SCHEDULE = "com.guardianos.shield.CHECK_SCHEDULE"
        
        fun start(context: Context) {
            val intent = Intent(context, ScheduleManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, ScheduleManager::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
        
        currentSchedule.addAll(defaultSchedule)
        loadScheduleFromDatabase()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            ACTION_CHECK_SCHEDULE -> checkSchedule()
            else -> startScheduleMonitoring()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScheduleMonitoring() {
        Log.d(TAG, "⏰ Monitoreo de horarios iniciado")
        
        // Verificar inmediatamente
        checkSchedule()
        
        // Programar verificaciones periódicas con AlarmManager (más confiable que coroutines)
        scheduleNextCheck()
    }

    private fun scheduleNextCheck() {
        val intent = Intent(this, ScheduleManager::class.java).apply {
            action = ACTION_CHECK_SCHEDULE
        }
        
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val triggerTime = System.currentTimeMillis() + CHECK_INTERVAL
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun checkSchedule() {
        val wasAllowed = isTimeAllowed
        isTimeAllowed = isCurrentTimeAllowed()
        
        Log.d(TAG, "🕐 Verificando horario: ${if (isTimeAllowed) "✅ Permitido" else "⛔ Bloqueado"}")
        
        when {
            !wasAllowed && isTimeAllowed -> {
                // Transición: Bloqueado → Permitido
                onTimeAllowed()
            }
            wasAllowed && !isTimeAllowed -> {
                // Transición: Permitido → Bloqueado
                onTimeBlocked()
            }
        }
        
        // Actualizar notificación
        updateNotification()
        
        // Programar siguiente verificación
        scheduleNextCheck()
    }

    private fun isCurrentTimeAllowed(): Boolean {
        // Verificar si estamos en algún slot permitido
        return currentSchedule.any { it.isNowInSlot() }
    }

    private fun onTimeAllowed() {
        Log.d(TAG, "✅ Horario permitido - Desbloqueando acceso")
        
        // Detener bloqueador de apps si estaba activo
        RealisticAppBlocker.stop(this)
        
        // Notificar al usuario
        showTimedNotification(
            "✅ Internet Disponible",
            "Ya puedes navegar según el horario permitido"
        )
        
        // Registrar en BD
        scope.launch {
            repository.logScheduleEvent("allowed")
        }
    }

    private fun onTimeBlocked() {
        Log.d(TAG, "⛔ Horario bloqueado - Restringiendo acceso")
        
        // Iniciar bloqueador de apps
        RealisticAppBlocker.start(this)
        
        // Notificar al usuario
        showTimedNotification(
            "⏰ Horario Terminado",
            "El tiempo de uso de internet ha finalizado",
            priority = NotificationCompat.PRIORITY_HIGH
        )
        
        // Llevar a pantalla de inicio
        goToHome()
        
        // Registrar en BD
        scope.launch {
            repository.logScheduleEvent("blocked")
        }
    }

    private fun goToHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error yendo a home: ${e.message}")
        }
    }

    private fun loadScheduleFromDatabase() {
        scope.launch {
            try {
                // TODO: Cargar horarios personalizados desde BD
                // val customSchedule = repository.getSchedule()
                // if (customSchedule.isNotEmpty()) {
                //     currentSchedule.clear()
                //     currentSchedule.addAll(customSchedule)
                // }
                Log.d(TAG, "📅 Horarios cargados: ${currentSchedule.size} slots")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando horarios: ${e.message}")
            }
        }
    }

    /**
     * Obtiene el próximo slot de tiempo permitido
     */
    private fun getNextAllowedTime(): String {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        // Buscar siguiente slot hoy
        val todaySlots = currentSchedule.filter { it.dayOfWeek == currentDay }
        val nextToday = todaySlots.firstOrNull { 
            val startMinutes = it.startHour * 60 + it.startMinute
            startMinutes > currentMinutes
        }
        
        if (nextToday != null) {
            return "hoy a las ${nextToday.startHour}:${String.format("%02d", nextToday.startMinute)}"
        }
        
        // Buscar siguiente slot en días siguientes
        for (daysAhead in 1..7) {
            val targetDay = (currentDay + daysAhead - 1) % 7 + 1
            val slots = currentSchedule.filter { it.dayOfWeek == targetDay }
            if (slots.isNotEmpty()) {
                val slot = slots.first()
                val dayName = getDayName(targetDay)
                return "$dayName a las ${slot.startHour}:${String.format("%02d", slot.startMinute)}"
            }
        }
        
        return "próximamente"
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "domingo"
            Calendar.MONDAY -> "lunes"
            Calendar.TUESDAY -> "martes"
            Calendar.WEDNESDAY -> "miércoles"
            Calendar.THURSDAY -> "jueves"
            Calendar.FRIDAY -> "viernes"
            Calendar.SATURDAY -> "sábado"
            else -> "día desconocido"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de Horarios",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Gestión de horarios de uso"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("⏰ Control de Horarios")
        .setContentText(if (isTimeAllowed) "Internet disponible" else "Fuera de horario")
        .setSmallIcon(R.drawable.ic_shield)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun updateNotification() {
        val text = if (isTimeAllowed) {
            "Internet disponible ahora"
        } else {
            "Próximo acceso: ${getNextAllowedTime()}"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Control de Horarios")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showTimedNotification(
        title: String, 
        text: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * API pública para actualizar horarios
     */
    fun updateSchedule(newSchedule: List<TimeSlot>) {
        currentSchedule.clear()
        currentSchedule.addAll(newSchedule)
        
        // Guardar en BD
        scope.launch {
            // repository.saveSchedule(newSchedule)
        }
        
        // Verificar inmediatamente
        checkSchedule()
        
        Log.d(TAG, "📅 Horarios actualizados: ${newSchedule.size} slots")
    }

    override fun onDestroy() {
        scope.cancel()
        Log.d(TAG, "🛑 Servicio de horarios detenido")
        super.onDestroy()
    }
}

// Extensión para GuardianRepository
suspend fun GuardianRepository.logScheduleEvent(eventType: String) {
    // TODO: Implementar en repository
    // addEvent(ScheduleEvent(type = eventType, timestamp = System.currentTimeMillis()))
}

suspend fun GuardianRepository.logBlockedAccess(packageName: String) {
    // TODO: Implementar en repository
    // addBlockedAccess(BlockedAccessEntity(packageName, timestamp = System.currentTimeMillis()))
}

fun GuardianRepository.getActiveProfileSync(): com.guardianos.shield.data.UserProfileEntity? {
    // Ya existe en tu repository
    return kotlinx.coroutines.runBlocking {
        // Implementación existente
        null // Placeholder
    }
}
