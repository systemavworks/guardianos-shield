# Guía de Migración de Seguridad

## Cambios Implementados (v1.1.0)

### ✅ Correcciones de Seguridad

1. **PIN Parental Encriptado**
   - Antes: Almacenado en texto plano en Room Database
   - Ahora: Encriptado con AndroidKeyStore + EncryptedSharedPreferences
   - Hash: SHA-256 (no reversible, solo verificación)

2. **Backup Deshabilitado**
   - Antes: `android:allowBackup="true"` permitía extraer DB con ADB
   - Ahora: `android:allowBackup="false"` previene acceso no autorizado

3. **Logs de Producción Protegidos**
   - Información sensible solo se registra en modo DEBUG
   - Logs de PIN eliminados en builds release

### 🔄 Migración Automática

Los PINs existentes se migrarán automáticamente la primera vez que se verifiquen:
- El PIN legacy se lee de Room Database
- Se hashea con SHA-256
- Se guarda en EncryptedSharedPreferences
- El campo `parentalPin` queda marcado como `@Deprecated`

**No se requiere acción del usuario**. La migración es transparente.

### 🛠️ Para Desarrolladores

#### Usar SecurityHelper para PINs

```kotlin
// Guardar PIN
SecurityHelper.savePin(context, profileId, pin)

// Verificar PIN
if (SecurityHelper.verifyPin(context, profileId, enteredPin)) {
    // PIN correcto
}

// Verificar si existe PIN
if (SecurityHelper.hasPin(context, profileId)) {
    // Solicitar PIN
}

// Eliminar PIN
SecurityHelper.deletePin(context, profileId)
```

#### Migrar PINs Legacy Manualmente

Si necesitas forzar la migración:

```kotlin
val profile = repository.getActiveProfile()
profile?.parentalPin?.let { oldPin ->
    SecurityHelper.migrateLegacyPin(context, profile.id, oldPin)
}
```

### 📋 Checklist de Seguridad Post-Migración

- [x] PINs almacenados en EncryptedSharedPreferences
- [x] Backup deshabilitado en AndroidManifest.xml
- [x] Logs de PIN protegidos con BuildConfig.DEBUG
- [ ] ProGuard/R8 ofuscación (recomendado para release)
- [ ] Certificate pinning para DNS queries (próxima versión)
- [ ] Rate limiting en intentos de PIN (próxima versión)

### 🔐 Ubicación de Datos Sensibles

**ANTES** (inseguro):
```
/data/data/com.guardianos.shield/databases/guardian_database.db
  → Tabla user_profiles, columna parentalPin (texto plano)
```

**AHORA** (seguro):
```
/data/data/com.guardianos.shield/shared_prefs/guardian_secure_prefs.xml
  → Encriptado con AES256-GCM via AndroidKeyStore
  → Requiere autenticación del dispositivo para descifrar
```

### ⚠️ Notas Importantes

1. **Backup de Usuario**: Los usuarios perderán PINs si desinstalan y reinstalan la app (por diseño de seguridad)
2. **Root/Debug**: Dispositivos rooteados pueden acceder a EncryptedSharedPreferences (mejor que texto plano)
3. **Compatibilidad**: Requiere Android API 23+ para AndroidKeyStore (ya soportado, minSdk 24)

### 🐛 Resolución de Problemas

**Error: "PIN incorrecto" después de actualizar**
- Causa: El PIN legacy no se migró correctamente
- Solución: Restablecer PIN desde Control Parental → Configurar nuevo PIN

**Error: "EncryptedSharedPreferences creation failed"**
- Causa: AndroidKeyStore corrupto o dispositivo sin soporte
- Solución: Limpiar datos de app y reiniciar dispositivo

### 📞 Soporte

Para reportar problemas de seguridad:
- Email: security@guardianos.es
- GitHub Issues (privado): https://github.com/systemavworks/guardianos-shield/security/advisories

---

**Versión**: 1.1.0  
**Fecha**: 5 de febrero de 2026  
**Severidad**: CRÍTICA - Actualización recomendada inmediatamente
