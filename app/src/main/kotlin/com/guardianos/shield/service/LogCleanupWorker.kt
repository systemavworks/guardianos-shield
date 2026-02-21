package com.guardianos.shield.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import java.util.Calendar
import kotlinx.coroutines.flow.first

class LogCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val db = GuardianDatabase.getDatabase(applicationContext)
            val repository = GuardianRepository(applicationContext, db)

            // 1. Eliminar logs de DNS con más de 30 días
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            db.dnsLogDao().deleteOldLogs(cutoff)

            // 2. Actualizar racha diaria si el día fue "limpio" (sin bloqueos activos hoy)
            val inicioDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val bloqueosHoy = db.blockedSiteDao().getTodayBlockedCount(inicioDia).first()
            if (bloqueosHoy == 0) {
                repository.registrarDiaLimpio()
            }

            // 3. Resetear presupuesto de autonomía para el nuevo día (60 min)
            repository.resetearMinutosAutonomia()

            // 4. Resetear bono de gaming concedido por el padre (los bonos son diarios)
            repository.resetearGamingExtra()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
