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
app/
├── src/main/java/com/guardianos/shield/
│   ├── MainActivity.kt                 # Actividad principal
│   ├── data/
│   │   ├── GuardianDatabase.kt        # Base de datos Room
│   │   ├── BlockedSiteEntity.kt       # Entidades de BD
│   │   └── GuardianRepository.kt      # Repositorio de datos
│   ├── service/
│   │   ├── TunelLocal.kt              # Servicio VPN de filtrado
│   │   ├── ContentFilter.kt           # Motor de filtrado
│   │   └── SafeBrowsingService.kt     # Integración con Safe Browsing
│   └── ui/
│       ├── ParentalControlScreen.kt   # Control parental
│       ├── StatisticsScreen.kt        # Estadísticas
│       ├── CustomFiltersScreen.kt     # Filtros personalizados
│       ├── SettingsScreen.kt          # Configuración
│       └── theme/
│           └── Theme.kt               # Tema de la app
└── AndroidManifest.xml                # Configuración de la app
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
