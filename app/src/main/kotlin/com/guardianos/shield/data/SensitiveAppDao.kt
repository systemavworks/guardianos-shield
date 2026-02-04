package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SensitiveAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SensitiveAppEntity)

    @Query("DELETE FROM sensitive_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT * FROM sensitive_apps")
    fun getAll(): Flow<List<SensitiveAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM sensitive_apps WHERE packageName = :packageName)")
    suspend fun isSensitive(packageName: String): Boolean

    @Query("DELETE FROM sensitive_apps")
    suspend fun deleteAll()
}
