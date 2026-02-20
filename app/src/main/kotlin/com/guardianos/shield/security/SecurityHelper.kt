package com.guardianos.shield.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Clase para manejo seguro de datos sensibles (PIN parental)
 * Usa EncryptedSharedPreferences con AndroidKeyStore
 */
object SecurityHelper {
    
    private const val PREFS_NAME = "guardian_secure_prefs"
    private const val PIN_KEY_PREFIX = "pin_"
    
    /**
     * Inicializa EncryptedSharedPreferences con MasterKey
     */
    private fun getEncryptedPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecurityHelper", "Error al crear EncryptedSharedPreferences", e)
        null
    }
    
    /**
     * Guarda un PIN de forma segura
     * @param context Contexto de la aplicación
     * @param profileId ID del perfil
     * @param pin PIN en texto plano
     * @return true si se guardó correctamente
     */
    fun savePin(context: Context, profileId: Int, pin: String): Boolean {
        if (pin.isEmpty()) return false
        
        return try {
            val prefs = getEncryptedPrefs(context) ?: return false
            val hashedPin = hashPin(pin)
            prefs.edit().putString("$PIN_KEY_PREFIX$profileId", hashedPin).apply()
            true
        } catch (e: Exception) {
            Log.e("SecurityHelper", "Error al guardar PIN", e)
            false
        }
    }
    
    /**
     * Verifica un PIN contra el almacenado
     * @param context Contexto de la aplicación
     * @param profileId ID del perfil
     * @param pin PIN a verificar
     * @return true si el PIN es correcto
     */
    fun verifyPin(context: Context, profileId: Int, pin: String): Boolean {
        if (pin.isEmpty()) return false
        
        return try {
            val prefs = getEncryptedPrefs(context) ?: return false
            val storedHash = prefs.getString("$PIN_KEY_PREFIX$profileId", null) ?: return false
            val inputHash = hashPin(pin)
            storedHash == inputHash
        } catch (e: Exception) {
            Log.e("SecurityHelper", "Error al verificar PIN", e)
            false
        }
    }
    
    /**
     * Verifica si existe un PIN para el perfil
     */
    fun hasPin(context: Context, profileId: Int): Boolean {
        return try {
            val prefs = getEncryptedPrefs(context) ?: return false
            prefs.contains("$PIN_KEY_PREFIX$profileId")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Elimina el PIN de un perfil
     */
    fun deletePin(context: Context, profileId: Int): Boolean {
        return try {
            val prefs = getEncryptedPrefs(context) ?: return false
            prefs.edit().remove("$PIN_KEY_PREFIX$profileId").apply()
            true
        } catch (e: Exception) {
            Log.e("SecurityHelper", "Error al eliminar PIN", e)
            false
        }
    }
    
    /**
     * Hash del PIN usando SHA-256
     * No es reversible, solo se puede verificar
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    /**
     * Migra PINs existentes desde Room a EncryptedSharedPreferences
     * Llamar una sola vez al actualizar la app
     */
    fun migrateLegacyPin(context: Context, profileId: Int, oldPin: String?) {
        if (!oldPin.isNullOrEmpty() && !hasPin(context, profileId)) {
            savePin(context, profileId, oldPin)
            Log.d("SecurityHelper", "PIN migrado para perfil $profileId")
        }
    }
}
