package com.guardianos.shield.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guardianos.shield.data.GuardianDatabase

class LogCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val db = GuardianDatabase.getDatabase(applicationContext)
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 días
            db.dnsLogDao().deleteOldLogs(cutoff)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
