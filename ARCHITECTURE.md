# 🏗️ GuardianOS Shield - Documentación de Arquitectura

> **Documento interno de arquitectura técnica**  
> 📍 Proyecto privado • GuardianOS Shield • Andalucía, España  
> 👨‍💻 Autor: Victor Shift Lara | ✉️ info@guardianos.es  
> 📅 Última actualización: Febrero 2026

---

## 📋 Tabla de Contenidos

1. [Visión General](#-visión-general)
2. [Principios de Diseño](#-principios-de-diseño)
3. [Arquitectura en Capas](#-arquitectura-en-capas)
4. [Componentes Críticos](#-componentes-críticos)
5. [Flujos de Datos Clave](#-flujos-de-datos-clave)
6. [Seguridad y Privacidad](#-seguridad-y-privacidad)
7. [Decisiones Técnicas Clave](#-decisiones-técnicas-clave)
8. [Testing y Calidad](#-testing-y-calidad)
9. [Guías de Desarrollo](#-guías-de-desarrollo)
10. [Roadmap Técnico](#-roadmap-técnico)

---

## 👁️ Visión General

**GuardianOS Shield** es una aplicación Android de control parental que protege a menores mediante filtrado DNS transparente, monitoreo de apps y controles parentales granulares — **100% local, sin telemetría, deGoogled**.

### 🎯 Objetivos técnicos

| Objetivo | Implementación |
|----------|---------------|
| **Privacidad real** | Sin analytics, sin cloud, todo en dispositivo |
| **Efectividad** | Triple capa de bloqueo: DNS + WebView + AccessibilityService |
| **Rendimiento** | Servicios ligeros, sin impacto en batería (<5% diario) |
| **Mantenibilidad** | Arquitectura limpia, MVVM, inyección de dependencias implícita |
| **Compatibilidad** | Android 12-15+, ROMs personalizadas (LineageOS, /e/OS) |

### 📐 Diagrama de alto nivel
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

## 🧭 Principios de Diseño

### 1. Privacy by Design
- **Zero telemetry**: Ningún dato sale del dispositivo
- **Minimal permissions**: Solo permisos estrictamente necesarios
- **Local-first**: Base de datos Room + DataStore, sin sincronización cloud

### 2. Defense in Depth (Defensa en profundidad)
```

Capa 1: DNS Filtering (CleanBrowsing) → Bloqueo a nivel de resolución
Capa 2: Local Blocklist (hardcoded) → Bloqueo en WebView antes de cargar
Capa 3: AccessibilityService → Bloqueo en tiempo real de apps sociales

```
### 3. Fail-Safe Defaults
- Si el servicio VPN falla → notificar al usuario, NO silenciar
- Si no se puede verificar horario → aplicar restricción por defecto (HIGH)
- Si el PIN no se puede cifrar → bloquear acceso a zona parental

### 4. Separation of Concerns
- Cada módulo tiene responsabilidad única y clara
- Comunicación vía interfaces (Repository pattern), no acoplamiento directo

---

## 🏗️ Arquitectura en Capas

### 📦 Capa de Datos (`data/`)
```

data/
├── GuardianDatabase.kt          # Room DB v4 • Singleton • Migraciones automáticas
├── GuardianRepository.kt        # Fuente única de verdad • Coordina DAOs + DataStore
├── SettingsDataStore.kt         # Preferencias asíncronas • Reemplaza SharedPreferences
│
├── entities/                    # Modelos de persistencia
│   ├── UserProfileEntity.kt     # Perfiles de menores • Edad, nivel, horarios
│   ├── CustomFilterEntity.kt    # Blacklist/Whitelist personalizada
│   ├── DnsLogEntity.kt          # Logs de consultas DNS bloqueadas
│   ├── BlockedSiteEntity.kt     # Historial de sitios bloqueados
│   ├── SensitiveAppEntity.kt    # Apps monitoreadas (redes sociales, navegadores)
│   ├── PetitionEntity.kt        # Pacto Digital: peticiones hijo→padre
│   ├── StatisticEntity.kt       # Métricas diarias para gráficas
│   └── DomainStat.kt            # DTO para agrupación por dominio
│
└── dao/                         # Interfaces Room con queries tipadas
    ├── UserProfileDao.kt
    ├── CustomFilterDao.kt
    ├── DnsLogDao.kt
    └── ...

```
**Patrones aplicados**:
- ✅ Repository Pattern: `GuardianRepository` abstrae fuentes de datos
- ✅ Flow reactive: DAOs retornan `Flow<List<T>>` para UI reactiva
- ✅ TypeConverters: Para enums, horarios y objetos complejos en Room

### ⚙️ Capa de Servicios (`service/`)
```

service/
├── DnsFilterService.kt          # VPN Service • DNS transparente • CleanBrowsing
├── LocalBlocklist.kt            # Lista hardcoded de dominios bloqueados
│
├── AppMonitorService.kt         # Foreground Service • Monitor persistente
├── UsageStatsMonitor.kt         # Detección de app foreground cada 2s
├── AppBlockerAccessibilityService.kt # Bloqueo en tiempo real sin root
├── LightweightMonitorService.kt # Monitor ligero de respaldo (LifecycleService)
├── RealisticAppBlocker.kt       # Lógica de bloqueo de navegadores/apps sociales
│
├── SafeBrowsingService.kt       # Lanza SafeBrowserActivity como servicio
├── ScheduleManager.kt           # Control de horarios con AlarmManager
└── LogCleanupWorker.kt          # Limpieza periódica con WorkManager

```
#### 🔑 DnsFilterService: Implementación crítica

```kotlin
// ⚠️ DECISIÓN ARQUITECTÓNICA: NO usar addRoute("0.0.0.0", 0)
// Esto evitaría capturar TODO el tráfico (requiere procesamiento de paquetes)
// En su lugar: solo configurar DNS → Android resuelve con CleanBrowsing

Builder()
    .setSession("GuardianOS Shield")
    .setMtu(1500)
    .addAddress("10.0.0.2", 32)              // IP virtual del túnel VPN
    .addDnsServer("185.228.168.168")         // CleanBrowsing Primary (Adult Filter)
    .addDnsServer("185.228.169.168")         // CleanBrowsing Secondary
    .addDisallowedApplication(packageName)   // Evitar bucle: app no se filtra a sí misma
    // ❌ NO añadir: .addRoute("0.0.0.0", 0) → eso bloquearía internet
    .establish()
```

**Por qué esta arquitectura**:

- ✅ Internet funciona normal para contenido permitido
- ✅ CleanBrowsing hace el filtrado en sus servidores (sin procesamiento local pesado)
- ✅ Patrón usado por apps como 1.1.1.1, DNS66, NextDNS
- ✅ Compatible con Android 12+ sin permisos root

#### 🔑 Triple Capa de Monitoreo de Apps

```
┌─────────────────────────────────┐
│ 1. UsageStatsMonitor (Principal)│
│ • Polling cada 2s • Bajo consumo│
│ • Detecta app foreground        │
└────────┬────────────────────────┘
         │
┌────────▼────────┐  ┌───────────────────────┐
│ 2. Accessibility│  │ 3. LightweightMonitor │
│    Service      │  │    (Respaldo)         │
│ • Eventos en    │  │ • LifecycleService    │
│   tiempo real   │  │ • Broadcast receiver  │
│ • Sin root      │  │ • Activo si UsageStats│
│                 │  │   falla               │
└─────────────────┘  └───────────────────────┘
```

**Ventaja**: Si una capa falla (ej: UsageStats desactivado por optimización de batería), las otras dos mantienen la protección.

### 🔐 Capa de Seguridad (`security/`)

```
security/
├── SecurityHelper.kt            # PIN cifrado • EncryptedSharedPreferences • AES256-GCM
├── DeviceAdminHelper.kt         # Anti-desinstalación • DevicePolicyManager
└── GuardianDeviceAdminReceiver.kt # BroadcastReceiver para eventos de admin
```

#### 🔑 Cifrado de PIN parental

```kotlin
// SecurityHelper.kt - Flujo seguro de almacenamiento de PIN
fun savePin(context: Context, profileId: Int, pin: String): Boolean {
    // 1. Generar MasterKey con AndroidKeyStore (hardware-backed si disponible)
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 2. Crear EncryptedSharedPreferences con cifrado automático
    val prefs = EncryptedSharedPreferences.create(
        context,
        "guardian_security_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 3. Hash del PIN + almacenamiento cifrado
    val hashedPin = hashPin(pin) // SHA-256 + salt único por perfil
    prefs.edit().putString("pin_$profileId", hashedPin).apply()

    return true
}
```

**Decisiones de seguridad**:

- ✅ **Nunca almacenar PIN en texto plano**: Siempre hash + cifrado
- ✅ **Salt único por perfil**: Evita ataques de diccionario cruzado
- ✅ **AndroidKeyStore**: Claves protegidas por hardware (si el dispositivo lo soporta)
- ✅ **EncryptedSharedPreferences**: Cifrado transparente, sin gestión manual de IVs

### 💳 Capa de Facturación (`billing/`)

```
billing/
├── BillingManager.kt            # Google Play Billing Library 6+ • Pago único premium
└── FreeTierLimits.kt            # Definición de límites del plan gratuito
```

#### 🔑 Gestión de estado premium

```kotlin
// BillingManager.kt - Patrón de restauración de compras
class BillingManager(private val context: Context) {

    // Consulta compras existentes al iniciar (restaura premium tras reinstalación)
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

    // Listener para compras en tiempo real
    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains("premium_guardianos")) {
                    // Activar features premium + acknowledge compra
                    acknowledgePurchase(purchase.purchaseToken)
                    notifyPremiumActivated()
                }
            }
        }
    }
}
```

**Consideraciones**:

- ✅ **Pago único, no suscripción**: `premium_guardianos` = 14,99 € vitalicio
- ✅ **Restauración automática**: `queryPurchasesAsync()` al iniciar la app
- ✅ **Acknowledgement obligatorio**: Google requiere confirmar recepción en <3 días
- ✅ **Sandbox testing**: Usar cuentas de test en Google Play Console

### 🎨 Capa de UI (`ui/` + `viewmodel/`)

```
ui/
├── MainActivity.kt              # Entry point • Jetpack Compose • Navigation Host
├── SafeBrowserActivity.kt       # WebView seguro • Bloqueo en URL loading
├── AppBlockedActivity.kt        # Pantalla inescapable • Full-screen • Sin back button
├── PactScreen.kt                # Pacto Digital • Dos pestañas: hijo/padre
├── PinLockScreen.kt             # Verificación de PIN • Compose • Animaciones
├── StreakWidget.kt              # Widget de racha • Badges • Animación de pulso
│
├── screens/                     # Pantallas principales
│   ├── ParentalControlScreen.kt
│   ├── CustomFiltersScreen.kt
│   ├── StatisticsScreen.kt
│   └── SettingsScreen.kt
│
├── components/                  # Componentes reutilizables
│   ├── PremiumGate.kt           # Guard para features premium
│   ├── FreeTrialBanner.kt       # Banner de prueba gratuita
│   └── ...
│
└── theme/                       # Material Design 3 • Dark mode • Tipografía

viewmodel/
├── MainViewModel.kt             # Estado global • VPN status • Perfil activo
├── ParentalViewModel.kt         # Lógica de control parental • Horarios • Restricciones
└── StatsViewModel.kt            # Procesamiento de estadísticas • Gráficas • Exportación
```

**Patrones UI**:

- ✅ **MVVM**: ViewModels separados, UI observa estados vía `StateFlow`
- ✅ **Unidirectional Data Flow**: Eventos → ViewModel → Estado → UI
- ✅ **Compose best practices**: `remember`, `derivedStateOf`, `LaunchedEffect` para side-effects

---

## 🔄 Flujos de Datos Clave

### 🔒 Flujo: Activación de VPN + Filtrado DNS

```
Usuario → MainActivity → DnsFilterService → Android VpnService → CleanBrowsing DNS

1. Usuario toca "Activar Protección"
2. MainActivity inicia DnsFilterService
3. DnsFilterService construye VpnService.Builder()
4. Configura DNS: 185.228.168.168 / 185.228.169.168
5. VPN establecida, notificación "VPN Activa"
6. Tráfico DNS del dispositivo → CleanBrowsing (filtra consultas)
7. Tráfico HTTP/HTTPS fluye normal (sin interceptación)
```

### 🚫 Flujo: Bloqueo de sitio web en SafeBrowser

```
Usuario introduce URL → SafeBrowserActivity → isDomainBlocked()

Verificaciones paralelas:
1. getActiveProfile()?.isWithinAllowedTime()
2. LocalBlocklist.socialMediaDomains.contains()
3. getCustomFilters().any { domain.contains(it) }

Si alguno bloquea:
→ showBlockNotification()
→ loadUrl("file:///android_asset/blocked.html")
→ Mostrar página de bloqueo con razón

Si todo permitido:
→ webView.loadUrl()
```

### 👨‍👦 Flujo: Pacto Digital (petición hijo → respuesta padre)

```
Hijo (Child UI) → GuardianRepository → createPetition()

1. Hijo crea petición (TIME_EXTENSION, APP_UNLOCK, SITE_UNLOCK)
2. Repository inserta PetitionEntity (status=PENDING)
3. Padre abre PactScreen > pestaña "Responder"
4. Padre verifica PIN con SecurityHelper
5. Si PIN correcto: updatePetition(id, APPROVED/REJECTED)
6. Hijo ve notificación al refrescar
```

---

## 🔐 Seguridad y Privacidad: Decisiones Críticas

### ✅ Lo que SÍ hacemos

| Medida                  | Implementación                                            | Beneficio                                              |
| ----------------------- | --------------------------------------------------------- | ------------------------------------------------------ |
| **Cifrado de PIN**      | EncryptedSharedPreferences + AES256-GCM + AndroidKeyStore | PIN nunca en texto plano, protegido por hardware       |
| **Anti-desinstalación** | DevicePolicyManager + GuardianDeviceAdminReceiver         | Menor no puede eliminar app sin PIN parental           |
| **Zero telemetry**      | Sin Firebase, sin analytics, sin logs remotos             | Privacidad real: nada sale del dispositivo             |
| **DNS seguro**          | CleanBrowsing Adult Filter (185.228.168.168)              | Filtrado a nivel de DNS sin procesamiento local pesado |
| **Permisos mínimos**    | Solo: VPN, UsageStats, Accessibility, PostNotifications   | Menor superficie de ataque, más confianza del usuario  |

### ❌ Lo que NUNCA hacemos

```kotlin
// ❌ NUNCA enviar datos a servidores externos
fun logUserActivity(domain: String) {
    // NO hacer esto:
    // RetrofitClient.send("https://api.guardianos.es/log", ...)

    // ✅ En su lugar, guardar localmente:
    repository.insertDnsLog(DnsLogEntity(domain, timestamp, blocked = true))
}

// ❌ NUNCA almacenar PIN sin cifrar
fun savePinInsecure(pin: String) {
    // NO hacer esto:
    // sharedPreferences.edit().putString("parent_pin", pin).apply()

    // ✅ Usar SecurityHelper con cifrado:
    SecurityHelper.savePin(context, profileId, pin)
}

// ❌ NUNCA interceptar tráfico HTTPS (requiere root / certificado personalizado)
// GuardianOS Shield NO hace MITM, NO inspecciona contenido cifrado
// El filtrado se hace a nivel DNS (resolución de dominios), no de paquetes
```

### 🛡️ Modelo de amenazas

```
Amenaza: Menor intenta desinstalar la app
Mitigación: Device Admin + PIN para desactivar admin

Amenaza: Menor cambia DNS del sistema para evadir filtrado
Mitigación: VPN Service fuerza DNS a CleanBrowsing; cambios manuales no afectan

Amenaza: Menor usa navegador alternativo (Brave, Firefox)
Mitigación: UsageStatsMonitor + AccessibilityService detectan y redirigen a SafeBrowser

Amenaza: Ataque de fuerza bruta al PIN parental
Mitigación: Hash SHA-256 + salt único + límite de intentos (3 fallos = bloqueo 15 min)

Amenaza: Extracción de datos si dispositivo es robado
Mitigación: Datos cifrados con AndroidKeyStore (hardware-backed si disponible)
```

---

## 🎯 Decisiones Técnicas Clave

### 1. ¿Por qué Apache 2.0 y no MIT?

| Criterio                   | Apache 2.0                | MIT         | Elección     |
| -------------------------- | ------------------------- | ----------- | ------------ |
| Protección de patentes     | ✅ Sí (cláusula explícita) | ❌ No        | ✅ Apache 2.0 |
| Requisito de NOTICE        | ✅ Sí (documenta terceros) | ❌ No        | ✅ Apache 2.0 |
| Compatibilidad con F-Droid | ✅ Sí                      | ✅ Sí        | Empate       |
| Claridad en uso de marcas  | ✅ Sección 6 explícita     | ❌ Implícita | ✅ Apache 2.0 |

**Conclusión**: Apache 2.0 ofrece mayor protección legal para un proyecto con marca comercial ("guardianos") y componentes de terceros.

### 2. ¿Por qué CleanBrowsing y no Cloudflare/NextDNS?

| Provider          | Adult Filter           | API gratuita      | Logs              | Elección    |
| ----------------- | ---------------------- | ----------------- | ----------------- | ----------- |
| CleanBrowsing     | ✅ Sí (185.228.168.168) | ✅ Sí              | ❌ Sin logs        | ✅ Elegido   |
| Cloudflare Family | ✅ Sí (1.1.1.3)         | ✅ Sí              | ⚠️ Logs 24h       | Alternativa |
| NextDNS           | ✅ Configurable         | ❌ Requiere cuenta | ✅ Logs detallados | ❌ Rechazado |

**Razón principal**: CleanBrowsing ofrece filtrado adulto estricto sin requerir cuenta ni API key, alineado con nuestro principio "zero configuration, zero telemetry".

### 3. ¿Por qué triple capa de monitoreo de apps?

```
Problema: Android restringe cada vez más el monitoreo en background
Solución: Estrategia de defensa en profundidad

Capa 1: UsageStats API
  ✅ Preciso, oficial, bajo consumo
  ❌ Puede ser desactivado por optimización de batería (especialmente en OPPO/Xiaomi)

Capa 2: AccessibilityService
  ✅ Detecta cambios de ventana en tiempo real, sin root
  ❌ Requiere permiso explícito del usuario, puede ser desactivado manualmente

Capa 3: LightweightMonitorService (LifecycleService)
  ✅ Activo incluso si las otras fallan, muy ligero
  ❌ Menos preciso (polling cada 5-10s)

Resultado: Si una capa falla, las otras dos mantienen la protección.
```

### 4. ¿Por qué no usar addRoute("0.0.0.0", 0) en la VPN?

```kotlin
// ❌ INCORRECTO: Esto captura TODO el tráfico del dispositivo
Builder()
    .addRoute("0.0.0.0", 0)  // ← Requiere procesar cada paquete → pesado, complejo, propenso a errores
    .establish()

// ✅ CORRECTO: Solo configurar DNS → Android resuelve nombres con CleanBrowsing
Builder()
    .addDnsServer("185.228.168.168")  // ← Filtrado a nivel de resolución DNS
    .addDisallowedApplication(packageName)  // ← Evitar bucle: app no se filtra a sí misma
    .establish()
```

**Ventajas del enfoque correcto**:

- ✅ Internet funciona normal para contenido permitido
- ✅ Sin procesamiento pesado de paquetes (batería, rendimiento)
- ✅ Patrón estándar usado por apps como 1.1.1.1, DNS66
- ✅ Compatible con Android 12+ sin permisos root

---

## 🧪 Testing y Calidad

### Estrategia de testing

```
📁 app/src/test/          # Tests unitarios (JUnit 4/5)
├── repository/           # GuardianRepository: mock DAOs + verificar lógica
├── security/             # SecurityHelper: test cifrado/descifrado de PIN
├── service/              # ScheduleManager: test franjas horarias, cruce de medianoche
└── utils/                # Helpers: hashPin, domain matching, etc.

📁 app/src/androidTest/   # Tests instrumentados (Espresso + Compose Testing)
├── ui/                   # Flujos completos: activar VPN, bloquear sitio, Pacto Digital
├── navigation/           # Verificar que no se puede bypassear PinLockScreen
└── persistence/          # Room migrations: verificar que datos sobreviven actualizaciones
```

### Testing premium sin compra real

El build `debug` incluye `BuildConfig.FORCE_PREMIUM = true` que activa automáticamente
todas las features premium al instalar el APK de debug. En release este valor es `false`
y R8 elimina el bloque como dead code — **no existe en el APK de Play Store**.

```bash
# Instalar debug con premium forzado (para testing de UI/capturas)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/guardianos-shield-v1.1.0-debug.apk
```

### Comandos de testing

```bash
# Tests unitarios rápidos (sin emulador)
./gradlew testDebugUnitTest

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedDebugAndroidTest

# Coverage report (necesita plugin jacoco configurado)
./gradlew jacocoTestReport

# Linting estático
./gradlew lintDebug

# Verificar dependencias con vulnerabilidades
./gradlew dependencyUpdates -Drevision=release
```

### Métricas de calidad objetivo

| Métrica                      | Objetivo | Herramienta                       |
| ---------------------------- | -------- | --------------------------------- |
| Cobertura de tests unitarios | ≥ 70%    | JaCoCo                            |
| Sin crashes en 1000 sesiones | ≥ 99.5%  | Firebase Crashlytics (solo debug) |
| Tiempo de inicio en frío     | < 2s     | Android Vitals                    |
| Consumo de batería diario    | < 5%     | Battery Historian                 |
| Tamaño APK release           | < 25 MB  | AppBundle + R8 shrinker           |

---

## 🛠️ Guías de Desarrollo

### ✅ Antes de hacer commit

```bash
# 1. Ejecutar tests locales
./gradlew testDebugUnitTest

# 2. Verificar linting
./gradlew lintDebug

# 3. Formatear código (ktlint)
./gradlew ktlintFormat

# 4. Verificar que no hay secrets hardcoded
git diff --cached | grep -E "api[_-]?key|secret|password|token" && echo "⚠️ Posible secreto detectado!"

# 5. Commit con mensaje convencional
git commit -m "feat: add time extension petition type to Pact Digital"
# Tipos: feat|fix|docs|style|refactor|test|chore
```

### 📐 Convenciones de código

```kotlin
// ✅ Nombres de archivos: PascalCase para clases, camelCase para funciones/variables
UserProfileEntity.kt      // ✅
user_profile_entity.kt    // ❌

// ✅ Visibilidad: explícita, mínima necesaria
private fun calculateHash(input: String): String { ... }  // ✅
fun calculateHash(input: String): String { ... }          // ❌ (público por defecto)

// ✅ Null safety: evitar !!, usar let/also/?:
val userName = profile?.name ?: "Anónimo"  // ✅
val userName = profile!!.name              // ❌

// ✅ Coroutines: scope adecuado, evitar GlobalScope
class MyViewModel : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.IO)  // ✅

    fun loadData() {
        viewModelScope.launch {  // ✅
            repository.fetchData()
        }
    }
}

// ✅ Recursos: usar string resources, no hardcodear textos
// strings.xml:
<string name="vpn_active">DNS Seguro Activado</string>
// En código:
stringResource(R.string.vpn_active)  // ✅
"DNS Seguro Activado"                // ❌
```

### 🐛 Debugging: Comandos útiles

```bash
# Logs específicos por módulo
adb logcat | grep GuardianVPN          # VPN/DNS filtering
adb logcat | grep UsageStatsMonitor    # Monitoreo de apps
adb logcat | grep AppBlockerA11y       # AccessibilityService
adb logcat | grep BillingManager       # Google Play Billing
adb logcat | grep ScheduleManager      # Control de horarios

# Verificar estado de la VPN
adb shell dumpsys connectivity | grep -A5 VPN

# Verificar DNS activo
adb shell dumpsys connectivity | grep "DNS servers"

# Forzar reinicio de servicios (útil tras cambios)
adb shell am force-stop com.guardianos.shield
adb shell am start -n com.guardianos.shield/.MainActivity

# Instalar APK debug con reemplazo
adb install -r app/build/outputs/apk/debug/guardianos-shield-v1.1.0-debug.apk
```

---

## 🗺️ Roadmap Técnico

### ✅ Q1 2026 (Completado)

- [x] VPN DNS transparente con CleanBrowsing Adult Filter
- [x] Navegador seguro con doble capa de bloqueo
- [x] Triple monitoreo de apps (UsageStats + Accessibility + Lightweight)
- [x] Pacto Digital Familiar (peticiones locales hijo→padre)
- [x] Billing con Google Play (pago único premium)
- [x] Anti-desinstalación con Device Admin

### 🔜 Q2 2026 (En desarrollo)

- [ ] Dashboard web para padres en guardianos.es (sincronización opcional E2E cifrada)
- [ ] Exportación de configuración (backup/restore cifrado)
- [ ] Modo kiosk: bloquear salida de GuardianOS Shield
- [ ] Soporte para tablets: UI adaptable + perfiles por dispositivo

### 🎯 Q3-Q4 2026 (Planificado)

- [ ] Integración con Google Family Link (como complemento, no reemplazo)
- [ ] App companion para Wear OS: alertas a padres en smartwatch
- [ ] Filtrado específico de YouTube (Detección de keywords en títulos/descripciones)
- [ ] Widget de estadísticas para pantalla de inicio (Android AppWidget)
- [ ] Soporte para ChromeOS: versión adaptada para dispositivos educativos

### 🚀 Más allá (Vision)

- [ ] Machine Learning local para detección de contenido inapropiado en imágenes (TensorFlow Lite)
- [ ] Multi-dispositivo sincronizado: perfiles que viajan entre tablet+móvil+portátil
- [ ] Integración con routers domésticos: filtrado a nivel de red local (requiere colaboración con fabricantes)

---

## 📎 Anexos

### A. Dependencias principales (build.gradle.kts)

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

### B. Permisos requeridos (AndroidManifest.xml)

```xml
<!-- Red -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Foreground services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<!-- API 34+: requerido por DnsFilterService (foregroundServiceType="connectedDevice") -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- Batería: mantener servicios de protección activos 24h -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Monitoreo de apps -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />

<!-- Overlay: pantalla de bloqueo parental sobre apps restringidas -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Notificaciones (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Nota: BIND_ACCESSIBILITY_SERVICE y BIND_DEVICE_ADMIN NO son uses-permission.
     Se declaran como android:permission en sus respectivos <service>/<receiver>. -->
```

### C. Variables de entorno y secrets (NUNCA commitear)

```bash
# .env.example (plantilla para colaboradores)
PLAY_BILLING_PUBLIC_KEY=your_public_key_here
SIGNING_STORE_PASSWORD=********
SIGNING_KEY_PASSWORD=********
SIGNING_KEY_ALIAS=guardian_release

# .gitignore (ya incluido en el repo)
*.keystore
*.jks
google-services.json
secrets.properties
api_keys.xml
local.properties
.env
```

---

> � Última revisión: Febrero 2026 • Próxima revisión: Mayo 2026  
> ✉️ Contacto: info@guardianos.es — https://guardianos.es/shield


