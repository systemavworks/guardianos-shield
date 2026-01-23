import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isActive = 1")
    fun getActiveProfile(): Flow<UserProfileEntity?>

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Insert
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles")
    suspend fun getAllProfiles(): List<UserProfileEntity>

    @Delete
    suspend fun delete(profile: UserProfileEntity)
}
