package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un perfil de usuario (normalmente un menor)
 * Utilizado para aplicar reglas de filtrado, horarios y niveles de restricción
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre visible del perfil (ej: "Juan", "Hijo mayor")
    val name: String = "Perfil sin nombre",

    // Edad exacta del menor (opcional)
    val age: Int? = null,

    // Grupo de edad para reglas predefinidas y UI (ej: "8-12", "13-17")
    // Este campo se muestra en el dashboard
    val ageGroup: String = "8-12",

    // PIN numérico o alfanumérico que debe introducir el padre para modificar el perfil
    val parentalPin: String? = null,

    // Nivel de restricción global del perfil
    // Valores típicos: "LOW", "MEDIUM", "HIGH", "STRICT"
    val restrictionLevel: String = "MEDIUM",

    // ──────────────────────────────────────────────
    //          CONTROL DE HORARIOS
    // ──────────────────────────────────────────────

    // Indica si se aplican restricciones de horario
    val scheduleEnabled: Boolean = false,

    // Hora de inicio permitida en minutos desde medianoche (0 = 00:00)
    // Ejemplos: 480 = 08:00, 540 = 09:00
    val startTimeMinutes: Int = 480,   // por defecto 8:00

    // Hora de fin permitida en minutos desde medianoche (1439 = 23:59)
    val endTimeMinutes: Int = 1260,    // por defecto 21:00

    // ──────────────────────────────────────────────
    //          CATEGORÍAS DE BLOQUEO ACTIVAS
    // ──────────────────────────────────────────────

    // Bloquear contenido adulto / +18
    val blockAdultContent: Boolean = true,

    // Bloquear gambling, apuestas, casinos
    val blockGambling: Boolean = true,

    // Bloquear redes sociales (Facebook, Instagram, TikTok, etc.)
    val blockSocialMedia: Boolean = false,

    // Bloquear videojuegos / gaming (puedes extender esta categoría)
    val blockGaming: Boolean = false,

    // Bloquear plataformas de streaming no educativo (opcional)
    val blockStreaming: Boolean = false,

    // ──────────────────────────────────────────────
    //          ESTADO Y METADATOS
    // ──────────────────────────────────────────────

    // Indica si este es el perfil actualmente activo/aplicado
    val isActive: Boolean = true,

    // Timestamp de creación (para ordenación y auditoría)
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Método de conveniencia: devuelve si el horario actual está dentro del permitido
     * @return true si scheduleEnabled=false o si la hora actual está en el rango
     */
    fun isWithinAllowedTime(): Boolean {
        if (!scheduleEnabled) {
            android.util.Log.d("UserProfile", "⏰ Horario DESACTIVADO (scheduleEnabled=false) - Permitiendo acceso")
            return true
        }

        val now = java.util.Calendar.getInstance()
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                            now.get(java.util.Calendar.MINUTE)
        
        val currentTime = String.format("%02d:%02d", 
            now.get(java.util.Calendar.HOUR_OF_DAY), 
            now.get(java.util.Calendar.MINUTE))
        val startTime = String.format("%02d:%02d", startTimeMinutes / 60, startTimeMinutes % 60)
        val endTime = String.format("%02d:%02d", endTimeMinutes / 60, endTimeMinutes % 60)

        val result = if (startTimeMinutes <= endTimeMinutes) {
            // Horario normal (ej: 8:00 a 21:00)
            currentMinutes in startTimeMinutes..endTimeMinutes
        } else {
            // Horario cruzado medianoche (ej: 22:00 a 08:00)
            currentMinutes >= startTimeMinutes || currentMinutes <= endTimeMinutes
        }
        
        android.util.Log.d("UserProfile", "⏰ Verificando horario: actual=$currentTime, rango=$startTime-$endTime, dentro=$result")
        return result
    }

    /**
     * Método auxiliar para saber si un perfil es "infantil" según ageGroup
     */
    fun isChildProfile(): Boolean {
        return ageGroup in listOf("0-7", "8-12", "13-15")
    }
}
