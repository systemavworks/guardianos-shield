package com.guardianos.shield.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * DeviceAdminHelper — Utilidades para gestionar la protección anti-desinstalación.
 */
object DeviceAdminHelper {

    fun getComponentName(context: Context): ComponentName =
        ComponentName(context, GuardianDeviceAdminReceiver::class.java)

    /** Devuelve true si GuardianOS Shield está registrado como Device Admin */
    fun estaActivo(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getComponentName(context))
    }

    /**
     * Crea el Intent para solicitar activación de Device Admin,
     * sin lanzarlo (lo lanza el caller vía ActivityResultLauncher para obtener resultado).
     */
    fun crearIntentActivacion(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "GuardianOS Shield needs this permission to prevent a child " +
                "from uninstalling the parental control app without the parent PIN."
            )
        }

    /**
     * Lanza el diálogo del sistema para que el usuario active el Device Admin.
     * Debe llamarse desde una Activity (no desde un Service).
     */
    fun solicitarActivacion(context: Context) {
        context.startActivity(crearIntentActivacion(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Desactiva el Device Admin (el padre debe haber verificado su PIN antes en la UI).
     */
    fun desactivar(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.removeActiveAdmin(getComponentName(context))
    }
}
