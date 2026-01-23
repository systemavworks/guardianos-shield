import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticDao {
    @Query("SELECT * FROM statistics ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): Flow<List<StatisticEntity>>

    @Query("SELECT * FROM statistics WHERE date = :date")
    suspend fun getStatisticByDate(date: String): StatisticEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StatisticEntity)

    @Update
    suspend fun update(entity: StatisticEntity)

    @Query("DELETE FROM statistics")
    suspend fun deleteAll()

    @Query("DELETE FROM statistics WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("SELECT SUM(totalBlocked) FROM statistics")
    suspend fun getTotalBlockedCount(): Int
}
