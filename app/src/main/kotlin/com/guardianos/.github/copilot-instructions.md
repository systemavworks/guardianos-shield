# GuardianOS Shield - AI Agent Guide

## Project Overview
**GuardianOS Shield** es una aplicación Android de control parental que combina filtrado DNS transparente, monitoreo de aplicaciones, y bloqueo de contenido. Usa VPN local (sin servidor remoto) para interceptar consultas DNS y aplicar filtros.

**Stack**: Kotlin, Jetpack Compose, Room Database, Coroutines, Android VpnService API  
**Location**: `/home/victor/guardianos-shield/app/src/main/kotlin/com/guardianos/shield/`

## Architecture

### 3-Layer Architecture
```
UI Layer (Compose)              → MainActivity, *Screen.kt
├─ Data Layer (Room + Repos)    → GuardianRepository, *Dao, *Entity
└─ Service Layer (Background)   → DnsFilterService, UsageStatsMonitor, AppMonitorService
```

### Core Services
- **DnsFilterService** (VpnService): Intercepta DNS sin procesar paquetes. Configura CleanBrowsing DNS (185.228.168.168/185.228.169.168) como filtro primario Adult Filter (más restrictivo). NO usa `addRoute("0.0.0.0", 0)` para evitar capturar todo el tráfico.
- **AppMonitorService**: Foreground service persistente que inicia `UsageStatsMonitor`. Usa `START_STICKY` para supervivencia en OEM restrictivos (OPPO, Motorola).
- **UsageStatsMonitor**: Monitorea apps en primer plano cada 2s (10s si pantalla apagada), bloquea navegadores/apps sensibles y redirige a SafeBrowserActivity con flags `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- **SafeBrowserActivity**: Navegador seguro con WebView filtrado que valida dominios contra `LocalBlocklist.isBlocked()` antes de cargar.
- **LightweightMonitorService**: LifecycleService alternativo (puede deshabilitarse con `disableLightweightMonitoring` en DataStore).

### Data Flow
1. **DNS Filtering**: DNS Query → DnsFilterService tunnel (10.0.0.2) → CleanBrowsing Adult Filter → Logs blocked domains
2. **App Blocking**: UsageStatsMonitor detects foreground app → Checks `browserPackages` + `socialMediaPackages` → Blocks if in blocklist + logs → Launches SafeBrowserActivity
3. **Logging**: Blocked event → `GuardianRepository.logDnsQuery()` → DnsLogEntity + BlockedSiteEntity → Statistics update via `updateTodayStatistics()`

## Key Patterns

### State Management
- **MainActivity**: Usa `mutableStateOf` directamente (no ViewModel). Estados: `isServiceRunning`, `protectionMode`, `blockedCount`, `currentProfile`.
- **Repository Pattern**: `GuardianRepository` centraliza lógica de datos (Room + coroutines). Métodos `suspend fun` para operaciones DB, Flows para observables.
- **Flow-based UI Updates**: DAOs exponen `Flow<>` que la UI colecta con `collectAsState()`. Ejemplo: `val recentBlocked by repository.recentBlocked.collectAsState(initial = emptyList())`.
- **State Persistence**: `SettingsRepository` (DataStore) guarda `isVpnActive`, `protectionMode`, `isMonitoringActive` para restaurar al reiniciar. CRÍTICO: Actualizar DataStore en cambios de estado para sincronización.

### Service Lifecycle
```kotlin
// Iniciar VPN (requiere permiso VpnService.prepare())
val intent = VpnService.prepare(context)
if (intent == null) {
    startService(Intent(context, DnsFilterService::class.java))
    settingsRepository.updateVpnActive(true)  // ← Persistir estado
} else vpnPermissionLauncher.launch(intent)

// Iniciar monitoreo (requiere permiso UsageStats)
if (hasUsageStatsPermission()) {
    AppMonitorService.start(context)
    settingsRepository.updateMonitoringActive(true)  // ← Persistir estado
}

