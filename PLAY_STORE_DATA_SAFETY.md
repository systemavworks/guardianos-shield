# GuardianOS Shield — Formulario "Seguridad de datos" de Play Console

Guía para rellenar la sección **Play Console → Contenido de la app → Seguridad de datos**.  
Es el paso que más revisiones genera en apps de control parental/VPN.

---

## 🗂️ SECCIÓN 1 — Tipos de datos recopilados o compartidos

### ¿Tu app recopila o comparte alguno de los datos del usuario con terceros?

**Respuesta: Sí (parcialmente)**

| Tipo de dato | ¿Se recopila? | ¿Se comparte? | Destino | Motivo |
|---|---|---|---|---|
| Nombres de dominio DNS consultados | Solo localmente (Room DB) | **SÍ** — con CleanBrowsing | CleanBrowsing DNS (185.228.168.168) | Filtrado de contenido parental |
| IP pública temporal del dispositivo | NO almacenada | **SÍ** implícito (en petición DNS) | CleanBrowsing DNS | Técnicamente necesario para responder la consulta DNS |
| Lista de apps instaladas | Solo localmente (Room DB) | **NO** | — | Configuración de control parental |
| Tiempos de uso de apps | Solo localmente (Room DB) | **NO** | — | Estadísticas para padres |
| Historial de bloqueos (dominios bloqueados) | Solo localmente (Room DB) | **NO** | — | Registro de uso parental |
| PIN parental | Solo localmente (EncryptedSharedPrefs) | **NO** | — | Autenticación del tutor |
| Datos de ubicación | **NO** | **NO** | — | No necesario |
| Identificadores de dispositivo | **NO** | **NO** | — | No necesario |
| Fotos/archivos | **NO** | **NO** | — | No necesario |

---

## 📋 SECCIÓN 2 — Respuestas exactas para el formulario

### ¿Recopila tu app datos de usuario?

**Respuesta: Sí**

#### → Actividad en apps y otros

- [x] **Otras acciones del usuario en apps**: tiempos de uso y apps en primer plano  
  - **Propósito**: Funciones de la app  
  - **¿Obligatorio?**: Sí  
  - **¿Cifrado en tránsito?**: N/A (local)  
  - **¿Puede solicitar eliminación el usuario?**: Sí ("Limpiar historial" en Configuración)

#### → Información de la app

- [x] **Apps instaladas**: lista de aplicaciones del menor  
  - **Propósito**: Funciones de la app (control parental)  
  - **¿Obligatorio?**: Sí  
  - **¿Cifrado en tránsito?**: N/A (local)  
  - **¿Puede solicitar eliminación el usuario?**: Sí

---

### ¿Comparte tu app datos de usuario con terceros?

**Respuesta: Sí**

#### → Historial web y de apps

- [x] **Otro historial de navegación**: nombres de dominio enviados a CleanBrowsing DNS  
  - **Receptor**: CleanBrowsing (cleanbrowsing.org)  
  - **Propósito**: Prevención de fraude, seguridad y cumplimiento  
  - **¿Obligatorio para el funcionamiento básico?**: Sí  
  - **¿Se puede desactivar?**: Sí (desactivando el filtro VPN DNS)

**Texto aclaratorio para el campo "Descripción de compartición de datos":**  
```
Los nombres de dominio de las consultas DNS se envían a CleanBrowsing
(filtro DNS público, 185.228.168.168) cuando la protección VPN está activada.
CleanBrowsing bloquea contenido inapropiado para menores. No se envían 
datos de usuario, credenciales, contenido de páginas ni identificadores 
personales. Solo nombres de dominio (ej: google.com). Consultar la política 
de privacidad de CleanBrowsing: https://cleanbrowsing.org/privacy
```

---

## 🔒 SECCIÓN 3 — Prácticas de seguridad

### ¿Cifras los datos en tránsito?
**Respuesta: Sí**  
*Las consultas DNS a CleanBrowsing se realizan sobre UDP (DNS estándar). Todo el tráfico HTTPS del usuario fluye con su cifrado TLS original, sin inspección.*

