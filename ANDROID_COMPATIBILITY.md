# 🤖 Compatibilidad Android 12-15+

## Resumen de Compatibilidad

GuardianOS Shield está optimizado para funcionar en **Android 12 (API 31) hasta Android 15+ (API 35+)**.

---

## 📊 Matriz de Compatibilidad

| Android Version | API Level | Soporte | Características |
|----------------|-----------|---------|-----------------|
| Android 12 | 31 | ✅ Completo | VPN, Monitoreo apps, Horarios |
| Android 12L | 32 | ✅ Completo | Todas las características |
| Android 13 | 33 | ✅ Completo | + Permisos notificaciones explícitos |
| Android 14 | 34 | ✅ Completo | + Foreground service types mejorados |
| Android 15+ | 35+ | ✅ Completo | + Modo non-blocking VPN |

---

## 🔧 Ajustes por Versión Android

### **Android 12 (API 31-32)**
```kotlin
// Configuración básica VPN
if (Build.VERSION.SDK_INT >= 31) {
    // Notificaciones habilitadas por defecto
    // Foreground service con tipo básico
}
```

**Características**:
- VPN DNS transparente funcional
- UsageStatsMonitor completo
- Control de horarios
- Notificaciones automáticas (sin permiso explícito)

---

### **Android 13 (API 33)**
```kotlin
// Permisos explícitos de notificaciones
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Solicitar permiso POST_NOTIFICATIONS
    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

**Cambios implementados**:
- Solicitud permiso `POST_NOTIFICATIONS`
- Registro receivers con `RECEIVER_NOT_EXPORTED`
- WebView: `mediaPlaybackRequiresUserGesture = true`

---

### **Android 14 (API 34)**
```kotlin
// Tipos de foreground service explícitos
if (Build.VERSION.SDK_INT >= 34) {
    // Usar FOREGROUND_SERVICE_CONNECTED_DEVICE
    startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
}
```

**Cambios implementados**:
- Permiso `FOREGROUND_SERVICE_CONNECTED_DEVICE` en manifest
- Tipo de servicio especificado en `DnsFilterService`
- Mejor manejo de errores en inicio de foreground service

---

### **Android 15+ (API 35)**
```kotlin
// Configuración VPN non-blocking
if (Build.VERSION.SDK_INT >= 35) {
    try {
        builder.setBlocking(false)
        Log.d("GuardianVPN", "✅ Modo non-blocking configurado (Android 15+)")
    } catch (e: NoSuchMethodError) {
        // SDK < 35, ignorar
    }
}
```

**Cambios críticos**:
- `builder.setBlocking(false)` para evitar bloqueos
- Manejo específico de `SecurityException` (re-lanzar en API 35+)
- Try-catch para `NoSuchMethodError` si se compila con SDK < 35
- Mensajes de error específicos para Android 15

---

## 🛠️ Implementación Técnica

### **1. DnsFilterService - Compatibilidad VPN**

```kotlin
private fun setupVpn(): Boolean {
    return try {
        Log.d("GuardianVPN", "🔧 Iniciando VPN (Android ${Build.VERSION.SDK_INT})")
        
        val builder = Builder()
            .setSession("GuardianOS Shield")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addDnsServer(DNS_PRIMARY)
            .addDnsServer(DNS_SECONDARY)
        
        // ✅ ANDROID 15+: Configuración adicional
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                builder.setBlocking(false)
            } catch (e: NoSuchMethodError) {
                Log.w("GuardianVPN", "setBlocking() no disponible")
            }
        }
        
        // Excluir propia app
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: SecurityException) {
            // Android 15: fatal, re-lanzar
            if (Build.VERSION.SDK_INT >= 35) throw e
            // Android 12-14: log y continuar
            Log.w("GuardianVPN", "No se pudo excluir app")
        }
        
        vpnInterface = builder.establish()
        vpnInterface != null
        
    } catch (e: SecurityException) {
        Log.e("GuardianVPN", "❌ SecurityException: ${e.message}")
        false
    } catch (e: IllegalArgumentException) {
        Log.e("GuardianVPN", "❌ Conflicto con otra VPN")
        false
    } catch (e: IllegalStateException) {
        Log.e("GuardianVPN", "❌ VPN ya en uso")
        false
    }
}
```

**Excepciones manejadas**:
- `SecurityException`: Permisos VPN revocados o conflicto (Android 15+)
- `IllegalArgumentException`: Otra VPN activa
- `IllegalStateException`: VPN en estado inválido
- `NoSuchMethodError`: Método no disponible en SDK de compilación

---

### **2. MainActivity - Mensajes por Versión**

```kotlin
DnsFilterService.ACTION_VPN_ERROR -> {
    val errorMsg = when {
        Build.VERSION.SDK_INT >= 35 -> 
            "Error VPN en Android 15+. Revoca y vuelve a conceder permiso"
        Build.VERSION.SDK_INT >= 33 -> 
            "Error VPN en Android 13+. Verifica permisos de notificaciones"
        else -> 
            "Error en servicio VPN. Verifica que no haya otra VPN activa"
    }
    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
}
```

**Log información dispositivo**:
```kotlin
private fun logDeviceInfo() {
    Log.i("MainActivity", "╔════════════════════════════════════╗")
    Log.i("MainActivity", "║  Versión Android: API ${Build.VERSION.SDK_INT}")
    Log.i("MainActivity", "║  Fabricante: ${Build.MANUFACTURER}")
    Log.i("MainActivity", "║  Modelo: ${Build.MODEL}")
    Log.i("MainActivity", "╚════════════════════════════════════╝")
}
```

---

### **3. AppMonitorService - Foreground Types**

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    } catch (e: Exception) {
        // Fallback sin tipo
        startForeground(NOTIFICATION_ID, notification)
    }
    return START_STICKY
}
```

