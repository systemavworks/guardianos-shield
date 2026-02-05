# Instrucciones para agentes IA — Guardianos Shield

Guía para agentes de IA trabajando en esta app Android de control parental con filtrado DNS y monitorización de apps.

## 📋 Datos básicos del proyecto

- **Lenguaje**: Kotlin (Android)
- **Min SDK**: 24 (Android 7.0), **Target SDK**: 34 (Android 14)
- **Build system**: Gradle Kotlin DSL — usa `./gradlew` (ver [app/build.gradle.kts](app/build.gradle.kts))
- **Base de datos**: Room v4 con KSP compiler (no kapt)
- **Preferencias**: DataStore (asíncrono)
- **UI**: Jetpack Compose + Material 3 + WebView ([ui/SafeBrowserActivity.kt](app/src/main/kotlin/com/guardianos/shield/ui/SafeBrowserActivity.kt))
- **Idioma**: Comentarios, UI y logs en **español** (proyecto desarrollado en Sevilla, España)

## 🏗️ Arquitectura y flujo de datos

### **Estrategia VPN DNS transparente** (NO procesamiento de paquetes):

`DnsFilterService` configura VPN con **CleanBrowsing Adult Filter** (185.228.168.168/185.228.169.168):
- **NO** usa `addRoute("0.0.0.0", 0)` — solo configura servidores DNS, el tráfico HTTP/HTTPS fluye directamente
- CleanBrowsing filtra en **sus servidores** (contenido adulto, redes sociales, juegos, malware, entretenimiento)
- `LocalBlocklist` + `GuardianRepository` añaden capa adicional de bloqueo local en WebView

**Importante**: Esta app **NO intercepta paquetes raw** ni procesa tráfico. Si ves referencias a `extractDomainFromDnsQuery()`, `isDnsPacket()`, o packet parsing, son restos de prototipos anteriores que no se usan en producción.

### **Flujo de bloqueo dual**:

1. **Nivel DNS** (CleanBrowsing): Servidores DNS externos rechazan dominios problemáticos antes de resolver IPs
2. **Nivel App/WebView** (Local): 
   - `LocalBlocklist.isBlocked(domain)` verifica keywords/dominios hardcoded antes de cargar URL
   - `SafeBrowserActivity.shouldOverrideUrlLoading()` valida URL → bloqueo local → muestra página de error
3. **Nivel Monitorización**: 
   - `AppMonitorService` + `UsageStatsMonitor` detectan apps en foreground cada 2s
   - Si navegador externo o app sensible (`SensitiveAppEntity`) → redirección forzada a `SafeBrowserActivity`

### **Monitorización de apps**:

- `AppMonitorService`: Foreground service persistente (tipo `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`)
- `UsageStatsMonitor`: Detecta app activa mediante `UsageStatsManager` cada 2s
- Redirección: `SafeBrowserActivity.createIntent()` con flag `FLAG_ACTIVITY_NEW_TASK`
- **Permiso crítico**: `PACKAGE_USAGE_STATS` (solicitar vía `Settings.ACTION_USAGE_ACCESS_SETTINGS`)

### **Persistencia Room v4**:

- `GuardianDatabase`: **version = 4**, usa `.fallbackToDestructiveMigration()` — **pérdida de datos en cambios de esquema sin migración**
- **Entities**: `BlockedSiteEntity`, `DnsLogEntity`, `UserProfileEntity`, `CustomFilterEntity`, `SensitiveAppEntity`, `StatisticEntity`
- `GuardianRepository`: **Acceso centralizado único** a DAOs, expone Flows reactivos (`recentBlocked`, `todayBlockedCount`)
- **REGLA CRÍTICA**: Siempre usar `GuardianRepository` desde UI/servicios, **nunca** DAOs directamente

## 🧭 Convenciones y patrones del proyecto

### Categorías de bloqueo (hardcoded strings):

```kotlin
"ADULT", "GAMBLING", "VIOLENCE", "SOCIAL_MEDIA" // NO cambiar sin migración DB
```

### Control de horarios:

`UserProfileEntity.isWithinAllowedTime()`:
- Verifica `scheduleEnabled` + compara hora actual vs `allowedStartHour/allowedEndHour`
- **Soporta cruce de medianoche** (ej: 22:00 - 08:00)
- Se chequea en `SafeBrowserActivity` antes de cargar URL
- Icono ⏰ en página de bloqueo si bloqueado por horario

### Compilador Room:

