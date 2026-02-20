package com.guardianos.shield.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.guardianos.shield.billing.FreeTierLimits
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.data.SettingsDataStore
import com.guardianos.shield.ui.AppBlockedActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * AppBlockerAccessibilityService
 *
 * Detecta en tiempo real qué app está en primer plano usando Accessibility Events.
 * Cuando detecta una app bloqueada (en SensitiveAppEntity) fuera del horario permitido,
 * lanza AppBlockedActivity encima — sin root, sin tocar la VPN.
 *
 * Equivalente a lo que hace Qustodio internamente, pero 100% local.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: GuardianRepository
    private lateinit var settingsDataStore: SettingsDataStore

    // Evitar disparos múltiples para la misma app en poco tiempo
    private var ultimaAppBloqueada: String? = null
    private var tiempoUltimoBloqueo: Long = 0
    private val INTERVALO_MIN_BLOQUEO = 3000L // 3 segundos

    companion object {
        private const val TAG = "AppBlockerA11y"

        /** Devuelve true si el servicio tiene el permiso de accesibilidad activado */
        fun estaActivado(context: android.content.Context): Boolean {
            val gestor = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                    as android.view.accessibility.AccessibilityManager
            val servicios = gestor.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC
            )
            val nombrePropio = "${context.packageName}/.service.AppBlockerAccessibilityService"
            return servicios.any { it.id == nombrePropio }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        settingsDataStore = SettingsDataStore(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        Log.i(TAG, "✅ Servicio de bloqueo de apps conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val paquete = event.packageName?.toString() ?: return

        // Ignorar el propio sistema y nuestra app
        if (paquete.startsWith("android") ||
            paquete.startsWith("com.android") ||
            paquete == this.packageName
        ) return

        val ahora = System.currentTimeMillis()
        if (ultimaAppBloqueada == paquete && ahora - tiempoUltimoBloqueo < INTERVALO_MIN_BLOQUEO) return

        scope.launch {
            try {
                val perfil = repository.getActiveProfile() ?: return@launch

                // Comprobar si la app es sensible (persistida en DB o por heurística)
                val esSensible = repository.isAppSensitivePersisted(paquete)
                    || repository.isAppSensitive(paquete)

                if (!esSensible) return@launch

                // Solo bloquear si está fuera del horario permitido
                if (perfil.isWithinAllowedTime()) {
                    Log.d(TAG, "✓ $paquete detectada — dentro de horario, permitida")
                    return@launch
                }

                // Actualizar anti-rebote
                ultimaAppBloqueada = paquete
                tiempoUltimoBloqueo = ahora

                // ── TrustFlow Engine: decide acción según nivel de confianza ─
                // Las ventajas de CAUTION/TRUSTED son premium; sin acceso → siempre LOCKED
                val isPremium = settingsDataStore.isPremium.firstOrNull() ?: false
                val isTrial = settingsDataStore.isFreeTrialActive()
                val trustFlowActivo = FreeTierLimits.canUseTrustFlow(isPremium, isTrial)

                val trustLevel = if (trustFlowActivo)
                    repository.getCurrentTrustLevel()
                else
                    com.guardianos.shield.data.TrustLevel.LOCKED

                Log.i(TAG, "🛡️ TrustLevel: $trustLevel (premium=$isPremium, trial=$isTrial) | $paquete")

                when (trustLevel) {
                    com.guardianos.shield.data.TrustLevel.TRUSTED -> {
                        // 30+ días de racha → comprobar presupuesto diario de minutos
                        val minutosRestantes = repository.getMinutosAutonomiaRestantes()
                        if (minutosRestantes > 0) {
                            Log.i(TAG, "🟢 TRUSTED ($minutosRestantes min restantes) — acceso registrado: $paquete")
                            repository.logTrustedAccess(paquete)
                            repository.consumirMinutoAutonomia()
                        } else {
                            // Presupuesto agotado: se comporta como LOCKED solo hoy
                            Log.i(TAG, "🔴 TRUSTED sin minutos — bloqueando hasta mañana: $paquete")
                            repository.addBlockedSite(domain = paquete, category = "TIEMPO_AGOTADO", threatLevel = 2)
                            val intent = Intent(this@AppBlockerAccessibilityService, AppBlockedActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                                putExtra(AppBlockedActivity.EXTRA_PAQUETE, paquete)
                                putExtra(AppBlockedActivity.EXTRA_START_HORA, perfil.startTimeMinutes)
                                putExtra(AppBlockedActivity.EXTRA_END_HORA, perfil.endTimeMinutes)
                                putExtra(AppBlockedActivity.EXTRA_MODO, AppBlockedActivity.MODO_LOCKED)
                                putExtra(AppBlockedActivity.EXTRA_RACHA, perfil.rachaActual)
                            }
                            startActivity(intent)
                        }
                    }
                    com.guardianos.shield.data.TrustLevel.CAUTION -> {
                        // 7-29 días → Friction Mode con cuenta atrás
                        Log.i(TAG, "🟡 CAUTION — lanzando friction screen: $paquete")
                        val intent = Intent(this@AppBlockerAccessibilityService, AppBlockedActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            putExtra(AppBlockedActivity.EXTRA_PAQUETE, paquete)
                            putExtra(AppBlockedActivity.EXTRA_START_HORA, perfil.startTimeMinutes)
                            putExtra(AppBlockedActivity.EXTRA_END_HORA, perfil.endTimeMinutes)
                            putExtra(AppBlockedActivity.EXTRA_MODO, AppBlockedActivity.MODO_CAUTION)
                            putExtra(AppBlockedActivity.EXTRA_RACHA, perfil.rachaActual)
                        }
                        startActivity(intent)
                    }
                    com.guardianos.shield.data.TrustLevel.LOCKED -> {
                        // 0-6 días → Bloqueo duro, solo se puede pedir permiso
                        Log.i(TAG, "⛔ LOCKED — bloqueo duro: $paquete")
                        repository.addBlockedSite(
                            domain = paquete,
                            category = "APP_BLOQUEADA",
                            threatLevel = 3
                        )
                        val intent = Intent(this@AppBlockerAccessibilityService, AppBlockedActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            putExtra(AppBlockedActivity.EXTRA_PAQUETE, paquete)
                            putExtra(AppBlockedActivity.EXTRA_START_HORA, perfil.startTimeMinutes)
                            putExtra(AppBlockedActivity.EXTRA_END_HORA, perfil.endTimeMinutes)
                            putExtra(AppBlockedActivity.EXTRA_MODO, AppBlockedActivity.MODO_LOCKED)
                            putExtra(AppBlockedActivity.EXTRA_RACHA, perfil.rachaActual)
                        }
                        startActivity(intent)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error procesando evento: ${e.message}")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
