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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    todayBlocked: Int,
    weeklyStats: List<StatisticEntity>,
    recentBlocked: List<BlockedSiteEntity>,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Hoy") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Exportar reporte */ }) {
                        Icon(Icons.Default.Share, "Compartir")
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
            item {
                QuickSummaryCard(
                    todayBlocked = todayBlocked,
                    weeklyBlocked = weeklyStats.sumOf { it.totalBlocked },
                    monthlyBlocked = weeklyStats.sumOf { it.totalBlocked } * 4
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
                            data = weeklyStats.map { it.totalBlocked },
                            labels = weeklyStats.map { stat ->
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateKey)
                                android.text.format.DateFormat.format("dd/MM", date).toString()
                            }
                        )
                    }
                }
            }

            item {
                CategoryBreakdownCard(
                    adultContent = weeklyStats.sumOf { it.adultContentBlocked },
                    violence = weeklyStats.sumOf { it.violenceBlocked },
                    malware = weeklyStats.sumOf { it.malwareBlocked },
                    socialMedia = weeklyStats.sumOf { it.socialMediaBlocked }
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

            items(recentBlocked.groupBy { it.domain }
                .map { it.key to it.value.size }
                .sortedByDescending { it.second }
                .take(10)
            ) { (domain, count) ->
                TopBlockedSiteItem(domain = domain, count = count)
            }

            item {
                RecommendationsCard(todayBlocked = todayBlocked)
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
            
            Divider(
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
    violence: Int,
    malware: Int,
    socialMedia: Int
) {
    val total = adultContent + violence + malware + socialMedia
    
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
                name = "Violencia",
                count = violence,
                total = total,
                color = Color(0xFFFFB74D)
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
fun RecommendationsCard(todayBlocked: Int) {
    val recommendation = when {
        todayBlocked > 50 -> "Alto nivel de intentos de acceso bloqueados. Considera revisar el nivel de restricción."
        todayBlocked > 20 -> "Actividad moderada detectada. El sistema está funcionando correctamente."
        else -> "Excelente. Pocos intentos de acceso a contenido restringido."
    }
    
    val icon = when {
        todayBlocked > 50 -> Icons.Default.Warning
        todayBlocked > 20 -> Icons.Default.Info
        else -> Icons.Default.Check
    }
    
    val color = when {
        todayBlocked > 50 -> MaterialTheme.colorScheme.errorContainer
        todayBlocked > 20 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Recomendación",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    recommendation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
