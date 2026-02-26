# INFORME DE SEGURIDAD — Guardianos Shield v1.1.0
**Fecha**: 24 de febrero de 2026  
**Analista**: GitHub Copilot (análisis estático automatizado)  
**Alcance**: APK release, APK debug, código fuente Kotlin  
**Herramientas**: apksigner, aapt2, androguard, apktool (smali), análisis estático de código fuente  

---

## RESUMEN EJECUTIVO

| Severidad | Hallazgos |
|-----------|-----------|
| 🔴 CRÍTICO | 0 |
| 🟠 ALTO    | 2 |
| 🟡 MEDIO   | 3 |
| 🔵 BAJO    | 3 |
| ✅ OK      | 17 |

La aplicación **no presenta vulnerabilidades críticas**. Los hallazgos más relevantes son:
broadcasts VPN sin permiso de protección (ALTO) y JavaScript habilitado en WebView sin Content Security Policy (ALTO).

---

## 1. FIRMA DIGITAL Y CERTIFICADO

### Resultado: 🟡 MEDIO (esquema de firma incompleto)

| Campo | Valor |
|-------|-------|
| Firma v1 (JAR) | ❌ No |
| Firma v2 (APK Signature Scheme v2) | ✅ Sí |
| Firma v3 (APK Signature Scheme v3) | ❌ No |
| Firma v3.1 | ❌ No |
| Firma v4 | ❌ No |
| Número de firmantes | 1 |
| Algoritmo clave | RSA 2048 bits |
| DN del certificado | CN=Guardianos, OU=Shield, O=Guardianos, L=Sevilla, ST=Andalucia, C=ES |
| SHA-256 cert | `2421519eeb7149b2ad4c769f7d4a99f577ac6014f850cb2a6538fa856b75c778` |
| APK Debug | ✅ Firmado (debug key — NO usar en producción) |

**Hallazgos**:
- ✅ Firmado con v2: compatible con Android 7+ (minSdk=31, suficiente)
- ✅ **[RESUELTO] v3 y v4 habilitados** en `app/build.gradle.kts`: `enableV3Signing = true`, `enableV4Signing = true`
- ⚠️ **[BAJO] RSA 2048 bits**: aceptable pero RSA 4096 o EC P-256 sería más robusto a largo plazo
- ✅ Certificado de producción personalizado (no debug key de Android)

**Acción recomendada**:
```bash
# Ya aplicado en app/build.gradle.kts dentro del bloque signingConfig:
enableV1Signing = false  # no necesario (minSdk 31)
enableV2Signing = true
enableV3Signing = true   # ✅ APLICADO
enableV4Signing = true   # ✅ APLICADO
```

---

## 2. ANDROIDMANIFEST.XML — PERMISOS Y COMPONENTES

### 2a. Configuración general

| Atributo | Valor | Estado |
|----------|-------|--------|
| `android:debuggable` | `null` (false implícito en release) | ✅ OK |
| `android:allowBackup` | `false` | ✅ OK |
| `networkSecurityConfig` | Presente (`@7F0E0005`) | ✅ OK |
| `minSdkVersion` | 31 (Android 12) | ✅ OK |
| `targetSdkVersion` | 34 (Android 14) | ✅ OK |

### 2b. Permisos — Análisis individual

| Permiso | Necesario | Riesgo | Observación |
|---------|-----------|--------|-------------|
| `INTERNET` | ✅ Sí | Bajo | Navegador web interno |
| `ACCESS_NETWORK_STATE` | ✅ Sí | Bajo | Verificar conectividad VPN |
| `FOREGROUND_SERVICE` | ✅ Sí | Bajo | AppMonitorService |
| `FOREGROUND_SERVICE_SPECIAL_USE` | ✅ Sí | Bajo | API 34, control parental |
| `POST_NOTIFICATIONS` | ✅ Sí | Bajo | Notificaciones de bloqueo |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | ✅ Eliminado | — | Mismatch con VPN resuelto: todos los servicios usan `specialUse` |
| `PACKAGE_USAGE_STATS` | ✅ Sí | **Medio** | Permiso especial — requiere justificación Play Store |
| `KILL_BACKGROUND_PROCESSES` | ⚠️ Revisar | **Medio** | No parece imprescindible — puede levantar alertas en revisión Play Store |
| `SYSTEM_ALERT_WINDOW` | ✅ Sí | **Medio** | Overlay de bloqueo — justificado en app parental |
| `QUERY_ALL_PACKAGES` | ✅ Sí | **Medio** | Listar apps instaladas — requiere declaración en Play Store |
| `WAKE_LOCK` | ✅ Sí | Bajo | Mantener monitorización activa |
| `RECEIVE_BOOT_COMPLETED` | ✅ Sí | Bajo | Autoarranque |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | ✅ Sí | Bajo | Servicios en primer plano |
| `com.android.vending.BILLING` | ✅ Sí | Bajo | Compras in-app |

