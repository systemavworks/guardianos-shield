package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Petición del menor al padre/madre — núcleo del "Pacto Digital Familiar".
 * Toda la comunicación permanece en el dispositivo, sin servidores externos.
 *
 * Tipos:
 * - TIME_EXTENSION: el menor pide más tiempo de pantalla
 * - APP_UNLOCK:     el menor pide desbloquear una app temporalmente
 * - SITE_UNLOCK:    el menor pide desbloquear un sitio web específico
 *
 * Estados:
 * - PENDING:   nueva petición, el padre aún no la ha visto
 * - APPROVED:  padre aprobó (+ nota opcional)
 * - REJECTED:  padre rechazó (+ motivo obligatorio)
 */
@Entity(tableName = "petitions")
data class PetitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Perfil del menor que hace la petición
    val profileId: Int = 1,

    // Tipo de petición: TIME_EXTENSION | APP_UNLOCK | SITE_UNLOCK
    val tipo: String,

    // Valor concreto: nombre del paquete, dominio, o minutos extra
    val valorSolicitado: String,

    // Razón del menor (texto libre, max ~200 chars)
    val razonHijo: String = "",

    // Estado actual de la petición
    val estado: String = "PENDING",

    // Nota del padre al responder (motivo si rechaza, mensaje si aprueba)
    val notaPadre: String = "",

    // Timestamp de creación
    val creadoEn: Long = System.currentTimeMillis(),

    // Timestamp de respuesta del padre (null si aún pendiente)
    val respondidoEn: Long? = null,

    // Minutos adicionales concedidos (solo para TIME_EXTENSION)
    val minutosAprobados: Int = 0
)
