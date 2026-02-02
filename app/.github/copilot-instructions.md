# Copilot instructions for Guardianos Shield 🚦

**Purpose:** Help AI coding agents get productive quickly with concise, actionable pointers about architecture, patterns, and workflows for this repository.

## Quick facts ✅
- Language: Kotlin (Android app)
- Min SDK: 24, Target SDK: 34
- Build system: Gradle (Kotlin DSL) — use `./gradlew` (see `app/build.gradle.kts`)
- DB: Room (KSP compiler). DB version = 3, `fallbackToDestructiveMigration()` is enabled.
- Preferences: DataStore (preferences)
- UI: Jetpack Compose + WebView in `SafeBrowserActivity`
- Comments & UI strings: mostly **Spanish** — keep consistency.

## Big-picture architecture 📂
- Network/filter: `service/DnsFilterService.kt` (VpnService). Receives TUN packets, extracts DNS queries, decides block/allow and either forges NXDOMAIN or forwards upstream.
- Local rules: `service/LocalBlocklist.kt` and `assets/blocklist_domains.txt` (keyword-based + optional large-list loader).
- Persistence: `data/GuardianRepository.kt` (single API for DB operations). DB entities: BlockedSite, Statistic, UserProfile, CustomFilter, DnsLog.
- UI: `ui/SafeBrowserActivity.kt` for the built-in safe browser, `ui/*Screen.kt` for settings and stats.
- Monitoring: `service/UsageStatsMonitor.kt` + `service/AppMonitorService.kt` for foreground-app/usage monitoring.

## Project-specific conventions & patterns 🧭
- Use KSP (not kapt) for Room (see `ksp(...)` in `app/build.gradle.kts`).
- Always use `GuardianRepository` from UI and services for DB access; avoid calling DAOs directly outside data layer.
- Prefer `Dispatchers.IO` for repository and file I/O; flows are used for reactive UI.
- WebView-based filtering and VPN packet parsing share domain-check heuristics — reuse `GuardianRepository` filter methods (e.g., `getAllCustomFilters()`, `isInBlacklist()`).

## Build & debug tips ⚙️
- Standard debug build: `./gradlew :app:assembleDebug`
- Low-resource optimized debug build: `./gradlew assembleDebugOptimized` (task in `app/build.gradle.kts` — limits Gradle workers)
- To test VPN behavior you must grant VpnService and Usage Access permissions (see `UsageStatsMonitor.requestPermission()`).
- Room is configured with destructive fallback — be careful changing entity schemas.

## Tests & safe change suggestions 🔬
- There are currently no tests. Prioritize unit tests for pure logic functions: `extractDomainFromDnsQuery()`, `isDnsPacket()`, `shouldBlockDomain()` and domain heuristics.
- For packet-level code, use recorded binary fixtures to create deterministic unit/integration tests.

## Where to look for real examples 📌
- `service/DnsFilterService.kt` — DNS parsing, forwarding and packet construction (IPv4 + IPv6 checksums)
- `service/LocalBlocklist.kt` & `assets/blocklist_domains.txt` — keyword lists & asset loading
- `data/GuardianRepository.kt` — canonical DB operations (logging DNS, adding blocked sites, custom filters)
- `ui/SafeBrowserActivity.kt` — how the app checks and blocks user navigations
- `data/SettingsDataStore.kt` — DataStore usage pattern

## PR reviewer checklist (short) ✅
- Use `GuardianRepository` for DB changes.
- Keep heavy I/O off Main thread; use proper dispatchers.
- Maintain Spanish localization/comment style where appropriate.
- Add unit tests for parsing/heuristics; use fixtures for network-level behavior.

---
If you want these instructions merged into the repository root `.github/copilot-instructions.md`, tell me and I will move it there. If any section needs expansion (tests, packet fixtures, or examples), say which one and I'll iterate. 🙌