# 📦 Guía de Distribución Web - GuardianOS Shield

## 🌐 Distribución desde https://guardianos.es/shield

### 1. Generar APK Release
```bash
cd /home/victor/guardianos-shield
./build-release-optimized.sh
```

El APK se generará en:
```
app/build/outputs/apk/release/guardianos-shield-v1.1.0-release.apk
```

### 2. Verificar APK
```bash
# Ver información del APK
aapt dump badging app/build/outputs/apk/release/guardianos-shield-v*.apk | head -5

# Ver tamaño
ls -lh app/build/outputs/apk/release/*.apk
```

### 3. Subir a tu servidor web

**Opción A: Via SCP (recomendado)**
```bash
# Copiar APK a tu servidor
scp app/build/outputs/apk/release/guardianos-shield-v1.1.0-release.apk \
    usuario@guardianos.es:/var/www/guardianos.es/shield/

# Renombrar para URL limpia (opcional)
ssh usuario@guardianos.es "cd /var/www/guardianos.es/shield && \
    cp guardianos-shield-v1.1.0-release.apk guardianos-shield-latest.apk"
```

**Opción B: Via FTP/SFTP**
- Usar FileZilla o cliente FTP
- Subir a: `/public_html/shield/` o `/var/www/guardianos.es/shield/`

**Opción C: Via panel de control del hosting**
- Acceder al File Manager de tu hosting
- Navegar a la carpeta `shield/`
- Subir el archivo APK

### 4. Configurar página de descarga

Crear `index.html` en tu servidor:

```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Descargar GuardianOS Shield</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
            text-align: center;
        }
        .download-btn {
            display: inline-block;
            background: #4CAF50;
            color: white;
            padding: 15px 40px;
            text-decoration: none;
            border-radius: 5px;
            font-size: 18px;
            margin: 20px 0;
        }
        .download-btn:hover {
            background: #45a049;
        }
        .version {
            color: #666;
            font-size: 14px;
        }
        .warning {
            background: #fff3cd;
            border: 1px solid #ffc107;
            border-radius: 5px;
            padding: 15px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <h1>🛡️ GuardianOS Shield</h1>
    <p>Control parental con filtrado DNS y monitoreo de aplicaciones</p>
    
    <a href="guardianos-shield-latest.apk" class="download-btn" download>
        📥 Descargar APK
    </a>
    
    <p class="version">Versión 1.1.0 | Tamaño: ~25MB</p>
    
    <div class="warning">
        <strong>⚠️ Instalación desde fuentes desconocidas</strong>
        <p>Para instalar esta app, necesitas habilitar "Fuentes desconocidas" en:</p>
        <p><em>Ajustes → Seguridad → Instalar apps de fuentes desconocidas</em></p>
    </div>
    
    <h3>✨ Características</h3>
    <ul style="text-align: left;">
        <li>🔒 Filtrado DNS transparente (CleanBrowsing Adult Filter)</li>
        <li>📱 Monitoreo de aplicaciones en tiempo real</li>
        <li>🚫 Bloqueo de contenido adulto y redes sociales</li>
        <li>📊 Estadísticas de uso y sitios bloqueados</li>
        <li>🔐 Protección con PIN parental</li>
        <li>⏰ Horarios personalizables por perfil</li>
    </ul>
    
    <h3>📋 Requisitos</h3>
    <ul style="text-align: left;">
        <li>Android 7.0 (Nougat) o superior</li>
        <li>Permisos: VPN, UsageStats, Notificaciones</li>
        <li>Sin conexión a servidores externos (100% local)</li>
    </ul>
    
    <hr>
    <p><small>© 2026 Guardianos ES | <a href="https://guardianos.es">guardianos.es</a></small></p>
</body>
</html>
```

### 5. Configurar .htaccess (Apache)

Para mejor SEO y seguridad:

```apache
# Forzar descarga de APK
<FilesMatch "\.apk$">
    Header set Content-Disposition "attachment"
    Header set Content-Type "application/vnd.android.package-archive"
</FilesMatch>

# Prevenir indexación de APKs directos (opcional)
<Files "*.apk">
    Header set X-Robots-Tag "noindex, nofollow"
</Files>

# Habilitar compresión para HTML
<IfModule mod_deflate.c>
    AddOutputFilterByType DEFLATE text/html text/css application/javascript
</IfModule>
```

### 6. Actualizar robots.txt

```
User-agent: *
Allow: /shield/
Disallow: /shield/*.apk
```

### 7. Verificar instalación

Después de subir, verifica:

1. **Descarga funciona**: https://guardianos.es/shield/guardianos-shield-latest.apk
2. **Página carga**: https://guardianos.es/shield/
3. **APK instala**: Descargar en Android y probar instalación

### 8. Promoción

Comparte el enlace:
```
https://guardianos.es/shield
```

O código QR (generar en qr-code-generator.com):
```
URL: https://guardianos.es/shield/guardianos-shield-latest.apk
```

### 9. Actualizaciones futuras

Cuando compiles nueva versión:

```bash
# 1. Incrementar versionCode y versionName en app/build.gradle.kts
# 2. Compilar nuevo APK
./build-release-optimized.sh

# 3. Subir al servidor
scp app/build/outputs/apk/release/guardianos-shield-v1.2.0-release.apk \
    usuario@guardianos.es:/var/www/guardianos.es/shield/

# 4. Actualizar enlace 'latest'
ssh usuario@guardianos.es "cd /var/www/guardianos.es/shield && \
    ln -sf guardianos-shield-v1.2.0-release.apk guardianos-shield-latest.apk"
```

---

## 🔐 Notas de Seguridad

1. **HTTPS obligatorio**: Asegura que tu web usa HTTPS para descargas seguras
2. **Checksum**: Considera publicar SHA256 del APK:
   ```bash
   sha256sum app/build/outputs/apk/release/*.apk > SHA256SUMS.txt
   ```
3. **Verificación**: Los usuarios pueden verificar integridad del APK

---

## 📊 Analytics (opcional)

Para trackear descargas, agregar a index.html:

```html
<script>
document.querySelector('.download-btn').addEventListener('click', function() {
    // Google Analytics
    gtag('event', 'download', {'event_category': 'APK', 'event_label': 'v1.1.0'});
    
    // O simple counter
    fetch('/api/count-download.php?app=shield&v=1.1.0');
});
</script>
```

---

**¡Tu app estará lista para descargar en guardianos.es/shield!** 🎉
