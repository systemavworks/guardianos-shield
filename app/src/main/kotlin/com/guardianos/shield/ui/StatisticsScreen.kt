// app/src/main/java/com/guardianos/shield/ui/StatisticsScreen.kt
package com.guardianos.shield.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardianos.shield.data.BlockedSiteEntity
import com.guardianos.shield.data.StatisticEntity
import com.guardianos.shield.billing.FreeTierLimits
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    todayBlocked: Int,
    weeklyStats: List<StatisticEntity>,
    recentBlocked: List<BlockedSiteEntity>,
    onBack: () -> Unit,
    isPremium: Boolean = false,
    onShowPremium: () -> Unit = {}
) {
    var selectedPeriod by remember { mutableStateOf("Hoy") }

    val now = System.currentTimeMillis()
    // FREE: historial limitado a 48h
    val cutoffMs = now - FreeTierLimits.MAX_HISTORY_HOURS * 60 * 60 * 1_000L
    // Corte temporal según período seleccionado
    val periodCutoffMs = when (selectedPeriod) {
        "Hoy"    -> now - 24 * 60 * 60 * 1_000L
        "Semana" -> now - 7 * 24 * 60 * 60 * 1_000L
        "Mes"    -> now - 30 * 24 * 60 * 60 * 1_000L
        "Año"    -> now - 365 * 24 * 60 * 60 * 1_000L
        else     -> now - 24 * 60 * 60 * 1_000L
    }
    // Filtrar por período + límite FREE, excluyendo registros de uso permitido
    val visibleBlocked = recentBlocked
        .filter { it.timestamp >= if (isPremium) periodCutoffMs else maxOf(cutoffMs, periodCutoffMs) }
        .filter { it.category != "APP_PERMITIDA" && it.category != "USO_RESPONSABLE" }
    // Contadores de categoría calculados desde datos reales del período activo
    val adultBlocked   = visibleBlocked.count { it.category == "ADULT" }
    val malwareBlocked = visibleBlocked.count { it.category == "MALWARE" || it.category == "MALWARE_PHISHING" }
    val socialBlocked  = visibleBlocked.count { it.category == "SOCIAL_MEDIA" || it.category == "APP_BLOQUEADA" }
    val gamblingBlocked = visibleBlocked.count { it.category == "GAMBLING" }
    val gamingBlocked  = visibleBlocked.count { it.category == "GAMING" }
    // Filtrar barras del gráfico según período seleccionado
    val chartStats = when (selectedPeriod) {
        "Hoy"    -> weeklyStats.takeLast(1)
        "Semana" -> weeklyStats.takeLast(7)
        else     -> weeklyStats  // Mes y Año: todos los disponibles (hasta 30 días)
    }

    // Banner de aviso FREE sobre datos limitados
    var showFreeLimitBanner by remember { mutableStateOf(!isPremium) }

    // Dialog gate para exportar
    var showExportGate by remember { mutableStateOf(false) }
    if (showExportGate) {
        PremiumGateDialog(
            feature = PremiumFeature.EXPORTAR_HISTORIAL,
            onUpgrade = { showExportGate = false; onShowPremium() },
            onDismiss = { showExportGate = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isPremium) { /* TODO: exportar */ }
                        else showExportGate = true
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Exportar historial",
                            tint = if (isPremium)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner FREE si aplica
            if (!isPremium && showFreeLimitBanner) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "⚠️ Plan FREE: solo se muestran las últimas 48h. Actualiza a Premium para 30 días.",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color(0xFFE65100),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showFreeLimitBanner = false },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            item {
                QuickSummaryCard(
                    todayBlocked = todayBlocked,
                    weeklyBlocked = weeklyStats.takeLast(7).sumOf { it.totalBlocked },
                    monthlyBlocked = weeklyStats.sumOf { it.totalBlocked }
                )
            }

            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Amenazas bloqueadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BarChart(
                            data = chartStats.map { it.totalBlocked },
                            labels = chartStats.map { stat ->
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateKey)
                                android.text.format.DateFormat.format("dd/MM", date).toString()
                            }
                        )
                    }
                }
            }

            item {
                CategoryBreakdownCard(
                    adultContent = adultBlocked,
                    malware = malwareBlocked,
                    socialMedia = socialBlocked,
                    gambling = gamblingBlocked,
                    gaming = gamingBlocked
                )
            }

            item {
                Text(
                    "Apps y sitios bloqueados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Incluye apps de redes sociales y navegación web",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(visibleBlocked.groupBy { it.domain }
                .map { it.key to it.value.size }
                .sortedByDescending { it.second }
                .take(20)
            ) { (domain, count) ->
                TopBlockedSiteItem(domain = domain, count = count)
            }

            item {
                RecommendationsCard(
                    totalInPeriod = visibleBlocked.size,
                    adultBlocked = adultBlocked,
                    gamblingBlocked = gamblingBlocked,
                    selectedPeriod = selectedPeriod
                )
            }
        }
    }
}

