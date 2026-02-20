# GuardianOS Shield - Requisitos Play Store

## ✅ YA CONFIGURADO

### 1. ProGuard/R8
- **Estado**: ✅ Activado (`isMinifyEnabled = true`)
- **Archivo**: `app/proguard-rules.pro` actualizado con reglas específicas
- **Incluye**: Protección VPN, SecurityHelper, Room, Compose

### 2. Política de Privacidad
- **URL**: https://guardianos.es/politica-privacidad
- **Ubicación**: AndroidManifest.xml (`android:privacyPolicy`)
- **Estado**: ✅ Configurada

### 3. Seguridad
- **PIN**: Encriptado con EncryptedSharedPreferences ✅
- **Backup**: Deshabilitado (`allowBackup="false"`) ✅
- **Logs**: Protegidos con BuildConfig.DEBUG ✅

## ⚠️ PENDIENTE ANTES DE PUBLICAR

### 1. Generar Keystore (OBLIGATORIO)

```bash
keytool -genkey -v -keystore ~/guardianos-release.keystore \
  -alias guardianos \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Datos a ingresar:**
- Nombre y apellido: Tu Nombre
- Unidad organizativa: GuardianOS
- Organización: Tu Empresa
- Ciudad/Localidad: Tu Ciudad
- Estado/Provincia: Tu Provincia
- Código de país: ES

**⚠️ CRÍTICO**: Guarda el archivo `.keystore` y las contraseñas de forma segura. Si los pierdes, NO podrás actualizar la app en Play Store.

### 2. Configurar Signing en build.gradle.kts

Agregar ANTES de `buildTypes`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../guardianos-release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "guardianos"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

Modificar en `buildTypes.release`:
```kotlin
release {
    signingConfig = signingConfigs.getByName("release")
    // ... resto igual
}
```

### 3. Compilar Release

```bash
# Configurar variables de entorno
export KEYSTORE_PATH=~/guardianos-release.keystore
export KEYSTORE_PASSWORD=tu_password_aqui
export KEY_PASSWORD=tu_password_aqui

# Generar AAB (Android App Bundle) para Play Store
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### 4. Justificación QUERY_ALL_PACKAGES

Play Store requiere justificación para este permiso sensible:

**Formulario de Play Console:**
```
Categoría: Control parental / Bienestar digital
Descripción: 
"GuardianOS Shield es una aplicación de control parental que requiere 
acceso a la lista de aplicaciones instaladas para:

1. Monitorear el uso de aplicaciones por parte de menores de edad
2. Bloquear aplicaciones sensibles según horarios configurados por padres
3. Generar estadísticas de uso para padres/tutores
4. Detectar instalación de aplicaciones potencialmente peligrosas

Este permiso es esencial para la funcionalidad principal de protección 
de menores. Los datos NO se comparten con terceros y se almacenan 
únicamente en el dispositivo local."
```

**Alternativa (si Play Store rechaza):**
Usar `<queries>` en AndroidManifest para listar apps específicas:

```xml
<queries>
    <package android:name="com.android.chrome" />
    <package android:name="com.android.browser" />
    <package android:name="com.facebook.katana" />
    <package android:name="com.instagram.android" />
    <!-- etc... -->
</queries>
```

### 5. Contenido de Política de Privacidad

Tu página https://guardianos.es/politica-privacidad debe incluir:

#### Datos Recopilados:
- ✅ Aplicaciones instaladas (nombres de paquete)
- ✅ Consultas DNS bloqueadas (dominios, no contenido)
- ✅ Horarios de uso de aplicaciones
- ✅ PIN parental (encriptado localmente)
- ❌ NO recopilamos: contenido de navegación, mensajes, ubicación

#### Almacenamiento:
- Todos los datos se almacenan localmente en el dispositivo
- NO se envían a servidores externos
- NO se comparten con terceros
- Datos permanecen en el dispositivo del usuario

#### Permisos Utilizados:
- **VPN Service**: Filtrado DNS (CleanBrowsing) sin inspeccionar tráfico
- **Usage Stats**: Monitoreo de apps para control parental
- **Query All Packages**: Listar apps instaladas para bloqueo selectivo

#### Derechos del Usuario:
- Eliminar todos los datos desde la app (Configuración → Limpiar historial)
- Desinstalar la app elimina todos los datos automáticamente
- Exportar estadísticas en CSV/JSON

### 6. Capturas de Pantalla (Play Console)

