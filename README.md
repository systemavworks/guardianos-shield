# 🛡️ GuardianOS Shield

**Filtrado web local para la protección de menores**  
Sin rastreo • Sin servidores externos • Privacidad total

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android%2012%2B-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple.svg)
![API](https://img.shields.io/badge/API-31%2B%20(Android%2012)-orange.svg)

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
│   ├── MainActivity.kt           # Activity principal (Jetpack Compose)
│   ├── data/                     # 📦 Capa de Datos
│   │   ├── GuardianDatabase.kt   # Room Database (v3)
│   │   ├── GuardianRepository.kt # Repositorio central (DAO + lógica)
│   │   ├── UserProfileEntity.kt  # Perfiles de usuario/menores
│   │   ├── CustomFilterEntity.kt # Filtros personalizados (blacklist/whitelist)
│   │   ├── DnsLogEntity.kt       # Logs de consultas DNS bloqueadas
│   │   ├── BlockedSiteEntity.kt  # Historial de sitios bloqueados
│   │   ├── SensitiveAppEntity.kt # Apps sensibles monitoreadas
│   │   └── SettingsDataStore.kt  # Configuración (DataStore)
│   ├── service/                  # ⚙️ Servicios en Segundo Plano
│   │   ├── DnsFilterService.kt   # VPN Service (DNS transparente)
│   │   ├── LocalBlocklist.kt     # Lista de dominios bloqueados local
│   │   ├── AppMonitorService.kt  # Monitoreo de apps foreground
│   │   ├── UsageStatsMonitor.kt  # Detección de apps sensibles
│   │   └── LogCleanupWorker.kt   # Limpieza periódica de logs
│   ├── ui/                       # 🎨 Interfaz de Usuario (Jetpack Compose)
│   │   ├── SafeBrowserActivity.kt# Navegador seguro con WebView
│   │   ├── ParentalControlScreen.kt # Pantalla de configuración parental
│   │   ├── CustomFiltersScreen.kt   # Gestión de filtros personalizados
│   │   ├── StatisticsScreen.kt      # Estadísticas y logs
│   │   ├── SettingsScreen.kt        # Configuración general
│   │   └── theme/                   # Material Design 3
│   └── viewmodel/                # 📊 ViewModels (MVVM)
│       ├── MainViewModel.kt
│       ├── ParentalViewModel.kt
│       └── StatsViewModel.kt
├── AndroidManifest.xml           # Permisos y servicios
└── build.gradle.kts              # Configuración Gradle
```

### 📦 Capa de Datos (`data/`)
- **Room Database**: Base de datos local SQLite con TypeConverters
- **Repository Pattern**: `GuardianRepository` centraliza acceso a datos
- **DataStore**: Configuración persistente asíncrona
- **Entities**: Modelos de datos con anotaciones Room
- **DAOs**: Interfaces con queries SQL y Flows reactivos

### ⚙️ Capa de Servicios (`service/`)
- **DnsFilterService**: VPN Service que configura DNS seguros sin procesar paquetes
- **LocalBlocklist**: Bloqueo local hardcoded de redes sociales/contenido adulto
- **AppMonitorService**: Foreground Service persistente para monitoreo
- **UsageStatsMonitor**: Detecta apps en foreground y redirige a navegador seguro

### 🎨 Capa de UI (`ui/`)
- **Jetpack Compose**: UI declarativa moderna
- **Material Design 3**: Theming dinámico
- **Navigation Component**: Navegación entre pantallas
- **SafeBrowserActivity**: WebView con bloqueo integrado antes de cargar URLs

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

### 🔔 Sistema de Notificaciones (NUEVO)
- **Sitios web bloqueados**: Notificación con categoría automática
  - Red Social bloqueada
  - Contenido Adulto bloqueado
  - Apuestas bloqueadas
  - Horario no permitido
- **Apps bloqueadas**: Cuando se redirige un navegador externo (Chrome, Brave, Firefox)
- **Prioridad ALTA** para alertas inmediatas
- **Auto-cancelables** al tocarlas
- **Canales separados**: "Sitios Bloqueados" y "Apps Bloqueadas"

### 📊 Monitoreo de Apps
- **Detección de navegadores externos** (Chrome, Brave, Firefox, Edge, Opera, etc.)
- **Redirección automática** al navegador seguro
- **UsageStats API** para detección precisa de app foreground
- **Foreground Service persistente** (resistente a task killers OPPO/Motorola)
- **Notificación por redirección**: "App bloqueada - [nombre]"

### 🔐 Privacidad y Seguridad
- ✅ **100% local**: Sin conexión a servidores externos
- ✅ **Sin analytics ni tracking**
- ✅ **Sin almacenamiento en la nube**
- ✅ **Datos cifrados** en Room Database
- ✅ **Open Source** y auditable

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
- **Funcionamiento**:
  - Si el menor abre Chrome/Brave/Firefox → se redirige a navegador seguro
  - Notificación: "App bloqueada - Chrome"
  - Foreground Service mantiene monitoreo activo

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
# Ver logs de VPN y DNS filtering
adb logcat | grep GuardianVPN

# Ver logs de monitoreo de apps
adb logcat | grep UsageStatsMonitor

# Ver logs del navegador seguro
adb logcat | grep SafeBrowser

# Ver todos los logs de la app
adb logcat | grep "com.guardianos.shield"

# Verificar permisos concedidos
adb shell dumpsys package com.guardianos.shield | grep permission

# Verificar estado de la VPN
adb shell dumpsys connectivity | grep VPN

# Forzar detener app (útil para reiniciar servicios)
adb shell am force-stop com.guardianos.shield

# Instalar y ejecutar directamente
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.guardianos.shield/.MainActivity
```

### Debugging común
1. **VPN no se activa**: 
   - Verificar que no haya otra VPN activa
   - Desactivar DNS privado en Ajustes > Red
   - Conceder permiso VPN cuando Android lo pida
   
2. **Sitios no se bloquean**: 
   - Verificar logs DNS: `adb logcat | grep GuardianVPN`
   - Confirmar DNS activo: debe mostrar 185.228.168.168
   - Verificar lista local en `LocalBlocklist.kt` y `SafeBrowserActivity.kt`
   
3. **Monitoreo no funciona**: 
   - Conceder UsageStats: Ajustes > Aplicaciones especiales > Acceso a uso
   - Verificar Foreground Service activo
   - Desactivar optimización de batería para la app
   
4. **Notificaciones no aparecen**: 
   - Android 13+ requiere permiso POST_NOTIFICATIONS
   - Verificar canales de notificación creados
   - Revisar configuración de notificaciones de la app

5. **Horarios no funcionan**:
   - Verificar perfil activo con `repository.getActiveProfile()`
   - Confirmar `scheduleEnabled = true` y horarios correctos
   - Ver logs: "BLOQUEADO POR HORARIO"

---

## 📄 Licencia

MIT License - Copyright (c) 2026 Victor Shift Lara - Andalucía, España

Ver el archivo [LICENSE](https://github.com/systemavworks/guardianos-shield/blob/main/LICENSE) para más detalles.

---

## 👨‍💻 Autor

**Victor Shift Lara**  
📍 Andalucía, España  
🌐 Web: [https://guardianos.es](https://guardianos.es)  
📧 Email: [info@guardianos.es](mailto:info@guardianos.es)  
💼 GitHub: [@systemavworks](https://github.com/systemavworks)

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

- [ ] Dashboard web para padres en guardianos.es
- [ ] Exportar configuración (backup/restore)
- [ ] Modo kiosk para bloquear salida de la app
- [ ] Soporte para múltiples dispositivos sincronizados (opcional)
- [ ] Integración con Google Family Link
- [ ] App companion para smartwatches (alertas a padres)
- [ ] Filtrado de contenido YouTube específico
- [ ] Bloqueo de compras in-app
- [ ] Soporte para tablets y ChromeOS
- [ ] Exportación de informes en PDF
- [ ] Widget de estadísticas para home screen

---

**Hecho con ❤️ en Andalucía**  
*Protegiendo a nuestros pequeños en el mundo digital*
