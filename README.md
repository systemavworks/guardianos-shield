# 🛡️ GuardianOS Shield

**Filtrado web local para la protección de menores**  
Sin rastreo • Sin servidores externos • Privacidad total

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android%2012%2B-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple.svg)
![API](https://img.shields.io/badge/API-31%2B%20(Android%2012)-orange.svg)

🇪🇸 **Español** | 🇬🇧 [English](README.en.md)

---

## 📋 Descripción

**GuardianOS Shield** es una aplicación Android de control parental que protege a los menores mediante:

- 🔒 **VPN DNS transparente** con CleanBrowsing Adult Filter (sin captura de tráfico)
- 🌐 **Navegador seguro** integrado con bloqueo local forzado de redes sociales
- ⏰ **Control de horarios** personalizables (con soporte cruce de medianoche)
- 📊 **Monitoreo de apps** con redirección automática al navegador seguro
- 🔔 **Notificaciones en tiempo real** de intentos de acceso bloqueados
- 🔐 **100% privado**: Sin almacenamiento en la nube, todo local, sin analytics
- ✅ **Compatible Android 12-15+**: Optimizado para todas las versiones modernas

**Requisitos mínimos**: Android 12 (API 31) o superior  
**Optimizado para**: Android 12, 13, 14 y 15+

Desarrollado en **Andalucía, España** por **Victor Shift Lara**  
🌐 Web oficial: [https://guardianos.es](https://guardianos.es)  
📧 Contacto: info@guardianos.es

---

## 🏗️ Arquitectura del Proyecto

La aplicación sigue una arquitectura en capas limpia y modular:

```
guardianos-shield/
├── app/src/main/kotlin/com/guardianos/shield/
│   ├── MainActivity.kt                      # Activity principal (Jetpack Compose)
│   ├── billing/                             # 💳 Facturación y Planes
│   │   ├── BillingManager.kt               # Google Play Billing 6+ (pago único 14,99 €)
│   │   └── FreeTierLimits.kt               # Límites plan gratuito (historial 48h, features)
│   ├── data/                                # 📦 Capa de Datos
│   │   ├── GuardianDatabase.kt             # Room Database (v4)
│   │   ├── GuardianRepository.kt           # Repositorio central (DAO + lógica)
│   │   ├── UserProfileEntity.kt            # Perfiles de usuario/menores
│   │   ├── CustomFilterEntity.kt           # Filtros personalizados (blacklist/whitelist)
│   │   ├── DnsLogEntity.kt                 # Logs de consultas DNS bloqueadas
│   │   ├── BlockedSiteEntity.kt            # Historial de sitios bloqueados
│   │   ├── SensitiveAppEntity.kt           # Apps sensibles monitoreadas
│   │   ├── PetitionEntity.kt               # Peticiones Pacto Digital (hijo→padre)
│   │   ├── StatisticEntity.kt              # Estadísticas de uso diarias
│   │   ├── DomainStat.kt                   # Agrupación estadística por dominio
│   │   └── SettingsDataStore.kt            # Configuración (DataStore)
│   ├── security/                            # 🔐 Seguridad y Administración
│   │   ├── SecurityHelper.kt               # PIN cifrado con EncryptedSharedPreferences AES256_GCM
│   │   ├── DeviceAdminHelper.kt            # Anti-desinstalación vía DevicePolicyManager
│   │   └── GuardianDeviceAdminReceiver.kt  # BroadcastReceiver de Device Admin
│   ├── service/                             # ⚙️ Servicios en Segundo Plano
│   │   ├── DnsFilterService.kt             # VPN Service (DNS transparente CleanBrowsing)
│   │   ├── LocalBlocklist.kt               # Lista de dominios bloqueados local
│   │   ├── AppMonitorService.kt            # Foreground Service persistente de monitoreo
│   │   ├── UsageStatsMonitor.kt            # Detección de apps foreground (UsageStats)
│   │   ├── AppBlockerAccessibilityService.kt # Bloqueo via Accessibility Events (sin root)
│   │   ├── LightweightMonitorService.kt    # Monitor ligero (LifecycleService, broadcast)
│   │   ├── RealisticAppBlocker.kt          # Bloqueo persistente de navegadores/apps sociales
│   │   ├── SafeBrowsingService.kt          # Lanza SafeBrowserActivity como servicio
│   │   ├── ScheduleManager.kt              # Control de horarios por días con AlarmManager
│   │   └── LogCleanupWorker.kt             # Limpieza periódica de logs (WorkManager)
│   ├── ui/                                  # 🎨 Interfaz de Usuario (Jetpack Compose)
│   │   ├── SafeBrowserActivity.kt          # Navegador seguro con WebView
│   │   ├── SafeBrowserViewModel.kt         # ViewModel dedicado para el navegador
│   │   ├── AppBlockedActivity.kt           # Pantalla de bloqueo encima de la app (no escapable)
│   │   ├── PactScreen.kt                   # Pacto Digital Familiar (buzón hijo + responder padre)
│   │   ├── PinLockScreen.kt                # Verificación de PIN parental (Compose)
│   │   ├── StreakWidget.kt                 # Widget de racha diaria con badges y animaciones
│   │   ├── PremiumPurchaseScreen.kt        # Pantalla de compra premium (14,99 € vitalicio)
│   │   ├── PremiumGate.kt                  # Componente guard para features premium
│   │   ├── FreeTrialBanner.kt              # Banner con días restantes de prueba
│   │   ├── FreeTrialExpiredDialog.kt       # Diálogo al expirar el periodo de prueba
│   │   ├── ParentalControlScreen.kt        # Configuración parental
│   │   ├── CustomFiltersScreen.kt          # Gestión de filtros personalizados
│   │   ├── StatisticsScreen.kt             # Estadísticas y logs
│   │   ├── SettingsScreen.kt               # Configuración general
│   │   └── theme/                          # Material Design 3
│   └── viewmodel/                           # 📊 ViewModels (MVVM)
│       ├── MainViewModel.kt
│       ├── ParentalViewModel.kt
│       └── StatsViewModel.kt
├── AndroidManifest.xml                      # Permisos y servicios
└── build.gradle.kts                         # Configuración Gradle
```

### 📦 Capa de Datos (`data/`)

- **Room Database v4**: Base de datos local SQLite con TypeConverters
- **Repository Pattern**: `GuardianRepository` centraliza el acceso a todos los DAOs
- **DataStore**: Configuración persistente asíncrona (reemplaza SharedPreferences)
- **Entities**: `UserProfileEntity`, `BlockedSiteEntity`, `DnsLogEntity`, `CustomFilterEntity`, `SensitiveAppEntity`, `PetitionEntity`, `StatisticEntity`
- **DAOs**: Interfaces con queries SQL y Flows reactivos para actualizaciones en tiempo real

### ⚙️ Capa de Servicios (`service/`)

- **DnsFilterService**: VPN Service que configura DNS seguros (CleanBrowsing) sin procesar paquetes
- **LocalBlocklist**: Bloqueo local hardcoded de redes sociales y contenido adulto
- **AppMonitorService**: Foreground Service persistente con notificación permanente
- **UsageStatsMonitor**: Detecta app foreground cada 2 s y redirige al navegador seguro
- **AppBlockerAccessibilityService**: Bloqueo en tiempo real vía `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`, sin root
- **LightweightMonitorService**: `LifecycleService` ligero con receptor de broadcasts para detección de apps
- **RealisticAppBlocker**: Service dedicado con listas hardcoded de navegadores y apps sociales a bloquear
- **SafeBrowsingService**: Lanza `SafeBrowserActivity` directamente al iniciarse
- **ScheduleManager**: Control de horarios con `AlarmManager` por días de la semana y franjas horarias
- **LogCleanupWorker**: `CoroutineWorker` de WorkManager para limpieza periódica de logs

### 🔐 Capa de Seguridad (`security/`)

- **SecurityHelper**: Guarda y verifica el PIN parental con `EncryptedSharedPreferences` (AES256-GCM) + AndroidKeyStore
- **DeviceAdminHelper**: Registra la app como Device Admin para impedir desinstalación sin PIN
- **GuardianDeviceAdminReceiver**: `DeviceAdminReceiver` que gestiona los eventos de administración del dispositivo

### 💳 Capa de Facturación (`billing/`)

- **BillingManager**: Integración con Google Play Billing Library 6+; pago único vitalicio `premium_guardianos` (14,99 €); restaura el estado premium tras reinstalación vía `queryPurchases()`
- **FreeTierLimits**: Define límites del plan gratuito — historial máximo 48 h, sin perfiles múltiples, sin horarios personalizados, sin filtros propios, sin alertas push

### 🎨 Capa de UI (`ui/`)

- **Jetpack Compose**: UI declarativa moderna
- **Material Design 3**: Theming dinámico
- **Navigation Component**: Navegación entre pantallas
- **SafeBrowserActivity + SafeBrowserViewModel**: WebView con bloqueo en URL loading, ahora con ViewModel desacoplado
- **AppBlockedActivity**: Pantalla full-screen que se superpone a cualquier app bloqueada; no puede cerrarse sin PIN parental o mediante Pacto Digital
- **PactScreen**: Dos pestañas — "Mi buzón" para el menor (enviar peticiones) y "Responder" para el padre (aprobar/rechazar con PIN)
- **PinLockScreen**: Pantalla Compose de verificación de PIN para acceder a zona parental
- **StreakWidget**: Widget de racha de días consecutivos "limpios" con badges, animación de pulso y récord personal
- **PremiumPurchaseScreen / PremiumGate**: Pantalla y guards de paywall para features premium
- **FreeTrialBanner / FreeTrialExpiredDialog**: Componentes para gestión del periodo de prueba

---

## ✨ Características Principales

### 🔒 VPN DNS Transparente

- **Tecnología**: Android VpnService sin captura de tráfico (no usa `addRoute("0.0.0.0", 0)`)
- **DNS Provider**: CleanBrowsing Adult Filter (185.228.168.168 / 185.228.169.168)
- **Filtrado automático**:
  - ✅ Contenido adulto y pornografía
  - ✅ Malware, phishing y scams
  - ✅ **Redes sociales** (TikTok, Facebook, Instagram, Discord, Twitter, Snapchat)
  - ✅ Juegos online y apuestas
  - ✅ Proxies y VPNs
  - ✅ Contenido mixto inapropiado
- **Internet funciona normalmente** para contenido educativo y productivo
- **No requiere root** ni permisos especiales
- **Bloqueo local adicional**: Lista hardcoded de redes sociales como respaldo

### 🌐 Navegador Seguro

- **WebView integrado** con bloqueo en tiempo real antes de cargar URLs
- **Doble capa de protección**: DNS filtering + bloqueo local forzado
- **Verificación de horario** automática antes de cargar cualquier página
- **Filtros personalizados**: Sistema de blacklist/whitelist funcional
- **Página de bloqueo visual** con iconos dinámicos (🛡️ restricción, ⏰ horario)
- **Historial de navegación** guardado localmente
- **Notificaciones por bloqueo** con categorización automática

### ⏰ Control de Horarios

- **Horario permitido configurable** (ej: 09:00 - 20:00)
- **Bloqueo automático fuera del horario** establecido
- **Soporte para horarios cruzando medianoche** (ej: 22:00 - 08:00)
- **Verificación en tiempo real** en cada navegación
- **Notificación al usuario** cuando se bloquea por horario
- **Sin modo bypass**: Control estricto aplicado

### 👤 Perfiles de Usuario

- **Múltiples perfiles** para diferentes menores
- **Configuración por edad** (0-7, 8-12, 13-15, 16-17)
- **Niveles de restricción**: LOW, MEDIUM, HIGH, STRICT
- **PIN parental** para proteger configuración (hash SHA-256)
- **Perfil activo** aplicado en tiempo real
- **Configuración granular**: Por categoría (adulto, social, gaming, etc.)

### 🔔 Sistema de Notificaciones

- **Sitios web bloqueados**: Notificación con categoría automática
  - Red Social bloqueada
  - Contenido Adulto bloqueado
  - Apuestas bloqueadas
  - Horario no permitido
- **Apps bloqueadas**: Cuando se redirige un navegador externo (Chrome, Brave, Firefox)
- **Prioridad ALTA** para alertas inmediatas
- **Auto-cancelables** al tocarlas
- **Canales separados**: "Sitios Bloqueados" y "Apps Bloqueadas"

### 📊 Monitoreo de Apps (Triple Capa)

- **UsageStats API** (principal): Detecta app foreground cada 2 s desde `UsageStatsMonitor`
- **AccessibilityService** (refuerzo): `AppBlockerAccessibilityService` reacciona a `TYPE_WINDOW_STATE_CHANGED` en tiempo real sin permisos root
- **LightweightMonitorService** (respaldo): `LifecycleService` ligero activo incluso cuando `UsageStats` no está disponible
- **Detección de navegadores** externos (Chrome, Brave, Firefox, Edge, Opera, Samsung Browser, UC, Mi Browser)
- **Detección de apps sociales**: Facebook, Instagram, TikTok, Snapchat, Twitter, Reddit, WhatsApp, Telegram, Discord
- **Redirección automática** al navegador seguro o a `AppBlockedActivity`
- **Foreground Service persistente** (resistente a task killers OPPO/Motorola)

### 🤝 Pacto Digital Familiar

- **Sistema de peticiones local**: El menor solicita permisos sin salir del dispositivo
  - ⏱️ `TIME_EXTENSION` — Solicitar más tiempo de pantalla
  - 📱 `APP_UNLOCK` — Pedir desbloqueo temporal de una app
  - 🌐 `SITE_UNLOCK` — Pedir desbloqueo de un sitio específico
- **Buzón del menor**: Historial de peticiones enviadas y sus estados
- **Panel del padre**: Responder con PIN → aprobar (con nota) o rechazar (con motivo)
- **Estados**: `PENDING` → `APPROVED` / `REJECTED`
- **100% offline**: Sin notificaciones push ni servidores externos

### 🏆 Racha Diaria (Streak)

- **Días consecutivos "limpios"** sin intentos de acceso a contenido bloqueado
- **Badges por hitos**: 7, 14, 30, 60, 90 días
- **Récord personal** guardado en `UserProfileEntity`
- **Animación de pulso** cuando la racha está activa
- **Widget** embebido en el dashboard principal

### 💳 Planes y Billing

- **Plan Gratuito**: VPN DNS activa siempre, historial últimas 48 h en todos los módulos, navegador seguro (10 URLs historial), monitoreo básico
- **Plan Premium** (14,99 € — pago único vitalicio, sin suscripción):
  - ✅ Control parental completo y perfiles múltiples
  - ✅ Horarios personalizados por días de la semana
  - ✅ Filtros personalizados (blacklist/whitelist ilimitados)
  - ✅ Historial extendido (30 días)
  - ✅ Exportación de historial a CSV
  - ✅ Alertas push premium
  - ✅ Pacto Digital Familiar completo
  - ✅ Periodo de prueba gratuito incluido
- **Google Play Billing Library 6+**: `BillingManager` restaura el estado premium automáticamente tras reinstalación
- **PremiumGate**: Componente Compose que muestra el diálogo de paywall cuando se accede a features premium desde el plan gratuito

### 🔐 Seguridad Avanzada

- ✅ **PIN parental cifrado**: `EncryptedSharedPreferences` con AES256-GCM + AndroidKeyStore
- ✅ **Anti-desinstalación**: Device Admin vía `DevicePolicyManager`; un menor no puede eliminar la app sin el PIN del padre
- ✅ **AppBlockedActivity inescapable**: La pantalla de bloqueo impide volver a la app bloqueada sin PIN o petición aprobada
- ✅ **100% local**: Sin analytics, sin almacenamiento en la nube
- ✅ **Open Source** y auditable

### 📊 Estadísticas Mejoradas

- **StatisticEntity**: Almacena métricas diarias de uso y bloqueos por categoría
- **DomainStat**: Agrupación por dominio más bloqueado para gráficas de top-10
- **Historial**: 48 h en plan gratuito → 30 días en premium
- **Exportación CSV**: Disponible en plan premium

---

## 🚀 Instalación y Compilación

### Requisitos Previos

- **Android Studio** Hedgehog (2023.1.1) o superior
- **JDK 17** o superior
- **Gradle 8.6+** (incluido en el proyecto)
- **Android SDK 34** (Android 14)
- **Kotlin 1.9.0+**

### Clonar el Repositorio

```bash
git clone https://github.com/systemavworks/guardianos-shield.git
cd guardianos-shield
```

### Compilar con Gradle

```bash
# Compilar APK de debug
./gradlew assembleDebug

# Compilar APK de release (requiere keystore)
./gradlew assembleRelease

# Ejecutar tests unitarios
./gradlew test

# Limpiar build
./gradlew clean
```

### Instalación en Dispositivo

```bash
# Instalar APK de debug via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# O arrastrar el APK directamente al dispositivo
```

**Ubicación del APK**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Uso de la Aplicación

### 1️⃣ Primera Configuración

1. **Instalar** la app y abrirla
2. **Crear perfil** del menor (nombre, edad, nivel de restricción)
3. **Configurar horario** permitido (opcional)
4. **Activar VPN**: Botón en pantalla principal
5. **Conceder permisos**:
   - VPN (Android pedirá confirmación)
   - UsageStats (para monitoreo de apps)
   - Accesibilidad: `AppBlockerAccessibilityService` en Ajustes > Accesibilidad (bloqueo reforzado sin root)
   - Device Admin (opcional): para impedir que el menor desinstale la app
   - Notificaciones (Android 13+)

### 2️⃣ Activar Protección VPN

- Toca el botón **"Activar Protección"** en la pantalla principal
- Android pedirá permiso para establecer VPN
- Una vez activo, verás notificación persistente: **"DNS Seguro Activado"**
- Internet funcionará normalmente, pero contenido bloqueado será inaccesible

### 3️⃣ Navegador Seguro

- **Abrir**: Toca el icono del navegador en la pantalla principal
- **Navegar**: Introduce URLs o búsquedas en Google
- **Bloqueos**: Verás página de bloqueo con razón (horario, categoría, etc.)
- **Historial**: Botón de historial para ver sitios visitados

### 4️⃣ Control Parental

- **Acceder**: Menú > Control Parental
- **Configurar**:
  - Horario permitido (activar y establecer inicio/fin)
  - Nivel de restricción (LOW, MEDIUM, HIGH, STRICT)
  - Categorías a bloquear (adulto, gambling, social, gaming)
- **Guardar**: Los cambios se aplican inmediatamente

### 5️⃣ Filtros Personalizados

- **Acceder**: Menú > Filtros Personalizados
- **Agregar a blacklist**: Introduce dominio (ej: `tiktok.com`) y presiona ➕
- **Agregar a whitelist**: Cambia a whitelist y agrega dominios permitidos
- **Eliminar**: Desliza para eliminar filtros
- **Se aplican instantáneamente** en el navegador seguro

### 6️⃣ Estadísticas

- **Ver logs**: Menú > Estadísticas
- **Sitios bloqueados hoy**: Contador en tiempo real
- **Historial semanal**: Gráfica de bloqueos
- **Exportar**: Botón para exportar logs a CSV

### 7️⃣ Monitoreo de Apps

- **Activar**: Menú > Configuración > Monitoreo de Apps
- **Conceder UsageStats**: Android te llevará a configuración
- **Conceder Accesibilidad** (refuerzo): Ajustes > Accesibilidad > GuardianOS Shield
- **Funcionamiento**:
  - Si el menor abre Chrome/Brave/Firefox → se redirige a navegador seguro
  - `AppBlockerAccessibilityService` actúa en tiempo real (sin root)
  - `LightweightMonitorService` como respaldo adicional
  - Notificación: "App bloqueada - Chrome"
  - Foreground Service mantiene monitoreo activo

### 8️⃣ Pacto Digital Familiar (Nuevo)

- **Acceder**: Menú > Pacto Digital
- **El menor** puede enviar peticiones desde su pestaña (sin PIN)
  - Más tiempo, desbloquear app, desbloquear sitio
- **El padre** abre la pestaña "Responder", introduce el PIN y aprueba o rechaza
- **Sin internet**: Todo queda en el dispositivo

### 9️⃣ Anti-desinstalación (Device Admin)

- **Activar**: Menú > Seguridad > Activar Device Admin
- Android mostrará un diálogo de confirmación del sistema
- Una vez activo, el menor **no puede desinstalar** la app sin el PIN parental
- **Desactivar**: El padre introduce el PIN en Seguridad > Desactivar Device Admin

### 🔟 Plan Premium

- **Ver plan actual**: Banner en la pantalla principal o Menú > Premium
- **Comprar (14,99 € — pago único)**: Pantalla Premium > "Activar Premium"
- Google Play gestiona el pago; el estado se restaura automáticamente tras reinstalación
- Las features premium (horarios, filtros propios, perfiles múltiples) se desbloquean inmediatamente

---

## 🔧 Funcionamiento Técnico

### VPN DNS Transparente

```kotlin
// DnsFilterService.kt - Configuración VPN simplificada
Builder()
    .setSession("GuardianOS Shield")
    .setMtu(1500)
    .addAddress("10.0.0.2", 32)                    // IP virtual del túnel
    .addDnsServer("185.228.168.168")               // DNS CleanBrowsing Primary
    .addDnsServer("185.228.169.168")               // DNS CleanBrowsing Secondary
    .addDisallowedApplication(packageName)         // Evitar bucles infinitos
    // ⚠️ CRÍTICO: NO usar addRoute() = tráfico fluye normal, solo DNS filtrado
    .establish()
```

**Arquitectura correcta (191 líneas vs 700+ anteriores)**:

- ✅ **Sin captura de paquetes**: No usa `addRoute("0.0.0.0", 0)` que bloqueaba internet
- ✅ **Solo DNS**: Android resuelve DNS usando servidores CleanBrowsing configurados
- ✅ **CleanBrowsing hace el filtrado** en sus servidores (Adult Filter = más restrictivo)
- ✅ **Internet funciona normalmente**: Todo el tráfico fluye sin interceptación
- ✅ **Patrón estándar**: Mismo que usan apps como 1.1.1.1, DNS66, NextDNS

### Bloqueo Local en SafeBrowserActivity (Doble Capa)

```kotlin
// Verificar dominio ANTES de cargar - crítico para redes sociales
private suspend fun isDomainBlocked(domain: String): Boolean {
    // 1️⃣ Verificar horario permitido (UserProfileEntity.isWithinAllowedTime())
    val profile = repository.getActiveProfile()
    if (profile != null && !profile.isWithinAllowedTime()) {
        showBlockNotification("Horario no permitido")
        return true
    }

    // 2️⃣ Lista local hardcoded de redes sociales (respaldo si DNS falla)
    val socialMediaDomains = setOf(
        "facebook.com", "instagram.com", "tiktok.com", "twitter.com", 
        "discord.com", "snapchat.com", "reddit.com", etc.
    )
    if (socialMediaDomains.any { domain.equals(it, ignoreCase = true) || 
                                   domain.endsWith(".$it") }) {
        showBlockNotification("Red Social bloqueada: $domain")
        return true
    }

    // 3️⃣ Filtros personalizados del usuario (blacklist isActive=true)
    val filters = repository.getAllCustomFilters()
    if (filters.any { it.isActive && domain.contains(it.domain, ignoreCase = true) }) {
        return true
    }

    // 4️⃣ Keywords adulto/gambling
    val adultKeywords = listOf("porn", "xxx", "adult", "sex", "casino", "bet")
    if (adultKeywords.any { domain.contains(it, ignoreCase = true) }) {
        return true
    }

    return false
}
```

### Monitoreo de Apps con UsageStats

```kotlin
// UsageStatsMonitor.kt - Detectar app foreground cada 2 segundos
private suspend fun monitorForegroundApp() {
    val statsManager = getSystemService(UsageStatsManager::class.java)
    val stats = statsManager.queryUsageStats(INTERVAL_DAILY, startTime, endTime)
    val foregroundApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName

    // Si es navegador externo → redirigir + notificar
    if (foregroundApp in browserPackages && foregroundApp != "com.guardianos.shield") {
        showAppBlockedNotification(getAppLabel(foregroundApp))
        startActivity(Intent(context, SafeBrowserActivity::class.java))
    }
}
```

### Sistema de Notificaciones

```kotlin
// Notificación automática al bloquear
private fun showBlockNotification(domain: String) {
    val category = when {
        domain.contains("facebook") || domain.contains("tiktok") -> "Red Social"
        domain.contains("porn") || domain.contains("xxx") -> "Contenido Adulto"
        domain.contains("casino") -> "Apuestas"
        else -> "Sitio Restringido"
    }

    NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle("🚫 Sitio bloqueado")
        .setContentText("$category: $domain")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
}
```

### Bloqueo de Apps con AccessibilityService

```kotlin
// AppBlockerAccessibilityService.kt — detecta cambios de ventana en tiempo real
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val packageName = event.packageName?.toString() ?: return

    scope.launch {
        val ahora = System.currentTimeMillis()
        if (packageName == ultimaAppBloqueada &&
            ahora - tiempoUltimoBloqueo < INTERVALO_MIN_BLOQUEO) return@launch

        val apps = repository.getAllSensitiveApps().firstOrNull() ?: return@launch
        val matching = apps.firstOrNull { it.packageName == packageName } ?: return@launch

        val profile = repository.getActiveProfile() ?: return@launch
        if (!profile.isWithinAllowedTime()) {
            ultimaAppBloqueada = packageName
            tiempoUltimoBloqueo = ahora
            val intent = AppBlockedActivity.createIntent(this@AppBlockerAccessibilityService, packageName)
            startActivity(intent)
        }
    }
}
```

### Control de Horarios con ScheduleManager

```kotlin
// ScheduleManager.kt — franjas por día de la semana con AlarmManager
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

### Anti-desinstalación con DeviceAdmin

```kotlin
// DeviceAdminHelper.kt
fun solicitarActivacion(context: Context) {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "GuardianOS Shield necesita este permiso para impedir que un menor " +
            "desinstale la app de control parental sin el PIN del padre/madre.")
    }
    context.startActivity(intent)
}
```

### PIN Cifrado con EncryptedSharedPreferences

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

## 🛠️ Desarrollo y Debugging

### Compilar versiones

```bash
# Debug (con logs)
./gradlew assembleDebug

