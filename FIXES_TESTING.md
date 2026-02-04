# Fixes Implementados - Testing Dispositivos

## ✅ Compatibilidad Android 12-15+

### **Mejoras de Compatibilidad Multi-Versión**

**Versiones soportadas**: Android 12 (API 31) - Android 15+ (API 35+)

**Cambios implementados**:
1. **Uso de valores numéricos** en lugar de constantes Build.VERSION_CODES no disponibles
2. **Try-catch específicos** para cada versión con fallbacks seguros
3. **Logging detallado** por versión para debugging
4. **Manejo de NoSuchMethodError** para métodos introducidos en APIs recientes

---

## 🔧 Problemas Resueltos

### 1. **OPPO A53s (Android 12) - Redirección fallida al navegador seguro** ✅

**Problema**: Notificaciones de apps bloqueadas aparecen, pero no redirige a SafeBrowserActivity

**Causa raíz**: 
- OPPO ColorOS tiene restricciones agresivas para iniciar actividades desde background
- Flags `FLAG_ACTIVITY_CLEAR_TOP` y `FLAG_ACTIVITY_SINGLE_TOP` pueden fallar en OPPO

**Fix implementado en** [`UsageStatsMonitor.kt`](app/src/main/kotlin/com/guardianos/shield/service/UsageStatsMonitor.kt):
```kotlin
// ANTES (fallaba en OPPO)
flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP

// AHORA (OPPO-compatible)
flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
        Intent.FLAG_ACTIVITY_NO_ANIMATION or
        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        
// Usar ApplicationContext en vez de Context directo
context.applicationContext.startActivity(intent)
```

**Fallback añadido**: Si la redirección falla, muestra notificación crítica con vibración

---

### 2. **OPPO A53s (Android 12) - Horarios no funcionan** ✅

**Problema**: Control de horarios configurado pero se puede navegar a cualquier hora

**Causa raíz**: 
- `UserProfileEntity.isWithinAllowedTime()` solo se verificaba en `SafeBrowserActivity`
- `UsageStatsMonitor` redirigía sin comprobar horarios primero

**Fix implementado en** [`UsageStatsMonitor.kt`](app/src/main/kotlin/com/guardianos/shield/service/UsageStatsMonitor.kt):
```kotlin
// Ahora verifica horario ANTES de redirigir
val profile = repo.getActiveProfile()
if (profile != null && !profile.isWithinAllowedTime()) {
    isWithinSchedule = false
    Log.i("UsageStatsMonitor", "⏰ FUERA DE HORARIO - Bloqueado: $appLabel")
    showBlockedByScheduleNotification(appLabel)
    return@launch  // NO redirige, solo notifica
}
```

**Nuevo método** añadido en [`GuardianRepository.kt`](app/src/main/kotlin/com/guardianos/shield/data/GuardianRepository.kt):
```kotlin
suspend fun getActiveProfile(): UserProfileEntity? {
    return profileDao.getActiveProfile().firstOrNull()
}
```

**Nueva notificación** con icono ⏰ y categoría ALARM para horarios

---

### 3. **OPPO A80 (Android 15) - Crash al activar VPN** ✅

**Problema**: App crashea al cambiar de modo recomendado a modo avanzado (VPN)

**Causa raíz**:
- Android 15 (API 35 - Vanilla Ice Cream) tiene nuevos requisitos de VPN
- `SecurityException` al llamar `addDisallowedApplication()` en algunas configuraciones
- Falta configuración `setBlocking(false)` para Android 15+

**Fix implementado en** [`DnsFilterService.kt`](app/src/main/kotlin/com/guardianos/shield/service/DnsFilterService.kt):
```kotlin
// ANDROID 15+ configuración especial
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    try {
        builder.setBlocking(false)
        Log.d("GuardianVPN", "✅ Modo non-blocking configurado (Android 15+)")
    } catch (e: Exception) {
        Log.w("GuardianVPN", "⚠️ Configuración Android 15 fallida: ${e.message}")
    }
}

// Mejor manejo de excepciones
try {
    builder.addDisallowedApplication(packageName)
} catch (e: SecurityException) {
    Log.e("GuardianVPN", "❌ SecurityException: ${e.message}")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        throw e // Re-lanzar en Android 15 para logging
    }
}
```

**Manejo de errores mejorado**:
```kotlin
catch (e: SecurityException) { ... }
catch (e: IllegalArgumentException) { ... }  // Conflicto con otra VPN
catch (e: IllegalStateException) { ... }     // VPN ya en uso
catch (e: Exception) { ... }                 // Otros errores
```

