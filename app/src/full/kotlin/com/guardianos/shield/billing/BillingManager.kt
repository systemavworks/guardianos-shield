package com.guardianos.shield.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * BillingManager: gestiona compras in-app con Google Play Billing Library 6+
 *
 * - Pago único vitalicio: Product ID "premium_guardianos" (configurar en Play Console)
 * - Implementa PurchasesUpdatedListener + BillingClientStateListener directamente
 * - Restaura el estado premium tras reconexión o reinstalación via queryPurchases()
 * - Las callbacks de Billing se procesan en Main para seguridad de StateFlow
 *
 * Contacto: info@guardianos.es — https://guardianos.es/shield
 */
class BillingManager(private val context: Context) :
    PurchasesUpdatedListener,
    BillingClientStateListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /** ID del producto en Google Play Console */
    private val premiumProductId = "premium_guardianos"

    /**
     * Flag para evitar procesar onBillingSetupFinished múltiples veces
     * (el sistema puede disparar este callback más de una vez)
     */
    private var isBillingReady = false

    /** Callback guardado para ejecutar cuando la conexión esté lista */
    private var pendingOnReady: (() -> Unit)? = null

    // ─────────────────────────── CONEXIÓN ───────────────────────────

    /**
     * Inicia la conexión con Play Billing.
     * Si ya está conectado, ejecuta [onReady] y consulta compras existentes de inmediato.
     */
    fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onReady?.invoke()
            queryPurchases()
            return
        }
        pendingOnReady = onReady
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        // Ignorar calls duplicados
        if (isBillingReady) return

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isBillingReady = true
            Log.d(TAG, "BillingClient conectado correctamente")
            queryPurchases()          // Restaurar estado premium (reinstalación / nuevo dispositivo)
            pendingOnReady?.invoke()
            pendingOnReady = null
        } else {
            Log.e(TAG, "Error al conectar BillingClient: ${billingResult.debugMessage} (código ${billingResult.responseCode})")
        }
    }

    override fun onBillingServiceDisconnected() {
        isBillingReady = false
        Log.w(TAG, "BillingClient desconectado — se reconectará en el próximo startConnection()")
        // No reconectar automáticamente aquí; MainActivity llama startConnection() en onResume si fuera necesario
    }

    /**
     * Libera recursos del BillingClient. Llamar desde MainActivity.onDestroy().
     */
    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        isBillingReady = false
        Log.d(TAG, "BillingClient desconectado por endConnection()")
    }

    // ─────────────────────────── COMPRA ───────────────────────────

    /**
     * Lanza el flujo de compra de Google Play para el producto premium.
     * [onError] se invoca si el producto no está disponible en Play Console
     * o si hay un error de conectividad.
     */
    fun launchPurchaseFlow(activity: Activity, onError: (() -> Unit)? = null) {
        if (!billingClient.isReady) {
            Log.w(TAG, "launchPurchaseFlow: BillingClient no está listo")
            onError?.invoke()
            return
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(premiumProductId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && productDetailsList.isNotEmpty()
            ) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetailsList[0])
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                Log.e(TAG, "Producto '$premiumProductId' no encontrado en Play Console: ${billingResult.debugMessage}")
                CoroutineScope(Dispatchers.Main).launch { onError?.invoke() }
            }
        }
    }

    // ─────────────────────────── CONSULTAR COMPRAS ───────────────────────────

    /**
     * Consulta compras existentes para restaurar el estado premium.
     * Se ejecuta automáticamente tras cada conexión exitosa.
     * Esencial para reinstalaciones y cambios de dispositivo.
     */
    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchasesList ->
            purchasesList.forEach { processPurchase(it) }
            // Si no hay ninguna compra premium válida, asegurarse de que isPremium = false
            if (purchasesList.none { it.products.contains(premiumProductId) }) {
                // No sobreescribir si ya hay un valor true desde DataStore (modo offline)
                Log.d(TAG, "queryPurchases: no se encontró compra premium activa")
            }
        }
    }

    /**
     * Fuerza una consulta de compras existentes a Google Play.
     * Usar desde el botón "Restaurar compras" en Ajustes.
     */
    fun restorePurchases() {
        if (!billingClient.isReady) {
            Log.w(TAG, "restorePurchases: BillingClient no está listo — reconectando...")
            startConnection { queryPurchases() }
            return
        }
        Log.i(TAG, "Iniciando restauración de compras...")
        queryPurchases()
    }

    // ─────────────────────────── PROCESAR COMPRA ───────────────────────────

    /**
     * Procesa una compra individual: verifica que sea del producto premium,
     * la acknowledges si no lo está (obligatorio en 3 días para evitar reembolso automático)
     * y actualiza el StateFlow.
     */
    private fun processPurchase(purchase: Purchase) {
        if (!purchase.products.contains(premiumProductId)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(ackParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Compra premium reconocida correctamente")
                    CoroutineScope(Dispatchers.Main).launch { _isPremium.value = true }
                } else {
                    Log.e(TAG, "Error al reconocer compra: ${result.debugMessage}")
                }
            }
        } else {
            // Ya reconocida → activar premium directamente en Main
            CoroutineScope(Dispatchers.Main).launch { _isPremium.value = true }
            Log.d(TAG, "Compra ya reconocida — premium activado")
        }
    }

    // ─────────────────────────── CALLBACK DE COMPRA ───────────────────────────

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { processPurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // El usuario canceló voluntariamente — no mostrar error
                Log.d(TAG, "Compra cancelada por el usuario")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Ya tiene el producto — restaurar estado
                Log.i(TAG, "Producto ya comprado — restaurando estado premium")
                queryPurchases()
            }
            else -> {
                Log.e(TAG, "Error en onPurchasesUpdated: ${billingResult.debugMessage} (código ${billingResult.responseCode})")
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
