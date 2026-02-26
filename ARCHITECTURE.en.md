# 🏗️ GuardianOS Shield — Architecture Documentation

> **Internal technical architecture document**  
> 📍 Private project • GuardianOS Shield • Andalusia, Spain  
> 👨‍💻 Author: Victor Shift Lara | ✉️ info@guardianos.es  
> 📅 Last updated: February 2026

🇪🇸 [Español](ARCHITECTURE.md) | 🇬🇧 **English**

---

## 📋 Table of Contents

1. [Overview](#-overview)
2. [Design Principles](#-design-principles)
3. [Layered Architecture](#-layered-architecture)
4. [Critical Components](#-critical-components)
5. [Key Data Flows](#-key-data-flows)
6. [Security & Privacy](#-security--privacy)
7. [Key Technical Decisions](#-key-technical-decisions)
8. [Testing & Quality](#-testing--quality)
9. [Development Guidelines](#-development-guidelines)
10. [Technical Roadmap](#-technical-roadmap)

---

## 👁️ Overview

**GuardianOS Shield** is an Android parental control app that protects children through transparent DNS filtering, app monitoring and granular parental controls — **100% local, no telemetry, deGoogled**.

### 🎯 Technical objectives

| Objective | Implementation |
|-----------|---------------|
| **Real privacy** | No analytics, no cloud, everything on-device |
| **Effectiveness** | Triple blocking layer: DNS + WebView + AccessibilityService |
| **Performance** | Lightweight services, no battery impact (<5% daily) |
| **Maintainability** | Clean architecture, MVVM, implicit dependency injection |
| **Compatibility** | Android 12–15+, custom ROMs (LineageOS, /e/OS) |

### 📐 High-level diagram

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  • MainActivity • Screens • ViewModels  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Domain / Repository             │
│  • GuardianRepository • Use Cases       │
└────────────────┬────────────────────────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
┌───▼───┐  ┌────▼────┐  ┌────▼────┐
│ Data  │  │ Service │  │Security │
│Layer  │  │ Layer   │  │ Layer   │
│• Room │  │• VPN    │  │• PIN    │
│• DAO  │  │• Monitor│  │• Admin  │
│• DS   │  │• Sched  │  │• Encrypt│
└───────┘  └─────────┘  └─────────┘
```

---

## 🧭 Design Principles

### 1. Privacy by Design
- **Zero telemetry**: No data leaves the device
- **Minimal permissions**: Only strictly necessary permissions
- **Local-first**: Room database + DataStore, no cloud synchronization

### 2. Defense in Depth
```
Layer 1: DNS Filtering (CleanBrowsing) → Blocking at resolution level
Layer 2: Local Blocklist (hardcoded)   → WebView blocking before loading
Layer 3: AccessibilityService          → Real-time blocking of social apps
```

### 3. Fail-Safe Defaults
- If the VPN service fails → notify the user, do NOT silently ignore
- If schedule cannot be verified → apply restriction by default (HIGH)
- If PIN cannot be encrypted → block access to the parental zone

### 4. Separation of Concerns
- Each module has a single, clear responsibility
- Communication via interfaces (Repository pattern), no direct coupling

---

## 🏗️ Layered Architecture

### 📦 Data Layer (`data/`)

```
data/
├── GuardianDatabase.kt          # Room DB v4 • Singleton • Automatic migrations
├── GuardianRepository.kt        # Single source of truth • Coordinates DAOs + DataStore
├── SettingsDataStore.kt         # Async preferences • Replaces SharedPreferences
│
├── entities/                    # Persistence models
│   ├── UserProfileEntity.kt     # Child profiles • Age, level, schedules
│   ├── CustomFilterEntity.kt    # Custom blacklist/whitelist
│   ├── DnsLogEntity.kt          # Blocked DNS query logs
│   ├── BlockedSiteEntity.kt     # Blocked sites history
│   ├── SensitiveAppEntity.kt    # Monitored apps (social networks, browsers)
│   ├── PetitionEntity.kt        # Digital Pact: child→parent requests
│   ├── StatisticEntity.kt       # Daily metrics for charts
│   └── DomainStat.kt            # DTO for per-domain grouping
│
└── dao/                         # Room interfaces with typed queries
    ├── UserProfileDao.kt
    ├── CustomFilterDao.kt
    ├── DnsLogDao.kt
    └── ...
```

**Patterns applied**:
- ✅ Repository Pattern: `GuardianRepository` abstracts data sources
- ✅ Reactive Flow: DAOs return `Flow<List<T>>` for reactive UI
- ✅ TypeConverters: For enums, schedules and complex Room objects

### ⚙️ Service Layer (`service/`)

```
service/
├── DnsFilterService.kt          # VPN Service • Transparent DNS • CleanBrowsing
├── LocalBlocklist.kt            # Hardcoded blocked domain list
│
├── AppMonitorService.kt         # Foreground Service • Persistent monitor
├── UsageStatsMonitor.kt         # Foreground app detection every 2s
├── AppBlockerAccessibilityService.kt # Real-time blocking without root
├── LightweightMonitorService.kt # Lightweight fallback monitor (LifecycleService)
├── RealisticAppBlocker.kt       # Blocking logic for browsers/social apps
│
├── SafeBrowsingService.kt       # Launches SafeBrowserActivity as a service
├── ScheduleManager.kt           # Schedule control with AlarmManager
└── LogCleanupWorker.kt          # Periodic cleanup with WorkManager
```

#### 🔑 DnsFilterService: Critical implementation

```kotlin
// ⚠️ ARCHITECTURAL DECISION: Do NOT use addRoute("0.0.0.0", 0)
// That would capture ALL traffic (requires packet processing)
// Instead: only configure DNS → Android resolves with CleanBrowsing

Builder()
    .setSession("GuardianOS Shield")
    .setMtu(1500)
    .addAddress("10.0.0.2", 32)              // Virtual VPN tunnel IP
    .addDnsServer("185.228.168.168")         // CleanBrowsing Primary (Adult Filter)
    .addDnsServer("185.228.169.168")         // CleanBrowsing Secondary
    .addDisallowedApplication(packageName)   // Prevent loop: app does not filter itself
    // ❌ Do NOT add: .addRoute("0.0.0.0", 0) → that would block internet
    .establish()
```

**Why this architecture**:

- ✅ Internet works normally for allowed content
- ✅ CleanBrowsing does the filtering on its servers (no heavy local processing)
- ✅ Pattern used by apps like 1.1.1.1, DNS66, NextDNS
- ✅ Compatible with Android 12+ without root permissions

#### 🔑 Triple-Layer App Monitoring

```
┌─────────────────────────────────┐
│ 1. UsageStatsMonitor (Primary)  │
│ • Polling every 2s • Low usage  │
│ • Detects foreground app        │
└────────┬────────────────────────┘
         │
┌────────▼────────┐  ┌───────────────────────┐
│ 2. Accessibility│  │ 3. LightweightMonitor │
│    Service      │  │    (Fallback)         │
│ • Real-time     │  │ • LifecycleService    │
│   events        │  │ • Broadcast receiver  │
│ • No root       │  │ • Active if UsageStats│
│                 │  │   fails               │
└─────────────────┘  └───────────────────────┘
```

**Advantage**: If one layer fails (e.g. UsageStats disabled by battery optimization), the other two maintain protection.

### 🔐 Security Layer (`security/`)

```
security/
├── SecurityHelper.kt            # Encrypted PIN • EncryptedSharedPreferences • AES256-GCM
├── DeviceAdminHelper.kt         # Anti-uninstall • DevicePolicyManager
└── GuardianDeviceAdminReceiver.kt # BroadcastReceiver for admin events
```

#### 🔑 Parental PIN encryption

```kotlin
// SecurityHelper.kt — secure PIN storage flow
fun savePin(context: Context, profileId: Int, pin: String): Boolean {
    // 1. Generate MasterKey with AndroidKeyStore (hardware-backed if available)
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 2. Create EncryptedSharedPreferences with automatic encryption
    val prefs = EncryptedSharedPreferences.create(
        context,
        "guardian_security_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 3. Hash PIN + encrypted storage
    val hashedPin = hashPin(pin) // SHA-256 + unique salt per profile
    prefs.edit().putString("pin_$profileId", hashedPin).apply()

    return true
}
```

**Security decisions**:

- ✅ **Never store PIN in plain text**: Always hash + encrypt
- ✅ **Unique salt per profile**: Prevents cross-profile dictionary attacks
- ✅ **AndroidKeyStore**: Hardware-protected keys (if the device supports it)
- ✅ **EncryptedSharedPreferences**: Transparent encryption, no manual IV management

### 💳 Billing Layer (`billing/`)

```
billing/
├── BillingManager.kt            # Google Play Billing Library 6+ • One-time premium payment
└── FreeTierLimits.kt            # Free plan limit definitions
```

#### 🔑 Premium state management

```kotlin
// BillingManager.kt — purchase restoration pattern
class BillingManager(private val context: Context) {

    // Query existing purchases on launch (restores premium after reinstallation)
    fun queryPurchasesAsync(onResult: (Boolean) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchasesList ->
            val isPremium = purchasesList?.any {
                it.products.contains("premium_guardianos") &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            } == true
            onResult(isPremium)
        }
    }

    // Real-time purchase listener
    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains("premium_guardianos")) {
                    // Activate premium features + acknowledge purchase
                    acknowledgePurchase(purchase.purchaseToken)
                    notifyPremiumActivated()
                }
            }
        }
    }
}
```

**Considerations**:

- ✅ **One-time payment, not a subscription**: `premium_guardianos` = €14.99 lifetime
- ✅ **Automatic restoration**: `queryPurchasesAsync()` on app launch
- ✅ **Mandatory acknowledgement**: Google requires confirmation within <3 days
- ✅ **Sandbox testing**: Use test accounts in Google Play Console

### 🎨 UI Layer (`ui/` + `viewmodel/`)

```
ui/
├── MainActivity.kt              # Entry point • Jetpack Compose • Navigation Host
├── SafeBrowserActivity.kt       # Secure WebView • Blocking at URL loading
├── AppBlockedActivity.kt        # Inescapable screen • Full-screen • No back button
├── PactScreen.kt                # Digital Pact • Two tabs: child/parent
├── PinLockScreen.kt             # PIN verification • Compose • Animations
├── StreakWidget.kt              # Streak widget • Badges • Pulse animation
│
├── screens/                     # Main screens
│   ├── ParentalControlScreen.kt
│   ├── CustomFiltersScreen.kt
│   ├── StatisticsScreen.kt
│   └── SettingsScreen.kt
│
├── components/                  # Reusable components
│   ├── PremiumGate.kt           # Guard for premium features
│   ├── FreeTrialBanner.kt       # Free trial banner
│   └── ...
│
└── theme/                       # Material Design 3 • Dark mode • Typography

viewmodel/
├── MainViewModel.kt             # Global state • VPN status • Active profile
├── ParentalViewModel.kt         # Parental control logic • Schedules • Restrictions
└── StatsViewModel.kt            # Statistics processing • Charts • Export
```

**UI patterns**:

- ✅ **MVVM**: Separate ViewModels, UI observes state via `StateFlow`
- ✅ **Unidirectional Data Flow**: Events → ViewModel → State → UI
- ✅ **Compose best practices**: `remember`, `derivedStateOf`, `LaunchedEffect` for side-effects

---

## 🔄 Key Data Flows

### 🔒 Flow: VPN Activation + DNS Filtering

```
User → MainActivity → DnsFilterService → Android VpnService → CleanBrowsing DNS

1. User taps "Activate Protection"
2. MainActivity starts DnsFilterService
3. DnsFilterService builds VpnService.Builder()
4. Configures DNS: 185.228.168.168 / 185.228.169.168
5. VPN established, notification "VPN Active"
6. Device DNS traffic → CleanBrowsing (filters queries)
7. HTTP/HTTPS traffic flows normally (no interception)
```

### 🚫 Flow: Site Blocking in SafeBrowser

```
User enters URL → SafeBrowserActivity → isDomainBlocked()

Parallel checks:
1. getActiveProfile()?.isWithinAllowedTime()
2. LocalBlocklist.socialMediaDomains.contains()
3. getCustomFilters().any { domain.contains(it) }

If any blocks:
→ showBlockNotification()
→ loadUrl("file:///android_asset/blocked.html")
→ Show block page with reason

If all allowed:
→ webView.loadUrl()
```

### 👨‍👦 Flow: Digital Pact (child request → parent response)

```
Child (UI) → GuardianRepository → createPetition()

1. Child creates request (TIME_EXTENSION, APP_UNLOCK, SITE_UNLOCK)
2. Repository inserts PetitionEntity (status=PENDING)
3. Parent opens PactScreen > "Reply" tab
4. Parent verifies PIN with SecurityHelper
5. If PIN correct: updatePetition(id, APPROVED/REJECTED)
6. Child sees notification on refresh
```

---

## 🔐 Security & Privacy: Critical Decisions

### ✅ What we DO

| Measure                 | Implementation                                            | Benefit                                              |
| ----------------------- | --------------------------------------------------------- | ------------------------------------------------------ |
| **PIN encryption**      | EncryptedSharedPreferences + AES256-GCM + AndroidKeyStore | PIN never in plain text, hardware-protected            |
| **Anti-uninstall**      | DevicePolicyManager + GuardianDeviceAdminReceiver         | Child cannot remove app without parental PIN           |
| **Zero telemetry**      | No Firebase, no analytics, no remote logs                 | Real privacy: nothing leaves the device                |
| **Secure DNS**          | CleanBrowsing Adult Filter (185.228.168.168)              | DNS-level filtering without heavy local processing     |
| **Minimal permissions** | Only: VPN, UsageStats, Accessibility, PostNotifications   | Smaller attack surface, greater user trust             |

### ❌ What we NEVER do

```kotlin
// ❌ NEVER send data to external servers
fun logUserActivity(domain: String) {
    // DO NOT do this:
    // RetrofitClient.send("https://api.guardianos.es/log", ...)

    // ✅ Instead, store locally:
    repository.insertDnsLog(DnsLogEntity(domain, timestamp, blocked = true))
}

// ❌ NEVER store PIN unencrypted
fun savePinInsecure(pin: String) {
    // DO NOT do this:
    // sharedPreferences.edit().putString("parent_pin", pin).apply()

    // ✅ Use SecurityHelper with encryption:
    SecurityHelper.savePin(context, profileId, pin)
}

// ❌ NEVER intercept HTTPS traffic (requires root / custom certificate)
// GuardianOS Shield does NOT perform MITM, does NOT inspect encrypted content
// Filtering is done at the DNS level (domain resolution), not at the packet level
```

### 🛡️ Threat model

```
Threat: Child tries to uninstall the app
Mitigation: Device Admin + PIN required to deactivate admin

Threat: Child changes system DNS to bypass filtering
Mitigation: VPN Service forces DNS to CleanBrowsing; manual changes have no effect

Threat: Child uses an alternative browser (Brave, Firefox)
Mitigation: UsageStatsMonitor + AccessibilityService detect and redirect to SafeBrowser

Threat: Brute-force attack on parental PIN
Mitigation: SHA-256 hash + unique salt + attempt limit (3 failures = 15 min lockout)

Threat: Data extraction if device is stolen
Mitigation: Data encrypted with AndroidKeyStore (hardware-backed if available)
```

---

## 🎯 Key Technical Decisions

### 1. Why Apache 2.0 and not MIT?

| Criterion                  | Apache 2.0                | MIT         | Choice       |
| -------------------------- | ------------------------- | ----------- | ------------ |
| Patent protection          | ✅ Yes (explicit clause)   | ❌ No        | ✅ Apache 2.0 |
| NOTICE requirement         | ✅ Yes (documents 3rd-party)| ❌ No       | ✅ Apache 2.0 |
| F-Droid compatibility      | ✅ Yes                     | ✅ Yes       | Tie          |
| Trademark use clarity      | ✅ Explicit Section 6      | ❌ Implicit  | ✅ Apache 2.0 |

**Conclusion**: Apache 2.0 provides greater legal protection for a project with a commercial trademark ("guardianos") and third-party components.

### 2. Why CleanBrowsing and not Cloudflare/NextDNS?

| Provider          | Adult Filter           | Free API          | Logs              | Choice      |
| ----------------- | ---------------------- | ----------------- | ----------------- | ------------ |
| CleanBrowsing     | ✅ Yes (185.228.168.168) | ✅ Yes             | ❌ No logs         | ✅ Chosen    |
| Cloudflare Family | ✅ Yes (1.1.1.3)         | ✅ Yes             | ⚠️ 24h logs       | Alternative |
| NextDNS           | ✅ Configurable         | ❌ Requires account | ✅ Detailed logs  | ❌ Rejected  |

**Main reason**: CleanBrowsing offers strict adult filtering without requiring an account or API key, aligned with our "zero configuration, zero telemetry" principle.

### 3. Why triple-layer app monitoring?

```
Problem: Android increasingly restricts background monitoring
Solution: Defense-in-depth strategy

Layer 1: UsageStats API
  ✅ Accurate, official, low consumption
  ❌ May be disabled by battery optimization (especially on OPPO/Xiaomi)

Layer 2: AccessibilityService
  ✅ Detects window changes in real time, no root
  ❌ Requires explicit user permission, can be manually disabled

Layer 3: LightweightMonitorService (LifecycleService)
  ✅ Active even if the others fail, very lightweight
  ❌ Less precise (polling every 5-10s)

Result: If one layer fails, the other two maintain protection.
```

### 4. Why not use addRoute("0.0.0.0", 0) in the VPN?

```kotlin
// ❌ INCORRECT: This captures ALL device traffic
Builder()
    .addRoute("0.0.0.0", 0)  // ← Requires processing every packet → heavy, complex, error-prone
    .establish()

// ✅ CORRECT: Only configure DNS → Android resolves names via CleanBrowsing
Builder()
    .addDnsServer("185.228.168.168")          // ← Filtering at DNS resolution level
    .addDisallowedApplication(packageName)   // ← Prevent loop: app does not filter itself
    .establish()
```

**Advantages of the correct approach**:

- ✅ Internet works normally for allowed content
- ✅ No heavy packet processing (battery, performance)
- ✅ Standard pattern used by apps like 1.1.1.1, DNS66
- ✅ Compatible with Android 12+ without root permissions

---

## 🧪 Testing & Quality

### Testing strategy

```
📁 app/src/test/          # Unit tests (JUnit 4/5)
├── repository/           # GuardianRepository: mock DAOs + verify logic
├── security/             # SecurityHelper: test PIN encryption/decryption
├── service/              # ScheduleManager: test time slots, midnight crossings
└── utils/                # Helpers: hashPin, domain matching, etc.

📁 app/src/androidTest/   # Instrumented tests (Espresso + Compose Testing)
├── ui/                   # Full flows: activate VPN, block site, Digital Pact
├── navigation/           # Verify PinLockScreen cannot be bypassed
└── persistence/          # Room migrations: verify data survives updates
```

### Testing premium without a real purchase

The `debug` build includes `BuildConfig.FORCE_PREMIUM = true` which automatically enables
all premium features when installing the debug APK. In release this value is `false`
and R8 eliminates the block as dead code — **it does not exist in the Play Store APK**.

```bash
# Install debug with forced premium (for UI testing/screenshots)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/guardianos-shield-v1.1.0-debug.apk
```

### Testing commands

```bash
# Quick unit tests (no emulator)
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Coverage report (requires jacoco plugin configured)
./gradlew jacocoTestReport

# Static linting
./gradlew lintDebug

# Check dependencies for vulnerabilities
./gradlew dependencyUpdates -Drevision=release
```

### Quality metrics target

| Metric                      | Target   | Tool                              |
| --------------------------- | -------- | --------------------------------- |
| Unit test coverage          | ≥ 70%    | JaCoCo                            |
| No crashes in 1000 sessions | ≥ 99.5%  | Firebase Crashlytics (debug only) |
| Cold start time             | < 2s     | Android Vitals                    |
| Daily battery consumption   | < 5%     | Battery Historian                 |
| Release APK size            | < 25 MB  | AppBundle + R8 shrinker           |

---

## 🛠️ Development Guidelines

### ✅ Before committing

```bash
# 1. Run local tests
./gradlew testDebugUnitTest

# 2. Check linting
./gradlew lintDebug

# 3. Format code (ktlint)
./gradlew ktlintFormat

# 4. Check for hardcoded secrets
git diff --cached | grep -E "api[_-]?key|secret|password|token" && echo "⚠️ Possible secret detected!"

# 5. Commit with conventional message
git commit -m "feat: add time extension petition type to Pact Digital"
# Types: feat|fix|docs|style|refactor|test|chore
```

### 📐 Code conventions

```kotlin
// ✅ File names: PascalCase for classes, camelCase for functions/variables
UserProfileEntity.kt      // ✅
user_profile_entity.kt    // ❌

// ✅ Visibility: explicit, minimal necessary
private fun calculateHash(input: String): String { ... }  // ✅
fun calculateHash(input: String): String { ... }          // ❌ (public by default)

// ✅ Null safety: avoid !!, use let/also/?:
val userName = profile?.name ?: "Anonymous"  // ✅
val userName = profile!!.name                // ❌

// ✅ Coroutines: appropriate scope, avoid GlobalScope
class MyViewModel : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.IO)  // ✅

    fun loadData() {
        viewModelScope.launch {  // ✅
            repository.fetchData()
        }
    }
}

// ✅ Resources: use string resources, do not hardcode UI text
// strings.xml:
<string name="vpn_active">Secure DNS Active</string>
// In code:
stringResource(R.string.vpn_active)  // ✅
"Secure DNS Active"                  // ❌
```

### 🐛 Debugging: Useful commands

```bash
# Logs per module
adb logcat | grep GuardianVPN          # VPN/DNS filtering
adb logcat | grep UsageStatsMonitor    # App monitoring
adb logcat | grep AppBlockerA11y       # AccessibilityService
adb logcat | grep BillingManager       # Google Play Billing
adb logcat | grep ScheduleManager      # Schedule controls

# Check VPN status
adb shell dumpsys connectivity | grep -A5 VPN

# Check active DNS
adb shell dumpsys connectivity | grep "DNS servers"

# Force restart services (useful after changes)
adb shell am force-stop com.guardianos.shield
adb shell am start -n com.guardianos.shield/.MainActivity

# Install debug APK with replacement
adb install -r app/build/outputs/apk/debug/guardianos-shield-v1.1.0-debug.apk
```

---

## 🗺️ Technical Roadmap

### ✅ Q1 2026 (Completed)

- [x] Transparent DNS VPN with CleanBrowsing Adult Filter
- [x] Safe browser with double-layer blocking
- [x] Triple app monitoring (UsageStats + Accessibility + Lightweight)
- [x] Family Digital Pact (local child→parent requests)
- [x] Billing with Google Play (one-time premium payment)
- [x] Anti-uninstall with Device Admin

### 🔜 Q2 2026 (In development)

- [ ] Web dashboard for parents at guardianos.es (optional E2E-encrypted sync)
- [ ] Configuration export (encrypted backup/restore)
- [ ] Kiosk mode: lock exit from GuardianOS Shield
- [ ] Tablet support: adaptive UI + per-device profiles

### 🎯 Q3–Q4 2026 (Planned)

- [ ] Google Family Link integration (as a complement, not a replacement)
- [ ] Wear OS companion app: parent alert notifications on smartwatch
- [ ] YouTube-specific filtering (keyword detection in titles/descriptions)
- [ ] Home-screen statistics widget (Android AppWidget)
- [ ] ChromeOS support: adapted version for educational devices

### 🚀 Further ahead (Vision)

- [ ] On-device Machine Learning for inappropriate image content detection (TensorFlow Lite)
- [ ] Multi-device sync: profiles that travel between tablet + phone + laptop
- [ ] Home router integration: network-level filtering (requires manufacturer collaboration)

---

## 📎 Appendices

### A. Main dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Kotlin & Compose
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Data
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

### B. Required permissions (AndroidManifest.xml)

```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Foreground services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<!-- API 34+: required by DnsFilterService (foregroundServiceType="connectedDevice") -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- Battery: keep protection services active 24h -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- App monitoring -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />

<!-- Overlay: parental block screen over restricted apps -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Note: BIND_ACCESSIBILITY_SERVICE and BIND_DEVICE_ADMIN are NOT uses-permission.
     They are declared as android:permission on their respective <service>/<receiver>. -->
```

### C. Environment variables and secrets (NEVER commit)

```bash
# .env.example (template for contributors)
PLAY_BILLING_PUBLIC_KEY=your_public_key_here
SIGNING_STORE_PASSWORD=********
SIGNING_KEY_PASSWORD=********
SIGNING_KEY_ALIAS=guardian_release

# .gitignore (already included in the repo)
*.keystore
*.jks
google-services.json
secrets.properties
api_keys.xml
local.properties
.env
```

---

> 📅 Last review: February 2026 • Next review: May 2026  
> ✉️ Contact: info@guardianos.es — https://guardianos.es/shield
