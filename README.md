🛡️ GuardianOS Shield

**Filtrado web local para la protección de menores**  
Sin rastreo • Sin servidores externos • Privacidad total

## 📋 Descripción

GuardianOS Shield es una aplicación Android de control parental que filtra contenido inapropiado directamente en el dispositivo, sin necesidad de enviar datos a servidores externos. Todo el filtrado se realiza localmente mediante un servicio VPN local.

### ✨ Características Principales

- 🔒 **Filtrado en tiempo real** de contenido adulto, violencia, malware y phishing
- 👨‍👩‍👧 **Control parental completo** con PIN de seguridad
- 📊 **Estadísticas detalladas** de actividad y bloqueos
- 🎯 **Listas personalizadas** (lista negra y lista blanca)
- ⏰ **Horarios de uso** configurables
- 🔐 **100% privado** - sin conexión a servidores externos
- 📱 **Interfaz moderna** con Material Design 3

## 🚀 Instalación

### Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.0+
- Gradle 8.0+

### Pasos de instalación

1. **Clonar el repositorio**
  
  ```bash
  git clone https://github.com/systemavworks/guardianos-shield.git
  cd guardianos-shield
  ```
  
2. **Abrir en Android Studio**
  
  - File → Open → Seleccionar la carpeta del proyecto
3. **Sincronizar dependencias**
  
  - El proyecto se sincronizará automáticamente
  - Si no, haz clic en "Sync Now" en la barra superior
4. **Compilar y ejecutar**
  
  - Conecta un dispositivo Android o inicia un emulador
  - Run → Run 'app' (o presiona Shift+F10)

## 📁 Estructura del Proyecto

```
Aquí está el esquema visual de las capas de la aplicación **Guardianos Shield**:

# Guardianos Shield - Arquitectura de la Aplicación

## 📋 Tabla de Contenidos
- [Descripción General](#descripción-general)
- [Arquitectura en Capas](#arquitectura-en-capas)
- [Componentes Principales](#componentes-principales)
- [Flujo de Datos](#flujo-de-datos)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)

## Descripción General

**Guardianos Shield** es una aplicación de control parental para Android que utiliza filtrado DNS mediante VPN local, monitoreo de aplicaciones y navegación segura para proteger a los menores en el uso de dispositivos móviles.

## Arquitectura en Capas
```
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA DE PRESENTACIÓN (UI)                  │
│  Material Design 3 + Jetpack Compose                            │
├─────────────────────────────────────────────────────────────────┤
│  • MainActivity.kt                                              │
│  • ParentalControlScreen.kt    - Gestión de controles          │
│  • StatisticsScreen.kt          - Dashboard de estadísticas    │
│  • SettingsScreen.kt            - Configuración de la app      │
│  • CustomFiltersScreen.kt       - Filtros personalizados       │
│  • SafeBrowserActivity.kt       - Navegador seguro integrado   │
└─────────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────────┐
│                   CAPA DE LÓGICA DE NEGOCIO                     │
├─────────────────────────────────────────────────────────────────┤
│  • GuardianRepository.kt   - Repositorio central de datos       │
│  • SettingsDataStore.kt    - Gestión de preferencias           │
└─────────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────────┐
│                       CAPA DE SERVICIOS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🛡️ FILTRADO Y PROTECCIÓN                                      │
│  ├─ DnsFilterService.kt      - Servicio VPN de filtrado DNS    │
│  ├─ LocalBlocklist.kt        - Lista de bloqueo local          │
│  └─ SafeBrowsingService.kt   - Navegación segura               │
│                                                                 │
│  📱 MONITOREO DE APLICACIONES                                   │
│  ├─ AppMonitorService.kt           - Monitor completo de apps  │
│  ├─ LightweightMonitorService.kt   - Monitor optimizado        │
│  ├─ UsageStatsMonitor.kt           - Estadísticas de uso       │
│  └─ RealisticAppBlocker.kt         - Bloqueo inteligente       │
│                                                                 │
│  ⚙️ GESTIÓN Y MANTENIMIENTO                                     │
│  ├─ ScheduleManager.kt       - Programación de horarios        │
│  └─ LogCleanupWorker.kt      - Limpieza automática de logs     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────────┐
│                  CAPA DE PERSISTENCIA (DATA)                    │
│  Room Database + DataStore                                      │
├─────────────────────────────────────────────────────────────────┤
│  • GuardianDatabase.kt - Base de datos principal                │
│                                                                 │
│  ENTIDADES Y DAOs:                                              │
│  ├─ BlockedSiteEntity + BlockedSiteDao   - Sitios bloqueados   │
│  ├─ CustomFilterEntity + CustomFilterDao - Filtros custom      │
│  ├─ DnsLogEntity + DnsLogDao            - Registro DNS         │
│  ├─ StatisticEntity + StatisticDao      - Métricas de uso      │
│  ├─ UserProfileEntity + UserProfileDao  - Perfiles usuarios    │
│  └─ DomainStat.kt                       - Estadísticas dominio │
└─────────────────────────────────────────────────────────────────┘
                              ↓↑