// Detener servicios con cleanup
stopService(Intent(context, DnsFilterService::class.java))
AppMonitorService.stop(context)
settingsRepository.updateVpnActive(false)
```

### Database Schema (Room v4)
- **BlockedSiteEntity**: `domain`, `category` (String), `threatLevel` (Int 1-3), `timestamp`
- **CustomFilterEntity**: `domain`, `pattern`, `isEnabled`, `isActive` (duplicado intencional para compatibilidad)
- **DnsLogEntity**: `profileId`, `domain`, `blocked`, `timestamp`
- **UserProfileEntity**: `id`, `parentalPin` (String plain text), `scheduleStart/End`, `restrictionLevel`
- **SensitiveAppEntity**: `packageName`, `appName`, `blockedAt` - Apps marcadas por padres
- **StatisticEntity**: `dateKey`, `totalBlocked`, `uniqueDomains`, `adultContentBlocked`, etc.

**Migration**: Usa `.fallbackToDestructiveMigration()` en desarrollo. Para producción, crear `Migration` objects.

### Permission Requirements
1. **VpnService.prepare()**: Mandatory para DnsFilterService. Launcher: `ActivityResultContracts.StartActivityForResult()`.
2. **UsageStats permission**: Mandatory para monitoreo. Check con `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)`. Redirect a `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
3. **Notification permission** (Android 13+): Para notificaciones de bloqueo. Request con `ActivityResultContracts.RequestPermission()`.
4. **Battery optimization**: Excluir app para foreground services persistentes. Intent: `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

### Security
- **PIN Storage**: `SecurityHelper` usa `EncryptedSharedPreferences` + `MasterKey` (AES256_GCM) para almacenar PINs. Fallback a UserProfileEntity.parentalPin (plain text) si falla.
- **PIN Validation**: `SecurityHelper.verifyPin(context, profileId, pin)` hash con SHA-256.
- **UI Protection**: `PinLockScreen` composable con callback `onPinVerified`. Usar `showPinDialog` + `pendingAction` pattern en MainActivity.

## Critical Implementation Details

### VPN Configuration
```kotlin
Builder()
    .addAddress("10.0.0.2", 32)
    .addDnsServer("185.228.168.168")  // CleanBrowsing Adult Filter Primary
    .addDnsServer("185.228.169.168")  // CleanBrowsing Adult Filter Secondary
    .addDisallowedApplication(packageName)  // Evitar bucle infinito VPN
    .setBlocking(false)  // Android 15+ only - non-blocking mode
    .setMtu(1500)
    // NO addRoute() - Dejar tráfico normal fluir, solo interceptar DNS
```
**IMPORTANTE**: CleanBrowsing Adult Filter es el más restrictivo disponible. Bloquea: pornografía, redes sociales, juegos, entretenimiento, proxies/VPNs.

### App Blocking Logic
`UsageStatsMonitor` revisa cada 2s (10s si pantalla apagada) la app en primer plano:
1. Obtiene foreground app con `UsageStatsManager.queryUsageStats()`
2. Verifica contra `browserPackages` (Chrome, Firefox, etc.) y `socialMediaPackages` (Instagram, TikTok, etc.)
3. Si es navegador NO seguro → Lanzar SafeBrowserActivity
4. Si es social media → Verificar horarios en UserProfile, bloquear si fuera de horario
5. Bloqueo: `Intent(context, SafeBrowserActivity::class.java).apply { flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK }`
6. Usar `lastDetectedApp` para evitar logs duplicados

### Domain Blocking
```kotlin
LocalBlocklist.isBlocked(domain, repository)
├─ 1. Bloqueo exacto: socialMediaDomains.contains(domain)
├─ 2. Keywords: "porn", "sex", "casino", "gambling", "porno", "sexo"
└─ 3. Assets file: blocklist_domains.txt (loadFromAssets) - opcional para futuro
```
**Categorías**: `getCategoryFromDomain()` clasifica en: ADULT, GAMBLING, VIOLENCE, SOCIAL_MEDIA. Null si no match.

### SafeBrowserActivity WebView
- WebViewClient con `shouldOverrideUrlLoading()` que valida URL contra `LocalBlocklist.isBlocked()`
- Si bloqueado: muestra diálogo de advertencia, llama `repository.logDnsQuery(domain)`
- Configuración: JavaScript enabled, DOM storage, seguro contra XSS con `setMixedContentMode(MIXED_CONTENT_NEVER_ALLOW)`

## Common Tasks

### Add New Category Block
1. Agregar keywords a `LocalBlocklist.blockedKeywords` list
2. Actualizar `getCategoryFromDomain()` en [GuardianRepository.kt](shield/data/GuardianRepository.kt) líneas ~60-75
3. Si necesitas estadísticas, agregar columna en `StatisticEntity` (ej: `socialMediaBlocked: Int`)
4. Actualizar `updateTodayStatistics()` para contar la nueva categoría

### Add Custom DNS Provider
1. Modificar constantes `DNS_PRIMARY`/`DNS_SECONDARY` en [DnsFilterService.kt](shield/service/DnsFilterService.kt) líneas ~55-57
2. Documentar qué bloquea el proveedor en comentarios del código
3. Opcional: Agregar selector en [SettingsScreen.kt](shield/ui/SettingsScreen.kt) con DataStore persistence

### Prevent Log Spam
- UsageStatsMonitor usa `lastDetectedApp: String?` para evitar registros duplicados de la misma app
- Solo registra cuando cambia la app en primer plano O cuando se produce un bloqueo efectivo
- Apps permitidas dentro de horario NO generan logs (solo bloqueadas realmente)
- DnsFilterService NO registra cada query, solo dominios bloqueados

### Add PIN Protection to UI Actions
```kotlin
// En MainActivity o Screen
var showPinDialog by remember { mutableStateOf(false) }
var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }

