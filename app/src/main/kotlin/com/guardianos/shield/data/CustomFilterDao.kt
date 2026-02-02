// app/src/main/java/com/guardianos/shield/data/CustomFilterDao.kt
package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFilterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: CustomFilterEntity)
    
    @Query("DELETE FROM custom_filters WHERE LOWER(domain) = LOWER(:domain)")
    suspend fun deleteByDomain(domain: String)
    
    // Blacklist = filtros activos (isActive = true)
    @Query("SELECT * FROM custom_filters WHERE isActive = 1")
    fun getBlacklist(): Flow<List<CustomFilterEntity>>
    
    // Whitelist = filtros habilitados (isEnabled = true)
    @Query("SELECT * FROM custom_filters WHERE isEnabled = 1")
    fun getWhitelist(): Flow<List<CustomFilterEntity>>
    
    // ✅ NUEVO: Obtener todos los filtros (usado en SafeBrowserActivity)
    @Query("SELECT * FROM custom_filters")
    suspend fun getAllFilters(): List<CustomFilterEntity>
    
    @Query("SELECT EXISTS(SELECT 1 FROM custom_filters WHERE LOWER(domain) = LOWER(:domain) AND isEnabled = 1)")
    suspend fun isInWhitelist(domain: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM custom_filters WHERE LOWER(domain) = LOWER(:domain) AND isActive = 1)")
    suspend fun isInBlacklist(domain: String): Boolean
    
    @Query("DELETE FROM custom_filters")
    suspend fun deleteAll()
}
