package com.guardianos.shield.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// HelpScreen — Guía de uso para padres no técnicos
//
// Estructura:
//   • ¿Qué hace esta app?       — explicación en 3 líneas sin tecnicismos
//   • Configuración inicial     — pasos numerados con estado (hecho/pendiente)
//   • Cómo funciona cada cosa   — acordeón expandible por función
//   • Preguntas frecuentes      — acordeón expandible con dudas comunes
//   • Consejos de seguridad     — tarjetas de recomendación
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    isVpnActive: Boolean = false,
    isMonitoringActive: Boolean = false,
    hasAccessibilityService: Boolean = false,
    hasPin: Boolean = false,
    hasProfileConfigured: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Guía de uso", fontWeight = FontWeight.Bold)
                        Text(
                            "Para padres",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cabecera ──────────────────────────────────────────────────
            item { Spacer(Modifier.height(4.dp)) }
            item { HeroCard() }

            // ── Pasos de configuración ────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Checklist,
                    title = "Configuración inicial",
                    subtitle = "Sigue estos pasos una sola vez"
                )
            }
            item {
                SetupStepsCard(
                    isVpnActive = isVpnActive,
                    isMonitoringActive = isMonitoringActive,
                    hasAccessibilityService = hasAccessibilityService,
                    hasPin = hasPin,
                    hasProfileConfigured = hasProfileConfigured
                )
            }

            // ── Qué hace cada función ─────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Info,
                    title = "¿Para qué sirve cada función?",
                    subtitle = "Toca para ver la explicación"
                )
            }
            item { FeaturesAccordion() }

            // ── Preguntas frecuentes ──────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Help,
                    title = "Preguntas frecuentes",
                    subtitle = "Las dudas más habituales"
                )
            }
            item { FaqAccordion() }

            // ── Consejos ─────────────────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Lightbulb,
                    title = "Consejos de seguridad",
                    subtitle = "Recomendaciones para que funcione bien"
                )
            }
            item { TipsCard() }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta hero — qué es la app en un vistazo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "GuardianOS Shield protege el móvil de tu hijo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "La app actúa como un filtro invisible: bloquea páginas web peligrosas, controla qué aplicaciones puede usar el menor y en qué horarios, y te avisa cuando intenta saltarse las restricciones.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroPill(icon = Icons.Default.Dns,      label = "Filtra la red",    modifier = Modifier.weight(1f))
                HeroPill(icon = Icons.Default.Block,    label = "Bloquea apps",     modifier = Modifier.weight(1f))
                HeroPill(icon = Icons.Default.Schedule, label = "Controla horarios",modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pasos de configuración con estado visual
// ─────────────────────────────────────────────────────────────────────────────

private data class SetupStep(
    val numero: Int,
    val titulo: String,
    val descripcion: String,
    val dondeIr: String,
    val completado: Boolean
)

@Composable
private fun SetupStepsCard(
    isVpnActive: Boolean,
    isMonitoringActive: Boolean,
    hasAccessibilityService: Boolean,
    hasPin: Boolean,
    hasProfileConfigured: Boolean
) {
    val pasos = listOf(
        SetupStep(
            numero = 1,
            titulo = "Activa el filtro de red (VPN)",
            descripcion = "Pulsa el botón grande del panel principal para encender la protección. La primera vez el sistema te pedirá permiso: acepta.",
            dondeIr = "Pantalla principal → botón central",
            completado = isVpnActive
        ),
        SetupStep(
            numero = 2,
            titulo = "Crea un PIN de seguridad",
            descripcion = "El PIN impide que tu hijo cambie la configuración o desactive la app. Elige uno que solo tú conozcas.",
            dondeIr = "Ajustes → Seguridad → Cambiar PIN",
            completado = hasPin
        ),
        SetupStep(
            numero = 3,
            titulo = "Activa el monitoreo de apps",
            descripcion = "Permite a la app saber qué aplicaciones usa el menor. Si el sistema te pide un permiso especial de \"Uso de estadísticas\", acéptalo.",
            dondeIr = "Pantalla principal → sección Monitoreo",
            completado = isMonitoringActive
        ),
        SetupStep(
            numero = 4,
            titulo = "Activa el bloqueo de apps (accesibilidad)",
            descripcion = "Ve a los Ajustes del móvil → Accesibilidad → General → Aplicaciones descargadas → GuardianOS Shield y actívalo. Solo hay que hacerlo una vez.",
            dondeIr = "Ajustes del móvil → Accesibilidad → General → Apps descargadas",
            completado = hasAccessibilityService
        ),
        SetupStep(
            numero = 5,
            titulo = "Configura el perfil de tu hijo",
            descripcion = "Indica el nombre y la edad del menor y establece los horarios en que puede usar el móvil.",
            dondeIr = "Control parental → Perfil",
            completado = hasProfileConfigured
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val completados = pasos.count { it.completado }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$completados de ${pasos.size} pasos completados",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completados == pasos.size)
                        Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (completados == pasos.size) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "✓ Todo listo",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = completados.toFloat() / pasos.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (completados == pasos.size) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            pasos.forEach { paso ->
                SetupStepRow(paso = paso)
                if (paso.numero < pasos.size) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(paso: SetupStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Indicador de estado
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (paso.completado) Color(0xFF2E7D32)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (paso.completado) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completado",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    "${paso.numero}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = paso.titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (paso.completado)
                    Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = paso.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = paso.dondeIr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Acordeón de funciones
// ─────────────────────────────────────────────────────────────────────────────

private data class FeatureItem(
    val icon: ImageVector,
    val titulo: String,
    val resumen: String,
    val detalle: String,
    val color: Color
)

@Composable
private fun FeaturesAccordion() {
    val features = listOf(
        FeatureItem(
            icon = Icons.Default.Dns,
            titulo = "Filtro DNS (protección de red)",
            resumen = "Bloquea webs peligrosas automáticamente",
            detalle = "Cuando tu hijo navega por internet, todas las páginas web pasan primero por un filtro especializado (CleanBrowsing). Si la web contiene contenido adulto, violencia, apuestas, malware o redes sociales, el filtro la bloquea antes de que se llegue a cargar. Funciona en cualquier navegador del móvil, no solo en el navegador de GuardianOS.",
            color = Color(0xFF1565C0)
        ),
        FeatureItem(
            icon = Icons.Default.Language,
            titulo = "Navegador seguro integrado",
            resumen = "Un navegador con protección extra para el menor",
            detalle = "GuardianOS incluye su propio navegador web con una capa extra de seguridad. Activa automáticamente el SafeSearch estricto en Google, Bing y el Modo Restringido de YouTube. Si intentas abrir una web peligrosa, aparece una pantalla de bloqueo que explica por qué no se puede acceder. Es el lugar más seguro para que el menor navegue.",
            color = Color(0xFF00695C)
        ),
        FeatureItem(
            icon = Icons.Default.Block,
            titulo = "Bloqueo de aplicaciones",
            resumen = "Cierra automáticamente apps no permitidas",
            detalle = "Cuando el menor abre una aplicación bloqueada (Instagram, TikTok, WhatsApp, juegos, apuestas, etc.), GuardianOS la cierra al instante y muestra una pantalla que explica que está bloqueada. También detecta intentos de evasión con VPN o proxies y los bloquea sin excepción. Para que esto funcione, necesitas activar el permiso de accesibilidad una sola vez (paso 4 de la configuración).",
            color = Color(0xFFC62828)
        ),
        FeatureItem(
            icon = Icons.Default.Schedule,
            titulo = "Control de horarios",
            resumen = "Define cuándo puede usar el móvil",
            detalle = "Establece una franja horaria (por ejemplo, de 16:00 a 20:00) en la que el menor puede usar el móvil con normalidad. Fuera de ese horario, las apps bloqueadas se cierran automáticamente y el filtro web se vuelve más estricto. También admite horarios que cruzan la medianoche (ej: permits hasta las 8:00 de la mañana).",
            color = Color(0xFFE65100)
        ),
        FeatureItem(
            icon = Icons.Default.BarChart,
            titulo = "Estadísticas e historial",
            resumen = "Ve qué intentó abrir tu hijo",
            detalle = "La sección de Estadísticas muestra un resumen de todas las webs y apps bloqueadas, organizadas por día, semana o mes. Puedes ver cuántas veces intentó entrar en redes sociales, qué webs fueron bloqueadas y en qué categorías (contenido adulto, apuestas, etc.). Los usuarios Premium tienen acceso a 30 días de historial.",
            color = Color(0xFF4527A0)
        ),
        FeatureItem(
            icon = Icons.Default.EmojiEvents,
            titulo = "Sistema de rachas (TrustFlow)",
            resumen = "Premia el buen comportamiento",
            detalle = "Si el menor no intenta saltarse las restricciones durante varios días seguidos, el sistema le va dando más autonomía de forma gradual. Con 7 días de racha entra en Modo Explorador (se le avisa antes de bloquear). Con 30 días pasa a Guardián y tiene 60 minutos diarios de acceso libre. Esto incentiva el cumplimiento sin conflictos.",
            color = Color(0xFF2E7D32)
        ),
        FeatureItem(
            icon = Icons.Default.Category,
            titulo = "Bloqueo por categorías",
            resumen = "Elige qué tipo de contenido bloquear",
            detalle = "En Control Parental puedes activar o desactivar bloques por categoría: 🔞 Contenido adulto (siempre activo), 🎰 Apuestas y casino (siempre activo), 📱 Redes sociales y 🎮 Videojuegos. Importante: las apps educativas (Duolingo, Khan Academy, YouTube Kids, Toca Boca, etc.) están en lista blanca y nunca se bloquean aunque el toggle de videojuegos esté activo.",
            color = Color(0xFF7B1FA2)
        ),
        FeatureItem(
            icon = Icons.Default.Store,
            titulo = "Play Store fuera de horario",
            resumen = "Evita instalaciones no autorizadas",
            detalle = "GuardianOS detecta cuando el menor abre Google Play Store fuera del horario permitido y lo bloquea automáticamente. Así evitas que instale apps nuevas cuando no debe estar usando el móvil.",
            color = Color(0xFF1976D2)
        ),
        FeatureItem(
            icon = Icons.Default.VpnLock,
            titulo = "Bloqueo anti-evasión VPN",
            resumen = "Cierra VPNs y proxies que intentan saltarse el filtro",
            detalle = "GuardianOS detecta y bloquea automáticamente los principales intentos de evasión: Psiphon, Turbo VPN, Orbot/Tor, ProtonVPN, Hotspot Shield, Cloudflare Warp y otros. Cuando el menor intenta abrir una de estas apps, se cierra al instante, queda registrado en el historial y tú recibes una alerta urgente en el móvil. El bloqueo es incondicional: no puede desactivarse desde el perfil del menor.",
            color = Color(0xFFB71C1C)
        ),
        FeatureItem(
            icon = Icons.Default.NotificationsActive,
            titulo = "Resumen semanal para padres",
            resumen = "Informe automático cada domingo a las 20:00",
            detalle = "Sin tener que abrir la app, cada semana recibes una notificación con el resumen de actividad: cuántas webs se bloquearon, intentos de abrir redes sociales, juegos, y si hubo algún intento de evasión con VPN. Es el informe que te da la tranquilidad de saber que todo está funcionando.",
            color = Color(0xFF00695C)
        ),
        FeatureItem(
            icon = Icons.Default.FamilyRestroom,
            titulo = "Pacto Digital Familiar",
            resumen = "El menor puede pedir permisos al padre",
            detalle = "En lugar de saltarse las restricciones, el menor puede enviar una petición para que el padre la apruebe o rechace: más tiempo de uso o acceder a una web específica. El padre responde con su PIN. Así se fomenta la comunicación en lugar del conflicto.",
            color = Color(0xFF1565C0)
        )
    )

    var expanded by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            features.forEachIndexed { index, feature ->
                FeatureRow(
                    feature = feature,
                    isExpanded = expanded == index,
                    onClick = { expanded = if (expanded == index) null else index }
                )
                if (index < features.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: FeatureItem, isExpanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(feature.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    tint = feature.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    feature.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    feature.resumen,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = feature.color.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feature.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preguntas frecuentes
// ─────────────────────────────────────────────────────────────────────────────

private data class FaqItem(val pregunta: String, val respuesta: String)

@Composable
private fun FaqAccordion() {
    val faqs = listOf(
        FaqItem(
            pregunta = "¿La app necesita internet para funcionar?",
            respuesta = "El filtro de red sí necesita conexión a internet (está basado en servidores DNS externos que bloquean el contenido). Sin embargo, el bloqueo de apps y el control de horarios funcionan sin conexión, directamente en el móvil."
        ),
        FaqItem(
            pregunta = "Mi hijo apagó la WiFi y siguió navegando, ¿por qué?",
            respuesta = "El filtro DNS solo actúa cuando la VPN de GuardianOS está activada y con datos (WiFi o datos móviles). Si el menor activa el modo avión o desactiva los datos, el filtro de red no puede actuar. Para evitarlo, activa también el bloqueo de apps (servicio de accesibilidad) y añade los ajustes del sistema a las apps bloqueadas."
        ),
        FaqItem(
            pregunta = "¿Puede mi hijo desinstalar GuardianOS?",
            respuesta = "Si activas el modo Administrador de dispositivo (en Ajustes → Seguridad Avanzada → Anti-desinstalación), el menor no podrá desinstalar la app sin introducir el PIN parental. Es una capa de protección extra recomendada."
        ),
        FaqItem(
            pregunta = "La app bloqueó algo que no debería bloquear, ¿qué hago?",
            respuesta = "Puedes añadir ese dominio o web a la lista blanca desde Filtros personalizados → Lista de permitidos. También puedes revisar las categorías activas en Control Parental para ajustar qué tipos de contenido se bloquean."
        ),
        FaqItem(
            pregunta = "¿Por qué necesita el permiso de Accesibilidad?",
            respuesta = "El permiso de accesibilidad es la única forma de cerrar otras aplicaciones en Android sin necesidad de root. GuardianOS lo usa exclusivamente para bloquear las apps que tú configures. No lee el contenido de la pantalla ni accede a contraseñas."
        ),
        FaqItem(
            pregunta = "¿Funciona con Netflix, YouTube o Spotify?",
            respuesta = "GuardianOS controla el tiempo de pantalla mediante horarios. El servicio de accesibilidad detecta si alguna app está activa fuera del horario permitido y devuelve al menor a la pantalla de inicio, lo que afecta también a Netflix, YouTube, Spotify y similares. El filtro DNS no actúa sobre estas apps porque usan sus propios protocolos cifrados, pero el bloqueo por horario sí aplica. No es posible permitir o bloquear estas apps de forma individual dentro de los horarios configurados."
        ),
        FaqItem(
            pregunta = "¿Qué pasa si mi hijo tiene más de un móvil o una tablet?",
            respuesta = "GuardianOS debe instalarse en cada dispositivo que quieras proteger. La configuración y las estadísticas son independientes por dispositivo. La licencia Premium (pago único vitalicio de 14,99 €) es válida para un solo dispositivo. Si necesitas proteger varios dispositivos, deberás adquirir una licencia por cada uno."
        ),
        FaqItem(
            pregunta = "¿La app consume mucha batería?",
            respuesta = "El impacto en batería es mínimo. Para que el servicio no sea detenido por el sistema, ve a Ajustes del móvil → Batería → Optimización de batería → GuardianOS Shield → Sin restricciones. Esto es especialmente importante en móviles OPPO, Xiaomi y Huawei."
        ),
        FaqItem(
            pregunta = "¿Puede mi hijo usar una VPN para saltarse el filtro?",
            respuesta = "GuardianOS detecta y bloquea automáticamente las principales apps de VPN y evasión: Psiphon, Turbo VPN, Orbot/Tor, Hotspot Shield, ProtonVPN, Cloudflare Warp y otras. Si el menor intenta abrir alguna de estas apps, se bloquea al instante y queda registrado en el historial con una alerta urgente para ti. Sin embargo, si el menor configura manualmente una VPN desde los Ajustes del sistema, el filtro DNS podría verse afectado. Para reducir ese riesgo, activa el modo Administrador de dispositivo (Ajustes → Seguridad Avanzada → Anti-desinstalación) para impedir que el menor acceda a los ajustes del sistema."
        )
    )

    var expanded by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            faqs.forEachIndexed { index, faq ->
                FaqRow(
                    faq = faq,
                    isExpanded = expanded == index,
                    onClick = { expanded = if (expanded == index) null else index }
                )
                if (index < faqs.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqRow(faq: FaqItem, isExpanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .wrapContentSize(Alignment.Center)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                faq.pregunta,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = faq.respuesta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjetas de consejos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TipsCard() {
    val consejos = listOf(
        Triple(Icons.Default.Battery5Bar, Color(0xFF1565C0),
            "Excluye GuardianOS de la optimización de batería" to
            "Ve a Ajustes del móvil → Batería → Optimización. Busca GuardianOS Shield y selecciona \"Sin restricciones\" o \"No optimizar\". Si no lo haces, el sistema puede cerrar el servicio de monitoreo automáticamente."),
        Triple(Icons.Default.Lock, Color(0xFFC62828),
            "Pon un PIN difícil (y no se lo digas a tu hijo)" to
            "Evita fechas de cumpleaños u otras combinaciones fáciles. El PIN protege toda la configuración de la app. Sin él, cualquiera puede desactivar la protección."),
        Triple(Icons.Default.PhoneAndroid, Color(0xFF2E7D32),
            "En OPPO, Xiaomi y Huawei: activa los ajustes restringidos" to
            "En estos móviles, ve a Ajustes → Apps → GuardianOS Shield → tres puntos (⋮) → Permitir ajustes restringidos. Sin este paso, el bloqueo de apps puede no funcionar correctamente."),
        Triple(Icons.Default.Wifi, Color(0xFFE65100),
            "El filtro de red no actúa sin conexión" to
            "La VPN de GuardianOS requiere datos activos (WiFi o datos móviles) para filtrar las webs. Complementa con el bloqueo de apps para una protección más completa cuando no hay red."),
        Triple(Icons.Default.Chat, Color(0xFF4527A0),
            "Habla con tu hijo sobre las reglas" to
            "La tecnología ayuda, pero la conversación es fundamental. Explícale por qué existen las restricciones y establece reglas claras de uso. El Pacto Digital Familiar de la app es una buena herramienta para hacerlo juntos.")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            consejos.forEachIndexed { index, (icon, color, contenido) ->
                val (titulo, descripcion) = contenido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            titulo,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            descripcion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                if (index < consejos.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilitario: encabezado de sección
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
