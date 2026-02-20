# 📦 Subir APK a GitHub y Enlazar desde tu Web

## Paso 1: Compilar APK de Release

```bash
cd ~/guardianos-shield

# Compilar APK firmado (necesitas haber configurado keystore primero)
./gradlew assembleRelease

# Verificar que se creó
ls -lh app/build/outputs/apk/release/app-release.apk
```

**Si falta keystore**, primero genera uno:
```bash
keytool -genkey -v -keystore guardianos-release.keystore \
  -alias guardianos -keyalg RSA -keysize 2048 -validity 10000
```

Y configura `app/build.gradle.kts` con signing (ver GUIA_PLAY_STORE.md sección 3)

---

## Paso 2: Crear Release en GitHub

### Opción A: Desde terminal con `gh` CLI

```bash
# Instalar gh si no lo tienes
# Ubuntu/Debian:
sudo apt install gh

# Fedora:
sudo dnf install gh

# Autenticar con GitHub
gh auth login

# Commit y push de cambios actuales
git add .
git commit -m "feat: v1.1.0 - Seguridad mejorada y persistencia de estado"
git push origin main

# Crear release con el APK
gh release create v1.1.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "GuardianOS Shield v1.1.0" \
  --notes "🛡️ Versión 1.1.0

✅ Filtrado DNS con CleanBrowsing
✅ Control parental con PIN encriptado (SHA-256 + AES256-GCM)
✅ Monitoreo de aplicaciones optimizado
✅ Estadísticas detalladas de uso
✅ Persistencia de estado VPN y modo
✅ Notificaciones cada 5 minutos (reducción de spam)

🔒 Mejoras de seguridad:
- PIN parental con EncryptedSharedPreferences
- Backup deshabilitado (android:allowBackup=false)
- Logs protegidos en builds release
- ProGuard/R8 activado

📦 Tamaño: ~25MB
📱 Requiere: Android 7.0+
🔑 Permisos: VPN, UsageStats, Notificaciones"
```

### Opción B: Desde interfaz web de GitHub

1. **Ir a tu repositorio**: https://github.com/systemavworks/guardianos-shield

2. **Hacer commit de cambios**:
   ```bash
   cd ~/guardianos-shield
   git add .
   git commit -m "feat: v1.1.0 - Seguridad mejorada"
   git push origin main
   ```

3. **Crear Release**:
   - Ir a: https://github.com/systemavworks/guardianos-shield/releases
   - Clic en **"Create a new release"**
   - **Tag version**: `v1.1.0`
   - **Release title**: `GuardianOS Shield v1.1.0`
   - **Description**: Copiar las notas de arriba
   - **Attach binaries**: Arrastrar `app-release.apk`
   - Clic en **"Publish release"**

---

## Paso 3: Obtener URL de Descarga Directa

Después de crear el release, la URL será:

```
https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk
```

**Formato genérico**:
```
https://github.com/{usuario}/{repo}/releases/download/{tag}/{archivo}
```

Para tu caso:
```
https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk
```

---

## Paso 4: Enlazar desde tu Web