---

### **4. UsageStatsMonitor - Screen Detection**

```kotlin
private fun isScreenOn(): Boolean {
    return try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= 20) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    } catch (e: Exception) {
        true // Asumir pantalla encendida si falla
    }
}
```

---

### **5. SafeBrowserActivity - WebView Seguro**

```kotlin
@SuppressLint("SetJavaScriptEnabled")
private fun setupWebView() {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        
        // Android 12+: Seguridad adicional
        if (Build.VERSION.SDK_INT >= 31) {
            @Suppress("DEPRECATION")
            savePassword = false
            @Suppress("DEPRECATION")
            saveFormData = false
        }
        
        // Android 13+: Control de medios
        if (Build.VERSION.SDK_INT >= 33) {
            mediaPlaybackRequiresUserGesture = true
        }
    }
}
```

---

## 🧪 Testing por Versión

### **Android 12 (OPPO A53s)**
```bash
# Verificar VPN funciona
adb logcat | grep "GuardianVPN.*Android 31"
# Debe mostrar: "✅ Foreground service iniciado (Android 31)"

# Verificar redirección
adb logcat | grep "UsageStatsMonitor.*Redirigido"
```

### **Android 13**
```bash
# Verificar permiso notificaciones
adb shell dumpsys notification | grep "com.guardianos.shield"
# Debe mostrar: "granted=true"
```

### **Android 15 (OPPO A80)**
```bash
# Verificar modo non-blocking
adb logcat | grep "non-blocking"
# Debe mostrar: "✅ Modo non-blocking configurado (Android 15+)"

# Verificar NO crashea
adb logcat | grep -E "FATAL|AndroidRuntime"
# No debe mostrar crashes
```

---

## 📱 Dispositivos Testeados

| Dispositivo | Android | API | Estado | Notas |
|-------------|---------|-----|--------|-------|
| OPPO A53s | 12 | 31 | ✅ OK | ColorOS requiere flags especiales |
| OPPO A80 | 15 | 35 | ✅ OK | Requiere setBlocking(false) |
| Emulador Pixel | 12-15 | 31-35 | ✅ OK | Testing completo |

---

## 🐛 Problemas Conocidos y Soluciones

### **OPPO ColorOS**
**Problema**: Restricciones agresivas para apps en background  
**Solución**:
```kotlin
flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
        Intent.FLAG_ACTIVITY_NO_ANIMATION
context.applicationContext.startActivity(intent)
```

### **Android 15 SecurityException**
**Problema**: `addDisallowedApplication()` lanza SecurityException  
**Solución**:
```kotlin
try {
    builder.addDisallowedApplication(packageName)
} catch (e: SecurityException) {
    if (Build.VERSION.SDK_INT >= 35) {
        throw e // Re-lanzar para logging completo
    }
}
```

### **SDK Compilación < 35**
**Problema**: `setBlocking()` no existe en SDKs anteriores  
**Solución**:
```kotlin
try {
    builder.setBlocking(false)
} catch (e: NoSuchMethodError) {
    // Método no disponible, ignorar
}
```

---

## ✅ Checklist de Compatibilidad

- [x] Valores numéricos en lugar de Build.VERSION_CODES no disponibles
- [x] Try-catch para NoSuchMethodError en métodos nuevos
- [x] Fallbacks seguros para todas las versiones
- [x] Logging detallado por versión Android
- [x] Permisos específicos por versión (manifest)
- [x] Foreground service types apropiados
- [x] WebView configurado seguro para todas las versiones
- [x] Mensajes de error específicos por versión
- [x] Device info logging para debugging

---

## 🚀 Compilación y Deploy

```bash
# Compilar para todas las versiones
./gradlew assembleDebug

# Verificar minSdk y targetSdk en build.gradle
# minSdk = 24  (Android 7.0 - mínimo realista)
# targetSdk = 34  (Android 14 - actual)
# compileSDK = 34  (puede no tener APIs de Android 15)

# APK compatible Android 12-15+
app/build/outputs/apk/debug/guardianos-shield-v1.0.0-debug.apk
```

---

**Última actualización**: 4 de febrero de 2026  
**Versiones soportadas**: Android 12 (API 31) - Android 15+ (API 35+)  
**Testing**: OPPO A53s (Android 12), OPPO A80 (Android 15)