// Cuando necesitas proteger una acción:
Button(onClick = {
    pendingAction = { /* acción a proteger */ }
    showPinDialog = true
}) { Text("Desactivar VPN") }

// Mostrar PIN dialog:
if (showPinDialog) {
    PinLockScreen(
        requiredPin = currentProfile?.parentalPin,
        profileId = currentProfile?.id,
        onPinVerified = {
            pendingAction?.invoke()
            showPinDialog = false
        },
        onBack = { showPinDialog = false }
    )
}
```

### Debug Service Crashes
- Revisar Logcat con filtros: `adb logcat | grep -E "Guardian|VPN|Monitor"`
- Tags clave: `"GuardianVPN"`, `"UsageStatsMonitor"`, `"AppMonitorService"`
- Verificar permisos en runtime: `hasUsageStatsPermission()`, `VpnService.prepare()` == null
- **OEM issues críticos**: OPPO/Motorola matan servicios foreground incluso con `START_STICKY`
  - Solución: Agregar app a exclusión de batería con Intent `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  - Verificar: `val pm = context.getSystemService<PowerManager>(); pm?.isIgnoringBatteryOptimizations(packageName)`
- Si VPN no inicia: Verificar que `vpnInterface != null` en logs, comprobar permisos VpnService
- Si monitoreo falla: Verificar UsageStats permission con `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)`

### Update Room Schema
1. Incrementar `version` en `@Database` annotation en [GuardianDatabase.kt](shield/data/GuardianDatabase.kt)
2. Usar `.fallbackToDestructiveMigration()` (ok para desarrollo - borra datos)
3. Para producción: Crear `Migration` objects con migración de schema
4. Agregar nueva Entity al array `entities = [...]` en @Database

## Project-Specific Conventions

### File Organization
- **UI**: Screens terminan en `*Screen.kt`, composables reutilizables en lowercase (ej: [logBlockedSite.kt](shield/ui/logBlockedSite.kt))
- **Services**: Un servicio por archivo, helpers en archivos separados (ej: [extractDomainFromDnsQuery.kt](shield/service/extractDomainFromDnsQuery.kt))
- **Data**: Pattern `*Entity.kt` + `*Dao.kt` por cada tabla. Entidades agrupadas en [GuardianDatabase.kt](shield/data/GuardianDatabase.kt)
- **No packages anidados**: Todos los archivos en subcarpetas directas: `shield/`, `shield/ui/`, `shield/data/`, `shield/service/`, `shield/security/`

### Logging
- Usar Log.d/i/w/e con tags descriptivos
- Emoji indicators: ✅ (success), ❌ (error), ⚠️ (warning), 🚀 (start), 🛑 (stop)
- Ejemplo: `Log.d("GuardianVPN", "✅ DNS SEGURO ACTIVADO")`
- Box drawing para eventos críticos:
```kotlin
Log.d("GuardianVPN", "╔════════════════════════════════════╗")
Log.d("GuardianVPN", "║   DNS SEGURO ACTIVADO              ║")
Log.d("GuardianVPN", "╚════════════════════════════════════╝")
```

### Coroutines
- **Services**: `CoroutineScope(Dispatchers.Default + SupervisorJob())` para operaciones background
- **Repository**: Métodos `suspend fun` para DB operations con `withContext(Dispatchers.IO)`
- **UI**: `lifecycleScope.launch` en MainActivity/Activity, evitar GlobalScope
- **Flows**: DAOs retornan `Flow<>` para observación reactiva, UI los consume con `collectAsState()`

