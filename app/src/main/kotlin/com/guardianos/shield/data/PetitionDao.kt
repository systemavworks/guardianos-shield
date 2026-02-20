package com.guardianos.shield.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetitionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(petition: PetitionEntity): Long

    /** Todas las peticiones ordenadas de más reciente a más antigua */
    @Query("SELECT * FROM petitions ORDER BY creadoEn DESC")
    fun getAll(): Flow<List<PetitionEntity>>

    /** Solo las pendientes — usada para el badge del padre */
    @Query("SELECT * FROM petitions WHERE estado = 'PENDING' ORDER BY creadoEn DESC")
    fun getPendientes(): Flow<List<PetitionEntity>>

    /** Contador de pendientes para badge en el dashboard */
    @Query("SELECT COUNT(*) FROM petitions WHERE estado = 'PENDING'")
    fun contarPendientes(): Flow<Int>

    /** Responder a una petición (padre aprueba o rechaza) */
    @Query("""
        UPDATE petitions
        SET estado = :estado,
            notaPadre = :nota,
            respondidoEn = :ahora,
            minutosAprobados = :minutos
        WHERE id = :id
    """)
    suspend fun responder(id: Int, estado: String, nota: String, ahora: Long, minutos: Int = 0)

    /** Últimas N peticiones para el historial del menor */
    @Query("SELECT * FROM petitions ORDER BY creadoEn DESC LIMIT :limite")
    suspend fun getRecientes(limite: Int): List<PetitionEntity>

    @Query("DELETE FROM petitions")
    suspend fun borrarTodo()
}
