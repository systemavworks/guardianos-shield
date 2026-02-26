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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.R

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
                        Text(stringResource(R.string.help_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.help_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.help_back_desc))
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
                    title = stringResource(R.string.help_sec_setup_title),
                    subtitle = stringResource(R.string.help_sec_setup_subtitle)
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
                    title = stringResource(R.string.help_sec_features_title),
                    subtitle = stringResource(R.string.help_sec_features_subtitle)
                )
            }
            item { FeaturesAccordion() }

            // ── Preguntas frecuentes ──────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Help,
                    title = stringResource(R.string.help_sec_faq_title),
                    subtitle = stringResource(R.string.help_sec_faq_subtitle)
                )
            }
            item { FaqAccordion() }

            // ── Consejos ─────────────────────────────────────────────────
            item {
                SectionTitle(
                    icon = Icons.Default.Lightbulb,
                    title = stringResource(R.string.help_sec_tips_title),
                    subtitle = stringResource(R.string.help_sec_tips_subtitle)
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
                text = stringResource(R.string.help_hero_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.help_hero_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroPill(icon = Icons.Default.Dns,      label = stringResource(R.string.help_pill_network),    modifier = Modifier.weight(1f))
                HeroPill(icon = Icons.Default.Block,    label = stringResource(R.string.help_pill_apps),       modifier = Modifier.weight(1f))
                HeroPill(icon = Icons.Default.Schedule, label = stringResource(R.string.help_pill_schedules),  modifier = Modifier.weight(1f))
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
            titulo = stringResource(R.string.help_step1_title),
            descripcion = stringResource(R.string.help_step1_desc),
            dondeIr = stringResource(R.string.help_step1_where),
            completado = isVpnActive
        ),
        SetupStep(
            numero = 2,
            titulo = stringResource(R.string.help_step2_title),
            descripcion = stringResource(R.string.help_step2_desc),
            dondeIr = stringResource(R.string.help_step2_where),
            completado = hasPin
        ),
        SetupStep(
            numero = 3,
            titulo = stringResource(R.string.help_step3_title),
            descripcion = stringResource(R.string.help_step3_desc),
            dondeIr = stringResource(R.string.help_step3_where),
            completado = isMonitoringActive
        ),
        SetupStep(
            numero = 4,
            titulo = stringResource(R.string.help_step4_title),
            descripcion = stringResource(R.string.help_step4_desc),
            dondeIr = stringResource(R.string.help_step4_where),
            completado = hasAccessibilityService
        ),
        SetupStep(
            numero = 5,
            titulo = stringResource(R.string.help_step5_title),
            descripcion = stringResource(R.string.help_step5_desc),
            dondeIr = stringResource(R.string.help_step5_where),
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
                    stringResource(R.string.help_steps_progress, completados, pasos.size),
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
                            stringResource(R.string.help_steps_all_done),
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
                    contentDescription = stringResource(R.string.help_step_done_desc),
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
            titulo = stringResource(R.string.help_feat1_title),
            resumen = stringResource(R.string.help_feat1_summary),
            detalle = stringResource(R.string.help_feat1_detail),
            color = Color(0xFF1565C0)
        ),
        FeatureItem(
            icon = Icons.Default.Language,
            titulo = stringResource(R.string.help_feat2_title),
            resumen = stringResource(R.string.help_feat2_summary),
            detalle = stringResource(R.string.help_feat2_detail),
            color = Color(0xFF00695C)
        ),
        FeatureItem(
            icon = Icons.Default.Block,
            titulo = stringResource(R.string.help_feat3_title),
            resumen = stringResource(R.string.help_feat3_summary),
            detalle = stringResource(R.string.help_feat3_detail),
            color = Color(0xFFC62828)
        ),
        FeatureItem(
            icon = Icons.Default.Schedule,
            titulo = stringResource(R.string.help_feat4_title),
            resumen = stringResource(R.string.help_feat4_summary),
            detalle = stringResource(R.string.help_feat4_detail),
            color = Color(0xFFE65100)
        ),
        FeatureItem(
            icon = Icons.Default.BarChart,
            titulo = stringResource(R.string.help_feat5_title),
            resumen = stringResource(R.string.help_feat5_summary),
            detalle = stringResource(R.string.help_feat5_detail),
            color = Color(0xFF4527A0)
        ),
        FeatureItem(
            icon = Icons.Default.EmojiEvents,
            titulo = stringResource(R.string.help_feat6_title),
            resumen = stringResource(R.string.help_feat6_summary),
            detalle = stringResource(R.string.help_feat6_detail),
            color = Color(0xFF2E7D32)
        ),
        FeatureItem(
            icon = Icons.Default.Category,
            titulo = stringResource(R.string.help_feat7_title),
            resumen = stringResource(R.string.help_feat7_summary),
            detalle = stringResource(R.string.help_feat7_detail),
            color = Color(0xFF7B1FA2)
        ),
        FeatureItem(
            icon = Icons.Default.Store,
            titulo = stringResource(R.string.help_feat8_title),
            resumen = stringResource(R.string.help_feat8_summary),
            detalle = stringResource(R.string.help_feat8_detail),
            color = Color(0xFF1976D2)
        ),
        FeatureItem(
            icon = Icons.Default.VpnLock,
            titulo = stringResource(R.string.help_feat9_title),
            resumen = stringResource(R.string.help_feat9_summary),
            detalle = stringResource(R.string.help_feat9_detail),
            color = Color(0xFFB71C1C)
        ),
        FeatureItem(
            icon = Icons.Default.NotificationsActive,
            titulo = stringResource(R.string.help_feat10_title),
            resumen = stringResource(R.string.help_feat10_summary),
            detalle = stringResource(R.string.help_feat10_detail),
            color = Color(0xFF00695C)
        ),
        FeatureItem(
            icon = Icons.Default.FamilyRestroom,
            titulo = stringResource(R.string.help_feat11_title),
            resumen = stringResource(R.string.help_feat11_summary),
            detalle = stringResource(R.string.help_feat11_detail),
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
        FaqItem(stringResource(R.string.help_faq1_q), stringResource(R.string.help_faq1_a)),
        FaqItem(stringResource(R.string.help_faq2_q), stringResource(R.string.help_faq2_a)),
        FaqItem(stringResource(R.string.help_faq3_q), stringResource(R.string.help_faq3_a)),
        FaqItem(stringResource(R.string.help_faq4_q), stringResource(R.string.help_faq4_a)),
        FaqItem(stringResource(R.string.help_faq5_q), stringResource(R.string.help_faq5_a)),
        FaqItem(stringResource(R.string.help_faq6_q), stringResource(R.string.help_faq6_a)),
        FaqItem(stringResource(R.string.help_faq7_q), stringResource(R.string.help_faq7_a)),
        FaqItem(stringResource(R.string.help_faq8_q), stringResource(R.string.help_faq8_a)),
        FaqItem(stringResource(R.string.help_faq9_q), stringResource(R.string.help_faq9_a))
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
            stringResource(R.string.help_tip1_title) to stringResource(R.string.help_tip1_desc)),
        Triple(Icons.Default.Lock, Color(0xFFC62828),
            stringResource(R.string.help_tip2_title) to stringResource(R.string.help_tip2_desc)),
        Triple(Icons.Default.PhoneAndroid, Color(0xFF2E7D32),
            stringResource(R.string.help_tip3_title) to stringResource(R.string.help_tip3_desc)),
        Triple(Icons.Default.Wifi, Color(0xFFE65100),
            stringResource(R.string.help_tip4_title) to stringResource(R.string.help_tip4_desc)),
        Triple(Icons.Default.Chat, Color(0xFF4527A0),
            stringResource(R.string.help_tip5_title) to stringResource(R.string.help_tip5_desc))
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