### Error Handling
- **VPN failures**: Broadcast `ACTION_VPN_ERROR` intent, mostrar Toast, llamar `stopSelf()`
- **Permission denials**: Redirigir a Settings con Intent explícito (`ACTION_USAGE_ACCESS_SETTINGS`, `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
- **DB errors**: Try-catch con fallback values - usar `firstOrNull()` en lugar de `first()` para evitar excepciones
- **Service crashes**: Log exhaustivamente con tags específicos para facilitar debugging post-mortem

## Testing & Debugging

### Build & Run
```bash
# Desde el directorio raíz del proyecto Android (donde está gradlew)
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# O directamente desde VS Code
# Terminal: gradle assembleDebug (si está configurado el wrapper)
```

### Build Release (Sistemas con Recursos Limitados)
**ADVERTENCIA**: `assembleRelease` es MUY exigente en recursos (se ha bloqueado al 60% en sistema con 8GB RAM + HDD lento).

**Optimizaciones recomendadas** para sistemas con poca RAM:
```bash
# 1. Limpiar antes de build release
./gradlew clean

# 2. Build release con optimizaciones de memoria
./gradlew assembleRelease \
  --no-daemon \
  --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError"

# 3. Si sigue fallando, deshabilitar temporalmente R8/ProGuard (SOLO para testing)
# En app/build.gradle: minifyEnabled false, shrinkResources false

# 4. Monitorear memoria durante build
watch -n 2 free -h  # En otra terminal
```

**Alternativas más ligeras**:
```bash
# Build debug firmado (más rápido que release, funcional para GitHub)
./gradlew assembleDebug

# Build release sin ofuscación (en gradle: minifyEnabled false)
./gradlew assembleRelease --no-daemon --max-workers=1

# Incrementar swap si tienes HDD lento (Linux)
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

**Síntomas de falta de recursos**:
- Build se detiene al 60-70% (típicamente en tarea `:app:minifyReleaseWithR8`)
- Sistema no responde, necesita reinicio
- Proceso `java` consume >4GB RAM
- HDD al 100% durante minutos

**Solución para GitHub Release**: Usar GitHub Actions para compilar en la nube (ejemplo en `.github/workflows/build.yml`)

### ADB Debugging
```bash
# Ver logs relevantes de GuardianOS Shield
adb logcat | grep -E "Guardian|VPN|Monitor"

# Verificar permisos de UsageStats
adb shell dumpsys usagestats

# Ver restricciones de red/batería
adb shell cmd netpolicy list restrictbackground
adb shell dumpsys battery

# Verificar VPN activa
adb shell ifconfig | grep tun
```

### Common Issues
- **VPN not blocking**: Verificar DNS config en logs, revisar que CleanBrowsing responda
- **Service killed**: Revisar battery optimization whitelist, probar con `START_STICKY`
- **App monitoring fails**: Revisar UsageStats permission con `AppOpsManager`, aumentar delay en OEMs restrictivos
- **VPN no se desconecta**: Asegurar que `cleanup()` llame `stopForeground(true)` y cierre `vpnInterface`
- **Estado no persiste**: Verificar que se llame `settingsRepository.updateVpnActive()` / `updateProtectionMode()` en TODOS los cambios de estado
- **Logs duplicados**: UsageStatsMonitor usa `lastDetectedApp` para evitar procesar la misma app repetidamente
- **Compose recomposition loops**: Verificar uso correcto de `remember`, `derivedStateOf`, evitar modificar state en composables sin LaunchedEffect

## External Dependencies
- **CleanBrowsing DNS**: 185.228.168.168 / 185.228.169.168 (Adult Filter Primary/Secondary) - Bloquea contenido adulto, redes sociales, juegos
- **Room**: Version 4, local persistence con `.fallbackToDestructiveMigration()` en desarrollo
- **DataStore**: Preferences para settings persistence (`SettingsRepository`)
- **EncryptedSharedPreferences**: Para almacenamiento seguro de PINs (AES256_GCM via AndroidKeyStore)
- **No external APIs**: Todo el filtrado es 100% local, sin conexión a servidores remotos

## Key Files Reference
- **[MainActivity.kt](shield/MainActivity.kt)**: Activity principal, 1225 líneas - hub central de navegación y estado
- **[DnsFilterService.kt](shield/service/DnsFilterService.kt)**: VpnService de 212 líneas - filtrado DNS transparente
- **[UsageStatsMonitor.kt](shield/service/UsageStatsMonitor.kt)**: 421 líneas - monitoreo y bloqueo de apps en tiempo real
- **[GuardianRepository.kt](shield/data/GuardianRepository.kt)**: 332 líneas - acceso centralizado a datos Room
- **[LocalBlocklist.kt](shield/service/LocalBlocklist.kt)**: Listas de dominios bloqueados + lógica de verificación
- **[SafeBrowserActivity.kt](shield/ui/SafeBrowserActivity.kt)**: 553 líneas - navegador seguro con WebView filtrado
- **[SecurityHelper.kt](shield/security/SecurityHelper.kt)**: 130 líneas - encriptación de PINs y validación
- **[GuardianDatabase.kt](shield/data/GuardianDatabase.kt)**: Configuración Room con 6 entidades (v4)
