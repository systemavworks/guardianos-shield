package com.guardianos.shield.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BillingManager — Implementación STUB para el flavor F-Droid.
 *
 * F-Droid exige que la app no contenga ninguna dependencia propietaria
 * (Google Play Billing Library viola esta regla). Esta implementación
 * reemplaza completamente la versión de Play Store con una clase que:
 *
 *  • Tiene la misma interfaz pública que el BillingManager real.
 *  • Siempre devuelve isPremium = true → todas las funciones desbloqueadas.
 *  • No importa ninguna clase de com.android.billingclient.
 *  • No realiza ninguna llamada de red ni a servicios de Google.
 *
 * Los usuarios de F-Droid obtienen la app completa sin restricciones freemium,
 * de acuerdo con la filosofía del software libre.
 */
class BillingManager(context: Context) {

    private val _isPremium = MutableStateFlow(true) // Siempre true en F-Droid
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /**
     * No-op: no hay servidor de billing al que conectarse.
     * Invoca [onReady] inmediatamente si se proporciona.
     */
    fun startConnection(onReady: (() -> Unit)? = null) {
        onReady?.invoke()
    }

    /**
     * No-op: no hay flujo de compra en F-Droid.
     */
    fun launchPurchaseFlow(activity: Activity, onError: (() -> Unit)? = null) {
        // Sin acción — la versión F-Droid no tiene compras
    }

    /**
     * No-op: no hay compras que restaurar.
     */
    fun restorePurchases() {
        // Sin acción — la versión F-Droid no tiene compras
    }

    /**
     * No-op: no hay conexión que cerrar.
     */
    fun endConnection() {
        // Sin acción
    }

    companion object {
        private const val TAG = "BillingManager[fdroid]"
    }
}
