# 🚀 GuardianOS Shield v1.1.0 — Preflight Review para Play Store

> Revisión completa realizada el **26-feb-2026** antes de la subida a Google Play Store.  
> Abarca: firma, seguridad de datos, idiomas, dispositivos de prueba, listings y riesgos.

---

## 🔴 PROBLEMAS CRÍTICOS (bloquean la subida)

### 1. Dos keystores con huellas SHA-256 DIFERENTES

| Archivo | Fecha | SHA-256 (primeros 4 bytes) | Estado |
|---|---|---|---|
| `app/guardianos-release-key.jks` | 18-feb-2026 | `24:21:51:9E…` | ✅ **ÉSTE firmó el AAB v1.1.0** |
| `guardianos-release-key.jks` (raíz) | 25-feb-2026 | `62:8C:06:27…` | ❌ **NO USAR — keystore distinto** |

**¿Qué pasa si usas el keystore equivocado?**  
Google Play rechazará *cualquier* actualización cuya firma no coincida con la primera subida.  
No hay recuperación: tendrías que publicar la app desde cero con nueva ficha y perder todas las reseñas.

**✅ Correcciones ya aplicadas:**
- `local.properties` ahora apunta al keystore correcto con ruta **absoluta**:  
  `KEYSTORE_FILE=/home/victor/guardianos-shield/app/guardianos-release-key.jks`
- Creado `guardianos-release-key-ADVERTENCIA.txt` junto al keystore erróneo.

**⚡ Acción manual que debes realizar:**
```bash
# Opción A (recomendada): mover el keystore erróneo a una carpeta de backup fuera del proyecto
mv guardianos-release-key.jks ~/backups/guardianos-release-key-UNUSED-25feb2026.jks

# Opción B: eliminarlo
rm guardianos-release-key.jks
```

---

### 2. Arquitectura de flavors incompatible con una sola ficha en Play Store

**Situación actual:**

| Flavor | applicationId | Resultado en Play Store |
|---|---|---|
| `langEs` | `com.guardianos.shield` | App A en Play Store |
| `langEn` | `com.guardianos.shield.en` | App B en Play Store (¡app SEPARADA!) |

Si subes ambos AABs a Play Store serán **dos aplicaciones distintas**, cada una con su propia página, sus propias reseñas y su propia revisión. Además, un usuario español que descargue la app en inglés no recibirá actualizaciones de la versión española.

**Lo que quieres:** una sola ficha donde Play Store muestre la descripción en ES si el dispositivo está en español, y en EN si está en inglés. La app en sí muestra el idioma según `Locale` del dispositivo.

**✅ Solución recomendada — Un solo AAB bilingüe:**

Paso 1 — Añadir `values-en/strings.xml` en `src/main/res/`:
```bash
mkdir -p app/src/main/res/values-en
cp app/src/langEn/res/values/strings.xml app/src/main/res/values-en/strings.xml
```

Paso 2 — En `app/build.gradle.kts`, añadir un tercer flavor (o cambiar `langEs`):
```kotlin
create("full") {
    dimension = "idioma"
    applicationId = "com.guardianos.shield"   // ← mismo ID para ambos idiomas
    // SIN resourceConfigurations → incluye ES y EN automáticamente
    setProperty("archivesBaseName", "guardianos-shield-v1.1.0")
}
```

Paso 3 — Subir UNO solo AAB `fullRelease` a Play Store.

Paso 4 — En Play Console → Ficha de la tienda → Añadir idioma `Español (España)` y `English (United States)` con sus textos (ya están en `fastlane/metadata/android/es-ES/` y `en-US/`).

**⚠️ En Play Console, la tienda muestra automáticamente la descripción en el idioma del dispositivo del usuario. El código de la app hace lo propio con los `strings.xml`.**

> Si prefieres mantener la arquitectura de dos flavors como APKs independientes para F-Droid (ES) y Play Store (ES+EN), es perfectamente válido — pero en ese caso sube SOLO el flavor `langEs` a Play Store y usa las descripciones ES+EN en Play Console.

---

