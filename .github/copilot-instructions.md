# GitHub Copilot / Instrucciones para Guardianos Shield (ES)

Propósito: guía corta y enfocada para que un agente de IA sea inmediatamente productivo en este proyecto Android de control parental y filtrado DNS.

## Resumen rápido (visión general) ✅
- Aplicación Android (Jetpack Compose) que ofrece control parental mediante un filtro DNS basado en VPN y monitoreo ligero de apps.
- Directorios principales:
  - `service/` — servicios en segundo plano (DNS VpnService, monitorización, workers)
  - `data/` — entidades Room/DAOs, `GuardianRepository`, DataStore (`SettingsRepository`) y Flows
  - `ui/` — pantallas Compose y `SafeBrowserActivity`
- En tiempo de ejecución: `DnsFilterService` intercepta paquetes DNS UDP, decide bloqueo con `LocalBlocklist` y reglas en `GuardianRepository`, registra en Room y puede redirigir al navegador seguro dentro de la app.

## Puntos de integración clave 🔧
- `DnsFilterService`:
  - Intercepta IPv4 UDP puerto 53 y analiza consultas DNS manualmente.
  - Llama `protect(socket)` antes de hacer consultas salientes.
  - Emite broadcasts: `ACTION_VPN_STARTED`, `ACTION_VPN_STOPPED`, `ACTION_VPN_ERROR`.
- `GuardianRepository` centraliza acceso a datos y expone Flows (por ejemplo `blacklist`, `whitelist`).
- `UsageStatsMonitor` detecta navegadores y lanza `SafeBrowserActivity` para bloquear/redirigir.
- `LogCleanupWorker` hace limpieza periódica de logs (no hay scheduling automático actualmente en el repo).

## Convenciones y puntos importantes ⚠️
- Categorías como `"ADULT"` y niveles de amenaza son `String`/`Int` (no cambiar sin migración).
- Comentarios y UI suelen estar en español — mantén coherencia lingüística en textos de la UI.
- Room está en `version = 3` con `fallbackToDestructiveMigration()` activado — los cambios de esquema borrarán datos a menos que añadas migraciones.
- Permisos críticos:
  - VPN (`VpnService.prepare(...)`) — prueba en dispositivo o emulador con soporte VPN.
  - Uso (`UsageStats`) — debe concederse por el usuario para monitorización.
  - Notificaciones en Android 13+ (TIRAMISU).

