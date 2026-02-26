package com.guardianos.shield.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.guardianos.shield.billing.FreeTierLimits
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.data.SettingsRepository
import com.guardianos.shield.data.TrustLevel
import com.guardianos.shield.data.UserProfileEntity
import com.guardianos.shield.ui.AppBlockedActivity
import com.guardianos.shield.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * AppBlockerAccessibilityService — v3 (accessibility-overlay-first)
 *
 * Estrategia de bloqueo en capas (compatible con Android 15 + OPPO/ColorOS 15):
 *   1. TYPE_ACCESSIBILITY_OVERLAY  — no requiere SYSTEM_ALERT_WINDOW, flota sobre TODO
 *      incluyendo status bar, nav bar y apps con filterTouchesWhenObscured (WhatsApp, TikTok).
 *      Los AccessibilityServices pueden usar este tipo sin permisos extra del sistema.
 *   2. GLOBAL_ACTION_BACK x3 + HOME — fuerza la salida antes de mostrar el overlay.
 *      HOME de respaldo para juegos que capturan BACK (Roblox, Free Fire).
 *   3. startActivity (AppBlockedActivity) — fallback si el overlay falla por error OEM.
 *
 * Problema resuelto respecto a v2:
 *   - TYPE_APPLICATION_OVERLAY en apps sideloaded sobre Android 15 / ColorOS 15
 *     falla silenciosamente: el permiso canDrawOverlays() puede devolver true pero
 *     el overlay queda tapado o no se muestra en OPPO A80 / Xiaomi / Samsung OneUI 7.
 *   - TYPE_ACCESSIBILITY_OVERLAY se dibuja dentro del contexto del AccessibilityService
 *     (capa del sistema), sin pasar por el pipeline BAL ni las restricciones sideload.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    // ── Coroutines ────────────────────────────────────────────────────────────
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // ── Dependencias ──────────────────────────────────────────────────────────
    private lateinit var repository: GuardianRepository
    private lateinit var settingsDataStore: SettingsRepository
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    // ── Estado del overlay ────────────────────────────────────────────────────
    private var overlayView: View? = null
    private var overlayPaquete: String? = null   // Qué app está bloqueando actualmente

    // ── Anti-rebote ───────────────────────────────────────────────────────────
    // Anti-rebote más corto (1s) para que apps con múltiples ventanas (TikTok, IG)
    // no tengan ventana de escape entre transiciones de actividad.
    private var ultimaAppBloqueada: String? = null
    private var tiempoUltimoBloqueo: Long = 0
    private val INTERVALO_MIN_BLOQUEO = 1000L

    companion object {
        private const val TAG = "AppBlockerA11y"

        fun estaActivado(context: Context): Boolean {
            val gestor = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            val servicios = gestor.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC
            )
            // El ID que devuelve el sistema puede tener dos formas según el build:
            //   Release: "com.guardianos.shield/.service.AppBlockerAccessibilityService"
            //     (forma abreviada, porque applicationId y package de la clase coinciden)
            //   Debug:   "com.guardianos.shield.debug/com.guardianos.shield.service.AppBlockerAccessibilityService"
            //     (FQN, porque applicationIdSuffix hace que el applicationId NO coincida
            //      con el package real de la clase Java)
            val classFqn = AppBlockerAccessibilityService::class.java.name
            return servicios.any { info ->
                info.id == "${context.packageName}/$classFqn" ||
                info.id == "${context.packageName}/.service.AppBlockerAccessibilityService"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ─────────────────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        settingsDataStore = SettingsRepository(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            // TYPE_WINDOW_STATE_CHANGED detecta cambios de actividad/fragmento en foreground.
            // TYPE_WINDOW_CONTENT_CHANGED como respaldo para apps que no disparan STATE_CHANGED
            // (p. ej. algunos launchers de Huawei).
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 50  // Más reactivo (antes 100ms)
        }

        Log.i(TAG, "✅ Servicio de bloqueo conectado (overlay-first mode)")
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ Servicio interrumpido")
    }

    override fun onDestroy() {
        eliminarOverlay()
        scope.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evento principal
    // ─────────────────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // TYPE_WINDOW_CONTENT_CHANGED: la app bloqueada sigue activa debajo del overlay
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.packageName?.toString() == overlayPaquete) {
            reforzarBloqueo()
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val paquete = event.packageName?.toString() ?: return

        // Ignorar propio package y sistema — con excepción explícita para tiendas de apps
        // com.android.vending (Play Store) debe pasar para poder bloquearse fuera de horario
        val esAppStore = paquete == "com.android.vending" ||
            paquete == "com.huawei.appmarket" ||
            paquete == "com.samsung.android.app.samsungapps"
        if (!esAppStore && (paquete.startsWith("android") ||
            paquete.startsWith("com.android") ||
            paquete == this.packageName)) return

        // Eliminar overlay SOLO si el usuario navegó explícitamente a una app
        // distinta a la bloqueada Y no es un launcher/sistema/popup transitorio.
        // Evita que TikTok abra un activity del sistema (share, pago) y el overlay desaparezca.
        if (overlayView != null && paquete != overlayPaquete) {
            val esLauncher = paquete.contains("launcher") ||
                             paquete.contains("home") ||
                             paquete == "com.google.android.apps.nexuslauncher" ||
                             paquete == "com.sec.android.app.launcher"
            val esAppReal = !paquete.startsWith("com.google.android.packageinstaller") &&
                            !paquete.startsWith("com.android") &&
                            !paquete.contains("systemui")
            // Solo quitar overlay si el usuario fue al launcher (HOME) o a una app real diferente
            if (esLauncher || esAppReal) {
                Log.d(TAG, "Usuario navegó a $paquete — eliminando overlay de $overlayPaquete")
                eliminarOverlay()
            }
        }

        // Anti-rebote
        val ahora = System.currentTimeMillis()
        if (ultimaAppBloqueada == paquete && ahora - tiempoUltimoBloqueo < INTERVALO_MIN_BLOQUEO) return

        scope.launch { evaluarYBloquear(paquete, ahora) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lógica de evaluación (en hilo IO)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun evaluarYBloquear(paquete: String, ahora: Long) {
        try {
            val perfil = repository.getActiveProfile() ?: return

            val esSensiblePersistido = repository.isAppSensitivePersisted(paquete)
            val categoria = repository.getCategoriaApp(paquete)

            // ── Bloqueo por categoría (independiente del horario) ─────────────────────────
            // blockGambling / blockGaming / blockSocialMedia / blockAdultContent del perfil
            val bloqueadoPorCategoria = esSensiblePersistido || when (categoria) {
                "BYPASS_TOOL"  -> true                  // VPN/proxy: SIEMPRE bloqueado
                "GAMBLING"     -> perfil.blockGambling
                "GAMING"       -> perfil.blockGaming
                "SOCIAL_MEDIA" -> perfil.blockSocialMedia
                "ADULT"        -> perfil.blockAdultContent
                "APP_STORE"    -> !perfil.isWithinAllowedTime() // Solo bloquear fuera de horario
                else           -> false
            }

            // ── Bloqueo por horario (apps sensibles genéricas fuera del horario) ────────
            val bloqueadoPorHorario = repository.isAppSensitive(paquete) && !perfil.isWithinAllowedTime()

            if (!bloqueadoPorCategoria && !bloqueadoPorHorario) return

            // ── Bonus de gaming del padre ─────────────────────────────────────────────
            // Si el padre concedió minutos extra hoy, éstos desactivan el bloqueo por
            // categoría GAMING (pero NO el bloqueo por horario escolar/nocturno).
            if (categoria == "GAMING" && bloqueadoPorCategoria && !bloqueadoPorHorario) {
                val gamingExtra = repository.getMinutosGamingExtra()
                if (gamingExtra > 0) {
                    Log.i(TAG, "🎮 Bonus gaming activo ($gamingExtra min restantes) — permitiendo: $paquete")
                    repository.logTrustedAccess(paquete)
                    repository.consumirMinutoGaming()
                    return  // acceso concedido sin bloqueo
                }
            }

            // Anti-rebote (actualizar después de validaciones)
            ultimaAppBloqueada = paquete
            tiempoUltimoBloqueo = ahora

            // TrustFlow Engine
            val isPremium = settingsDataStore.isPremium.firstOrNull() ?: false
            val isTrial = settingsDataStore.isFreeTrialActive()
            val trustFlowActivo = FreeTierLimits.canUseTrustFlow(isPremium, isTrial)

            val trustLevel = if (trustFlowActivo) repository.getCurrentTrustLevel()
                             else TrustLevel.LOCKED

            Log.i(TAG, "🛡️ TrustLevel=$trustLevel | categoria=$categoria | paquete=$paquete")

            when (trustLevel) {
                TrustLevel.TRUSTED -> manejarTrusted(paquete, perfil)
                TrustLevel.CAUTION -> activarBloqueo(paquete, perfil, AppBlockedActivity.MODO_CAUTION)
                TrustLevel.LOCKED  -> {
                    repository.addBlockedSite(paquete, if (categoria == "UNKNOWN") "APP_BLOQUEADA" else categoria, threatLevel = 3)
                    activarBloqueo(paquete, perfil, AppBlockedActivity.MODO_LOCKED)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error evaluando bloqueo: ${e.message}", e)
        }
    }

    private suspend fun manejarTrusted(paquete: String, perfil: Any) {
        val minutosRestantes = repository.getMinutosAutonomiaRestantes()
        if (minutosRestantes > 0) {
            Log.i(TAG, "🟢 TRUSTED — acceso concedido ($minutosRestantes min): $paquete")
            repository.logTrustedAccess(paquete)
            repository.consumirMinutoAutonomia()
        } else {
            Log.i(TAG, "🔴 TRUSTED sin minutos — bloqueando: $paquete")
            repository.addBlockedSite(paquete, "TIEMPO_AGOTADO", threatLevel = 2)
            activarBloqueo(paquete, perfil, AppBlockedActivity.MODO_LOCKED)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mecanismo de bloqueo en capas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Paso de entrada del bloqueo:
     *   1. BACK×3 + HOME (750ms combinados)
     *   2. Overlay vía TYPE_ACCESSIBILITY_OVERLAY — no depende de canDrawOverlays()
     *      Si falla (error OEM inesperado) → fallback a startActivity
     */
    private fun activarBloqueo(paquete: String, perfil: Any, modo: String) {
        Log.i(TAG, "⛔ Activando bloqueo [$modo]: $paquete")

        // Paso 1: BACK x3 → si la app captura BACK internamente (juegos), el HOME final garantiza salida
        mainHandler.post {
            repeat(3) { i ->
                mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, i * 150L)
            }
            // HOME de respaldo a los 600ms — cubre apps que capturan BACK (Roblox, Free Fire, etc.)
            mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 600L)
        }

        // Paso 2: overlay siempre — TYPE_ACCESSIBILITY_OVERLAY no necesita canDrawOverlays()
        mainHandler.postDelayed({
            mostrarOverlay(paquete, perfil, modo)
        }, 750L)
    }

    /**
     * Overlay con WindowManager usando TYPE_ACCESSIBILITY_OVERLAY.
     *
     * Por qué este tipo y no TYPE_APPLICATION_OVERLAY:
     *   - Disponible directamente para AccessibilityServices sin SYSTEM_ALERT_WINDOW.
     *   - En Android 15 / ColorOS 15 / OPPO sideload, TYPE_APPLICATION_OVERLAY puede
     *     quedar tapado o silenciado por el sistema aunque canDrawOverlays() sea true.
     *   - TYPE_ACCESSIBILITY_OVERLAY se renderiza en la capa del sistema, sobre
     *     filterTouchesWhenObscured (WhatsApp, TikTok, Instagram).
     *
     * Flags:
     *   - FLAG_NOT_FOCUSABLE: el IME (teclado) funciona normal debajo; no roba foco.
     *     En TYPE_ACCESSIBILITY_OVERLAY esto es seguro: el overlay sigue capturando
     *     toques aunque no sea focal (a diferencia de TYPE_APPLICATION_OVERLAY donde
     *     este flag activaba NOT_TOUCH_MODAL y los toques pasaban al fondo).
     *   - FLAG_LAYOUT_IN_SCREEN + FLAG_WATCH_OUTSIDE_TOUCH: cubre todo.
     *   - FLAG_TURN_SCREEN_ON: visible aunque la pantalla estuviera apagada.
     */
    private fun mostrarOverlay(paquete: String, perfil: Any, modo: String) {
        // Si ya hay overlay para este paquete, no duplicar
        if (overlayView != null && overlayPaquete == paquete) return

        // Si hay overlay de otra app, quitarlo primero
        eliminarOverlay()

        try {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(com.guardianos.shield.R.layout.overlay_blocker, null)

            configurarOverlayView(view, paquete, perfil, modo)

            val tipoOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                tipoOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )

            windowManager.addView(view, params)
            overlayView = view
            overlayPaquete = paquete

            Log.i(TAG, "✅ Overlay activo (TYPE_ACCESSIBILITY_OVERLAY) sobre: $paquete")

        } catch (e: Exception) {
            Log.e(TAG, "Error creando overlay: ${e.message} — usando fallback Activity", e)
            lanzarActividadFallback(paquete, perfil, modo)
        }
    }

    /**
     * Configura el contenido del overlay con los IDs de overlay_blocker.xml.
     */
    private fun configurarOverlayView(view: View, paquete: String, perfil: Any, modo: String) {
        val profile = perfil as? UserProfileEntity
        val nombreApp = resolverNombreApp(paquete)

        view.findViewById<android.widget.TextView>(R.id.tv_titulo)?.text =
            if (modo == AppBlockedActivity.MODO_CAUTION) "⚠️ Zona de Precaución" else "Acceso Bloqueado"

        view.findViewById<android.widget.TextView>(R.id.tv_modo)?.text = when (modo) {
            AppBlockedActivity.MODO_CAUTION -> "🟡 Modo Precaución"
            else -> "🔴 Bloqueo Total"
        }

        view.findViewById<android.widget.TextView>(R.id.tv_app_name)?.text = nombreApp

        view.findViewById<android.widget.TextView>(R.id.tv_horario)?.apply {
            if (profile != null && profile.scheduleEnabled) {
                val startH = profile.startTimeMinutes / 60
                val startM = profile.startTimeMinutes % 60
                val endH   = profile.endTimeMinutes / 60
                val endM   = profile.endTimeMinutes % 60
                text = "⏰ Permitido de %02d:%02d a %02d:%02d".format(startH, startM, endH, endM)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        view.findViewById<android.widget.TextView>(R.id.tv_racha)?.text =
            "🔥 ${profile?.rachaActual ?: 0} días de racha"

        view.findViewById<android.view.View>(R.id.btn_ir_inicio)?.setOnClickListener {
            // NO llamar eliminarOverlay() aquí — el overlay se elimina automáticamente
            // cuando el TYPE_WINDOW_STATE_CHANGED del launcher dispara en onAccessibilityEvent.
            // Eliminar antes crearía un frame donde la app bloqueada es visible.
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        view.findViewById<android.view.View>(R.id.btn_pedir_permiso)?.setOnClickListener {
            eliminarOverlay()
            lanzarActividadFallback(paquete, perfil, modo)
        }

        Log.d(TAG, "Overlay configurado para $paquete [modo=$modo, app=$nombreApp]")
    }

    /** Elimina el overlay actual de forma segura */
    fun eliminarOverlay() {
        mainHandler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                    Log.d(TAG, "Overlay eliminado (era de $overlayPaquete)")
                } catch (e: Exception) {
                    Log.w(TAG, "Error eliminando overlay: ${e.message}")
                }
            }
            overlayView = null
            overlayPaquete = null
        }
    }

    /**
     * Fallback cuando no hay permiso SYSTEM_ALERT_WINDOW.
     * Construye el intent correctamente para Android 10+ BAL restrictions.
     */
    private fun lanzarActividadFallback(paquete: String, perfil: Any, modo: String) {
        try {
            // Reflection para obtener los campos del perfil sin romper si cambia la clase
            val startHora = runCatching {
                perfil::class.java.getMethod("getStartTimeMinutes").invoke(perfil) as Int
            }.getOrDefault(0)
            val endHora = runCatching {
                perfil::class.java.getMethod("getEndTimeMinutes").invoke(perfil) as Int
            }.getOrDefault(1440)
            val racha = runCatching {
                perfil::class.java.getMethod("getRachaActual").invoke(perfil) as Int
            }.getOrDefault(0)

            val intent = Intent(this, AppBlockedActivity::class.java).apply {
                // FLAG_ACTIVITY_NEW_TASK: obligatorio desde Service
                // FLAG_ACTIVITY_CLEAR_TASK: evita que el usuario navegue "atrás" a la app bloqueada
                // FLAG_ACTIVITY_NO_ANIMATION: transición instantánea (menos chance de que el usuario vea la app)
                // FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS: no aparece en recientes
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(AppBlockedActivity.EXTRA_PAQUETE, paquete)
                putExtra(AppBlockedActivity.EXTRA_START_HORA, startHora)
                putExtra(AppBlockedActivity.EXTRA_END_HORA, endHora)
                putExtra(AppBlockedActivity.EXTRA_MODO, modo)
                putExtra(AppBlockedActivity.EXTRA_RACHA, racha)
            }
            startActivity(intent)
            Log.i(TAG, "📱 AppBlockedActivity lanzada (fallback) para $paquete")

        } catch (e: Exception) {
            Log.e(TAG, "Fallback startActivity también falló: ${e.message}", e)
            // Último recurso: ir a la pantalla de inicio
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * Refuerza el bloqueo si la app bloqueada sigue activa debajo del overlay.
     * Se llama cuando TYPE_WINDOW_CONTENT_CHANGED detecta actividad de la app bloqueada.
     */
    private fun reforzarBloqueo() {
        if (overlayView == null) return
        // Solo si el overlay existe pero la app está tratando de interactuar
        mainHandler.post {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    private fun resolverNombreApp(paquete: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(paquete, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            paquete
        }
    }
}

/*
════════════════════════════════════════════════════════════════════════════════
CHECKLIST DE INTEGRACIÓN
════════════════════════════════════════════════════════════════════════════════

1. AndroidManifest.xml — Añadir permiso:
   <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

2. res/xml/accessibility_service_config.xml — Añadir para Android 15:
   <accessibility-service
       ...
       android:isAccessibilityTool="true" />

3. res/layout/overlay_blocker.xml — Crear layout full-screen:
   - ConstraintLayout con background negro semi-transparente (#CC000000)
   - TextView: nombre de la app bloqueada
   - TextView: hora en que se desbloquea
   - Button: "Solicitar permiso" (abre flujo de aprobación parental)
   - Opcional en MODO_CAUTION: cuenta atrás de 15s antes de permitir acceso

4. Onboarding — Pedir permiso SYSTEM_ALERT_WINDOW al usuario:
   if (!Settings.canDrawOverlays(this)) {
       val intent = Intent(
           Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
           Uri.parse("package:$packageName")
       )
       startActivity(intent)
   }

5. Optimización de batería — El usuario debe excluir la app:
   - Ajustes → Batería → Optimización de batería → GuardianOS Shield → No optimizar

6. Para dispositivos Xiaomi/MIUI — Pedir permiso adicional:
   - Ajustes → Apps → Permisos → Mostrar ventanas emergentes en segundo plano

════════════════════════════════════════════════════════════════════════════════
CÓMO TESTEAR
════════════════════════════════════════════════════════════════════════════════

adb logcat | grep -E "AppBlockerA11y|ActivityManager"

Verifica en logcat:
  ✅ "Overlay activo sobre: com.instagram.android"  → overlay funcionando
  ✅ "AppBlockedActivity lanzada (fallback)"        → fallback funcionando
  ❌ "Error creando overlay"                        → falta permiso SYSTEM_ALERT_WINDOW
  ❌ "Fallback startActivity también falló"         → falta excepción batería o restricción OEM

════════════════════════════════════════════════════════════════════════════════
*/
