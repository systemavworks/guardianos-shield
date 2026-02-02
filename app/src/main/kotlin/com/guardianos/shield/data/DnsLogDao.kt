// app/src/main/java/com/guardianos/shield/data/DnsLogDao.kt
package com.guardianos.shield.data

import com.guardianos.shield.data.DnsLogEntity
import androidx.room.*

@Dao
interface DnsLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DnsLogEntity)
    
    @Query("DELETE FROM dns_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldLogs(cutoffTimestamp: Long)
    
    @Query("DELETE FROM dns_logs")
    suspend fun deleteAll()
}
