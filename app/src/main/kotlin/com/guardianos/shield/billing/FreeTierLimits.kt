package com.guardianos.shield.billing

/**
 * FreeTierLimits — Límites y restricciones del plan gratuito de GuardianOS Shield.
 *
 * Reglas FREE exactas:
 *   • HISTORIAL: limitado a últimas 48 horas en todos los módulos
 *   • DNS: filtrado activo (sin límite de tiempo — VPN no se toca)
 *   • MODO PARENTAL: NO disponible → PremiumGateDialog
 *   • HORARIOS: NO disponibles → PremiumGateDialog
 *   • FILTROS / BLOQUEO DE DOMINIOS: NO disponibles → PremiumGateDialog
 *   • MONITOREO DE APPS y AMENAZAS: datos limitados a 48h
 *   • NAVEGADOR SEGURO: historial limitado a 48h / 10 URLs
 *   • PROTECCIÓN (malware + adulto + redes sociales): datos mostrados 48h
 *
 * Precio: 14,99 € — pago único vitalicio, sin suscripciones.
 * Contacto: info@guardianos.es — https://guardianos.es/shield
 */
object FreeTierLimits {

    // ────────────────────── HISTORIAL (48h — todos los módulos) ─────

    /**
     * Horas máximas de historial visible en FREE para TODOS los módulos:
     * estadísticas, monitoreo, navegador seguro y protección.
     */
    const val MAX_HISTORY_HOURS = 48

    // ─────────────────────────── ESTADÍSTICAS ──────────────────────────────

    /** Días de estadísticas visibles en FREE (2 días = 48 horas) */
    const val MAX_STATS_DAYS_FREE = 2

    /** Días de estadísticas visibles en PREMIUM */
    const val MAX_STATS_DAYS_PREMIUM = 30

    // ─────────────────────────── NAVEGADOR ──────────────────────────────────

    /** Máximo de entradas en el historial del Navegador Seguro en FREE */
    const val MAX_BROWSER_HISTORY_FREE = 10

    /** Máximo de entradas en el historial del Navegador Seguro en PREMIUM */
    const val MAX_BROWSER_HISTORY_PREMIUM = 200

    // ── FEATURES BLOQUEADAS TOTALMENTE EN FREE ─────────────────────────────

    /**
     * Funcionalidades NO disponibles en FREE — muestran PremiumGateDialog.
     */
    fun canUseParentalMode(isPremium: Boolean) = isPremium
    fun canUseSchedules(isPremium: Boolean) = isPremium
    fun canUseCustomFilters(isPremium: Boolean) = isPremium
    fun canExportHistory(isPremium: Boolean) = isPremium
    fun canUseMultipleProfiles(isPremium: Boolean) = isPremium
    fun canUsePushAlerts(isPremium: Boolean) = isPremium

    // ── Features nuevas — accesibles durante trial y en Premium ───────────────

    /** Pacto Digital Familiar: peticiones hijo→padre dentro del dispositivo */
    fun canUsePactoDigital(isPremium: Boolean) = isPremium

    /** Bloqueo real de apps instaladas vía AccessibilityService */
    fun canUseAppBlocker(isPremium: Boolean) = isPremium

    /** Anti-tampering: Device Admin para proteger contra desinstalación */
    fun canUseAntiTampering(isPremium: Boolean) = isPremium

    /** TrustFlow Engine: niveles CAUTION y TRUSTED son funcionalidades premium.
     *  En FREE (sin trial), el servicio de accesibilidad siempre usa modo LOCKED. */
    fun canUseTrustFlow(isPremium: Boolean, isFreeTrialActive: Boolean) =
        isPremium || isFreeTrialActive

    /**
     * Devuelve true si el usuario puede acceder a una feature premium
     * teniendo en cuenta el trial activo.
     * Usar este helper en las UIs nuevas en lugar de solo isPremium.
     */
    fun canAccessPremiumFeature(isPremium: Boolean, isFreeTrialActive: Boolean) =
        isPremium || isFreeTrialActive

    // ─────────────────────────── HELPERS ────────────────────────────────────

    /** Timestamp de corte (48h atrás) para filtrar datos mostrados en FREE */
    fun historyStartMs(): Long =
        System.currentTimeMillis() - MAX_HISTORY_HOURS * 60 * 60 * 1_000L

    /**
     * Devuelve el número de días de estadísticas disponibles según el plan.
     */
    fun maxStatsDays(isPremium: Boolean): Int =
        if (isPremium) MAX_STATS_DAYS_PREMIUM else MAX_STATS_DAYS_FREE

    /**
     * Devuelve el máximo de entradas en el historial del navegador.
     */
    fun maxBrowserHistory(isPremium: Boolean): Int =
        if (isPremium) MAX_BROWSER_HISTORY_PREMIUM else MAX_BROWSER_HISTORY_FREE
}