### HTML para https://guardianos.es/shield/

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>GuardianOS Shield - Descarga</title>
  <style>
    body {
      font-family: 'Inter', sans-serif;
      background: #0f1c2e;
      color: #e2e8f0;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      margin: 0;
      padding: 2rem;
    }
    .container {
      max-width: 600px;
      text-align: center;
      background: #1a2332;
      padding: 3rem;
      border-radius: 16px;
      box-shadow: 0 10px 40px rgba(0,0,0,0.3);
    }
    h1 {
      font-size: 2.5rem;
      margin-bottom: 1rem;
      background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .subtitle {
      color: #94a3b8;
      margin-bottom: 2rem;
      font-size: 1.1rem;
    }
    .download-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
      color: white;
      padding: 1.25rem 2.5rem;
      border-radius: 12px;
      text-decoration: none;
      font-weight: 600;
      font-size: 1.2rem;
      transition: transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 4px 20px rgba(59, 130, 246, 0.3);
    }
    .download-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 30px rgba(59, 130, 246, 0.5);
    }
    .version {
      margin-top: 2rem;
      color: #64748b;
      font-size: 0.9rem;
    }
    .features {
      margin-top: 2rem;
      text-align: left;
      background: rgba(59, 130, 246, 0.1);
      padding: 1.5rem;
      border-radius: 8px;
      border-left: 4px solid #3b82f6;
    }
    .features ul {
      margin: 0;
      padding-left: 1.5rem;
      color: #94a3b8;
    }
    .features li {
      margin-bottom: 0.5rem;
    }
    .warning {
      margin-top: 1.5rem;
      background: rgba(234, 179, 8, 0.1);
      border-left: 4px solid #eab308;
      padding: 1rem;
      border-radius: 4px;
      font-size: 0.9rem;
      color: #fbbf24;
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>🛡️ GuardianOS Shield</h1>
    <p class="subtitle">Control Parental con Filtrado DNS Seguro</p>
    
    <a href="https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk" 
       class="download-btn"
       download="GuardianOS-Shield-v1.1.0.apk">
      📥 Descargar APK v1.1.0
    </a>
    
    <div class="version">
      Versión 1.1.0 • 25 MB • Android 7.0+
    </div>
    
    <div class="features">
      <strong style="color: #3b82f6;">✨ Características:</strong>
      <ul>
        <li>Filtrado DNS con CleanBrowsing</li>
        <li>Control parental con PIN encriptado</li>
        <li>Monitoreo de aplicaciones</li>
        <li>Estadísticas detalladas</li>
        <li>100% código abierto</li>
      </ul>
    </div>
    
    <div class="warning">
      ⚠️ <strong>Importante:</strong> Activa "Instalar desde fuentes desconocidas" 
      en tu dispositivo para instalar esta APK.
    </div>
    
    <p style="margin-top: 2rem; font-size: 0.9rem; color: #64748b;">
      <a href="https://github.com/systemavworks/guardianos-shield" 
         style="color: #3b82f6; text-decoration: none;">
        Ver código fuente en GitHub →
      </a>
    </p>
  </div>
</body>
</html>
```

---

## Paso 5: Actualizar versiones futuras

### Cada vez que actualices la app:

```bash
# 1. Incrementar versión en app/build.gradle.kts
# versionCode = 3
# versionName = "1.2.0"

# 2. Compilar nuevo APK
./gradlew assembleRelease

# 3. Commit cambios
git add .
git commit -m "feat: v1.2.0 - Nueva funcionalidad"
git push origin main

# 4. Crear nuevo release
gh release create v1.2.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "GuardianOS Shield v1.2.0" \
  --notes "🚀 Nuevas funcionalidades..."

# 5. Actualizar HTML en tu web con nueva URL:
# https://github.com/systemavworks/guardianos-shield/releases/download/v1.2.0/app-release.apk
```

---

## Paso 6: Verificar descarga

Después de publicar, prueba el enlace:

```bash
# Descargar con curl para verificar
curl -L -o test-download.apk \
  "https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk"

# Verificar tamaño
ls -lh test-download.apk

# Limpiar
rm test-download.apk
```

---

## Alternativa: GitHub Pages para landing page

Si quieres alojar el HTML directamente en GitHub:

1. **Crear rama gh-pages**:
   ```bash
   git checkout -b gh-pages
   ```

2. **Crear index.html** con el código de arriba

3. **Push**:
   ```bash
   git add index.html
   git commit -m "docs: Add download page"
   git push origin gh-pages
   ```

4. **Configurar GitHub Pages**:
   - Ir a Settings → Pages
   - Source: gh-pages branch
   - URL será: https://systemavworks.github.io/guardianos-shield/

5. **Redireccionar desde tu dominio**:
   En tu hosting de guardianos.es, crear redirección:
   ```
   https://guardianos.es/shield/ → https://systemavworks.github.io/guardianos-shield/
   ```

---

## Comandos Resumidos

```bash
# Compilar APK
cd ~/guardianos-shield
./gradlew assembleRelease

# Verificar APK
ls -lh app/build/outputs/apk/release/app-release.apk

# Subir a Git
git add .
git commit -m "feat: v1.1.0 - Release inicial"
git push origin main

# Crear release en GitHub (opción CLI)
gh release create v1.1.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "GuardianOS Shield v1.1.0" \
  --notes "Primera versión pública"

# URL de descarga resultante:
# https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk
```

---

## 🎯 Resultado Final

**Tu web**: https://guardianos.es/shield/  
**Descarga directa**: https://github.com/systemavworks/guardianos-shield/releases/download/v1.1.0/app-release.apk  
**Código fuente**: https://github.com/systemavworks/guardianos-shield  

Los usuarios podrán descargar la APK directamente desde tu web, y tú tendrás control total sobre las versiones en GitHub Releases.