┌─────────────────────────────────────────────────────────────────┐
│                    RECURSOS Y CONFIGURACIÓN                     │
├─────────────────────────────────────────────────────────────────┤
│  📂 Assets                                                       │
│  └─ blocklist_domains.txt - Lista maestra de dominios          │
│                                                                 │
│  📂 Raw Resources                                               │
│  └─ blocklist_backup.txt - Backup de listas de bloqueo         │
│                                                                 │
│  📂 XML Configuration                                           │
│  ├─ network_security_config.xml                                │
│  ├─ backup_rules.xml                                            │
│  └─ data_extraction_rules.xml                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Componentes Principales

### 🔐 Sistema VPN de Filtrado DNS

El núcleo de la protección se basa en un servicio VPN local que intercepta y filtra peticiones DNS:

- **DnsFilterService**: Implementa `VpnService` para crear un túnel VPN local
- **LocalBlocklist**: Gestiona listas de dominios bloqueados
- Sin servidores externos - toda la filtración ocurre en el dispositivo

### 📊 Sistema de Monitoreo

Seguimiento en tiempo real del uso de aplicaciones:

- **AppMonitorService**: Monitoreo completo de aplicaciones
- **LightweightMonitorService**: Versión optimizada para bajo consumo
- **UsageStatsMonitor**: Integración con Android UsageStats API
- **RealisticAppBlocker**: Bloqueo inteligente basado en patrones de uso

### 🗄️ Persistencia de Datos

Arquitectura de datos robusta usando Room:
```kotlin
GuardianDatabase
├── BlockedSite (Sitios bloqueados por el usuario/admin)
├── CustomFilter (Reglas de filtrado personalizadas)
├── DnsLog (Registro de consultas DNS)
├── Statistic (Métricas de uso y actividad)
└── UserProfile (Perfiles de usuarios/menores)
```

### 🎨 Interfaz de Usuario

Desarrollada con Jetpack Compose y Material Design 3:

- **Pantalla de Control Parental**: Gestión de restricciones
- **Estadísticas**: Visualización de uso y actividad
- **Navegador Seguro**: WebView integrado con filtrado
- **Configuración**: Personalización de la aplicación
- **Filtros Custom**: Creación de reglas personalizadas

## Flujo de Datos

### Flujo de Filtrado DNS
```
Internet/Red
    ↓
DnsFilterService (VPN)
    ↓
LocalBlocklist (verificación)
    ↓
[PERMITIR] → Conexión normal
[BLOQUEAR] → Bloqueo + Log
    ↓
DnsLogEntity (Room DB)
    ↓
StatisticsScreen (UI)
```

### Flujo de Monitoreo de Apps
```
Apps del Usuario
    ↓
UsageStatsMonitor
    ↓
RealisticAppBlocker
    ↓
ScheduleManager (verificación de horarios)
    ↓
[PERMITIR] → Continuar
[BLOQUEAR] → Interrupción de app
    ↓
StatisticEntity (Room DB)
    ↓
UserProfileEntity (actualización de métricas)
```

## Permisos de Android Requeridos

La aplicación requiere los siguientes permisos del sistema:

