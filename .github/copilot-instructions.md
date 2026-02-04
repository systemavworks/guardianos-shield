
# Instrucciones para agentes IA — Guardianos Shield

Guía para agentes de IA trabajando en esta app Android de control parental con filtrado DNS y monitorización de apps.

## Arquitectura y flujo de datos

**Estrategia VPN DNS transparente** (NO interceptación de paquetes):
- `DnsFilterService` configura VPN con DNS seguros CleanBrowsing Adult Filter (185.228.168.168)
- NO usa `addRoute("0.0.0.0", 0)` — solo redirige DNS, el tráfico va directo
- CleanBrowsing filtra en sus servidores (adult, social media, juegos, malware)
- `LocalBlocklist` y `GuardianRepository` añaden capa adicional de bloqueo local

**Flujo de bloqueo dual**:
1. **Nivel DNS**: CleanBrowsing rechaza dominios problemáticos (servidores externos)
2. **Nivel App**: `LocalBlocklist.isBlocked()` verifica keywords/dominios hardcoded antes de WebView load
3. `SafeBrowserActivity.shouldOverrideUrlLoading()` valida URL → llama `LocalBlocklist.isBlocked()` → muestra página de bloqueo
4. `UsageStatsMonitor` detecta apps sensibles (navegadores externos) → redirige a `SafeBrowserActivity`

**Monitorización de apps**:
- `AppMonitorService` (foreground persistent) + `UsageStatsMonitor` detectan app en foreground cada 2s
- Si es navegador externo → `SafeBrowserActivity.createIntent()` con flag `NEW_TASK`
- Si es app sensible (configurada en `SensitiveAppEntity`) → redirección forzada
- Requiere permiso `PACKAGE_USAGE_STATS` (Settings.ACTION_USAGE_ACCESS_SETTINGS)

**Persistencia Room v4**:
- `GuardianDatabase` usa `.fallbackToDestructiveMigration()` — **pérdida de datos en cambios de esquema**
- Si preservar datos: incrementa `version`, añade `Migration(X, X+1)` con `ALTER TABLE`
- Entities: `BlockedSiteEntity`, `DnsLogEntity`, `UserProfileEntity`, `CustomFilterEntity`, `SensitiveAppEntity`
- `GuardianRepository`: acceso centralizado, Flows reactivos (`recentBlocked`, `todayBlockedCount`)

## Convenciones críticas del proyecto

**Categorías de bloqueo** (hardcoded strings, no cambiar sin migración):
```kotlin
"ADULT", "GAMBLING", "VIOLENCE", "SOCIAL_MEDIA" // en getCategoryFromDomain()
```

**Control de horarios** (`UserProfileEntity.isWithinAllowedTime()`):
- Verifica `scheduleEnabled` y compara hora actual con `allowedStartHour/allowedEndHour`
- Soporta cruce de medianoche (ej: 22:00 - 08:00)
- Se chequea en `SafeBrowserActivity` antes de cargar URL
- Icono ⏰ en página de bloqueo si es por horario

**Tags de logs para debugging**:
```bash
adb logcat | grep -E "GuardianVPN|UsageStatsMonitor|SafeBrowser"
```
- `GuardianVPN`: inicio/parada VPN, configuración DNS
- `UsageStatsMonitor`: detección apps, redirecciones
- `SafeBrowser`: bloqueos WebView, validaciones URL

**Idioma**: Comentarios, UI y logs en **español** (proyecto desarrollado en Sevilla, España)

## Patrones de implementación comunes

**Añadir nuevo dominio bloqueado**:
1. Si keyword simple → añadir a `LocalBlocklist.blockedKeywords`
2. Si dominio exacto → añadir a `LocalBlocklist.socialMediaDomains`
3. Si lista grande → crear archivo `assets/blocklist_domains.txt` → usar `LocalBlocklist.loadFromAssets()`

