// app/src/main/java/com/guardianos/shield/data/BlockedSiteDao.kt
package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedSiteEntity)
    
    @Query("SELECT * FROM blocked_sites ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBlocked(limit: Int): Flow<List<BlockedSiteEntity>>
    
    // ✅ NUEVO: Versión suspend que devuelve List directamente (usado en MainActivity)
    @Query("SELECT * FROM blocked_sites ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBlockedList(limit: Int): List<BlockedSiteEntity>
    
    @Query("SELECT COUNT(*) FROM blocked_sites WHERE timestamp >= :startTime")
    fun getTodayBlockedCount(startTime: Long): Flow<Int>
    
    @Query("SELECT * FROM blocked_sites WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getBlockedSitesByDateRange(start: Long, end: Long): List<BlockedSiteEntity>
    
    @Query("SELECT * FROM blocked_sites WHERE timestamp BETWEEN :start AND :end")
    suspend fun getBlockedForDate(start: Long, end: Long): List<BlockedSiteEntity>
    
    @Query("DELETE FROM blocked_sites WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM blocked_sites")
    suspend fun deleteAll()
}
