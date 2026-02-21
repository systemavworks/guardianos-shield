package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// TrustFlow Engine — Nivel de confianza dinámico basado en la racha diaria
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Nivel de confianza que determina el comportamiento del AppBlocker:
 *   LOCKED   (0–6 días)   → Bloqueo duro. Solo se puede pedir permiso.
 *   CAUTION  (7–29 días)  → Friction Mode: cuenta atrás de 15 s. con advertencia.
 *   TRUSTED  (30+ días)   → Zona de confianza: acceso libre (60 min/día), uso registrado.
 *
 * Los campos [nombreMostrar], [etiqueta] y [emoji] se usan directamente en la UI
 * sin necesidad de when/mapping en los composables.
 */
enum class TrustLevel(
    val nombreMostrar: String, // Texto largo para tarjetas e informes
    val etiqueta: String,      // Nombre de gamificación visble al menor
    val emoji: String
) {
    LOCKED ("Bloqueo Total",        "Cadete",      "🔴"),
    CAUTION("Modo Precaución",      "Explorador",  "🟡"),
    TRUSTED("Zona de Confianza",    "Guardián",    "🟢")
}

/** Calcula el TrustLevel según la racha actual del perfil. */
fun getTrustLevel(streak: Int): TrustLevel = when {
    streak >= 30 -> TrustLevel.TRUSTED
    streak >= 7  -> TrustLevel.CAUTION
    else         -> TrustLevel.LOCKED
}

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

    // ⚠️ DEPRECADO: PIN ahora se almacena en EncryptedSharedPreferences por seguridad
    // Usar SecurityHelper.savePin() / verifyPin() para gestionar PINs
    // Este campo se mantiene solo para migración de datos legacy
    @Deprecated("Usar SecurityHelper en su lugar")
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
    // 900 = 15:00 — a partir de cuando el menor llega del colegio
    val startTimeMinutes: Int = 900,   // por defecto 15:00

    // Hora de fin permitida en minutos desde medianoche (1439 = 23:59)
    val endTimeMinutes: Int = 1260,    // por defecto 21:00

    // ──────────────────────────────────────────────
    //    HORARIO FIN DE SEMANA (sábado y domingo)
    // ──────────────────────────────────────────────

    // Si false, los fines de semana se aplica el horario de semana normal
    val weekendScheduleEnabled: Boolean = false,

    // Hora de inicio permitida el fin de semana en minutos desde medianoche
    // 600 = 10:00 — pueden levantarse más tarde y tener más tiempo libre
    val weekendStartTimeMinutes: Int = 600,  // por defecto 10:00

    // Hora de fin permitida el fin de semana en minutos desde medianoche
    // 1320 = 22:00 — se pueden acostar una hora más tarde que entre semana
    val weekendEndTimeMinutes: Int = 1320,   // por defecto 22:00

    // ──────────────────────────────────────────────
    //    HORARIO DE COLEGIO (bloqueo L-V durante horas lectivas)
    // ──────────────────────────────────────────────

    // Si true, bloquea el móvil de lunes a viernes durante las horas del colegio,
    // independientemente del horario libre configurado.
    val schoolScheduleEnabled: Boolean = false,

    // Hora de entrada al colegio en minutos desde medianoche
    // 540 = 09:00 (hora habitual de entrada en colegios españoles)
    val schoolStartTimeMinutes: Int = 540,  // 09:00

    // Hora de salida del colegio en minutos desde medianoche
    // 840 = 14:00 (hora habitual de salida en colegios españoles)
    val schoolEndTimeMinutes: Int = 840,    // 14:00

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
    val createdAt: Long = System.currentTimeMillis(),

    // ──────────────────────────────────────────────
    //          GAMIFICACIÓN — RACHA DIARIA
    // ──────────────────────────────────────────────

    // Días consecutivos sin intentar acceder a contenido bloqueado
    val rachaActual: Int = 0,

    // Racha más larga conseguida (récord personal)
    val rachaMaxima: Int = 0,

    // Fecha del último día "limpio" (YYYY-MM-DD). Vacío si nunca se ha registrado.
    val ultimoDiaLimpio: String = "",

    // ──────────────────────────────────────────────
    //     TRUSTFLOW ENGINE — MINUTOS DE AUTONOMÍA
    // ──────────────────────────────────────────────

    // Minutos disponibles hoy para el modo TRUSTED (se resetea cada día a 60).
    // Cuando llega a 0, el modo TRUSTED se comporta como LOCKED hasta mañana.
    val minutosAutonomiaDiarios: Int = 60,

    // ──────────────────────────────────────────────
    //   RECOMPENSA DEL PADRE — BONUS GAMING
    // ──────────────────────────────────────────────

    // Minutos de gaming extra concedidos manualmente por el padre.
    // Funciona en CUALQUIER nivel de confianza (incluido LOCKED).
    // Solo se aplica a apps con categoría GAMING. Se consume minuto a minuto.
    // El padre puede otorgar hasta 120 min/día. Se resetea a 0 cada noche.
    val minutosGamingExtra: Int = 0
) {
    /**
     * Propiedad calculada (no persistida por Room) — nivel de confianza actual.
     * Usar en UI y en AppBlockerAccessibilityService para decidir el modo de bloqueo.
     */
    @get:Ignore
    val trustLevel: TrustLevel get() = getTrustLevel(rachaActual)

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
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) // 1=Dom, 7=Sáb
        val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY ||
                        dayOfWeek == java.util.Calendar.SUNDAY

        // ── Capa 1: horario de colegio (L-V, tiene prioridad) ───────────────
        if (!isWeekend && schoolScheduleEnabled) {
            val inSchool = currentMinutes in schoolStartTimeMinutes..schoolEndTimeMinutes
            if (inSchool) {
                val h = currentMinutes / 60; val m = currentMinutes % 60
                android.util.Log.d("UserProfile",
                    "🏫 BLOQUEADO horario cole: %02d:%02d (cole %02d:%02d–%02d:%02d)".format(
                        h, m,
                        schoolStartTimeMinutes/60, schoolStartTimeMinutes%60,
                        schoolEndTimeMinutes/60,   schoolEndTimeMinutes%60))
                return false
            }
        }

        // ── Capa 2: ventana de tiempo libre (semana o fin de semana) ────────
        val startMin = if (isWeekend && weekendScheduleEnabled) weekendStartTimeMinutes else startTimeMinutes
        val endMin   = if (isWeekend && weekendScheduleEnabled) weekendEndTimeMinutes   else endTimeMinutes
        val tipoHorario = if (isWeekend && weekendScheduleEnabled) "fin de semana" else "semana"

        val currentTime = String.format("%02d:%02d",
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE))
        val startTime = String.format("%02d:%02d", startMin / 60, startMin % 60)
        val endTime   = String.format("%02d:%02d", endMin   / 60, endMin   % 60)

        val result = if (startMin <= endMin) {
            // Horario normal (ej: 8:00 a 21:00)
            currentMinutes in startMin..endMin
        } else {
            // Horario cruzado medianoche (ej: 22:00 a 08:00)
            currentMinutes >= startMin || currentMinutes <= endMin
        }

        android.util.Log.d("UserProfile", "⏰ Verificando horario [$tipoHorario]: actual=$currentTime, rango=$startTime-$endTime, dentro=$result")
        return result
    }

    /**
     * Método auxiliar para saber si un perfil es "infantil" según ageGroup
     */
    fun isChildProfile(): Boolean {
        return ageGroup in listOf("0-7", "8-12", "13-15")
    }
}
