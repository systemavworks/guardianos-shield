import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSiteDao {
    @Query("SELECT * FROM blocked_sites ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBlocked(limit: Int): Flow<List<BlockedSiteEntity>>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE timestamp BETWEEN :start AND :end")
    suspend fun getBlockedCountForDate(start: Long, end: Long): Int

    @Query("SELECT * FROM blocked_sites WHERE timestamp BETWEEN :start AND :end")
    suspend fun getBlockedSitesByDateRange(start: Long, end: Long): List<BlockedSiteEntity>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE timestamp >= :todayStart")
    fun getTodayBlockedCount(todayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedSiteEntity)

    @Query("DELETE FROM blocked_sites")
    suspend fun deleteAll()

    @Query("DELETE FROM blocked_sites WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