**Hallazgo [MEDIO] RESUELTO**:  
`FOREGROUND_SERVICE_CONNECTED_DEVICE` — eliminado del Manifest. `DnsFilterService` migrado a `foregroundServiceType="specialUse"` junto con `startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`. Todos los servicios foreground usan ahora el mismo tipo, consistente con la propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` ya declarada.

### 2c. Componentes exportados

| Componente | Tipo | exported | Protección | Estado |
|-----------|------|----------|-----------|--------|
| `MainActivity` | Activity | ✅ true | — (necesario, launcher) | ✅ OK |
| `SafeBrowserActivity` | Activity | ✅ false | — | ✅ OK |
| `AppBlockedActivity` | Activity | ✅ false | — | ✅ OK |
| `DnsFilterService` | Service (VPN) | ✅ false | `BIND_VPN_SERVICE` | ✅ OK |
| `AppMonitorService` | Service | ✅ false | — | ✅ OK |
| `LightweightMonitorService` | Service | ✅ false | — | ✅ OK |
| `AppBlockerAccessibilityService` | Service | ✅ true | `BIND_ACCESSIBILITY_SERVICE` | ✅ OK (requerido) |
| `GuardianDeviceAdminReceiver` | Receiver | ✅ true | `BIND_DEVICE_ADMIN` | ✅ OK (requerido) |
| `SystemJobService` (WorkManager) | Service | true | `BIND_JOB_SERVICE` | ✅ OK (librería) |
| `DiagnosticsReceiver` (WorkManager) | Receiver | true | `DUMP` | ✅ OK (librería) |
| `ProfileInstallReceiver` (AndroidX) | Receiver | true | `DUMP` | ✅ OK (librería) |
| `InitializationProvider` (AndroidX) | Provider | ✅ false | — | ✅ OK |

**Ningún componente propio exportado sin protección** (excepto MainActivity que es el launcher, correcto).

---

## 3. NETWORK SECURITY CONFIG

### Resultado: ✅ EXCELENTE

```xml
<base-config cleartextTrafficPermitted="false">
    <trust-anchors>
        <certificates src="system" />
    </trust-anchors>
</base-config>
```

- ✅ `cleartextTrafficPermitted="false"` — prohíbe HTTP sin cifrar
- ✅ Solo certificados del sistema (no `user` certificates)
- ✅ No se confía en CAs instaladas por el usuario
- ✅ No hay dominios con `cleartext=true` exceptions

---

## 4. WEBVIEW — SEGURIDAD

### Resultado: 🟠 ALTO (JavaScript habilitado sin CSP)

**Configuración detectada en `SafeBrowserActivity.kt`**:

```kotlin
settings.javaScriptEnabled = true        // ← Necesario para navegador
settings.domStorageEnabled = true         // ← Necesario para navegador
settings.savePassword = false             // ✅ Bien
settings.saveFormData = false             // ✅ Bien
settings.mediaPlaybackRequiresUserGesture = true  // ✅ Bien
```

| Check | Estado |
|-------|--------|
| `javaScriptEnabled = true` | ⚠️ Habilitado (necesario para navegador) |
| `addJavascriptInterface` | ✅ NO encontrado |
| `setAllowUniversalAccessFromFileURLs` | ✅ NO (false por defecto) |
| `setAllowFileAccessFromFileURLs` | ✅ NO (false por defecto) |
| `savePassword = false` | ✅ OK |
| `saveFormData = false` | ✅ OK |
| `setWebContentsDebuggingEnabled` | ✅ NO detectado |
| `onReceivedSslError` override | ✅ NO sobreescrito (usa comportamiento seguro por defecto) |
| `shouldOverrideUrlLoading` | ✅ Implementado correctamente con validación de dominio |

**Hallazgo [ALTO]**:  
JavaScript está habilitado y el navegador carga URLs externas. Aunque el vector de ataque está muy mitigado por `shouldOverrideUrlLoading` + `LocalBlocklist`, **no hay Content Security Policy (CSP)** global configurada. Un site malicioso que pase el filtro DNS podría ejecutar JavaScript arbitrario en el WebView.

**Hallazgo [BAJO]**:  
En `shouldOverrideUrlLoading`, hay logs con `Log.d("SafeBrowser", ...)` que incluyen el nombre del perfil y estado del horario. En release, estos logs son visibles para otras apps con `READ_LOGS` en versiones < Android 4.1.

**Acción recomendada**:
```kotlin
// Añadir en setupWebView():
webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE // si la privacidad es prioritaria
// Para páginas de bloqueo generadas localmente, añadir CSP en el HTML:
// <meta http-equiv="Content-Security-Policy" content="default-src 'none';">
```

---

## 5. SSL/TLS Y CIFRADO

### Resultado: ✅ CORRECTO

- ✅ **No hay `TrustAllCerts`** ni `X509TrustManager` personalizado que omita validación
- ✅ **No hay `ALLOW_ALL_HOSTNAME_VERIFIER`** ni verificador de hostname inseguro
- ✅ **No hay `onReceivedSslError` con `handler.proceed()`** (error SSL no se ignora)
- ✅ **No se usa crypto propio**: No se detectó `MessageDigest(MD5/SHA1)`, `DES`, `AES/ECB`, ni `Random()` para generar claves en el código propio
- ✅ Google Tink (`com.google.crypto.tink`) presente como dependencia (crypto moderno)
- ✅ Comunicación DNS sobre UDP/53 con CleanBrowsing (185.228.168.168/169) — no HTTP

---

## 6. SECRETS Y DATOS SENSIBLES HARDCODED

### Resultado: ✅ LIMPIO

- ✅ **0 claves de API, passwords o tokens** encontrados en código fuente Kotlin
- ✅ **0 strings sospechosas** en smali del paquete `com.guardianos.shield`
- ✅ URLs presentes en smali son solo: `https://guardianos.es/shield`, `https://guardianos.es/politica-privacidad`, `https://github.com/systemavworks/guardianos-shield` — todas legítimas
- ✅ IPs DNS (185.228.168.168) visibles en código — correcto (son servidores públicos de CleanBrowsing)