## 🟡 PROBLEMAS IMPORTANTES

### 3. Contraseña del keystore débil y en texto plano

`local.properties` contiene `KEYSTORE_PASSWORD=Guardianos2026`.  
Aunque `.gitignore` la excluye del repositorio, si envías el archivo por email o lo copias en otro lugar queda expuesto.

**Recomendación:**
```bash
# Cambiar la contraseña del keystore (opción sin romper la firma):
keytool -storepasswd -keystore app/guardianos-release-key.jks
# → introducir contraseña actual y elegir una nueva fuerte (20+ chars, sin palabras del diccionario)
# → actualizar KEYSTORE_PASSWORD en local.properties
```

---

### 4. URL de política de privacidad — verificar antes de subir

El formulario de Play Console requiere una URL que devuelva HTTP 200 y cargue correctamente en móvil.

| Dato | Valor |
|---|---|
| URL introducida en Play Console | `https://guardianos.es/politica-privacidad` |
| URL alternativa EN | `https://guardianos.es/privacy-policy` |
| Archivo local | `politica-privacidad-shield.html` |

**⚡ Acción manual — verificar antes de subir:**
```bash
curl -sI https://guardianos.es/politica-privacidad | head -3
# Debe responder: HTTP/2 200
```

Si la URL devuelve 404, hay dos opciones:
- **Opción A**: Alojar el archivo HTML en `guardianos.es/politica-privacidad` (ruta sin extensión).
- **Opción B**: Usar la ruta con el nombre exacto del archivo: `https://guardianos.es/politica-privacidad-shield.html`  
  (y adaptar el PLAY_STORE_DATA_SAFETY.md en Sección 7).

> La web se aloja según `DEPLOY-WEB.md` — consultar ese fichero para asegurarse de que el deploy incluye la ruta correcta.

---

### 5. `minSdk = 31` excluye Android < 12 — implicaciones de prueba

| Dispositivo | Android | API | Estado |
|---|---|---|---|
| OPPO A53s | Android 12 | **API 31** | ✅ Mínimo soportado (borde inferior) |
| OPPO A80 | Android 15 | **API 35** | ✅ Sobre targetSdk=34, funciona en modo compat |

**El OPPO A53s con Android 12 (API 31) es el dispositivo de testing más crítico** porque prueba exactamente el límite inferior de compatibilidad. Es donde más fácilmente aparecerán crash por APIs no disponibles.

---

## 🟢 CORRECTO — No requiere acción

### ✅ Firma y seguridad
- Keystores excluidos de git (`.gitignore` con `*.jks`, `*.keystore`) ✅
- `allowBackup="false"` en `AndroidManifest.xml` ✅
- Firma v2 + v3 + v4 en release ✅
- `isMinifyEnabled = true` y `isShrinkResources = true` en release ✅
- `network_security_config.xml` aplicado ✅
- `FORCE_PREMIUM=false` en release (R8 lo elimina como dead code) ✅

### ✅ Data Safety Form (Sección "Seguridad de datos" en Play Console)
- Formulario completo en `PLAY_STORE_DATA_SAFETY.md` con textos listos para copiar ✅
- Includes: declaración VPN, declaración Accessibility Service, cuestionario IARC ✅
- Textos para permisos especiales en `PERMISSIONS_DECLARATION.md` ✅

### ✅ Listings de la tienda
- Título, descripción corta y descripción larga en ES y EN ✅
- 5 capturas de pantalla en ES y EN ✅
- `featureGraphic.png` (1024×500) en ES y EN ✅
- `icon.png` (512×512) en ES y EN ✅
- Changelogs `2.txt` (versionCode=2) en ES y EN — corregidos (quitada referencia F-Droid) ✅

### ✅ Manifest y permisos
- Todos los permisos críticos declarados con `tools:ignore` donde aplica ✅
- Servicios no exportados (`exported="false"`) excepto MainActivity ✅
- `targetSdk = 34`, `minSdk = 31` ✅
- `versionCode = 2`, `versionName = "1.1.0"` ✅

---

## 📱 Testing en dispositivos reales