### ¿Puedes solicitar la eliminación de tus datos?
**Respuesta: Sí**  
*El usuario puede eliminar todos los datos desde la app (Configuración → Limpiar historial) o desinstalando la app.*

### ¿Sigues la Política de Familias de Google Play?
**Respuesta: Sí**  
*La app está diseñada como herramienta para padres/tutores y no recopila datos de menores.*

---

## 📌 SECCIÓN 4 — Declaración VPN (obligatoria para apps con VpnService)

Play Console muestra un formulario especial para apps VPN. Copia estos textos:

### Tipo de VPN:
**Parental controls (Control parental)**

### Descripción del uso de la VPN:
```
GuardianOS Shield usa la API VpnService de Android exclusivamente para 
redirigir consultas DNS al servidor de filtrado de contenido familiar 
CleanBrowsing (185.228.168.168 / 185.228.169.168, Adult Filter).

La VPN NO intercepta paquetes HTTP/HTTPS del usuario.
La VPN NO redirige tráfico a servidores propios.
La VPN NO analiza el contenido de las comunicaciones.
La VPN NO almacena datos de navegación en la nube.

El túnel VPN local solo cambia los servidores DNS del dispositivo.
CleanBrowsing filtra dominios inapropiados en sus propios servidores
(contenido adulto, redes sociales, malware) antes de resolver la IP.

Toda la funcionalidad de filtrado ocurre en los servidores DNS de 
CleanBrowsing, no en nuestra infraestructura.
```

### ¿La VPN cifra el tráfico del usuario?
**Respuesta: No** *(solo cambia los servidores DNS; el tráfico HTTPS mantiene su cifrado TLS habitual)*

### ¿La VPN envía tráfico a un servidor controlado por el desarrollador?
**Respuesta: No** *(el DNS se envía a CleanBrowsing, un servicio DNS público externo)*

---

## ♿ SECCIÓN 5 — Declaración Accessibility Service (obligatoria)

Play Console exige formulario especial para `BIND_ACCESSIBILITY_SERVICE`:

### Función principal que requiere Accesibilidad:
**Control parental / Bloqueo de aplicaciones**

### Texto para el formulario de Accesibilidad:
```
AppBlockerAccessibilityService utiliza la API de Accesibilidad de Android 
ÚNICAMENTE para leer el nombre del paquete de la app en primer plano 
(packageName del evento TYPE_WINDOW_STATE_CHANGED).

Este dato se compara localmente con la lista de apps bloqueadas configurada 
por el padre/tutor. Si hay coincidencia, se lanza AppBlockedActivity (pantalla 
de bloqueo).

NO se accede a: texto en pantalla, contraseñas, contenido web, imágenes, 
credenciales, mensajes ni ningún otro dato sensible.

El servicio de accesibilidad NO puede reemplazarse por ningún otro mecanismo 
sin root, ya que es el único método oficial de Android para detectar la app 
en primer plano en tiempo real en dispositivos de usuario final.
```

---

## 🏷️ SECCIÓN 6 — Clasificación de contenido IARC

### Categoría recomendada:
**Everyone (Todos)** con etiqueta `Parental Guidance`

### Cuestionario IARC — respuestas sugeridas:

| Pregunta | Respuesta |
|---|---|
| ¿Contiene violencia? | No |
| ¿Contiene lenguaje soez? | No |
| ¿Contiene contenido sexual? | No |
| ¿Incluye compras dentro de la app? | No |
| ¿Los usuarios pueden interactuar entre sí? | No |
| ¿Es una app de uso familiar / herramienta parental? | **Sí** |
| ¿Requiere acceso a Internet? | Sí (filtrado DNS) |

**Nota importante**: En el campo "Descripción para revisores de contenido", incluir:
```
Esta app es una herramienta para PADRES/TUTORES. El menor NO usa la app 
directamente; los padres la configuran para proteger el dispositivo de su hijo.
La app no genera contenido; solo bloquea contenido inapropiado.
```

---

