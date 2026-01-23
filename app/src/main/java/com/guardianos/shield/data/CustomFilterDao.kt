import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFilterDao {
    @Query("SELECT * FROM custom_filters WHERE type = 'blacklist'")
    fun getBlacklist(): Flow<List<CustomFilterEntity>>

    @Query("SELECT * FROM custom_filters WHERE type = 'whitelist'")
    fun getWhitelist(): Flow<List<CustomFilterEntity>>

    @Insert
    suspend fun insert(filter: CustomFilterEntity)

    @Query("DELETE FROM custom_filters WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("SELECT EXISTS(SELECT 1 FROM custom_filters WHERE type = 'whitelist' AND domain = :domain)")
    suspend fun isInWhitelist(domain: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM custom_filters WHERE type = 'blacklist' AND domain = :domain)")
    suspend fun isInBlacklist(domain: String): Boolean
}
