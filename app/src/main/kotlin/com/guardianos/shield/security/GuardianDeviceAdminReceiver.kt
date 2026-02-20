package com.guardianos.shield.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * GuardianDeviceAdminReceiver — Anti-tampering via Device Admin API.
 *
 * Cuando está habilitado, el sistema muestra un diálogo de confirmación
 * antes de permitir la desinstalación de la app, disuadiendo al menor
 * de eliminar la protección sin el conocimiento del padre.
 *
 * El padre activa esto desde Ajustes → Seguridad → Administradores de dispositivo.
 * NO requiere root ni permisos especiales más allá del consentimiento del usuario.
 *
 * IMPORTANTE: Una vez activado, solo se puede desactivar introduciendo el PIN parental
 * (gestionado en onDisableRequested) o manualmente en Ajustes del sistema.
 */
class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "GuardianDeviceAdmin"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "✅ Device Admin activado — desinstalación protegida")
        Toast.makeText(
            context,
            "🛡️ Protección anti-desinstalación activada",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Este mensaje aparece en el diálogo de desactivación del sistema
        return "⚠️ Para desactivar la protección de GuardianOS Shield necesitas el PIN parental. " +
               "¿Estás seguro de que quieres desactivarla?"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "⚠️ Device Admin desactivado — la app puede desinstalarse")
        Toast.makeText(
            context,
            "⚠️ Protección anti-desinstalación desactivada",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        Log.w(TAG, "Intento fallido de PIN en pantalla de desbloqueo")
    }
}