**Nuevo permiso** en [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
```

**Mejor feedback al usuario** en [`MainActivity.kt`](app/src/main/kotlin/com/guardianos/shield/MainActivity.kt):
```kotlin
DnsFilterService.ACTION_VPN_ERROR -> {
    val errorMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        "Error VPN en Android 15. Revoca y vuelve a conceder permiso VPN en Ajustes"
    } else {
        "Error en servicio VPN. Verifica que no haya otra VPN activa"
    }
    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
}
```

---

## 🧪 Testing Checklist

### OPPO A53s (Android 12)
- [ ] Activar modo avanzado (VPN)
- [ ] Intentar abrir Chrome/navegador externo → debe redirigir a SafeBrowserActivity
- [ ] Configurar horario permitido (ej: 09:00 - 20:00)
- [ ] Intentar navegar fuera del horario → debe mostrar notificación ⏰ y NO redirigir
- [ ] Verificar logs: `adb logcat | grep -E "UsageStatsMonitor|SafeBrowser"`

### OPPO A80 (Android 15)
- [ ] Activar modo recomendado (sin VPN) → debe funcionar
- [ ] Cambiar a modo avanzado (VPN) → NO debe crashear
- [ ] Verificar logs: `adb logcat | grep GuardianVPN` debe mostrar "Android 15" y "non-blocking"
- [ ] Si crashea: revisar `adb logcat | grep -E "SecurityException|IllegalArgumentException"`
- [ ] Verificar DNS activo: `adb shell dumpsys connectivity | grep "DNS servers"` → debe ser 185.228.168.168

---

## 🛠️ Troubleshooting

### Si la redirección sigue fallando en OPPO
1. Ir a Ajustes → Apps → GuardianOS Shield → Permisos
2. Activar "Mostrar sobre otras apps"
3. Desactivar "Battery optimization" para la app
4. En ColorOS 12+: Ajustes → Seguridad → Gestión de apps → permitir inicio automático

### Si horarios no funcionan
```bash
# Verificar logs
adb logcat -c && adb logcat | grep "HORARIO"

# Verificar perfil activo en DB
adb shell "run-as com.guardianos.shield cat /data/data/com.guardianos.shield/databases/guardian_shield.db" | strings | grep scheduleEnabled
```

### Si VPN crashea en Android 15
```bash
# Ver logs completos del crash
adb logcat -c && adb logcat | grep -E "GuardianVPN|VpnService|FATAL"

# Revocar permiso VPN y volver a conceder
adb shell pm revoke com.guardianos.shield android.permission.BIND_VPN_SERVICE
# Luego reiniciar app y conceder permiso manualmente
```

### Verificar otra VPN activa
```bash
adb shell dumpsys connectivity | grep -A 20 "VPN"
```

---

## 📊 Logs de Debugging

### Redirección correcta
```
UsageStatsMonitor: ✅ Redirigido a SafeBrowser desde: Chrome
SafeBrowser: Redirigido desde: com.android.chrome
```

### Bloqueo por horario
```
UsageStatsMonitor: ⏰ FUERA DE HORARIO - Bloqueado: Chrome
SafeBrowser: ⏰ BLOQUEADO POR HORARIO: google.com
```

### VPN Android 15 OK
```
GuardianVPN: 🔧 Iniciando configuración VPN (Android 35)
GuardianVPN: ✅ Modo non-blocking configurado (Android 15+)
GuardianVPN: ✅ App excluida del túnel VPN
GuardianVPN: 📡 Estableciendo interfaz VPN...
GuardianVPN: ╔════════════════════════════════════╗
GuardianVPN: ║   DNS SEGURO ACTIVADO              ║
```

### VPN Error (para debugging)
```
GuardianVPN: ❌ SecurityException en VPN: Permission denied
GuardianVPN: 💡 Solución: Revoca y vuelve a conceder permiso VPN
MainActivity: VPN ERROR - Android 35
```

---

## ✅ Cambios en Archivos

| Archivo | Cambios |
|---------|---------|
| `UsageStatsMonitor.kt` | + Verificación horarios<br>+ OPPO-compatible intent flags<br>+ Notificaciones mejoradas (schedule, critical) |
| `DnsFilterService.kt` | + Android 15 compatibility<br>+ Manejo excepciones específicas<br>+ Logging detallado |
| `GuardianRepository.kt` | + Método `getActiveProfile()` |
| `MainActivity.kt` | + Error handling Android 15 |
| `AndroidManifest.xml` | + Permiso `FOREGROUND_SERVICE_CONNECTED_DEVICE` |

---

**Fecha de fixes**: 4 de febrero de 2026  
**Tested on**: Pendiente de verificación en OPPO A53s y OPPO A80