@Composable
fun QuickSummaryCard(
    todayBlocked: Int,
    weeklyBlocked: Int,
    monthlyBlocked: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = todayBlocked.toString(),
                label = "Hoy",
                icon = Icons.Default.Check,
                color = Color(0xFF4CAF50)
            )
            
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )
            
            StatItem(
                value = weeklyBlocked.toString(),
                label = "Esta semana",
                icon = Icons.Default.DateRange,
                color = Color(0xFF2196F3)
            )
            
            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )
            
            StatItem(
                value = monthlyBlocked.toString(),
                label = "Este mes",
                icon = Icons.Default.Star,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf("Hoy", "Semana", "Mes", "Año")
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            periods.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(period) },
                    leadingIcon = if (selectedPeriod == period) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOrNull() ?: 1
    val animatedProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val barWidth = size.width / (data.size * 2f)
            val spacing = barWidth / 2f

            data.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / maxValue) * size.height * animatedProgress.value
                val x = index * (barWidth + spacing) + spacing

                drawRect(
                    color = Color(0xFF2196F3),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )

                val text = value.toString()
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = TextStyle.Default.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                val textX = x + barWidth / 2f - textLayoutResult.size.width / 2f
                val textY = size.height - barHeight - 30f

                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(textX, textY),
                    style = TextStyle.Default.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    adultContent: Int,
    malware: Int,
    socialMedia: Int,
    gambling: Int = 0,
    gaming: Int = 0
) {
    val total = adultContent + malware + socialMedia + gambling + gaming
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Desglose por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CategoryItem(
                name = "Contenido adulto",
                count = adultContent,
                total = total,
                color = Color(0xFFE57373)
            )
            CategoryItem(
                name = "Malware/Phishing",
                count = malware,
                total = total,
                color = Color(0xFF64B5F6)
            )
            CategoryItem(
                name = "Redes sociales",
                count = socialMedia,
                total = total,
                color = Color(0xFF81C784)
            )
            CategoryItem(
                name = "Videojuegos/Gaming",
                count = gaming,
                total = total,
                color = Color(0xFFFFB74D)
            )
            if (gambling > 0) {
                CategoryItem(
                    name = "Apuestas/Casino",
                    count = gambling,
                    total = total,
                    color = Color(0xFFAB47BC)
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    name: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$count ($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = count.toFloat() / max(total, 1),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun TopBlockedSiteItem(domain: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    domain,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "$count veces",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(
    totalInPeriod: Int,
    adultBlocked: Int,
    gamblingBlocked: Int,
    selectedPeriod: String
) {
    // Umbrales adaptados al período seleccionado
    val highThreshold = when (selectedPeriod) {
        "Hoy"    -> 30
        "Semana" -> 120
        "Mes"    -> 400
        else     -> 800
    }
    val moderateThreshold = highThreshold / 4

    // Porcentaje de contenido especialmente sensible (adulto + apuestas)
    val sensitivePct = if (totalInPeriod > 0)
        (adultBlocked + gamblingBlocked).toFloat() / totalInPeriod
    else 0f

    data class RecoState(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val message: String,
        val bgColor: androidx.compose.ui.graphics.Color,
        val iconColor: androidx.compose.ui.graphics.Color,
        val borderColor: androidx.compose.ui.graphics.Color
    )

    val state = when {
        totalInPeriod == 0 -> RecoState(
            icon        = Icons.Default.Check,
            message     = "Sin bloqueos registrados en este período. Asegúrate de que el filtrado DNS esté activo para garantizar la protección.",
            bgColor     = Color(0xFF1B5E20).copy(alpha = 0.08f),
            iconColor   = Color(0xFF2E7D32),
            borderColor = Color(0xFF2E7D32).copy(alpha = 0.4f)
        )
        sensitivePct >= 0.5f -> RecoState(
            icon        = Icons.Default.Warning,
            message     = "⚠️ El ${(sensitivePct * 100).toInt()}% de los bloqueos son contenido adulto o apuestas (${adultBlocked + gamblingBlocked} de $totalInPeriod). Verifica que los filtros DNS estén activos.",
            bgColor     = Color(0xFFB71C1C).copy(alpha = 0.08f),
            iconColor   = Color(0xFFC62828),
            borderColor = Color(0xFFC62828).copy(alpha = 0.4f)
        )
        totalInPeriod > highThreshold -> RecoState(
            icon        = Icons.Default.Warning,
            message     = "Actividad elevada: $totalInPeriod intentos bloqueados en el período. Revisa qué apps o dominios generan más intentos en el desglose.",
            bgColor     = Color(0xFFB71C1C).copy(alpha = 0.08f),
            iconColor   = Color(0xFFC62828),
            borderColor = Color(0xFFC62828).copy(alpha = 0.4f)
        )
        totalInPeriod > moderateThreshold -> RecoState(
            icon        = Icons.Default.Info,
            message     = "Actividad moderada: $totalInPeriod bloqueos en el período. El sistema protege correctamente. Revisa el desglose si alguna categoría destaca.",
            bgColor     = Color(0xFFE65100).copy(alpha = 0.08f),
            iconColor   = Color(0xFFE65100),
            borderColor = Color(0xFFE65100).copy(alpha = 0.4f)
        )
        else -> RecoState(
            icon        = Icons.Default.Check,
            message     = "Actividad baja: $totalInPeriod bloqueos en el período. La protección funciona con normalidad.",
            bgColor     = Color(0xFF1B5E20).copy(alpha = 0.08f),
            iconColor   = Color(0xFF2E7D32),
            borderColor = Color(0xFF2E7D32).copy(alpha = 0.4f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = state.bgColor,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, state.borderColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector       = state.icon,
                contentDescription = null,
                tint              = state.iconColor,
                modifier          = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text       = "Recomendación",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}
