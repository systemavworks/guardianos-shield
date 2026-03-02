package com.guardianos.shield.billing

/**
 * FreeTierLimits — Limits and restrictions for GuardianOS Shield plans.
 *
 * Plan rules:
 *   • FREE TRIAL (48h): FULL ACCESS to every feature, including parental controls,
 *     schedules, custom filters, app blocking, Pact, statistics, safe browser.
 *     One child profile allowed.
 *   • FREE EXPIRED (after 48h, no purchase): ALL premium features locked.
 *     VPN/DNS protection stays technical-active but UI is fully gated.
 *   • PREMIUM (€14.99 one-time, lifetime): ALL features unlocked forever.
 *     Up to 3 child profiles, full 30-day history.
 *
 * Price: €14.99 — single lifetime payment, no subscriptions.
 * Contact: info@guardianos.es — https://guardianos.es/shield
 */
object FreeTierLimits {

    // ─────────────────────── HISTORY (48 h — all modules) ───────────────────

    /** Maximum visible history hours in FREE for ALL modules */
    const val MAX_HISTORY_HOURS = 48

    // ────────────────────────── STATISTICS ──────────────────────────────────

    /** Days of statistics visible in FREE (2 days = 48 hours) */
    const val MAX_STATS_DAYS_FREE = 2

    /** Days of statistics visible in PREMIUM */
    const val MAX_STATS_DAYS_PREMIUM = 30

    // ───────────────────────────── BROWSER ──────────────────────────────────

    /** Max Safe Browser history entries in FREE */
    const val MAX_BROWSER_HISTORY_FREE = 10

    /** Max Safe Browser history entries in PREMIUM */
    const val MAX_BROWSER_HISTORY_PREMIUM = 200

    // ────────────────────────────── PROFILES ────────────────────────────────

    /** Max child profiles in FREE plan (trial or expired): 1 child */
    const val MAX_PROFILES_FREE = 1

    /** Max child profiles in PREMIUM: up to 3 children in the same family */
    const val MAX_PROFILES_PREMIUM = 3

    // ── ACCESS RULES ────────────────────────────────────────────────────────
    //
    // All helpers follow the same pattern:
    //   true  → feature accessible
    //   false → show PremiumGateDialog
    //
    // During the 48-hour free trial EVERYTHING is accessible (isPremium OR
    // isFreeTrialActive). After expiry, features are locked until purchase.

    fun canUseParentalMode(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUseSchedules(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUseCustomFilters(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canExportHistory(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    /** Multiple profiles: only Premium (trial stays at 1 profile) */
    fun canUseMultipleProfiles(isPremium: Boolean) = isPremium

    fun canUsePushAlerts(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUsePactoDigital(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUseAppBlocker(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUseAntiTampering(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    fun canUseTrustFlow(isPremium: Boolean, isFreeTrialActive: Boolean = false) =
        isPremium || isFreeTrialActive

    /**
     * Generic helper: returns true if the user can access a premium feature,
     * taking the active trial into account.
     * Preferred over checking isPremium alone in new UI code.
     */
    fun canAccessPremiumFeature(isPremium: Boolean, isFreeTrialActive: Boolean) =
        isPremium || isFreeTrialActive

    // ──────────────────────────── HELPERS ───────────────────────────────────

    /** Cutoff timestamp (48 h ago) for filtering data shown in FREE */
    fun historyStartMs(): Long =
        System.currentTimeMillis() - MAX_HISTORY_HOURS * 60 * 60 * 1_000L

    /** Returns the number of stats days available for the given plan. */
    fun maxStatsDays(isPremium: Boolean): Int =
        if (isPremium) MAX_STATS_DAYS_PREMIUM else MAX_STATS_DAYS_FREE

    /**
     * Devuelve el máximo de entradas en el historial del navegador.
     */
    fun maxBrowserHistory(isPremium: Boolean): Int =
        if (isPremium) MAX_BROWSER_HISTORY_PREMIUM else MAX_BROWSER_HISTORY_FREE
}
