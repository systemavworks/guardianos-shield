# Copilot instructions for Guardianos Shield 🚦

**Purpose:** Help AI coding agents get productive quickly with concise, actionable pointers about architecture, patterns, and workflows for this repository.

## Quick facts ✅
- Language: Kotlin (Android app)
- Min SDK: 24, Target SDK: 34
- Build system: Gradle (Kotlin DSL) — use `./gradlew` (see `app/build.gradle.kts`)
- DB: Room (KSP compiler). DB version = 3, `fallbackToDestructiveMigration()` is enabled — be cautious when changing schemas.
- Preferences: DataStore (preferences)
- UI: Jetpack Compose + WebView (`ui/SafeBrowserActivity.kt`)
- Comments & UI strings: mostly **Spanish** — preserve style and phrasing.

## Big-picture architecture 📂
- VPN/DNS filtering: `service/DnsFilterService.kt` (extends `VpnService`). Reads TUN packets, detects DNS queries (`isDnsPacket()` + `extractDomainFromDnsQuery()`), decides block/allow (`shouldBlockDomain()`), and either forges NXDOMAIN responses or forwards queries upstream. Includes IPv4/IPv6 and UDP checksum logic.
- Local block rules: `service/LocalBlocklist.kt` + `app/src/main/assets/blocklist_domains.txt` (keyword based - fast and simple checks).
- Persistence & domain logic: `data/GuardianRepository.kt` is the single API for DB and filtering helpers. DB entities: `BlockedSite`, `Statistic`, `UserProfile`, `CustomFilter`, `DnsLog` (see `data/`).
- UI & blocking UX: `ui/SafeBrowserActivity.kt` (WebView used to show blocked pages). `ui/*Screen.kt` files hold settings and statistics screens.
- Monitoring & app-blocking: `service/UsageStatsMonitor.kt`, `service/AppMonitorService.kt`, and `service/RealisticAppBlocker.kt` monitor foreground apps and log or launch `SafeBrowserActivity` for blocked apps.
- Experimental: `DnsFilterService` can attempt to start an external `tun2socks` binary (`EXPERIMENTAL_TUN2SOCKS_ENABLED` flag). Treat as optional and experimental.

## Project-specific conventions & patterns 🧭
- Use KSP (not kapt) for Room — check `ksp(...)` in `app/build.gradle.kts`.
- Always use `GuardianRepository` from UI/services for DB operations. **Do not** call DAOs directly from services/UI.
- Keep heavy I/O on `Dispatchers.IO` and off the Main thread; repository methods often suspend or return `Flow`.
- Use repository helpers for filter logic (e.g., `isInBlacklist()`, `getAllCustomFilters()`), and respect `CustomFilter.isActive`.
- `ScheduleManager.kt` contains helpful repository extension functions (sync wrappers) used by services — read before adding sync helpers.

## Build & debug tips ⚙️
- Standard debug build: `./gradlew :app:assembleDebug` and install with `./gradlew :app:installDebug`.
- Low-resource optimized debug build: `./gradlew :app:assembleDebugOptimized` (task in `app/build.gradle.kts` — limits Gradle workers).
- To test VPN behavior you must grant VpnService (user confirmation) and Usage Access permission (see `UsageStatsMonitor.requestPermission()` and `MainActivity` flows).
- For runtime diagnostics use `adb logcat` filtered by tags like `GuardianVPN`, `DnsFilterService`, `UsageStatsMonitor`.

## Tests & change suggestions 🔬
- There are currently **no tests** for packet logic. Priority tests: `extractDomainFromDnsQuery()`, `isDnsPacket()`, `shouldBlockDomain()` and domain heuristics.
- Use binary packet fixtures for deterministic packet-level unit/integration tests.
- DB tests: use an in-memory Room DB (`Room.inMemoryDatabaseBuilder`) and the real `GuardianRepository` where convenient.
- Remember `fallbackToDestructiveMigration()` — if preserving data is required, add explicit migrations.

## Integration points & external dependencies 🔗
- Optional `tun2socks` external binary (experimental) — `DnsFilterService` launches it if enabled. Use `EXPERIMENTAL_TUN2SOCKS_ENABLED = true` locally to test whether `tun2socks` restores full TCP/HTTPS connectivity on problematic devices.
- Passthrough UDP experimental: `EXPERIMENTAL_PASSTHROUGH_ENABLED` in `DnsFilterService.kt` permite reenviar UDP no-DNS para pruebas (útil para QUIC/DoH). TCP/HTTP requiere `tun2socks` o una implementación userspace completa.
- Known issue — OPPO A80: en algunos dispositivos (p. ej. OPPO A80) el sistema puede destruir sockets de UIDs externos cuando la VPN está activa (logs: `destroyAllSocketForUid`, `blockedReasons=40`). Diagnóstico: habilitar `EXPERIMENTAL_TUN2SOCKS_ENABLED` o usar `EXPERIMENTAL_PASSTHROUGH_ENABLED` para verificar si la conectividad se restaura; recolectar `adb logcat` filtrando por `netd`, `ConnectivityService`, y `GuardianVPN`.
- Assets: `assets/blocklist_domains.txt` — large lists are loaded by `LocalBlocklist`.
- Android permissions: VpnService, Usage Access, notifications are essential for testing and debugging behaviors.
- Blocking flow often triggers `SafeBrowserActivity` via explicit `Intent` from services (see `UsageStatsMonitor` / `RealisticAppBlocker`).

## Where to look for real examples 📌
- `service/DnsFilterService.kt` — packet parsing, checksum logic, NXDOMAIN forging, forwarding.
- `service/LocalBlocklist.kt` & `assets/blocklist_domains.txt` — keyword blocking.
- `data/GuardianRepository.kt` — canonical DB operations and filtering helpers.
- `ui/SafeBrowserActivity.kt` — WebView + custom filter usage.
- `data/SettingsDataStore.kt` — DataStore patterns and settings flow.

## PR reviewer checklist (short) ✅
1. Use `GuardianRepository` for DB and filter changes.
2. Keep heavy I/O off the Main thread (`Dispatchers.IO` / coroutines).
3. Maintain Spanish localization/comment style where appropriate.
4. Add unit tests for parsing/heuristics; use packet fixtures for network-level behavior.

---
If any section needs more detail (tests, packet fixtures, developer scripts, or moving these instructions to the repo root), tell me which one and I will expand it. 🙌