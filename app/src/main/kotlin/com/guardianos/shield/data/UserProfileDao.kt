// app/src/main/java/com/guardianos/shield/data/UserProfileDao.kt
package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity): Long

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles")
    suspend fun getAll(): List<UserProfileEntity>

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    // ✅ NUEVO: Método getByIdSuspend (usado en GuardianRepository.setActiveProfile)
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getByIdSuspend(id: Int): UserProfileEntity?

    // ✅ NUEVO: Método deleteByIdSuspend (usado en GuardianRepository.deleteProfile)
    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteByIdSuspend(id: Int)

    @Query("UPDATE user_profiles SET isActive = 0 WHERE id != :activeId")
    suspend fun deactivateOtherProfiles(activeId: Long)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAll()

    /** Actualiza los campos de racha del perfil — usado por la gamificación */
    @Query("""
        UPDATE user_profiles
        SET rachaActual = :rachaActual,
            rachaMaxima = :rachaMaxima,
            ultimoDiaLimpio = :ultimoDiaLimpio
        WHERE id = :profileId
    """)
    suspend fun actualizarRacha(profileId: Int, rachaActual: Int, rachaMaxima: Int, ultimoDiaLimpio: String)

    /** Resetea los minutos de autonomía diaria (se llama cada noche desde LogCleanupWorker) */
    @Query("UPDATE user_profiles SET minutosAutonomiaDiarios = :minutos WHERE id = :profileId")
    suspend fun actualizarMinutosAutonomia(profileId: Int, minutos: Int)

    /** Actualiza el bono de minutos de gaming concedido por el padre */
    @Query("UPDATE user_profiles SET minutosGamingExtra = :minutos WHERE id = :profileId")
    suspend fun actualizarGamingExtra(profileId: Int, minutos: Int)

    /**
     * Adelanta la racha al umbral mínimo del siguiente nivel (7 o 30 días).
     * Solo permite subir un nivel, nunca bajar.
     */
    @Query("""
        UPDATE user_profiles
        SET rachaActual = :nuevaRacha,
            rachaMaxima = MAX(rachaMaxima, :nuevaRacha)
        WHERE id = :profileId
    """)
    suspend fun adelantarRacha(profileId: Int, nuevaRacha: Int)
}