### OPPO A80 — Android 15 (API 35)

**Issues conocidos documentados** (ver `ANDROID_COMPATIBILITY.md`):
- El sistema OPPO destruye sockets UIDs externos cuando la VPN está activa  
  (`destroyAllSocketForUid`, `blockedReasons=40`)
- Diagnóstico: `adb logcat | grep -E "netd|ConnectivityService|GuardianVPN"`

**Checklist de prueba en Android 15:**
- [ ] VPN DNS se activa (botón "Activar Protección")
- [ ] Internet funciona con VPN activa (abrir google.com en SafeBrowser)
- [ ] Dominio adulto queda bloqueado (probar `pornhub.com` → debe mostrar página de bloqueo)
- [ ] TikTok bloqueado tanto por DNS como por UsageStats
- [ ] `FOREGROUND_SERVICE_SPECIAL_USE` no genera error en logcat  
  (Android 14+ requiere declarar `foregroundServiceType` — ya está en manifest)
- [ ] Notificaciones visibles (POST_NOTIFICATIONS requerido Android 13+)
- [ ] PIN parental funciona y persiste tras reinicio
- [ ] Device Admin: el menor no puede desinstalar la app sin PIN

### OPPO A53s — Android 12 (API 31)

**Este es el dispositivo más crítico** (borde inferior del `minSdk`):
- [ ] App instala correctamente (verificar `minSdk=31` vs versión exacta del dispositivo)
- [ ] VPN DNS activa — confirmar que `addDnsServer` funciona en Android 12
- [ ] `AppBlockerAccessibilityService` aprobado en Settings > Accessibility (Android 12 cambia el flow de activación)
- [ ] `PACKAGE_USAGE_STATS` concedido desde `Settings.ACTION_USAGE_ACCESS_SETTINGS`
- [ ] `SYSTEM_ALERT_WINDOW`: verificar `Settings.canDrawOverlays()` en Android 12
- [ ] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: el diálogo aparece y se concede
- [ ] Horario parental: bloqueo fuera de horas funciona
- [ ] SafeBrowserActivity: WebView carga y bloquea correctamente
- [ ] **OPPO A53s específico**: verificar que la VPN NO desconecta internet (OPPO A80 tiene el bug documentado; el A53s con Android 12 puede tener comportamiento diferente)

### Acer Aspire E5-571G con Zorin OS (tu máquina de desarrollo)

**Para testing via emulador AVD:**
```bash
# Crear AVD con API 31 (Android 12)
$ANDROID_HOME/tools/bin/avdmanager create avd \
  -n "TestAndroid12" -k "system-images;android-31;google_apis_playstore;x86_64"

# Crear AVD con API 35 (Android 15)
$ANDROID_HOME/tools/bin/avdmanager create avd \
  -n "TestAndroid15" -k "system-images;android-35;google_apis_playstore;x86_64"

# Compilar e instalar debug
./gradlew assembleLangEsDebug && \
  adb install -r app/build/outputs/apk/langEs/debug/guardianos-shield-v1.1.0-es-debug.apk

# Ver logs en tiempo real
adb logcat -c && adb logcat | grep -E "GuardianVPN|UsageStatsMonitor|SafeBrowser|BillingManager"
```

> **Nota sobre el emulador y VPN**: Los emuladores de Android no soportan completamente VpnService.  
> Para probar VPN DNS real, necesitas dispositivo físico. Para el resto de funcionalidades (SafeBrowser, horarios, PIN, Pacto Digital), el emulador funciona correctamente.

---

## 📋 Checklist de acciones antes de subir a Play Store

### Acciones ya realizadas ✅
- [x] `local.properties` apunta al keystore correcto con ruta absoluta
- [x] Changelogs corregidos (sin mención a "F-Droid build")
- [x] `guardianos-release-key-ADVERTENCIA.txt` creado junto al keystore erróneo
- [x] `README.en.md` y `ARCHITECTURE.en.md` creados (documentación bilingüe)

### Acciones manuales — ANTES de subir el AAB