---

## 7. BROADCASTS NO PROTEGIDOS

### Resultado: 🟠 ALTO

**Código en `DnsFilterService.kt`**:
```kotlin
sendBroadcast(Intent(ACTION_VPN_STARTED))   // línea 86
sendBroadcast(Intent(ACTION_VPN_ERROR))     // línea 89
sendBroadcast(Intent(ACTION_VPN_STOPPED))   // línea 163
```

Estos broadcasts son **implícitos y sin permiso de receptor**, lo que significa que **cualquier aplicación del dispositivo** puede registrar un `BroadcastReceiver` para `ACTION_VPN_STARTED/STOPPED/ERROR` y conocer el estado de la VPN de Guardianos Shield.

**Impacto**: Un malware podría detectar cuándo la VPN está activa/inactiva y actuar en consecuencia (por ejemplo, esperar a que se detenga para enviar datos).

**Acción recomendada**:
```kotlin
// Opción 1: añadir permiso de firmante al broadcast
sendBroadcast(Intent(ACTION_VPN_STARTED), "com.guardianos.shield.permission.VPN_STATE")

// Opción 2 (mejor): usar LocalBroadcastManager dentro del proceso
import androidx.localbroadcastmanager.content.LocalBroadcastManager
LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_VPN_STARTED))

// En MainActivity, registrar con LocalBroadcastManager:
LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver, filter)
```

---

## 8. VERIFICACIÓN DE COMPRAS IN-APP (BILLING)

### Resultado: 🟡 MEDIO (sin verificación server-side)

- ✅ `acknowledgePurchase` correcto — se reconocen compras
- ✅ `purchaseToken` se usa correctamente
- ⚠️ **[MEDIO] No hay verificación de signature server-side**: la validación de que la compra es legítima se hace únicamente en cliente (Google Play Billing SDK). Un atacante con acceso root o usando herramientas como Lucky Patcher podría falsificar el estado de compra.

**Recomendación**: Para apps de pago, verificar la compra contra la API de Google Play Developer en un backend propio, comparando `purchaseToken` con la respuesta del servidor.

---

## 9. LOGS EN PRODUCCIÓN

### Resultado: 🔵 BAJO → ✅ RESUELTO

Se detectaron numerosos `Log.d/i/w/e(TAG, ...)` en clases de producción:
- `BillingManager.kt`: logs de estado de compras con `debugMessage`
- `AppBlockerAccessibilityService.kt`: logs de nombre de app bloqueada, paquete
- `SafeBrowserActivity.kt`: logs de nombre de perfil, URL intentada, estado horario

