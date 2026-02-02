// app/src/main/java/com/guardianos/shield/data/StatisticDao.kt
package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StatisticEntity)
    
    @Update
    suspend fun update(entity: StatisticEntity)
    
    @Query("SELECT * FROM statistics WHERE dateKey = :dateKey")
    suspend fun getStatisticByDate(dateKey: String): StatisticEntity?
    
    @Query("SELECT * FROM statistics ORDER BY dateKey DESC LIMIT 30")
    fun getLast30Days(): Flow<List<StatisticEntity>>
    
    @Query("SELECT SUM(totalBlocked) FROM statistics")
    suspend fun getTotalBlockedCount(): Int?
    
    @Query("DELETE FROM statistics WHERE dateKey < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: String)
    
    @Query("DELETE FROM statistics")
    suspend fun deleteAll()
}