- **Usa KSP** (no kapt): `ksp(libs.room.compiler)` en [app/build.gradle.kts](app/build.gradle.kts)
- Nunca cambiar a kapt sin verificar compatibilidad

### Threading y coroutines:

- Heavy I/O **siempre** en `Dispatchers.IO`, nunca en Main thread
- Repository methods son `suspend` o retornan `Flow`
- `ScheduleManager.kt` contiene sync wrappers para servicios (read antes de añadir helpers)

### Tags de logs para debugging:

```bash
adb logcat | grep -E "GuardianVPN|UsageStatsMonitor|SafeBrowser"
```
- `GuardianVPN`: inicio/parada VPN, configuración DNS (185.228.168.168/169)
- `UsageStatsMonitor`: detección apps foreground, redirecciones
- `SafeBrowser`: bloqueos WebView, validaciones URL
- `DnsFilterService`: (legacy) packet parsing — **no se usa en producción**

## 🔧 Patrones de implementación comunes

### Añadir nuevo dominio bloqueado:

1. **Keyword simple** → añadir a `LocalBlocklist.blockedKeywords`
2. **Dominio exacto** → añadir a `LocalBlocklist.socialMediaDomains`
3. **Lista grande** → crear archivo `assets/blocklist_domains.txt` → usar `LocalBlocklist.loadFromAssets()`

Ejemplo:
```kotlin
// LocalBlocklist.kt
private val socialMediaDomains = setOf(
    "example.com", "www.example.com", // ← Añadir aquí
    "facebook.com", "www.facebook.com"
)
```

### Registrar bloqueo:

```kotlin
// En GuardianRepository
suspend fun logDnsQuery(domain: String) {
    val isBlocked = LocalBlocklist.isBlocked(domain, this)
    if (isBlocked) {
        val category = getCategoryFromDomain(domain)
        addBlockedSite(domain, category, threatLevel = "MEDIUM")
    }
    dnsLogDao.insert(DnsLogEntity(profileId, domain, blocked = isBlocked))
}
```

### Extension function de validación de tiempo:

```kotlin
// UserProfileEntity.kt
fun UserProfileEntity.isCustomTimeValid(customStart: Int, customEnd: Int): Boolean {
    if (!scheduleEnabled) return true
    val now = Calendar.getInstance()
    val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    
    return if (customEnd < customStart) { // Cruce medianoche
        current >= customStart || current < customEnd
    } else {
        current in customStart until customEnd
    }
}
```

## ⚙️ Workflows de desarrollo

### Build y ejecución:

```bash
./gradlew assembleDebug                    # Genera APK en app/build/outputs/apk/debug/
./gradlew installDebug                     # Instala en dispositivo conectado
adb logcat -c && adb logcat | grep Guardian  # Logs en tiempo real
```

**Entornos de testing**:
- **Emulador**: usar AVD con API 24+ y Google Play (para VPN support)
- **Dispositivo físico**: habilitar permisos VPN, UsageStats, notificaciones (Android 13+), deshabilitar battery optimization

### Testing unitario (patrones actuales):

- **Testear funciones puras**: `extractDomainFromDnsQuery()`, `isDnsPacket()`, `isWithinAllowedTime()`
- **NO testear**: `VpnService`, `UsageStatsManager` (requieren permisos runtime)
- **DB tests**: usar `Room.inMemoryDatabaseBuilder()` con real `GuardianRepository`
- **Mock repository** para tests de UI/ViewModels
- **Packet fixtures**: para tests deterministas de parsing (no usados en producción actual)

### Cambios de esquema DB:

```kotlin
// GuardianDatabase.kt
@Database(entities = [...], version = 5) // ← Incrementar
abstract class GuardianDatabase : RoomDatabase() {
    companion object {
        fun getDatabase(context: Context): GuardianDatabase {
            return Room.databaseBuilder(...)
                .addMigrations(MIGRATION_4_5) // ← Añadir
                .fallbackToDestructiveMigration() // Solo si no hay migración
                .build()
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN new_field INTEGER DEFAULT 0")
            }
        }
    }
}
```

### Debugging VPN issues:

1. **Verificar permiso**: `VpnService.prepare(context)` retorna `null` si concedido
2. **Logs DNS**: `adb logcat | grep "Filtro DNS Activo"` debe mostrar IPs CleanBrowsing
3. **Test conectividad**: `adb shell dumpsys connectivity | grep "DNS servers"` → debe mostrar 185.228.168.168
4. **Si internet no funciona**: verificar que NO hay `addRoute("0.0.0.0", 0)` en `DnsFilterService.setupVpn()`