- [ ] **Keystore de la raíz**: mover/eliminar `guardianos-release-key.jks` (SHA diferente)
  ```bash
  mv guardianos-release-key.jks ~/backups/guardianos-key-UNUSED-25feb.jks
  ```

- [ ] **Verificar URL de política de privacidad**:
  ```bash
  curl -sI https://guardianos.es/politica-privacidad | grep "HTTP/"
  ```
  Debe responder `HTTP/2 200`. Si no, subir el HTML a esa ruta exacta.

- [ ] **Decidir estrategia de idiomas** (ver Problema #2):
  - Opción A: Subir SOLO `langEs` AAB + añadir listing EN en Play Console (más simple)
  - Opción B: Crear flavor `full` bilingüe y subir un solo AAB

- [ ] **Compilar y verificar AAB release definitivo**:
  ```bash
  ./gradlew bundleLangEsRelease --stacktrace
  # Verificar que usa el keystore correcto:
  keytool -list -keystore app/guardianos-release-key.jks -storepass Guardianos2026
  # SHA-256 debe ser: 24:21:51:9E...
  ```

- [ ] **Testing físico** en OPPO A53s (Android 12) y OPPO A80 (Android 15) con el APK release (no debug, ya que `FORCE_PREMIUM=false`)

### Acciones en Play Console — al subir

1. **Subir AAB**: `release/v1.1.0/guardianos-shield-v1.1.0-release.aab`  
   (o el nuevo compilado con el keystore verificado)

2. **App content > Data safety**: copiar textos de `PLAY_STORE_DATA_SAFETY.md`

3. **App content > Privacy policy URL**: `https://guardianos.es/politica-privacidad`

4. **App content > Permisos especiales** (formularios específicos):
   - VPN → textos de `PLAY_STORE_DATA_SAFETY.md` Sección 4
   - Accessibility Service → textos de `PLAY_STORE_DATA_SAFETY.md` Sección 5
   - `QUERY_ALL_PACKAGES` → textos de `PERMISSIONS_DECLARATION.md` Sección 2
   - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → `PERMISSIONS_DECLARATION.md` Sección 3
   - `SYSTEM_ALERT_WINDOW` → `PERMISSIONS_DECLARATION.md` Sección 1

5. **Store listing** → Añadir idioma `Español (España)`:
   - Copiar desde `fastlane/metadata/android/es-ES/`

6. **Store listing** → Añadir idioma `English (United States)`:
   - Copiar desde `fastlane/metadata/android/en-US/`

7. **App content > IARC / Content rating**: completar cuestionario (ver `PLAY_STORE_DATA_SAFETY.md` Sección 6)

8. **App content > Target audience**: Dirigida a adultos/padres (NO a menores directamente)

9. **Clasificación Play Families**: "Parental controls" en subcategoría "Herramientas para padres"

10. **Fijar precio**: Gratis + Premium in-app (€14,99 pago único `premium_guardianos`)

---

## 📊 Resumen ejecutivo de riesgos

| Riesgo | Severidad | Estado |
|---|---|---|
| Keystore incorrecto en raíz | 🔴 Crítico | ✅ Mitigado (local.properties corregido, advertencia creada) |
| Keystore aún en disco (raíz) | 🟠 Alto | ⚡ Pendiente: mover/eliminar manualmente |
| Dos applicationIds (bilingüismo) | 🟠 Alto | ⚡ Pendiente: decidir estrategia Play Store |
| URL política de privacidad | 🟡 Medio | ⚡ Pendiente: verificar curl |
| OPPO bug VPN Android 15 | 🟡 Medio | ⚡ Pendiente: test físico |
| Contraseña keystore débil | 🟡 Medio | ⚡ Pendiente: cambiar opcionalmente |
| AccessibilityService revisión Google | 🟡 Medio | ✅ Textos preparados en PERMISSIONS_DECLARATION.md |
| Changelog con "F-Droid" en Play Store | 🟢 Bajo | ✅ Corregido |

---

*Revisión realizada por GitHub Copilot — 26-feb-2026 | GuardianOS Shield v1.1.0*