## 📄 SECCIÓN 7 — URL de política de privacidad

**URL a introducir en Play Console:**  
`https://guardianos.es/politica-privacidad`

**Verificar antes de enviar:**
- [ ] La URL responde con HTTP 200
- [ ] El contenido carga correctamente en móvil (responsive)
- [ ] Menciona CleanBrowsing y los datos DNS compartidos
- [ ] Incluye datos de contacto (info@guardianos.es)
- [ ] Está en español (el idioma principal de la app)

**URL secundaria (inglés)** — recomendable para revisores de Google en EE.UU.:  
`https://guardianos.es/privacy-policy`

---

## ✅ SECCIÓN 8 — Checklist final antes de enviar la app a revisión

### Técnico (código)
- [x] `targetSdk = 34` ✅
- [x] `minSdk = 31` ✅ (Android 12 — FOREGROUND_SERVICE_SPECIAL_USE disponible)
- [x] `allowBackup="false"` ✅
- [x] `isMinifyEnabled = true` en release ✅
- [x] `isShrinkResources = true` en release ✅
- [x] PIN encriptado con `SecurityHelper` (EncryptedSharedPreferences + SHA-256) ✅
- [x] Logs de debugging protegidos con `BuildConfig.DEBUG` ✅
- [x] `network_security_config.xml` sin cleartext permitido ✅
- [x] Permisos no usados eliminados (`CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE`) ✅
- [x] Servicios no exportados (`exported="false"`) excepto los que lo requieren ✅

### Play Console
- [ ] Subir el AAB (no APK) para publicación: `release/v1.1.0/guardianos-shield-v1.1.0-release.aab`
- [ ] Rellenar sección "Seguridad de datos" con los valores de este documento
- [ ] Rellenar declaración VPN especial
- [ ] Rellenar formulario de Accessibility Service
- [ ] Introducir URL de política de privacidad
- [ ] Completar cuestionario IARC
- [ ] Clasificar como app "Para familias" → subcategoría "Herramientas para padres"
- [ ] Añadir capturas de pantalla de al menos 2 tamaños (teléfono + tablet 7")
- [ ] Fijar precio: Gratis
- [ ] Añadir descripción corta (≤80 caracteres) y descripción completa (≤4000 caracteres)
- [ ] Subir icono de 512×512 px (ya disponible en `play_store_assets/`)
- [ ] Subir feature graphic 1024×500 px (ya disponible en `play_store_assets/`)

### Permisos que requieren formulario especial en Play Console
- [ ] **VPN** — usar textos de Sección 4 de este documento
- [ ] **Accessibility** — usar textos de Sección 5 de este documento  
- [ ] **QUERY_ALL_PACKAGES** — usar textos de `PERMISSIONS_DECLARATION.md` → Sección 2
- [ ] **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** — usar textos de `PERMISSIONS_DECLARATION.md` → Sección 3
- [ ] **SYSTEM_ALERT_WINDOW** — usar textos de `PERMISSIONS_DECLARATION.md` → Sección 1

---

## ⚠️ Riesgos de rechazo y cómo mitigarlos

| Riesgo | Probabilidad | Mitigación |
|---|---|---|
| Rechazo por AccessibilityService sin justificación suficiente | Alta | Usar texto de Sección 5; detallar que no hay alternativa sin root |
| Rechazo por VPN + falta de declaración | Media | Completar formulario VPN en Play Console antes de enviar |
| Rechazo por QUERY_ALL_PACKAGES | Media | Categoría "Parental Controls" tiene excepción documentada |
| Suspensión por SYSTEM_ALERT_WINDOW | Baja | App es control parental; Google tiene excepción para este caso |
| Rechazo por política de privacidad inaccesible | Baja | Verificar URL antes de enviar |

**Tiempo estimado de revisión**: 3-7 días hábiles para apps nuevas con VPN + Accessibility.  
**Si hay rechazo**: responder en el plazo de 30 días con justificación por escrito.

---

*Última revisión: Febrero 2026 — GuardianOS Shield v1.1.0*