## 📁 Archivos clave de referencia

### Servicios core:

- [service/DnsFilterService.kt](app/src/main/kotlin/com/guardianos/shield/service/DnsFilterService.kt) — VPN setup, configuración DNS CleanBrowsing, broadcasts
- [service/LocalBlocklist.kt](app/src/main/kotlin/com/guardianos/shield/service/LocalBlocklist.kt) — Keywords bloqueadas, dominios hardcoded, helpers assets
- [service/UsageStatsMonitor.kt](app/src/main/kotlin/com/guardianos/shield/service/UsageStatsMonitor.kt) — Detección apps foreground, lógica redirección
- [service/AppMonitorService.kt](app/src/main/kotlin/com/guardianos/shield/service/AppMonitorService.kt) — Foreground service persistente con notificación

### Datos y persistencia:

- [data/GuardianDatabase.kt](app/src/main/kotlin/com/guardianos/shield/data/GuardianDatabase.kt) — Room config v4, entities, migraciones
- [data/GuardianRepository.kt](app/src/main/kotlin/com/guardianos/shield/data/GuardianRepository.kt) — API única DAOs, `logDnsQuery()`, Flows reactivos
- [data/SettingsDataStore.kt](app/src/main/kotlin/com/guardianos/shield/data/SettingsDataStore.kt) — DataStore patterns, configuración asíncrona

### UI y navegación:

- [ui/SafeBrowserActivity.kt](app/src/main/kotlin/com/guardianos/shield/ui/SafeBrowserActivity.kt) — WebView, `shouldOverrideUrlLoading()`, página de bloqueo con icono ⏰
- [MainActivity.kt](app/src/main/kotlin/com/guardianos/shield/MainActivity.kt) — Compose navigation, solicitud permisos, inicio servicios

### Build y configuración:

- [app/build.gradle.kts](app/build.gradle.kts) — minSdk=24, targetSdk=34, Room KSP, Compose BOM, resConfigs español/inglés
- [ANDROID_COMPATIBILITY.md](ANDROID_COMPATIBILITY.md) — Matriz compatibilidad Android 12-15+, permisos por versión

### Documentación técnica:

- [README.md](README.md) — Arquitectura completa, características, requisitos sistema
- [FIXES_TESTING.md](FIXES_TESTING.md) — Issues conocidos, pruebas realizadas, compatibilidad dispositivos

## 🔗 Integration points y dependencias externas

### CleanBrowsing DNS:

- **Primary DNS**: 185.228.168.168 (Adult Filter)
- **Secondary DNS**: 185.228.169.168 (Adult Filter)
- **Bloqueo automático**: pornografía, redes sociales (TikTok, Facebook, Instagram, Discord, Twitter), juegos, entretenimiento, malware, phishing

### Android Permissions críticos:

- **VpnService**: Permiso usuario via `VpnService.prepare()` — muestra diálogo sistema
- **PACKAGE_USAGE_STATS**: Permiso especial via `Settings.ACTION_USAGE_ACCESS_SETTINGS`
- **POST_NOTIFICATIONS**: Android 13+ (API 33) — solicitar explícitamente
- **FOREGROUND_SERVICE**: Required para `AppMonitorService`
- **FOREGROUND_SERVICE_SPECIAL_USE**: Android 14+ (API 34) — tipo específico de foreground service

### Assets y recursos:

- `assets/blocklist_domains.txt`: Lista extendida de dominios (carga opcional via `LocalBlocklist.loadFromAssets()`)
- Localización: `res/values-es/strings.xml` (español por defecto)

### Flags experimentales (NO usar en producción):

- `EXPERIMENTAL_TUN2SOCKS_ENABLED`: Lanzar binario `tun2socks` externo (issues dispositivos OPPO A80)
- `EXPERIMENTAL_PASSTHROUGH_ENABLED`: Reenviar UDP no-DNS (útil para QUIC/DoH testing)

### Issues conocidos por dispositivo:

- **OPPO A80**: Sistema destruye sockets UIDs externos con VPN activa (`destroyAllSocketForUid`, `blockedReasons=40`)
- **Diagnóstico**: `adb logcat | grep -E "netd|ConnectivityService|GuardianVPN"`

---

**IMPORTANTE**: Esta app **NO procesa paquetes raw**. Referencias a `extractDomainFromDnsQuery()`, `isDnsPacket()`, packet parsing, checksums, NXDOMAIN forging son **código legacy de prototipos** que no se ejecutan en producción. La arquitectura actual delega 100% del filtrado a CleanBrowsing DNS.