| Permiso | Propósito |
|---------|-----------|
| `BIND_VPN_SERVICE` | Crear servicio VPN para filtrado DNS |
| `PACKAGE_USAGE_STATS` | Acceder a estadísticas de uso de apps |
| `INTERNET` | Conexión a internet |
| `FOREGROUND_SERVICE` | Ejecutar servicios en primer plano |
| `RECEIVE_BOOT_COMPLETED` | Iniciar servicios al arrancar el dispositivo |
| `QUERY_ALL_PACKAGES` | Consultar aplicaciones instaladas |

## Tecnologías Utilizadas

### Framework y Lenguaje
- **Kotlin** - Lenguaje principal
- **Android SDK** - Platform target

### Jetpack Components
- **Compose** - UI moderna declarativa
- **Room** - Base de datos local
- **DataStore** - Almacenamiento de preferencias
- **WorkManager** - Tareas en segundo plano
- **Lifecycle** - Gestión del ciclo de vida

### Servicios Android
- **VpnService** - Filtrado de red
- **Foreground Service** - Monitoreo continuo
- **UsageStatsManager** - Estadísticas del sistema

### Construcción
- **Gradle (KTS)** - Sistema de compilación
- **ProGuard** - Ofuscación y optimización

## Estructura del Proyecto
```
guardianos-shield/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/guardianos/shield/
│   │   │   ├── data/          # Capa de persistencia
│   │   │   ├── service/       # Servicios de background
│   │   │   ├── ui/            # Interfaz de usuario
│   │   │   └── MainActivity.kt
│   │   ├── assets/            # Recursos estáticos
│   │   ├── res/               # Recursos Android
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Características Principales

✅ **Filtrado DNS sin servidor externo** - Privacidad total  
✅ **Control parental completo** - Bloqueo de apps y sitios  
✅ **Monitoreo en tiempo real** - Seguimiento de actividad  
✅ **Navegador seguro integrado** - Navegación protegida  
✅ **Gestión de horarios** - Restricciones temporales  
✅ **Estadísticas detalladas** - Reportes de uso  
✅ **Filtros personalizables** - Control total del usuario  
✅ **Trabajo offline** - No requiere conexión constante  

---

**Licencia**: [Ver LICENSE](LICENSE)  
**Contribuciones**: Las pull requests son bienvenidas
```

## 🔧 Configuración

### 1. Google Safe Browsing API (Opcional)

Para habilitar la integración con Google Safe Browsing:

