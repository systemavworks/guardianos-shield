# GuardianOS Shield — Declaraciones de Permisos para Play Console

Textos listos para copiar en **Play Console → Contenido de la app → Declaraciones de permisos**
o en el formulario de revisión especial requerido por Google.

---

## 1. `SYSTEM_ALERT_WINDOW` — Dibujar sobre otras apps

**¿Por qué lo necesita la app?**

GuardianOS Shield necesita mostrar una pantalla de bloqueo parental encima de apps restringidas
cuando el Servicio de Accesibilidad detecta que el menor ha abierto una aplicación que el tutor
ha marcado como no permitida.

Sin este permiso, la pantalla de bloqueo no podría superponerse al contenido de la app bloqueada
y el control parental sería ineficaz.

**Uso concreto en el código:** `AppBlockerAccessibilityService` comprueba `Settings.canDrawOverlays()`
y, si no está concedido, redirige al usuario a `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
El overlay usa `TYPE_ACCESSIBILITY_OVERLAY` (tipo que no requiere el permiso en la mayoría de casos),
y `SYSTEM_ALERT_WINDOW` actúa como respaldo en dispositivos donde el tipo de accesibilidad falla.

**No se usa para:** publicidad, seguimiento, ni acceso a datos del usuario.

---

## 2. `QUERY_ALL_PACKAGES` — Consultar apps instaladas

**¿Por qué lo necesita la app?**

La aplicación permite al tutor seleccionar, desde el panel parental, qué aplicaciones instaladas
en el dispositivo del menor deben ser monitorizadas o bloqueadas (función `SensitiveAppEntity`).

En Android 11 (API 30) y superior, sin este permiso el sistema oculta aplicaciones de terceros
al enumerar paquetes instalados con `PackageManager`, lo que impediría mostrar al tutor la lista
completa de apps del menor para configurar el control.

**No se usa para:** rastreo, publicidad, envío de datos a servidores externos ni ningún fin
que no sea la configuración del control parental local.

---

## 3. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — Exención de optimización de batería

**¿Por qué lo necesita la app?**

GuardianOS Shield incluye dos servicios en segundo plano que deben funcionar de forma continua
las 24 horas para garantizar la protección del menor:

- **`AppMonitorService`**: detecta cada 2 segundos qué app está en primer plano y bloquea apps
  no autorizadas.
- **`DnsFilterService`**: mantiene activo el filtro VPN DNS que bloquea contenido adulto.

Si Android aplica la optimización de batería a estos servicios, puede suspenderlos o matarlos,
dejando al menor sin protección. La exención es imprescindible para que la app cumpla su función.

**El permiso no afecta a otras apps** ni supone abuso del sistema; es el mecanismo estándar
recomendado por Android para servicios de protección y seguridad de largo recorrido.

---

## 4. `VPN` — Servicio VPN (declaración en Play Console)

**Nota:** La app usa la API estándar de Android `VpnService` **exclusivamente** para configurar
servidores DNS seguros en el dispositivo (CleanBrowsing Adult Filter: 185.228.168.168).

- **NO intercepta** paquetes de tráfico HTTP/HTTPS del usuario.
- **NO analiza** el contenido de las comunicaciones.
- **NO envía** tráfico a servidores propios.
- **NO almacena** datos de navegación en la nube.

El túnel VPN local solo redirige las consultas DNS a los servidores de CleanBrowsing, que bloquean
dominios de contenido adulto, redes sociales y malware en el lado del servidor.

**Categoría recomendada en Play Console:** Parental Controls / VPN Usage Declaration:
"La VPN se usa únicamente para enrutar consultas DNS a un servidor de filtrado de contenido
(CleanBrowsing Adult Filter) con el fin de proteger a menores de contenido inapropiado.
No se captura, almacena ni transmite tráfico de usuario."

---

## 5. `BIND_ACCESSIBILITY_SERVICE` — Servicio de Accesibilidad

**¿Por qué lo necesita la app?**

`AppBlockerAccessibilityService` utiliza la API de Accesibilidad de Android para detectar en
tiempo real qué aplicación está mostrándose en pantalla, sin necesidad de root ni permisos de
sistema especiales.

Cuando se detecta una app bloqueada por el tutor, el servicio lanza inmediatamente la pantalla
de bloqueo parental (`AppBlockedActivity`).

**Datos accedidos:** solo el nombre del paquete de la app en primer plano
(`AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`). No se accede a texto, contraseñas, contenido
de pantalla ni ningún otro dato sensible.

**Declaración para Google (formulario de uso de accesibilidad):**
"El servicio de accesibilidad de GuardianOS Shield solo lee el nombre del paquete de la app
activa en primer plano (`packageName` del evento). Este dato se compara localmente con la lista
de apps bloqueadas configurada por el tutor. No se envía ningún dato fuera del dispositivo,
no se registra historial de navegación, y no se accede al contenido visual de la pantalla."

---

## Vídeos de demostración recomendados (requeridos por Google)

Para la aprobación del Servicio de Accesibilidad Google exigirá un vídeo. Graba los siguientes
flujos:

1. **Flujo principal de bloqueo de app**: padre configura una app como bloqueada → hijo la abre
   → aparece pantalla de bloqueo parental inmediatamente.
2. **Configuración del tutor**: inicio de la app, PIN parental, añadir app a lista de bloqueadas.
3. **Control de horarios**: configurar horario permitido → intentar usar el dispositivo fuera
   del horario → bloqueo activado.
4. **Filtro DNS VPN**: activar la VPN → intentar visitar un dominio de contenido adulto en el
   navegador seguro integrado → página de bloqueo.

**Formato recomendado:** MP4, 1080p, sin audio o con voz en off en español/inglés,
duración 1–3 minutos por flujo.
