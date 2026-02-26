# 🛡️ GuardianOS Shield

**Local web filtering for child protection**  
No tracking • No external servers • Total privacy

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android%2012%2B-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple.svg)
![API](https://img.shields.io/badge/API-31%2B%20(Android%2012)-orange.svg)

🇪🇸 [Español](README.md) | 🇬🇧 **English**

---

## 📋 Description

**GuardianOS Shield** is an Android parental control application that protects children through:

- 🔒 **Transparent DNS VPN** with CleanBrowsing Adult Filter (no traffic capture)
- 🌐 **Integrated safe browser** with forced local blocking of social networks
- ⏰ **Customizable schedule controls** (with midnight-crossing support)
- 📊 **App monitoring** with automatic redirection to the safe browser
- 🔔 **Real-time notifications** of blocked access attempts
- 🔐 **100% private**: No cloud storage, everything local, no analytics
- ✅ **Compatible with Android 12–15+**: Optimized for all modern versions

**Minimum requirements**: Android 12 (API 31) or higher  
**Optimized for**: Android 12, 13, 14 and 15+

Developed in **Andalusia, Spain** by **Victor Shift Lara**  
🌐 Official website: [https://guardianos.es](https://guardianos.es)  
📧 Contact: info@guardianos.es

---

## 🏗️ Project Architecture

The application follows a clean, modular, multi-layered architecture:

```
guardianos-shield/
├── app/src/main/kotlin/com/guardianos/shield/
│   ├── MainActivity.kt                      # Main Activity (Jetpack Compose)
│   ├── billing/                             # 💳 Billing & Plans
│   │   ├── BillingManager.kt               # Google Play Billing 6+ (one-time €14.99)
│   │   └── FreeTierLimits.kt               # Free plan limits (48h history, features)
│   ├── data/                                # 📦 Data Layer
│   │   ├── GuardianDatabase.kt             # Room Database (v4)
│   │   ├── GuardianRepository.kt           # Central repository (DAO + logic)
│   │   ├── UserProfileEntity.kt            # User/child profiles
│   │   ├── CustomFilterEntity.kt           # Custom filters (blacklist/whitelist)
│   │   ├── DnsLogEntity.kt                 # Blocked DNS query logs
│   │   ├── BlockedSiteEntity.kt            # Blocked sites history
│   │   ├── SensitiveAppEntity.kt           # Monitored sensitive apps
│   │   ├── PetitionEntity.kt               # Digital Pact requests (child→parent)
│   │   ├── StatisticEntity.kt              # Daily usage statistics
│   │   ├── DomainStat.kt                   # Per-domain statistical grouping
│   │   └── SettingsDataStore.kt            # Settings (DataStore)
│   ├── security/                            # 🔐 Security & Administration
│   │   ├── SecurityHelper.kt               # Encrypted PIN with EncryptedSharedPreferences AES256_GCM
│   │   ├── DeviceAdminHelper.kt            # Anti-uninstall via DevicePolicyManager
│   │   └── GuardianDeviceAdminReceiver.kt  # Device Admin BroadcastReceiver
│   ├── service/                             # ⚙️ Background Services
│   │   ├── DnsFilterService.kt             # VPN Service (transparent DNS via CleanBrowsing)
│   │   ├── LocalBlocklist.kt               # Local blocked domain list
│   │   ├── AppMonitorService.kt            # Persistent Foreground Service
│   │   ├── UsageStatsMonitor.kt            # Foreground app detection (UsageStats)
│   │   ├── AppBlockerAccessibilityService.kt # Blocking via Accessibility Events (no root)
│   │   ├── LightweightMonitorService.kt    # Lightweight monitor (LifecycleService, broadcast)
│   │   ├── RealisticAppBlocker.kt          # Persistent blocking of browsers/social apps
│   │   ├── SafeBrowsingService.kt          # Launches SafeBrowserActivity as a service
│   │   ├── ScheduleManager.kt              # Day-of-week schedule control with AlarmManager
│   │   └── LogCleanupWorker.kt             # Periodic log cleanup (WorkManager)
│   ├── ui/                                  # 🎨 User Interface (Jetpack Compose)
│   │   ├── SafeBrowserActivity.kt          # Safe browser with WebView
│   │   ├── SafeBrowserViewModel.kt         # Dedicated ViewModel for the browser
│   │   ├── AppBlockedActivity.kt           # Blocking screen over the app (inescapable)
│   │   ├── PactScreen.kt                   # Family Digital Pact (child mailbox + parent reply)
│   │   ├── PinLockScreen.kt                # Parental PIN verification (Compose)
│   │   ├── StreakWidget.kt                 # Daily streak widget with badges and animations
│   │   ├── PremiumPurchaseScreen.kt        # Premium purchase screen (€14.99 lifetime)
│   │   ├── PremiumGate.kt                  # Guard component for premium features
│   │   ├── FreeTrialBanner.kt              # Banner with remaining trial days
│   │   ├── FreeTrialExpiredDialog.kt       # Dialog shown when trial period expires
│   │   ├── ParentalControlScreen.kt        # Parental configuration
│   │   ├── CustomFiltersScreen.kt          # Custom filter management
│   │   ├── StatisticsScreen.kt             # Statistics and logs
│   │   ├── SettingsScreen.kt               # General settings
│   │   └── theme/                          # Material Design 3
│   └── viewmodel/                           # 📊 ViewModels (MVVM)
│       ├── MainViewModel.kt
│       ├── ParentalViewModel.kt
│       └── StatsViewModel.kt
├── AndroidManifest.xml                      # Permissions and services
└── build.gradle.kts                         # Gradle configuration
```

### 📦 Data Layer (`data/`)

- **Room Database v4**: Local SQLite database with TypeConverters
- **Repository Pattern**: `GuardianRepository` centralizes all DAO access
- **DataStore**: Async persistent settings (replaces SharedPreferences)
- **Entities**: `UserProfileEntity`, `BlockedSiteEntity`, `DnsLogEntity`, `CustomFilterEntity`, `SensitiveAppEntity`, `PetitionEntity`, `StatisticEntity`
- **DAOs**: Interfaces with SQL queries and reactive Flows for real-time UI updates

### ⚙️ Service Layer (`service/`)

- **DnsFilterService**: VPN Service that configures secure DNS (CleanBrowsing) without processing packets
- **LocalBlocklist**: Hardcoded local blocking of social networks and adult content
- **AppMonitorService**: Persistent Foreground Service with permanent notification
- **UsageStatsMonitor**: Detects foreground app every 2 s and redirects to safe browser
- **AppBlockerAccessibilityService**: Real-time blocking via `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`, no root required
- **LightweightMonitorService**: Lightweight `LifecycleService` with broadcast receiver for app detection
- **RealisticAppBlocker**: Dedicated service with hardcoded lists of browsers and social apps to block
- **SafeBrowsingService**: Starts `SafeBrowserActivity` directly on launch
- **ScheduleManager**: Schedule control with `AlarmManager` per day-of-week and time slots
- **LogCleanupWorker**: `CoroutineWorker` via WorkManager for periodic log cleanup

### 🔐 Security Layer (`security/`)

- **SecurityHelper**: Stores and verifies parental PIN with `EncryptedSharedPreferences` (AES256-GCM) + AndroidKeyStore
- **DeviceAdminHelper**: Registers the app as Device Admin to prevent uninstallation without PIN
- **GuardianDeviceAdminReceiver**: `DeviceAdminReceiver` that handles device administration events

### 💳 Billing Layer (`billing/`)

- **BillingManager**: Integration with Google Play Billing Library 6+; lifetime one-time purchase `premium_guardianos` (€14.99); restores premium status after reinstallation via `queryPurchases()`
- **FreeTierLimits**: Defines free plan limits — 48-hour maximum history, no multiple profiles, no custom schedules, no custom filters, no push alerts

### 🎨 UI Layer (`ui/`)

- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Dynamic theming
- **Navigation Component**: Screen-to-screen navigation
- **SafeBrowserActivity + SafeBrowserViewModel**: WebView with URL-load blocking, now with decoupled ViewModel
- **AppBlockedActivity**: Full-screen overlay on any blocked app; cannot be closed without parental PIN or approved Digital Pact request
- **PactScreen**: Two tabs — "My Mailbox" for the child (send requests) and "Reply" for the parent (approve/reject with PIN)
- **PinLockScreen**: Compose PIN verification screen to access the parental zone
- **StreakWidget**: Daily clean-day streak widget with badges, pulse animation and personal record
- **PremiumPurchaseScreen / PremiumGate**: Paywall screen and guards for premium features
- **FreeTrialBanner / FreeTrialExpiredDialog**: Components for trial period management

---

## ✨ Key Features

### 🔒 Transparent DNS VPN

- **Technology**: Android VpnService without traffic capture (does not use `addRoute("0.0.0.0", 0)`)
- **DNS Provider**: CleanBrowsing Adult Filter (185.228.168.168 / 185.228.169.168)
- **Automatic filtering**:
  - ✅ Adult content and pornography
  - ✅ Malware, phishing and scams
  - ✅ **Social networks** (TikTok, Facebook, Instagram, Discord, Twitter, Snapchat)
  - ✅ Online gaming and gambling
  - ✅ Proxies and VPNs
  - ✅ Mixed inappropriate content
- **Internet works normally** for educational and productive content
- **No root required** and no special permissions
- **Additional local blocking**: Hardcoded social network list as a fallback

### 🌐 Safe Browser

- **Integrated WebView** with real-time blocking before loading URLs
- **Double-layer protection**: DNS filtering + forced local blocking
- **Automatic schedule verification** before loading any page
- **Custom filters**: Functional blacklist/whitelist system
- **Visual block page** with dynamic icons (🛡️ restriction, ⏰ schedule)
- **Browsing history** stored locally
- **Notifications per block** with automatic categorization

### ⏰ Schedule Controls

- **Configurable allowed schedule** (e.g. 09:00 – 20:00)
- **Automatic blocking outside set hours**
- **Midnight-crossing schedule support** (e.g. 22:00 – 08:00)
- **Real-time verification** on every navigation
- **User notification** when blocked due to schedule
- **No bypass mode**: Strict control applied at all times

### 👤 User Profiles

- **Multiple profiles** for different children
- **Age-based configuration** (0–7, 8–12, 13–15, 16–17)
- **Restriction levels**: LOW, MEDIUM, HIGH, STRICT
- **Parental PIN** to protect configuration (SHA-256 + salt hash)
- **Active profile** applied in real time
- **Granular configuration**: By category (adult, social, gaming, etc.)

### 🔔 Notification System

- **Blocked websites**: Notification with automatic category
  - Social network blocked
  - Adult content blocked
  - Gambling blocked
  - Schedule not permitted
- **Blocked apps**: When an external browser is redirected (Chrome, Brave, Firefox)
- **HIGH priority** for immediate alerts
- **Auto-dismissible** on tap
- **Separate channels**: "Blocked Sites" and "Blocked Apps"

### 📊 App Monitoring (Triple Layer)

- **UsageStats API** (primary): Detects foreground app every 2 s via `UsageStatsMonitor`
- **AccessibilityService** (reinforcement): `AppBlockerAccessibilityService` reacts to `TYPE_WINDOW_STATE_CHANGED` in real time without root
- **LightweightMonitorService** (fallback): Lightweight `LifecycleService` active even when `UsageStats` is unavailable
- **External browser detection** (Chrome, Brave, Firefox, Edge, Opera, Samsung Browser, UC, Mi Browser)
- **Social app detection**: Facebook, Instagram, TikTok, Snapchat, Twitter, Reddit, WhatsApp, Telegram, Discord
- **Automatic redirection** to the safe browser or `AppBlockedActivity`
- **Persistent Foreground Service** (resistant to OPPO/Motorola task killers)

### 🤝 Family Digital Pact

- **Local request system**: Child requests permissions without leaving the device
  - ⏱️ `TIME_EXTENSION` — Request more screen time
  - 📱 `APP_UNLOCK` — Request temporary unlock of an app
  - 🌐 `SITE_UNLOCK` — Request unlock of a specific site
- **Child's mailbox**: History of sent requests and their statuses
- **Parent panel**: Reply with PIN → approve (with note) or reject (with reason)
- **Statuses**: `PENDING` → `APPROVED` / `REJECTED`
- **100% offline**: No push notifications or external servers

### 🏆 Daily Streak

- **Consecutive "clean" days** without attempts to access blocked content
- **Milestone badges**: 7, 14, 30, 60, 90 days
- **Personal record** stored in `UserProfileEntity`
- **Pulse animation** when the streak is active
- **Widget** embedded in the main dashboard

### 💳 Plans & Billing

- **Free Plan**: DNS VPN always active, last 48 h history across all modules, safe browser (10 URL history), basic monitoring
- **Premium Plan** (€14.99 — one-time lifetime, no subscription):
  - ✅ Full parental controls and multiple profiles
  - ✅ Custom schedules by day of the week
  - ✅ Custom filters (unlimited blacklist/whitelist)
  - ✅ Extended history (30 days)
  - ✅ CSV history export
  - ✅ Premium push alerts
  - ✅ Full Family Digital Pact
  - ✅ Free trial period included
- **Google Play Billing Library 6+**: `BillingManager` automatically restores premium status after reinstallation
- **PremiumGate**: Compose component that shows the paywall dialog when a free-plan user accesses premium features

### 🔐 Advanced Security

- ✅ **Encrypted parental PIN**: `EncryptedSharedPreferences` with AES256-GCM + AndroidKeyStore
- ✅ **Anti-uninstall**: Device Admin via `DevicePolicyManager`; a child cannot remove the app without the parent's PIN
- ✅ **Inescapable AppBlockedActivity**: The block screen prevents returning to the blocked app without PIN or an approved request
- ✅ **100% local**: No analytics, no cloud storage
- ✅ **Open Source** and auditable

### 📊 Enhanced Statistics

- **StatisticEntity**: Stores daily usage metrics and blocks by category
- **DomainStat**: Per-domain grouping for top-10 charts
- **History**: 48 h on free plan → 30 days on premium
- **CSV export**: Available on premium plan

---

## 🚀 Installation & Build

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or later
- **Gradle 8.6+** (bundled with the project)
- **Android SDK 34** (Android 14)
- **Kotlin 1.9.0+**

### Clone the Repository

```bash
git clone https://github.com/systemavworks/guardianos-shield.git
cd guardianos-shield
```

### Build with Gradle

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Clean build
./gradlew clean
```

### Install on Device

```bash
# Install debug APK via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or drag the APK directly onto the device
```

**APK location**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Using the App

### 1️⃣ Initial Setup

1. **Install** the app and open it
2. **Create profile** for the child (name, age, restriction level)
3. **Configure schedule** allowed (optional)
4. **Activate VPN**: Button on the main screen
5. **Grant permissions**:
   - VPN (Android will ask for confirmation)
   - UsageStats (for app monitoring)
   - Accessibility: `AppBlockerAccessibilityService` in Settings > Accessibility (reinforced blocking without root)
   - Device Admin (optional): to prevent the child from uninstalling the app
   - Notifications (Android 13+)

### 2️⃣ Activate VPN Protection

- Tap **"Activate Protection"** on the main screen
- Android will ask for permission to establish a VPN
- Once active, you will see a persistent notification: **"Secure DNS Active"**
- Internet will work normally, but blocked content will be inaccessible

### 3️⃣ Safe Browser

- **Open**: Tap the browser icon on the main screen
- **Browse**: Enter URLs or Google searches
- **Blocks**: You will see a block page with reason (schedule, category, etc.)
- **History**: History button to view visited sites

### 4️⃣ Parental Control

- **Access**: Menu > Parental Control
- **Configure**:
  - Allowed schedule (enable and set start/end)
  - Restriction level (LOW, MEDIUM, HIGH, STRICT)
  - Categories to block (adult, gambling, social, gaming)
- **Save**: Changes apply immediately

### 5️⃣ Custom Filters

- **Access**: Menu > Custom Filters
- **Add to blacklist**: Enter domain (e.g. `tiktok.com`) and press ➕
- **Add to whitelist**: Switch to whitelist and add allowed domains
- **Delete**: Swipe to delete filters
- **Applied instantly** in the safe browser

### 6️⃣ Statistics

- **View logs**: Menu > Statistics
- **Sites blocked today**: Real-time counter
- **Weekly history**: Block chart
- **Export**: Button to export logs to CSV

### 7️⃣ App Monitoring

- **Enable**: Menu > Settings > App Monitoring
- **Grant UsageStats**: Android will take you to the settings
- **Grant Accessibility** (reinforcement): Settings > Accessibility > GuardianOS Shield
- **How it works**:
  - If the child opens Chrome/Brave/Firefox → redirected to safe browser
  - `AppBlockerAccessibilityService` acts in real time (no root)
  - `LightweightMonitorService` as an additional fallback
  - Notification: "App blocked - Chrome"
  - Foreground Service keeps monitoring active

### 8️⃣ Family Digital Pact

- **Access**: Menu > Digital Pact
- **The child** can send requests from their tab (no PIN required)
  - More time, app unlock, site unlock
- **The parent** opens the "Reply" tab, enters the PIN and approves or rejects
- **No internet required**: Everything stays on the device

### 9️⃣ Anti-uninstall (Device Admin)

- **Enable**: Menu > Security > Enable Device Admin
- Android will show a system confirmation dialog
- Once active, the child **cannot uninstall** the app without the parental PIN
- **Disable**: Parent enters PIN in Security > Disable Device Admin

### 🔟 Premium Plan

- **View current plan**: Banner on main screen or Menu > Premium
- **Purchase (€14.99 — one-time)**: Premium screen > "Activate Premium"
- Google Play manages payment; status is automatically restored after reinstallation
- Premium features (schedules, custom filters, multiple profiles) are unlocked immediately

---

## 🔧 Technical Overview

### Transparent DNS VPN

```kotlin
// DnsFilterService.kt — simplified VPN configuration
Builder()
    .setSession("GuardianOS Shield")
    .setMtu(1500)
    .addAddress("10.0.0.2", 32)                    // Virtual tunnel IP
    .addDnsServer("185.228.168.168")               // CleanBrowsing Primary DNS
    .addDnsServer("185.228.169.168")               // CleanBrowsing Secondary DNS
    .addDisallowedApplication(packageName)         // Avoid infinite loops
    // ⚠️ CRITICAL: Do NOT use addRoute() = traffic flows normally, only DNS filtered
    .establish()
```

**Correct architecture**:

- ✅ **No packet capture**: Does not use `addRoute("0.0.0.0", 0)` which blocked internet
- ✅ **DNS only**: Android resolves DNS using the configured CleanBrowsing servers
- ✅ **CleanBrowsing does the filtering** on its servers (Adult Filter = most restrictive)
- ✅ **Internet works normally**: All traffic flows without interception
- ✅ **Standard pattern**: Same approach used by apps like 1.1.1.1, DNS66, NextDNS

### Local Blocking in SafeBrowserActivity (Double Layer)

```kotlin
// Verify domain BEFORE loading — critical for social networks
private suspend fun isDomainBlocked(domain: String): Boolean {
    // 1️⃣ Check allowed schedule (UserProfileEntity.isWithinAllowedTime())
    val profile = repository.getActiveProfile()
    if (profile != null && !profile.isWithinAllowedTime()) {
        showBlockNotification("Schedule not allowed")
        return true
    }

    // 2️⃣ Hardcoded local list of social networks (fallback if DNS fails)
    val socialMediaDomains = setOf(
        "facebook.com", "instagram.com", "tiktok.com", "twitter.com",
        "discord.com", "snapchat.com", "reddit.com", etc.
    )
    if (socialMediaDomains.any { domain.equals(it, ignoreCase = true) ||
                                   domain.endsWith(".$it") }) {
        showBlockNotification("Social network blocked: $domain")
        return true
    }

    // 3️⃣ User-defined custom filters (blacklist isActive=true)
    val filters = repository.getAllCustomFilters()
    if (filters.any { it.isActive && domain.contains(it.domain, ignoreCase = true) }) {
        return true
    }

    // 4️⃣ Adult/gambling keywords
    val adultKeywords = listOf("porn", "xxx", "adult", "sex", "casino", "bet")
    if (adultKeywords.any { domain.contains(it, ignoreCase = true) }) {
        return true
    }

    return false
}
```

### App Monitoring with UsageStats

```kotlin
// UsageStatsMonitor.kt — detect foreground app every 2 seconds
private suspend fun monitorForegroundApp() {
    val statsManager = getSystemService(UsageStatsManager::class.java)
    val stats = statsManager.queryUsageStats(INTERVAL_DAILY, startTime, endTime)
    val foregroundApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName

    // If it's an external browser → redirect + notify
    if (foregroundApp in browserPackages && foregroundApp != "com.guardianos.shield") {
        showAppBlockedNotification(getAppLabel(foregroundApp))
        startActivity(Intent(context, SafeBrowserActivity::class.java))
    }
}
```

### Notification System

```kotlin
// Automatic notification when blocking
private fun showBlockNotification(domain: String) {
    val category = when {
        domain.contains("facebook") || domain.contains("tiktok") -> "Social Network"
        domain.contains("porn") || domain.contains("xxx") -> "Adult Content"
        domain.contains("casino") -> "Gambling"
        else -> "Restricted Site"
    }

    NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle("🚫 Site blocked")
        .setContentText("$category: $domain")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
}
```

### App Blocking with AccessibilityService

```kotlin
// AppBlockerAccessibilityService.kt — detects window changes in real time
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val packageName = event.packageName?.toString() ?: return

    scope.launch {
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedApp &&
            now - lastBlockTime < MIN_BLOCK_INTERVAL) return@launch

        val apps = repository.getAllSensitiveApps().firstOrNull() ?: return@launch
        val matching = apps.firstOrNull { it.packageName == packageName } ?: return@launch

        val profile = repository.getActiveProfile() ?: return@launch
        if (!profile.isWithinAllowedTime()) {
            lastBlockedApp = packageName
            lastBlockTime = now
            val intent = AppBlockedActivity.createIntent(this@AppBlockerAccessibilityService, packageName)
            startActivity(intent)
        }
    }
}
```

### Schedule Control with ScheduleManager

```kotlin
// ScheduleManager.kt — day-of-week time slots with AlarmManager
data class TimeSlot(
    val dayOfWeek: Int,   // Calendar.MONDAY ... Calendar.SUNDAY
    val startHour: Int, val startMinute: Int,
    val endHour: Int,   val endMinute: Int
) {
    fun isNowInSlot(): Boolean {
        val now = Calendar.getInstance()
        if (now.get(Calendar.DAY_OF_WEEK) != dayOfWeek) return false
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return current in (startHour * 60 + startMinute)..(endHour * 60 + endMinute)
    }
}
```

### Anti-uninstall with DeviceAdmin

```kotlin
// DeviceAdminHelper.kt
fun requestActivation(context: Context) {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "GuardianOS Shield needs this permission to prevent a child from " +
            "uninstalling the parental control app without the parent's PIN.")
    }
    context.startActivity(intent)
}
```

### Encrypted PIN with EncryptedSharedPreferences

```kotlin
// SecurityHelper.kt
fun savePin(context: Context, profileId: Int, pin: String): Boolean {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val prefs = EncryptedSharedPreferences.create(
        context, PREFS_NAME, masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    prefs.edit().putString("pin_$profileId", hashPin(pin)).apply()
    return true
}
```

## 🛠️ Development & Debugging

### Build versions

```bash
# Debug (with logs)
./gradlew assembleDebug

# Release (optimized and signed)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean and rebuild
./gradlew clean && ./gradlew assembleDebug
```

### Useful debugging commands

```bash
# VPN and DNS filtering
adb logcat | grep GuardianVPN

# App monitoring (UsageStats)
adb logcat | grep UsageStatsMonitor

# AccessibilityService (reinforced blocking)
adb logcat | grep AppBlockerA11y

# Lightweight monitor
adb logcat | grep LightweightMonitor

# Safe browser
adb logcat | grep SafeBrowser

# ScheduleManager (schedules)
adb logcat | grep ScheduleManager

# Billing / Premium
adb logcat | grep BillingManager

# Device Admin
adb logcat | grep DeviceAdmin

# All app logs
adb logcat | grep "com.guardianos.shield"

# Check granted permissions
adb shell dumpsys package com.guardianos.shield | grep permission

# Check VPN status
adb shell dumpsys connectivity | grep VPN

# Check active DNS
adb shell dumpsys connectivity | grep "DNS servers"

# Force-stop app (restart services)
adb shell am force-stop com.guardianos.shield

# Install and run
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.guardianos.shield/.MainActivity
```

### Common debugging

1. **VPN won't activate**:
   - Check that no other VPN is active
   - Disable Private DNS in Settings > Network
   - Grant VPN permission when Android prompts

2. **Sites not being blocked**:
   - Check DNS logs: `adb logcat | grep GuardianVPN` — should show 185.228.168.168
   - Verify local list in `LocalBlocklist.kt` and `SafeBrowserActivity.kt`

3. **Monitoring not working**:
   - Grant UsageStats: Settings > Apps > Special access > Usage access
   - Check that `AppBlockerAccessibilityService` is enabled in Accessibility
   - Disable battery optimization for the app

4. **Notifications not appearing**:
   - Android 13+ requires POST_NOTIFICATIONS permission
   - Check notification channels created
   - Review app settings in Settings > Notifications

5. **Schedules not working**:
   - Check active profile with `repository.getActiveProfile()`
   - Confirm `scheduleEnabled = true` and slots in `ScheduleManager`
   - Check logs: `adb logcat | grep ScheduleManager`

6. **Device Admin won't activate**:
   - Call `DeviceAdminHelper.requestActivation(context)` from an Activity
   - Check that `GuardianDeviceAdminReceiver` is declared in `AndroidManifest.xml`
   - Verify with: `adb shell dpm list-owners`

7. **Premium not restored after reinstallation**:
   - Check Play Store connection
   - Check logs: `adb logcat | grep BillingManager`
   - `BillingManager.queryPurchases()` queries existing purchases in the background

---

## 📄 License

Licensed under the Apache License, Version 2.0 (the "License");  
you may not use this file except in compliance with the License.  
Copyright © 2026 Victor Shift Lara — GuardianOS Project  
📍 Andalusia, Spain | https://guardianos.es

See the [LICENSE](LICENSE) file for details.

> ⚠️ **Trademark Notice**  
> This license covers the source code only.  
> The trademarks "GuardianOS", "GuardianOS Shield", the logo and any associated visual assets are the property of their respective owners and are **NOT** covered by this open-source license.  
> For trademark use enquiries: info@guardianos.es

---

## 👨‍💻 Author

**Victor Shift Lara**  
📍 Andalusia, Spain  
🌐 Web: [https://guardianos.es](https://guardianos.es)  
📧 Email: [info@guardianos.es](mailto:info@guardianos.es)  
💼 GitHub: [@systemavworks](https://github.com/systemavworks/guardianos-shield)

---

## 🙏 Acknowledgements

- **CleanBrowsing** for their public, free DNS filtering service
- **Android Open Source Project** for VpnService API and UsageStats API
- **Google Jetpack** for modern libraries (Compose, Room, Navigation)
- **Material Design 3** for the design system
- **Cloudflare** for alternative DNS servers

---

## 📞 Support

Questions or issues?

- 🐛 **Issues**: [GitHub Issues](https://github.com/systemavworks/guardianos-shield/issues)
- 📧 **Email**: info@guardianos.es
- 🌐 **Web**: [https://guardianos.es/support](https://guardianos.es/support)
- 📖 **Wiki**: [GitHub Wiki](https://github.com/systemavworks/guardianos-shield/wiki)

---

## 📝 Roadmap

### ✅ Implemented

- [x] Transparent DNS VPN with CleanBrowsing Adult Filter
- [x] Safe browser (WebView + double-layer blocking)
- [x] Schedule controls with midnight-crossing support and per-day slots
- [x] Triple app monitoring (UsageStats + AccessibilityService + LightweightMonitor)
- [x] Inescapable app-block screen (`AppBlockedActivity`)
- [x] Family Digital Pact (child → parent requests, fully local)
- [x] Daily streak with badges and animations (`StreakWidget`)
- [x] Encrypted parental PIN with AES256-GCM + AndroidKeyStore
- [x] Anti-uninstall via Device Admin
- [x] Premium plan with Google Play Billing 6+ (€14.99 lifetime)
- [x] Free Plan with 48 h history limit
- [x] Enhanced statistics (`StatisticEntity`, `DomainStat`)
- [x] Room Database v4 with migrations

### 🔜 Upcoming versions

- [ ] Web dashboard for parents at guardianos.es
- [ ] Full configuration export (backup/restore)
- [ ] Kiosk mode to lock users inside the app
- [ ] Support for multiple synchronized devices (optional)
- [ ] Google Family Link integration
- [ ] Companion app for smartwatches (alerts to parents)
- [ ] YouTube-specific content filtering
- [ ] In-app purchase blocking
- [ ] Tablet and ChromeOS support
- [ ] PDF report export
- [ ] Home-screen statistics widget (Android AppWidget)

---

**Made with ❤️ in Andalusia**  
*Protecting our little ones in the digital world*
