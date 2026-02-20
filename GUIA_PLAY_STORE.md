# 🚀 Guía Paso a Paso: Subir GuardianOS Shield a Google Play Store

## 📋 TABLA DE CONTENIDOS
1. [Preparación Previa](#1-preparación-previa)
2. [Generar Keystore (Firma de la App)](#2-generar-keystore)
3. [Configurar Build para Release](#3-configurar-build-para-release)
4. [Compilar APK/AAB de Release](#4-compilar-apkaab-de-release)
5. [Crear Cuenta de Google Play Console](#5-crear-cuenta-play-console)
6. [Crear Nueva Aplicación](#6-crear-nueva-aplicación)
7. [Preparar Materiales Gráficos](#7-preparar-materiales-gráficos)
8. [Subir el AAB](#8-subir-el-aab)
9. [Completar Ficha de Play Store](#9-completar-ficha-play-store)
10. [Responder Cuestionarios](#10-responder-cuestionarios)
11. [Enviar a Revisión](#11-enviar-a-revisión)

---

## 1. PREPARACIÓN PREVIA

### ✅ Checklist antes de empezar:

- [ ] Política de privacidad publicada en https://guardianos.es/politica-privacidad
- [ ] App compilando correctamente en modo debug
- [ ] Tienes una cuenta de Gmail (necesaria para Play Console)
- [ ] Tienes $25 USD para pagar la tarifa única de desarrollador
- [ ] Icono de app en alta resolución (512x512px)

### 📁 Archivos necesarios:
```
politica-privacidad-shield.html → Subir a tu web
PLAY_STORE_REQUISITOS.md → Guía de referencia
SECURITY_AUDIT.txt → Documentación de seguridad
```

---

## 2. GENERAR KEYSTORE

El **keystore** es un archivo que firma digitalmente tu app. **MUY IMPORTANTE**: Si lo pierdes, NO podrás actualizar tu app nunca más.

### Paso 1: Abrir terminal
```bash
cd ~/guardianos-shield
```

### Paso 2: Generar keystore
```bash
keytool -genkey -v -keystore guardianos-release.keystore \
  -alias guardianos \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Paso 3: Responder preguntas
```
Enter keystore password: [Escribe contraseña fuerte, ej: Tu#Pass123!]
Re-enter new password: [Repite la contraseña]

What is your first and last name?
  [Tu nombre]: Victor Shift Lara

What is the name of your organizational unit?
  [Equipo/departamento]: Desarrollo

What is the name of your organization?
  [Tu empresa]: GuardianOS

What is the name of your City or Locality?
  [Tu ciudad]: Sevilla

What is the name of your State or Province?
  [Tu provincia]: Sevilla

What is the two-letter country code for this unit?
  [Código país]: ES

Is CN=Victor Shift Lara, OU=Desarrollo, O=GuardianOS, L=Sevilla, ST=Sevilla, C=ES correct?
  [no]: yes

Enter key password for <guardianos>
  (RETURN if same as keystore password): [Presiona Enter para usar la misma contraseña]
```

### Paso 4: GUARDAR CONTRASEÑAS ⚠️
Crea un archivo **FUERA** del repositorio:
```bash
# En un lugar seguro (NO en GitHub)
cat > ~/guardianos-keystore-info.txt << EOF
KEYSTORE: ~/guardianos-shield/guardianos-release.keystore
ALIAS: guardianos
CONTRASEÑA KEYSTORE: Tu#Pass123!
CONTRASEÑA KEY: Tu#Pass123!
FECHA CREACIÓN: $(date)
⚠️ NO COMPARTIR ESTE ARCHIVO
⚠️ HACER BACKUP EN LUGAR SEGURO
EOF
```

### Paso 5: Hacer backup del keystore
```bash
# Copiar a USB, Dropbox, Google Drive, etc.
cp guardianos-release.keystore ~/Documentos/BACKUP-KEYSTORE/
```

---

## 3. CONFIGURAR BUILD PARA RELEASE

### Paso 1: Editar `app/build.gradle.kts`

Busca la línea `android {` y ANTES de `buildTypes {`, agrega:

```kotlin
android {
    namespace = "com.guardianos.shield"
    compileSdk = 34

    // ========== AGREGAR ESTO AQUÍ ==========
    signingConfigs {
        create("release") {
            storeFile = file("../guardianos-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "Tu#Pass123!"
            keyAlias = "guardianos"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "Tu#Pass123!"
        }
    }
    // ========================================

    defaultConfig {
        // ... resto de código
```

### Paso 2: Modificar `buildTypes.release`

Busca esta sección y modifica:
```kotlin
release {
    signingConfig = signingConfigs.getByName("release")  // ← AGREGAR ESTA LÍNEA
    isDebuggable = false
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

### Paso 3: Incrementar versión

En `defaultConfig`:
```kotlin
versionCode = 2  // Incrementar (era 1)
versionName = "1.1.0"  // Nueva versión
```

---

## 4. COMPILAR APK/AAB DE RELEASE

### Opción A: Compilar AAB (Recomendado para Play Store)

```bash
cd ~/guardianos-shield

# Configurar variables de entorno (opcional, sino usa las del build.gradle.kts)
export KEYSTORE_PASSWORD="Tu#Pass123!"
export KEY_PASSWORD="Tu#Pass123!"

# Compilar AAB
./gradlew bundleRelease

# Resultado en:
# app/build/outputs/bundle/release/app-release.aab
```

### Opción B: Compilar APK (Para testear antes de subir)

```bash
./gradlew assembleRelease

# Resultado en:
# app/build/outputs/apk/release/app-release.apk
```

### Verificar compilación exitosa:
```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
# Debe mostrar algo como: -rw-r--r-- 1 usuario grupo 25M feb 5 16:30 app-release.aab
```

### Testear APK antes de subir:
```bash
# Instalar en dispositivo/emulador
adb install app/build/outputs/apk/release/app-release.apk

# Probar funcionalidades críticas:
# - VPN se activa correctamente
# - PIN funciona
# - Bloqueo de apps funciona
# - No hay crashes
```

---

## 5. CREAR CUENTA PLAY CONSOLE

### Paso 1: Ir a Play Console
Abrir: https://play.google.com/console/signup

### Paso 2: Registrarse como desarrollador
1. **Iniciar sesión** con tu cuenta de Gmail
2. **Aceptar** el Acuerdo de Distribución para Desarrolladores
3. **Pagar** la tarifa de $25 USD (una sola vez, de por vida)
   - Tarjeta de crédito/débito
   - No es reembolsable
4. **Completar** información de cuenta:
   - Nombre del desarrollador: **GuardianOS** o **Victor Shift Lara**
   - Email de contacto: info@guardianos.es
   - Sitio web: https://guardianos.es
   - Dirección (opcional pero recomendado)

### Paso 3: Verificar cuenta
- Google puede tardar 24-48 horas en verificar tu cuenta
- Recibirás un email cuando esté lista

---

## 6. CREAR NUEVA APLICACIÓN

### Paso 1: Crear app en Play Console
1. En Play Console, clic en **"Crear aplicación"**
2. **Nombre de la app**: GuardianOS Shield
3. **Idioma predeterminado**: Español (España)
4. **Tipo de app**: Aplicación
5. **Gratis o de pago**: Gratis
6. **Marcar casillas**:
   - ✅ Política de desarrollador
   - ✅ Leyes de exportación de EE.UU.
7. Clic en **"Crear aplicación"**

### Paso 2: Panel de control de la app
Ahora verás un panel con varias tareas pendientes:
- Ficha de Play Store
- Versión de producción
- Clasificación de contenido
- Público objetivo
- etc.

---

## 7. PREPARAR MATERIALES GRÁFICOS

### A) Icono de la aplicación (OBLIGATORIO)
**Requisitos**: 512x512px, PNG, sin transparencia

```bash
# Si tienes el icono en Android Studio:
# Exportarlo a alta resolución desde:
# app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# Redimensionar si es necesario:
convert ic_launcher.png -resize 512x512 ic_launcher_512.png
```

### B) Feature Graphic (OBLIGATORIO)
**Requisitos**: 1024x500px, PNG/JPG

Crear un banner con:
- Logo de GuardianOS Shield
- Texto: "Control Parental Seguro"
- Fondo con colores de la app

Puedes usar Canva, Photoshop, GIMP o este comando rápido:
```bash
# Plantilla simple con ImageMagick:
convert -size 1024x500 gradient:#0f1c2e-#1a2332 \
  -gravity center -pointsize 60 -fill white \
  -annotate +0+0 "GuardianOS Shield\nControl Parental" \
  feature_graphic.png
```

### C) Capturas de pantalla (MÍNIMO 2)
**Requisitos**: 
- Teléfono: 320-3840px (ancho o alto)
- Recomendado: 1080x1920px (portrait)
- Formato: PNG o JPG
- Mínimo 2, máximo 8

**Pantallas a capturar**:
1. **Pantalla principal** (con VPN activo mostrando estadísticas)
2. **Control parental** (configuración de horarios)
3. **Estadísticas** (gráficos de sitios bloqueados)
4. **Configuración** (ajustes de la app)

**Cómo capturar en dispositivo Android**:
1. Abrir app en dispositivo
2. Presionar **Volumen Abajo + Botón Power** simultáneamente
3. Las capturas se guardan en `/sdcard/Pictures/Screenshots/`
4. Transferir al PC:
   ```bash
   adb pull /sdcard/Pictures/Screenshots/ ~/capturas-shield/
   ```

### D) Vídeo de promoción (OPCIONAL)
- Duración: 30 segundos - 2 minutos
- Mostrar funcionalidades principales
- Subir a YouTube como "No listado"
- Copiar enlace para Play Store

---

## 8. SUBIR EL AAB

### Paso 1: Crear versión de producción
1. En Play Console → **Producción** → **Crear nueva versión**
2. Clic en **"Subir"** y seleccionar: `app/build/outputs/bundle/release/app-release.aab`
3. Esperar a que se procese (1-5 minutos)

### Paso 2: Completar información de versión
```
Nombre de la versión: 1.1.0 (Versión inicial de seguridad mejorada)

Notas de la versión (español):
🛡️ Versión 1.1.0
✅ Filtrado DNS con CleanBrowsing
✅ Control parental con PIN encriptado
✅ Monitoreo de aplicaciones
✅ Estadísticas detalladas de uso
✅ Protección de privacidad mejorada
```

### Paso 3: Revisar y guardar
- Play Console validará el AAB automáticamente
- Mostrará errores si los hay (permisos, versión, firma, etc.)
- Si todo está correcto, clic en **"Guardar"**

---

## 9. COMPLETAR FICHA PLAY STORE

### A) Información principal

**Ir a**: Ficha de Play Store → Detalles principales

**Nombre de la app**: GuardianOS Shield (30 caracteres máx)

**Descripción corta**: (80 caracteres máx)
```
Control parental con DNS seguro y monitoreo de apps
```

**Descripción completa**: (4000 caracteres máx)
```
🛡️ GuardianOS Shield - Control Parental Avanzado

Protege a tus hijos mientras usan sus dispositivos Android con filtrado DNS 
profesional y monitoreo inteligente de aplicaciones.

✨ CARACTERÍSTICAS PRINCIPALES:

🔒 Filtrado DNS Seguro
• CleanBrowsing DNS integrado
• Bloqueo automático de contenido adulto
• Sin inspección de tráfico cifrado
• Protección en tiempo real

📱 Monitoreo de Aplicaciones
• Control de horarios de uso
• Bloqueo selectivo de apps sensibles
• Estadísticas detalladas para padres
• Alertas de apps bloqueadas

🔐 Protección PIN Parental
• Encriptación con AndroidKeyStore
• Los niños no pueden desactivar protecciones
• Verificación segura con hash SHA-256

📊 Estadísticas y Reportes
• Historial de sitios bloqueados
• Tiempo de uso por aplicación
• Gráficos visuales intuitivos
• Exportación de datos en CSV/JSON

🌟 PRIVACIDAD GARANTIZADA:
• Todos los datos se almacenan localmente
• Sin servidores externos ni tracking
• Código open source auditado
• Compatible con RGPD y COPPA

💪 MODOS DE PROTECCIÓN:
• Recomendado: Monitoreo ligero y eficiente
• Avanzado: VPN + filtrado DNS completo
• Manual: Control total personalizado

🎯 IDEAL PARA:
• Padres preocupados por la seguridad digital
• Familias que buscan protección sin comprometer privacidad
• Control parental efectivo y fácil de usar
• Protección de menores en línea

📱 REQUISITOS:
• Android 7.0 o superior
• Permisos: VPN, UsageStats, Notificaciones
• Sin conexión a Internet requerida (excepto DNS)

🔓 SIN COSTOS OCULTOS:
• Totalmente gratis
• Sin compras dentro de la app
• Sin publicidad
• Sin suscripciones

Desarrollado por GuardianOS - Tu seguridad, tu control.
```

**Icono de la aplicación**: Subir `ic_launcher_512.png`

**Feature Graphic**: Subir `feature_graphic.png` (1024x500px)

**Capturas de pantalla**: Subir mínimo 2 capturas

### B) Detalles de la app

**Categoría**: Herramientas

**Información de contacto**:
- Email: info@guardianos.es
- Sitio web: https://guardianos.es
- Teléfono: (Opcional)

**Política de privacidad**: https://guardianos.es/politica-privacidad

### C) Clasificación de contenido

**Ir a**: Clasificación de contenido → Iniciar cuestionario

1. **Dirección de email**: info@guardianos.es
2. **Categoría**: Herramientas
3. **¿Contiene violencia?**: NO
4. **¿Contiene contenido sexual?**: NO
5. **¿Permite interacción entre usuarios?**: NO
6. **¿Comparte ubicación del usuario?**: NO
7. **¿Comparte información personal?**: NO

**Resultado esperado**: PEGI 3 / Todos

---

## 10. RESPONDER CUESTIONARIOS

### A) Público objetivo y contenido

**Ir a**: Público objetivo → Responder preguntas

1. **Público objetivo**: 
   - ✅ Padres (18+)
   - Justificación: Control parental para supervisión de menores

2. **¿Dirigida a niños?**: NO
   - Es una app para padres que supervisan a niños

3. **¿Tiene anuncios?**: NO

4. **¿Tiene compras dentro de la app?**: NO

### B) Declaración de acceso a datos

**Ir a**: Seguridad de los datos → Empezar

**Tipos de datos recopilados**: (Marcar según corresponda)

1. **Ubicación**: NO

2. **Información personal**:
   - NO recopilamos nombre, email, dirección

3. **Información financiera**: NO

4. **Fotos y vídeos**: NO

5. **Archivos y documentos**: NO

6. **Actividad en apps**:
   - ✅ SÍ - Interacciones en apps
   - ✅ SÍ - Historial de búsqueda en apps
   - **Opcional**: NO
   - **Se comparte con terceros**: NO
   - **Se puede solicitar eliminación**: SÍ
   - **Propósito**: Control parental

7. **Actividad web**:
   - ✅ SÍ - Historial de navegación web
   - **Opcional**: NO
   - **Se comparte con terceros**: NO (excepto CleanBrowsing DNS)
   - **Se puede solicitar eliminación**: SÍ
   - **Propósito**: Filtrado de contenido

8. **ID de dispositivo**: NO

**¿Cifrado en tránsito?**: SÍ (VPN y HTTPS)

**¿Los usuarios pueden solicitar eliminación de datos?**: SÍ (Limpiar historial en app)

### C) Permisos con declaración especial

**QUERY_ALL_PACKAGES** - Requiere justificación:

```
Categoría: Control parental / Bienestar digital

Justificación:
GuardianOS Shield es una aplicación de control parental que requiere 
acceso a la lista de aplicaciones instaladas para:

1. Monitorear el uso de aplicaciones por parte de menores de edad
2. Bloquear aplicaciones sensibles según horarios configurados por padres
3. Generar estadísticas de uso para padres/tutores legales
4. Detectar instalación de aplicaciones potencialmente peligrosas

Este permiso es esencial para la funcionalidad principal de protección 
de menores. Los datos NO se comparten con terceros y se almacenan 
únicamente en el dispositivo local.

La app cumple con políticas de privacidad (RGPD/COPPA) y el código 
fuente está disponible públicamente en GitHub para auditoría.
```

**VPN Service** - Declaración:

```
La app usa VPN Service para:
- Interceptar consultas DNS (solo nombres de dominio)
- Redirigir a CleanBrowsing DNS para filtrado de contenido
- NO inspecciona tráfico de datos
- NO redirige tráfico a servidores remotos propios
- VPN es local, solo para filtrado DNS

Propósito: Protección infantil mediante filtrado de contenido inapropiado.
```

---

## 11. ENVIAR A REVISIÓN

### Checklist final:

- [ ] AAB subido y validado
- [ ] Ficha de Play Store completada (descripción, capturas, icono)
- [ ] Clasificación de contenido completada (PEGI)
- [ ] Público objetivo configurado
- [ ] Declaración de seguridad de datos completada
- [ ] Permisos especiales justificados (QUERY_ALL_PACKAGES, VPN)
- [ ] Política de privacidad accesible online
- [ ] Email de contacto válido

### Enviar a revisión:

1. **Ir a**: Panel general de la app
2. Verificar que **todas** las secciones tienen ✅ verde
3. Clic en **"Enviar a revisión"** o **"Publicar"**
4. Confirmar envío

### ⏳ Tiempo de revisión:
- **Normal**: 1-7 días
- **Primera vez**: Puede tardar hasta 14 días
- **Con permisos especiales**: Puede requerir revisión manual adicional

### 📧 Notificaciones:
Recibirás emails en:
- Inicio de revisión
- Aprobación (¡tu app está publicada!)
- Rechazo (con motivo y pasos para corregir)

---

## 12. SI TE RECHAZAN LA APP

### Motivos comunes de rechazo:

**A) QUERY_ALL_PACKAGES no justificado**
**Solución**:
1. Ir a Play Console → Tu app → Permisos
2. Editar justificación con más detalles
3. Agregar capturas de pantalla mostrando funcionalidad
4. Enviar formulario de apelación

**B) Política de privacidad no accesible**
**Solución**:
1. Verificar que https://guardianos.es/politica-privacidad está online
2. Probar en navegador incógnito
3. Asegurar que no requiere login
4. Verificar que el contenido coincide con la app

**C) Funcionalidad VPN no declarada**
**Solución**:
1. Ir a Contenido de la app → Funcionalidades de la app
2. Marcar "Usa servicio VPN"
3. Explicar propósito (filtrado DNS, no inspección de tráfico)

**D) Crash en dispositivo de prueba de Google**
**Solución**:
1. Descargar crash report de Play Console
2. Revisar ProGuard rules (puede haber ofuscado algo crítico)
3. Testear en emulador limpio
4. Corregir bug y subir nueva versión

---

## 13. DESPUÉS DE LA PUBLICACIÓN

### Tu app está en Play Store! 🎉

**URL de tu app**:
```
https://play.google.com/store/apps/details?id=com.guardianos.shield
```

### Próximos pasos:

1. **Compartir en redes sociales**
2. **Monitorear estadísticas** en Play Console
3. **Responder reseñas** de usuarios
4. **Actualizar regularmente** (cada 2-3 meses)

### Cómo actualizar la app:

```bash
# 1. Modificar código
# 2. Incrementar versionCode y versionName en build.gradle.kts
versionCode = 3  # Era 2
versionName = "1.2.0"  # Nueva versión

# 3. Compilar nuevo AAB
./gradlew bundleRelease

# 4. Subir a Play Console → Producción → Crear nueva versión
# 5. Agregar notas de la versión
# 6. Publicar actualización
```

---

## 📞 SOPORTE Y RECURSOS

**Documentación oficial**:
- Play Console Help: https://support.google.com/googleplay/android-developer
- Políticas de desarrollador: https://play.google.com/about/developer-content-policy/

**Tu documentación**:
- PLAY_STORE_REQUISITOS.md
- SECURITY_AUDIT.txt
- politica-privacidad-shield.html

**Contacto GuardianOS**:
- Email: info@guardianos.es
- Web: https://guardianos.es
- GitHub: https://github.com/systemavworks/guardianos-shield

---

## ✅ RESUMEN RÁPIDO

```bash
# 1. Generar keystore
keytool -genkey -v -keystore guardianos-release.keystore -alias guardianos -keyalg RSA -keysize 2048 -validity 10000

# 2. Configurar build.gradle.kts (signingConfigs)

# 3. Compilar AAB
./gradlew bundleRelease

# 4. Crear cuenta Play Console ($25)

# 5. Crear app en Play Console

# 6. Preparar materiales (icono 512x512, feature graphic 1024x500, capturas)

# 7. Subir AAB

# 8. Completar ficha (descripción, capturas, clasificación)

# 9. Responder cuestionarios (contenido, permisos, datos)

# 10. Enviar a revisión

# 11. Esperar aprobación (1-7 días)

# 12. ¡Tu app está en Play Store!
```

---

**Última actualización**: 5 de Febrero de 2026  
**Versión de la guía**: 1.0  
**App**: GuardianOS Shield v1.1.0