## Dónde mirar primero 🔎
- `service/DnsFilterService.kt` — manejo del VPN y parsing de paquetes (`isDnsPacket`, `extractDomainFromDnsQuery`, `createIpUdpPacket`).
- `data/GuardianRepository.kt` — reglas y persistencia (`logDnsQuery`, `addBlockedSite`, flows observables).
- `service/LocalBlocklist.kt` — keywords locales y helper para cargar listas de assets (`blocklist_domains.txt`).
- `service/UsageStatsMonitor.kt`` y `service/AppMonitorService.kt` — monitorización y redirección al Safe Browser.
- `ui/SafeBrowserActivity.kt` — cómo se muestran páginas bloqueadas y comprobación de filtros personalizados.

## Depuración y pruebas 🐞
- Logs útiles: `GuardianVPN`, `UsageStatsMonitor`, `SafeBrowser`. `adb logcat | grep GuardianVPN` es práctico para investigar DNS/VPN.
- `DnsFilterService` requiere dispositivo/emulador con soporte de VpnService; unit-testea funciones puras como `extractDomainFromDnsQuery`, `isDnsPacket`, `isWithinAllowedTime`.
- Al cambiar código de paquetes DNS/IP, valida con paquetes capturados en dispositivo y mantén las comprobaciones de longitud de cabeceras para evitar crashes.

## Ejemplos accionables para tareas comunes 💡
- Nueva regla de bloqueo por sufijo: añadir lógica en `LocalBlocklist.isBlocked()` o un helper suspend y llamarlo desde `DnsFilterService.shouldBlockDomain()` y `SafeBrowserActivity.isDomainBlocked()`.
- Nuevo helper en repo: `suspend fun logBlockedAttempt(domain:String, source:String)` y usarlo donde sea necesario (UsageStats/DnsFilter).
- Migración de DB: aumentar `version` en `GuardianDatabase` y proveer `Migration` para preservar datos (evitar `fallbackToDestructiveMigration()` si quieres mantener datos).

## Checklist rápido para desarrolladores ✅
1. Compilar & ejecutar: usar Android Studio o `./gradlew assembleDebug` y ejecutar en emulador/dispositivo con soporte VPN.
2. Logs: `adb logcat | grep GuardianVPN` para ver DNS/VPN logs y `| grep UsageStatsMonitor` para monitorización.
3. Pruebas unitarias: crear tests para funciones puras (`extractDomainFromDnsQuery`, `isDnsPacket`, `isWithinAllowedTime`). Mantén tests separados de código que necesita permisos/servicios nativos.
4. Cambios de esquema: si modificas entidades Room, añade `Migration` y actualiza `version` en `GuardianDatabase`.
5. PRs: describir pasos reproducibles (emulador/device, permisos necesarios) y notas de integración si tocas `DnsFilterService` o `UsageStatsMonitor`.

---

Si quieres, también puedo añadir ejemplos de tests (Kotlin/JUnit) para `extractDomainFromDnsQuery` y `isDnsPacket`, o traducir al español otros documentos del repo. ¿Qué prefieres? Gracias. 👋
A continuación está una versión en español con las mismas ideas clave y un checklist rápido para desarrolladores.

### Resumen rápido (visión general) ✅
- Aplicación Android (Jetpack Compose) que ofrece control parental mediante un filtro DNS basado en VPN y monitoreo ligero de apps.
- Directorios principales:
  - `service/` — servicios en segundo plano (DNS VpnService, monitorización, workers)
  - `data/` — entidades Room/DAOs, `GuardianRepository`, DataStore (`SettingsRepository`) y Flows
  - `ui/` — pantallas Compose y `SafeBrowserActivity`
- En tiempo de ejecución: `DnsFilterService` intercepta paquetes DNS UDP, decide bloqueo con `LocalBlocklist` y reglas en `GuardianRepository`, registra en Room y puede redirigir al navegador seguro dentro de la app.

### Puntos de integración clave 🔧
- `DnsFilterService`:
  - Intercepta IPv4 UDP puerto 53 y analiza consultas DNS manualmente.
  - Llama `protect(socket)` antes de hacer consultas salientes.
  - Emite broadcasts: `ACTION_VPN_STARTED`, `ACTION_VPN_STOPPED`, `ACTION_VPN_ERROR`.
- `GuardianRepository` centraliza acceso a datos y expone Flows (por ejemplo `blacklist`, `whitelist`).
- `UsageStatsMonitor` detecta navegadores y lanza `SafeBrowserActivity` para bloquear/redirigir.
- `LogCleanupWorker` hace limpieza periódica de logs (no hay scheduling automático actualmente en el repo).

### Convenciones y puntos importantes ⚠️
- Categorías como `"ADULT"` y niveles de amenaza son `String`/`Int` (no cambiar sin migración).
- Comentarios y UI often en español — mantén coherencia lingüística en textos de la UI.
- Room está en `version = 3` con `fallbackToDestructiveMigration()` activado — los cambios de esquema borrarán datos a menos que añadas migraciones.
- Permisos críticos:
  - VPN (`VpnService.prepare(...)`) — prueba en dispositivo o emulador con soporte VPN.
  - Uso (`UsageStats`) — debe concederse por el usuario para monitorización.
  - Notificaciones en Android 13+ (TIRAMISU).

### Dónde mirar primero 🔎
- `service/DnsFilterService.kt` — manejo del VPN y parsing de paquetes.
- `data/GuardianRepository.kt` — reglas y persistencia.
- `service/LocalBlocklist.kt` — keywords y helpers para cargar listas grandes.
- `ui/SafeBrowserActivity.kt` — control de navegación y reglas de bloqueo de la UI.

### Checklist rápido para desarrolladores ✅
1. Compilar & ejecutar: usar Android Studio o línea de comandos `./gradlew assembleDebug` y ejecutar en un emulador/dispositivo con soporte VPN.
2. Logs: `adb logcat | grep GuardianVPN` para ver DNS/VPN logs, `| grep UsageStatsMonitor` para monitor.
3. Pruebas unitarias: crear tests para funciones puras (`extractDomainFromDnsQuery`, `isDnsPacket`, `isWithinAllowedTime`). Mantén tests separados de código que necesita permisos/servicios nativos.
4. Cambios de esquema: si modificas entidades Room, añade `Migration` y actualiza `version` en `GuardianDatabase` (evita `fallbackToDestructiveMigration()` si quieres preservar datos).
5. PRs: describir pasos reproducibles (emulador/device, permisos necesarios), y añadir notas de integración si tocas `DnsFilterService` o `UsageStatsMonitor`.

---

Si quieres, traduzco esta versión completa al español como el único contenido (sin la sección en inglés) o añado ejemplos de tests (Kotlin/JUnit) para `extractDomainFromDnsQuery` y `isDnsPacket`. ¿Qué prefieres? Gracias. 👋