# Release (optimizada y firmada)
./gradlew assembleRelease

# Tests unitarios
./gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Limpiar y recompilar
./gradlew clean && ./gradlew assembleDebug
```

### Comandos de debugging útiles

```bash
# VPN y DNS filtering
adb logcat | grep GuardianVPN

# Monitoreo de apps (UsageStats)
adb logcat | grep UsageStatsMonitor

# AccessibilityService (bloqueo reforzado)
adb logcat | grep AppBlockerA11y

# Monitor ligero
adb logcat | grep LightweightMonitor

# Navegador seguro
adb logcat | grep SafeBrowser

# ScheduleManager (horarios)
adb logcat | grep ScheduleManager

# Billing / Premium
adb logcat | grep BillingManager

# Device Admin
adb logcat | grep DeviceAdmin

# Todos los logs de la app
adb logcat | grep "com.guardianos.shield"

# Verificar permisos concedidos
adb shell dumpsys package com.guardianos.shield | grep permission

# Verificar estado de la VPN
adb shell dumpsys connectivity | grep VPN

# Verificar DNS activo
adb shell dumpsys connectivity | grep "DNS servers"

# Forzar detener app (reiniciar servicios)
adb shell am force-stop com.guardianos.shield

# Instalar y ejecutar
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.guardianos.shield/.MainActivity
```

### Debugging común

1. **VPN no se activa**: 
   
   - Verificar que no haya otra VPN activa
   - Desactivar DNS privado en Ajustes > Red
   - Conceder permiso VPN cuando Android lo pida

2. **Sitios no se bloquean**: 
   
   - Verificar logs DNS: `adb logcat | grep GuardianVPN` — debe mostrar 185.228.168.168
   - Verificar lista local en `LocalBlocklist.kt` y `SafeBrowserActivity.kt`

3. **Monitoreo no funciona**: 
   
   - Conceder UsageStats: Ajustes > Apps > Acceso especial > Acceso a uso
   - Comprobar que `AppBlockerAccessibilityService` está habilitado en Accesibilidad
   - Desactivar optimización de batería para la app

4. **Notificaciones no aparecen**: 
   
   - Android 13+ requiere permiso POST_NOTIFICATIONS
   - Verificar canales de notificación creados
   - Revisar configuración de la app en Ajustes > Notificaciones

5. **Horarios no funcionan**:
   
   - Verificar perfil activo con `repository.getActiveProfile()`
   - Confirmar `scheduleEnabled = true` y franjas en `ScheduleManager`
   - Ver logs: `adb logcat | grep ScheduleManager`

6. **Device Admin no se activa**:
   
   - Llamar a `DeviceAdminHelper.solicitarActivacion(context)` desde una Activity
   - Verificar que `GuardianDeviceAdminReceiver` está declarado en `AndroidManifest.xml`
   - Comprobar con: `adb shell dpm list-owners`

7. **Premium no se restaura tras reinstalación**:
   
   - Verificar conexión a Play Store
   - Comprobar logs: `adb logcat | grep BillingManager`
   - `BillingManager.queryPurchases()` consulta las compras existentes en segundo plano

---

## 📄 Licencia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
Copyright © 2026 Victor Shift Lara - GuardianOS Project
📍 Andalucía, España | https://guardianos.es

See the LICENSE file for details.
⚠️ Aviso de Marca Registrada
Esta licencia cubre únicamente el código fuente.
Las marcas comerciales "GuardianOS", "GuardianOS Shield", "guardianos", el logotipo guardianos_shield_logo.png, y cualquier asset visual asociado son propiedad de sus respectivos dueños y NO están cubiertas por esta licencia de código abierto.
El uso no autorizado de estas marcas, nombres o assets visuales puede constituir una infracción de propiedad industrial. Para solicitar permiso de uso de marca, contacta a: info@guardianos.es

---

## 👨‍💻 Autor

**Victor Shift Lara**  
📍 Andalucía, España  
🌐 Web: [https://guardianos.es](https://guardianos.es)  
📧 Email: [info@guardianos.es](mailto:info@guardianos.es)  
💼 GitHub: [@systemavworks](https://github.com/systemavworks/guardianos-shield)

---

## 🙏 Agradecimientos

- **CleanBrowsing** por su servicio DNS de filtrado público y gratuito
- **Android Open Source Project** por VpnService API y UsageStats API
- **Google Jetpack** por las librerías modernas (Compose, Room, Navigation)
- **Material Design 3** por el sistema de diseño
- **Cloudflare** por los servidores DNS alternativos

---

## 📞 Soporte

¿Problemas o preguntas?

- 🐛 **Issues**: [GitHub Issues](https://github.com/systemavworks/guardianos-shield/issues)
- 📧 **Email**: info@guardianos.es
- 🌐 **Web**: [https://guardianos.es/soporte](https://guardianos.es/soporte)
- 📖 **Wiki**: [GitHub Wiki](https://github.com/systemavworks/guardianos-shield/wiki)

---

## 📝 Roadmap

### ✅ Implementado

- [x] VPN DNS transparente con CleanBrowsing Adult Filter
- [x] Navegador seguro (WebView + doble capa de bloqueo)
- [x] Control de horarios con soporte cruce de medianoche y franjas por día
- [x] Monitoreo triple de apps (UsageStats + AccessibilityService + LightweightMonitor)
- [x] Bloqueo de app inescapable (`AppBlockedActivity`)
- [x] Pacto Digital Familiar (peticiones hijo → padre, todo local)
- [x] Racha diaria con badges y animaciones (`StreakWidget`)
- [x] PIN parental cifrado con AES256-GCM + AndroidKeyStore
- [x] Anti-desinstalación via Device Admin
- [x] Plan Premium con Google Play Billing 6+ (14,99 € vitalicio)
- [x] Plan Gratuito con límites de 48 h de historial
- [x] Estadísticas ampliadas (`StatisticEntity`, `DomainStat`)
- [x] Room Database v4 con migraciones

### 🔜 Próximas versiones

- [ ] Dashboard web para padres en guardianos.es
- [ ] Exportar configuración completa (backup/restore)
- [ ] Modo kiosk para bloquear salida de la app
- [ ] Soporte para múltiples dispositivos sincronizados (opcional)
- [ ] Integración con Google Family Link
- [ ] App companion para smartwatches (alertas a padres)
- [ ] Filtrado de contenido YouTube específico
- [ ] Bloqueo de compras in-app
- [ ] Soporte para tablets y ChromeOS
- [ ] Exportación de informes en PDF
- [ ] Widget de estadísticas para pantalla de inicio (Android AppWidget)

---

**Hecho con ❤️ en Andalucía**  
*Protegiendo a nuestros pequeños en el mundo digital*