Necesitas:
- **Mínimo 2, máximo 8** capturas por dispositivo
- Resolución: 1080x1920 (portrait) o 1920x1080 (landscape)
- Formato: PNG o JPG (sin transparencias)
- Mostrar: Panel principal, VPN activo, control parental, estadísticas

### 7. Descripción Play Store

**Título corto** (max 30 caracteres):
```
GuardianOS Shield
```

**Descripción corta** (max 80 caracteres):
```
Control parental con filtrado DNS y monitoreo de apps para protección infantil
```

**Descripción larga**:
```
🛡️ GuardianOS Shield - Control Parental Avanzado

Protege a tus hijos mientras usan sus dispositivos Android con filtrado DNS 
profesional y monitoreo inteligente de aplicaciones.

✨ CARACTERÍSTICAS PRINCIPALES:

🔒 Filtrado DNS Seguro
• CleanBrowsing DNS integrado
• Bloqueo automático de contenido adulto
• Sin inspección de tráfico cifrado

📱 Monitoreo de Aplicaciones
• Control de horarios de uso
• Bloqueo selectivo de apps sensibles
• Estadísticas detalladas para padres

🔐 Protección PIN Parental
• Encriptación con AndroidKeyStore
• Los niños no pueden desactivar protecciones

📊 Estadísticas y Reportes
• Historial de sitios bloqueados
• Tiempo de uso por aplicación
• Exportación de datos en CSV/JSON

🌟 PRIVACIDAD GARANTIZADA:
• Todos los datos se almacenan localmente
• Sin servidores externos ni tracking
• Código open source auditado

💪 MODOS DE PROTECCIÓN:
• Recomendado: Monitoreo ligero
• Avanzado: VPN + filtrado DNS completo
• Manual: Control total personalizado

Ideal para padres que buscan protección efectiva sin comprometer la privacidad.
```

### 8. Clasificación de Contenido

- **Categoría**: Herramientas / Productividad
- **Público objetivo**: Padres y tutores
- **Clasificación**: Todos (PEGI 3)
- **Contiene anuncios**: NO
- **Compras dentro de la app**: NO

### 9. Checklist Final Pre-Publicación

- [ ] Keystore generado y guardado de forma segura
- [ ] Signing configurado en build.gradle.kts
- [ ] Compilación release exitosa (`./gradlew bundleRelease`)
- [ ] AAB firmado generado
- [ ] Política de privacidad publicada en https://guardianos.es/politica-privacidad
- [ ] Justificación QUERY_ALL_PACKAGES preparada
- [ ] Capturas de pantalla tomadas (mín. 2)
- [ ] Icono de app de alta resolución (512x512 PNG)
- [ ] Feature graphic (1024x500 PNG) para Play Store
- [ ] Descripción y textos revisados
- [ ] Testeado en múltiples dispositivos (diferentes APIs)
- [ ] Verificado funcionamiento en modo release (ProGuard)

## 📋 Comandos Resumen

```bash
# 1. Generar keystore (una sola vez)
keytool -genkey -v -keystore ~/guardianos-release.keystore -alias guardianos -keyalg RSA -keysize 2048 -validity 10000

# 2. Configurar variables
export KEYSTORE_PATH=~/guardianos-release.keystore
export KEYSTORE_PASSWORD=tu_password
export KEY_PASSWORD=tu_password

# 3. Compilar release
./gradlew bundleRelease

# 4. Verificar APK generado
ls -lh app/build/outputs/bundle/release/app-release.aab

# 5. Testear APK antes de subir
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

## 🚀 Subida a Play Console

1. Ir a https://play.google.com/console
2. Crear nueva aplicación
3. Subir AAB en "Producción" → "Crear versión"
4. Completar ficha de Play Store
5. Responder cuestionario de contenido
6. Enviar a revisión (tarda 1-7 días)

## ⚠️ Posibles Rechazos y Soluciones

**"QUERY_ALL_PACKAGES no justificado"**
→ Revisar formulario de permisos, agregar capturas mostrando funcionalidad

**"Política de privacidad inaccesible"**
→ Verificar que https://guardianos.es/politica-privacidad está online y no requiere login

**"App crash en dispositivo de prueba"**
→ Testear con ProGuard activado, revisar logs de Google Play Console

**"Funcionalidad VPN no declarada"**
→ En "Contenido de la app" marcar que usa VPN y explicar propósito

---

**Versión**: 1.1.0  
**Última actualización**: 5 de febrero de 2026
