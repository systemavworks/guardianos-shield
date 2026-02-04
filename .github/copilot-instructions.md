
# Instrucciones para agentes IA — Guardianos Shield

Guía concisa para que agentes de IA sean productivos en este proyecto Android de control parental y filtrado DNS.

## Arquitectura y componentes principales
- **App Android Jetpack Compose**: control parental mediante filtro DNS (VPN) y monitorización de apps.
- **service/**: servicios en segundo plano (DNS VpnService, monitorización, workers). Ejemplo: `DnsFilterService.kt` intercepta UDP:53, parsea DNS, decide bloqueo y registra en Room.
- **data/**: entidades Room/DAOs, `GuardianRepository` (reglas, persistencia, Flows), DataStore (`SettingsRepository`).
- **ui/**: pantallas Compose y `SafeBrowserActivity` (navegador seguro/redirección).

## Flujos críticos y patrones
- **Filtro DNS**: `DnsFilterService` analiza paquetes, usa `LocalBlocklist` y reglas de `GuardianRepository`, registra logs y puede redirigir a `SafeBrowserActivity`.
- **Monitorización apps**: `UsageStatsMonitor` y `AppMonitorService` detectan apps sensibles y fuerzan navegación segura.
- **Persistencia**: Room v3 (`fallbackToDestructiveMigration()` activado, ¡cuidado con migraciones!).
- **Permisos clave**: VPN (`VpnService.prepare`), UsageStats, notificaciones (Android 13+).

## Convenciones y decisiones relevantes
- **Categorías y amenazas**: `String`/`Int` (ej: "ADULT"), no cambiar sin migración.
- **Idioma**: comentarios y UI en español.
- **Logs útiles**: `GuardianVPN`, `UsageStatsMonitor`, `SafeBrowser` (`adb logcat | grep GuardianVPN`).
- **No hay scheduling automático** para workers (ej: `LogCleanupWorker`).

## Ejemplos y puntos de entrada
- **Bloqueo por sufijo**: añade lógica en `LocalBlocklist.isBlocked()` o helper suspend, llama desde `DnsFilterService.shouldBlockDomain()` y `SafeBrowserActivity.isDomainBlocked()`.
- **Nuevo log de intento bloqueado**: crea `suspend fun logBlockedAttempt(domain: String, source: String)` en repo y úsalo donde corresponda.
- **Migraciones Room**: sube `version` en `GuardianDatabase` y añade `Migration` para evitar pérdida de datos.

## Workflows de desarrollo
1. **Compilar y ejecutar**: Android Studio o `./gradlew assembleDebug` (emulador/dispositivo con soporte VPN).
2. **Logs**: `adb logcat | grep GuardianVPN` y/o `| grep UsageStatsMonitor`.
3. **Pruebas unitarias**: testea funciones puras (`extractDomainFromDnsQuery`, `isDnsPacket`, `isWithinAllowedTime`). Mantén tests separados de código que requiere permisos/servicios nativos.
4. **Cambios de esquema**: añade `Migration` y sube `version` en `GuardianDatabase` (evita `fallbackToDestructiveMigration()` si quieres preservar datos).
5. **Pull Requests**: describe pasos reproducibles (emulador/dispositivo, permisos necesarios) y notas de integración si modificas `DnsFilterService` o `UsageStatsMonitor`.

## Archivos clave para IA
- `service/DnsFilterService.kt`: lógica VPN/DNS, parsing, bloqueo, logs.
- `data/GuardianRepository.kt`: reglas, persistencia, Flows.
- `service/LocalBlocklist.kt`: keywords, helpers para listas grandes.
- `ui/SafeBrowserActivity.kt`: control de navegación y reglas de bloqueo UI.
- `service/UsageStatsMonitor.kt`, `service/AppMonitorService.kt`: monitorización y redirección.

---
¿Falta algún flujo, convención o integración importante? Indícalo para mejorar estas instrucciones.