1. Obtén una API key en [Google Safe Browsing](https://developers.google.com/safe-browsing/v4/get-started)
2. Abre `SafeBrowsingService.kt`
3. Reemplaza `YOUR_API_KEY_HERE` con tu API key:

```kotlin
private const val API_KEY = "tu-api-key-aqui"
```

### 2. Configurar listas de bloqueo

El filtro viene con listas predefinidas, pero puedes personalizarlas en `ContentFilter.kt`:

```kotlin
private val adultContent = setOf(
    "sitio1.com", 
    "sitio2.com",
    // Agregar más dominios...
)
```

### 3. Configurar DNS seguro

Por defecto usa Cloudflare for Families. Puedes cambiarlo en `TunelLocal.kt`:

```kotlin
.addDnsServer("1.1.1.3")  // Cloudflare for Families
.addDnsServer("1.0.0.3")
```

Alternativas:

- Google Safe DNS: `8.8.8.8`
- OpenDNS Family Shield: `208.67.222.123`

## 📱 Uso de la Aplicación

### Primera configuración

1. **Abrir la app** por primera vez
2. **Crear PIN parental** (4 dígitos)
3. **Configurar perfil del menor** (nombre, edad, nivel de restricción)
4. **Activar protección** tocando el botón "Activar Protección"
5. **Conceder permisos VPN** cuando se solicite

### Control Parental

- **Acceder**: Toca el ícono de Control Parental en la pantalla principal
- **PIN requerido**: Introduce tu PIN de 4 dígitos
- **Configurar**:
  - Nivel de restricción (Estricto/Moderado/Suave)
  - Horarios de uso permitido
  - Permitir/bloquear redes sociales

### Filtros Personalizados

1. **Lista Negra**: Dominios bloqueados manualmente
  
  - Toca "Filtros" → "Lista Negra" → "+"
  - Introduce el dominio (ej: `ejemplo.com`)
2. **Lista Blanca**: Dominios siempre permitidos
  
  - Toca "Filtros" → "Lista Blanca" → "+"
  - Introduce el dominio
3. **Wildcards**: Usa `*.dominio.com` para bloquear todos los subdominios
  

### Estadísticas

- **Ver en tiempo real**: Pantalla principal muestra el contador
- **Estadísticas detalladas**: Toca "Ver estadísticas"
  - Gráficos de bloqueos por día
  - Desglose por categoría
  - Top sitios bloqueados
  - Recomendaciones

## 🔐 Seguridad y Privacidad

### Garantías de privacidad

✅ **Sin recopilación de datos**: No se envía ninguna información a servidores externos  
✅ **Filtrado local**: Todo ocurre en tu dispositivo  
✅ **Sin registro de navegación**: Solo se guardan los sitios bloqueados (opcionalmente)  
✅ **PIN encriptado**: El PIN se almacena con hash SHA-256  
✅ **Datos locales**: Base de datos SQLite cifrada

### Permisos requeridos

- **VPN**: Para interceptar el tráfico y filtrar contenido
- **Notificaciones**: Para alertar sobre bloqueos
- **Foreground Service**: Para mantener el servicio activo

## 🛠️ Desarrollo

### Compilar versión de depuración

```bash
./gradlew assembleDebug
```

### Compilar versión de producción

```bash
./gradlew assembleRelease
```

### Ejecutar tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 📊 Base de Datos

La app usa **Room** para persistencia local:

### Tablas

1. **blocked_sites**: Historial de sitios bloqueados
2. **statistics**: Estadísticas diarias
3. **custom_filters**: Listas personalizadas
4. **user_profiles**: Perfiles de usuario

### Retención de datos

- Por defecto: 30 días
- Configurable en Settings
- Limpieza automática de datos antiguos

## 🌐 Categorías de Filtrado

### Contenido bloqueado automáticamente

1. **Contenido adulto**: Pornografía, desnudos
2. **Violencia**: Gore, contenido violento extremo
3. **Malware y Phishing**: Sitios maliciosos, scams
4. **Palabras clave**: xxx, porn, sex, etc.

### Detección avanzada

- Análisis de patrones de URL
- Detección de dominios sospechosos
- Verificación con Google Safe Browsing (opcional)
- Múltiples capas de protección

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Roadmap

- [ ] Integración con más APIs de seguridad
- [ ] Múltiples perfiles de usuario
- [ ] Exportación de reportes en PDF
- [ ] Modo incógnito temporal
- [ ] Widget de estadísticas
- [ ] Soporte para tablets
- [ ] Modo offline mejorado
- [ ] Sincronización entre dispositivos (opcional)

## ⚠️ Limitaciones Conocidas

1. No puede filtrar apps que no usen la VPN del sistema
2. Algunas apps pueden bypassear la VPN (configurables)
3. Requiere permisos de VPN para funcionar
4. El filtrado DNS tiene limitaciones con HTTPS

## 🐛 Problemas Conocidos

Si encuentras problemas:

1. Verifica que los permisos VPN estén concedidos
2. Reinicia el servicio de protección
3. Limpia la caché de la app
4. Reporta en [Issues](https://github.com/systemavworks/guardianos-shield/issues)

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Autores

- - **Victor Shift Lara** Desarrollo inicial* - [TuGitHub](https://github.com/systemavworks)
    

## 🙏 Agradecimientos

- Material Design 3 por el sistema de diseño
- Google Safe Browsing por la API de seguridad
- Cloudflare por los servidores DNS seguros
- Comunidad de Android por las librerías open source

## 📞 Soporte

- Email: info@guardianos.es
- Issues: [issue](https://github.com/systemavworks/guardianos-shield/issues)
- Documentación: [wiki](https://github.com/systemavworks/guardianos-shield/wiki)

---

**Hecho con ❤️ para proteger a nuestros pequeños en el mundo digital**