**Impacto**: En Android < 4.1, cualquier app con `READ_LOGS` podía leer logcat. En Android 4.1+ esto requiere permiso `READ_LOGS` (signature/system). Riesgo bajo en dispositivos modernos pero puede revelar información sensible en entornos de testing.

**Acción aplicada en `proguard-rules.pro`**:
```proguard
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);  # ← AÑADIDO
    # Log.e se conserva intencionalmente para Crashlytics / Play Console
}
```

---

## 10. OBFUSCACIÓN R8

### Resultado: ✅ ACTIVA

- ✅ R8/ProGuard activo — clases renombradas a `A00, A01, a0, a1...`
- ✅ Código fuente no recuperable directamente desde el APK
- ✅ `minifyEnabled = true` en build release (inferido por el renaming masivo)

---

## RESUMEN DE HALLAZGOS Y PRIORIDADES

### 🟠 ALTO — Acción inmediata recomendada

| # | Hallazgo | Archivo | Acción |
|---|---------|--------|--------|
| 1 | Broadcasts VPN sin permiso receptor | `DnsFilterService.kt:86,89,163` | Usar `LocalBroadcastManager` o añadir permiso protector |
| 2 | WebView JS habilitado sin CSP en páginas de bloqueo | `SafeBrowserActivity.kt:271` | Añadir CSP en HTML de página de bloqueo generado localmente |

### 🟡 MEDIO — Planificar para próxima versión

| # | Hallazgo | Archivo | Acción |
|---|---------|--------|--------|
| 3 | ~~Firma APK solo con esquema v2 (sin v3/v4)~~ | ~~`app/build.gradle.kts`~~ | ✅ Resuelto — v3/v4 habilitados |
| 4 | Billing sin verificación server-side | `BillingManager.kt` | Considerar backend de verificación |
| 5 | ~~`FOREGROUND_SERVICE_CONNECTED_DEVICE` / mismatch `connectedDevice`~~ | ~~`AndroidManifest.xml`~~ | ✅ Resuelto — todos los servicios usan `specialUse` |

### 🔵 BAJO — Deuda técnica

| # | Hallazgo | Archivo | Acción |
|---|---------|--------|--------|
| 6 | ~~Logs de producción con info sensible~~ | ~~Múltiples~~ | ✅ Resuelto — d/v/i/w eliminados por R8 en release |
| 7 | RSA 2048 bits (debería ser 4096 o EC) | Keystore | Al renovar keystore, usar EC P-256 |
| 8 | `minSdkVersion=31` ≠ documentación (que indica 24) | `copilot-instructions.md` | Actualizar documentación |
| 9 | `KILL_BACKGROUND_PROCESSES` posiblemente no necesario | `AndroidManifest.xml` | Verificar uso real |

### ✅ CORRECTO — Buenos puntos de seguridad

- ~~Logs en producción con info sensible~~ — ✅ eliminados vía R8/ProGuard (d/v/i/w)
- ~~Firma APK solo v2~~ — ✅ v3 y v4 habilitados en `build.gradle.kts`
- ~~`DnsFilterService` con `connectedDevice` en lugar de `specialUse`~~ — ✅ migrado, todos los servicios usan `specialUse`
- `allowBackup="false"` — datos no respaldados
- `debuggable` no presente en release
- Network Security Config: solo TLS, solo CAs del sistema
- Ningún componente propio exportado sin protección
- 0 secretos/APIs hardcoded en código fuente
- No usa `TrustAllCerts` ni bypasses SSL/TLS
- No tiene `addJavascriptInterface` (sin API bridge JavaScript→Java)
- `setAllowUniversalAccessFromFileURLs = false` (por defecto)
- `savePassword = false`, `saveFormData = false` en WebView
- R8 obfuscación activa
- No accede a CONTACTS, CAMERA, MICROPHONE, LOCATION
- CleanBrowsing DNS como filtro externo (no intercepta paquetes raw)

---

## ARCHIVOS DE EVIDENCIA

Los resultados detallados están en `security-report/`:
- `01_firma.txt` — salida completa de apksigner
- `02_manifest.txt` — AndroidManifest decodificado por aapt2
- `03_static_analysis.txt` — análisis grep del código fuente
- `04_smali_analysis.txt` — análisis del bytecode smali
- `05_androguard_fast.txt` — metadatos APK via androguard
- `apktool_out/` — APK decompilado completo (smali + recursos)

---

*Informe generado automáticamente — complementar con pruebas dinámicas en dispositivo físico (fuzzing de intents, análisis de tráfico de red con proxy MITM como mitmproxy/Burp Suite)*