**Registrar bloqueo**:
```kotlin
// En GuardianRepository
suspend fun logDnsQuery(domain: String) {
    val isBlocked = LocalBlocklist.isBlocked(domain, this)
    if (isBlocked) addBlockedSite(domain, category, threatLevel)
    dnsLogDao.insert(DnsLogEntity(profileId, domain, blocked = isBlocked))
}
```

**Nueva función de tiempo permitido** (como `isWithinAllowedTime()`):
```kotlin
// UserProfileEntity.kt extension function
fun UserProfileEntity.isCustomTimeValid(customStart: Int, customEnd: Int): Boolean {
    if (!scheduleEnabled) return true
    val now = Calendar.getInstance()
    val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    // Lógica similar a isWithinAllowedTime(), maneja cruce medianoche
}
```

## Workflows de desarrollo

**Build y ejecución**:
```bash
./gradlew assembleDebug                    # Genera APK en app/build/outputs/apk/debug/
./gradlew installDebug                     # Instala en dispositivo conectado
adb logcat -c && adb logcat | grep Guardian  # Logs en tiempo real
```
- **Emulador**: usar AVD con API 24+ y Google Play (para VPN support)
- **Dispositivo físico**: habilitar permisos VPN, UsageStats, notificaciones (Android 13+), deshabilitar battery optimization

**Testing unitario** (patrones actuales):
- Testear funciones puras: `extractDomainFromDnsQuery()`, `isDnsPacket()`, `isWithinAllowedTime()`
- NO testear VpnService, UsageStatsManager (requieren permisos runtime)
- Mock `GuardianRepository` para tests de UI/ViewModels

**Cambios de esquema DB**:
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

**Debugging VPN issues**:
1. Verificar permiso: `VpnService.prepare(context)` retorna null si concedido
2. Logs DNS: `adb logcat | grep "Filtro DNS Activo"` debe mostrar IPs CleanBrowsing
3. Test conectividad: `adb shell dumpsys connectivity | grep "DNS servers"` → debe mostrar 185.228.168.168
4. Si internet no funciona: verificar que NO hay `addRoute("0.0.0.0", 0)` en `DnsFilterService.setupVpn()`

## Archivos clave de referencia

**Servicios core**:
- [service/DnsFilterService.kt](app/src/main/kotlin/com/guardianos/shield/service/DnsFilterService.kt) — VPN setup, configuración DNS, broadcasts
- [service/LocalBlocklist.kt](app/src/main/kotlin/com/guardianos/shield/service/LocalBlocklist.kt) — Keywords, dominios hardcoded, helpers assets
- [service/UsageStatsMonitor.kt](app/src/main/kotlin/com/guardianos/shield/service/UsageStatsMonitor.kt) — Detección apps foreground, lógica redirección
- [service/AppMonitorService.kt](app/src/main/kotlin/com/guardianos/shield/service/AppMonitorService.kt) — Foreground service persistente

**Datos y persistencia**:
- [data/GuardianDatabase.kt](app/src/main/kotlin/com/guardianos/shield/data/GuardianDatabase.kt) — Room config, entities, migraciones
- [data/GuardianRepository.kt](app/src/main/kotlin/com/guardianos/shield/data/GuardianRepository.kt) — DAO wrappers, `logDnsQuery()`, Flows

**UI y navegación**:
- [ui/SafeBrowserActivity.kt](app/src/main/kotlin/com/guardianos/shield/ui/SafeBrowserActivity.kt) — WebView, `shouldOverrideUrlLoading()`, página de bloqueo
- [MainActivity.kt](app/src/main/kotlin/com/guardianos/shield/MainActivity.kt) — Compose navigation, permisos, inicio servicios

**Gradle y dependencias**:
- [app/build.gradle.kts](app/build.gradle.kts) — minSdk=24, targetSdk=34, Room KSP, Compose BOM

---
**Nota**: Esta app NO usa interceptación de paquetes (no procesa tráfico raw). Si ves referencias a `extractDomainFromDnsQuery()` o packet parsing, son restos de prototipos anteriores — la arquitectura actual delega filtrado a CleanBrowsing DNS.
