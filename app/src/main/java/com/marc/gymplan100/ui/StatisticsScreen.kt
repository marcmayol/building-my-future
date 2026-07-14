@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.Statistics
import com.marc.gymplan100.ui.theme.BrandMagenta
import com.marc.gymplan100.ui.theme.BrandOrange
import java.time.LocalDate

@Composable
fun StatisticsScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()

    val summary = remember(progress, history) { Statistics.summary(progress, history) }
    val progression = remember(progress) { Statistics.weightProgression(progress) }
    val weeks = remember(history) { Statistics.workoutsPerWeek(history, weeks = 10) }
    val trained = remember(history) { Statistics.trainedDays(history) }
    val records = remember(progress) { Statistics.personalRecords(progress) }
    val longest = remember(history) { Statistics.longestSession(history) }
    val productive = remember(history) { Statistics.mostProductiveSession(history) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { inner ->
        if (summary.completedDays == 0 && history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = inner.calculateTopPadding() + 40.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    "Aún no hay datos. Cuando completes tu primer entrenamiento, aquí verás tu " +
                        "progreso: racha, evolución del peso por ejercicio, constancia y records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { SummarySection(summary) }
            item { ProgressionSection(progression) }
            item { ConsistencySection(weeks, trained, history) }
            item { RecordsSection(records, longest, productive) }
        }
    }
}

// ------------------------------------------------------------------ Resumen global

@Composable
private fun SummarySection(s: Statistics.Summary) {
    Column {
        SectionTitle("Resumen")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "${s.completedDays}/${s.totalDays}",
                label = "días · ${s.percent}%",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "🔥 ${s.currentStreak}",
                label = "racha actual",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${s.bestStreak}",
                label = "mejor racha",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "${s.totalWorkouts}",
                label = if (s.extraWorkouts > 0) "entrenos · ${s.extraWorkouts} extra" else "entrenos",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = formatDurationLong(s.totalTrainingSeconds),
                label = "tiempo total",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${s.totalSets}",
                label = "series totales",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------------- Progresión de peso

@Composable
private fun ProgressionSection(progression: Map<String, List<Statistics.WeightPoint>>) {
    Column {
        SectionTitle("Progresión de peso")
        Spacer(Modifier.height(4.dp))
        if (progression.isEmpty()) {
            Text(
                "Registra el peso de tus series para ver aquí cómo evoluciona en cada ejercicio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        // Ejercicio con más puntos como selección inicial (mejor señal de progreso).
        val names = progression.keys.toList()
        val default = remember(progression) {
            names.maxByOrNull { progression[it]?.size ?: 0 } ?: names.first()
        }
        var selected by remember(progression) { mutableStateOf(default) }
        val points = progression[selected].orEmpty()

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            names.forEach { name ->
                FilterChip(
                    selected = name == selected,
                    onClick = { selected = name },
                    label = { Text(name) }
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val first = points.firstOrNull()?.weight
                val last = points.lastOrNull()?.weight
                val max = points.maxOfOrNull { it.weight }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            last?.let { "${fmtWeight(it)} kg" } ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "peso actual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (first != null && last != null && points.size > 1) {
                        val delta = last - first
                        val sign = if (delta >= 0) "+" else ""
                        Text(
                            "$sign${fmtWeight(delta)} kg desde el inicio",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (delta >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                WeightChart(points)
                Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append("${points.size} ${if (points.size == 1) "registro" else "registros"}")
                        if (max != null) append(" · máx ${fmtWeight(max)} kg")
                        points.firstOrNull()?.let { append(" · desde el día ${it.day}") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Gráfica de línea del peso a lo largo de los días, dibujada con Canvas. */
@Composable
private fun WeightChart(points: List<Statistics.WeightPoint>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val fill = Brush.verticalGradient(
        listOf(line.copy(alpha = 0.22f), line.copy(alpha = 0.0f))
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val w = size.width
        val h = size.height
        val vPad = 14f
        val minW = points.minOf { it.weight }
        val maxW = points.maxOf { it.weight }
        val minD = points.first().day
        val maxD = points.last().day

        fun px(day: Int): Float =
            if (maxD == minD) w / 2f else (day - minD).toFloat() / (maxD - minD) * w
        fun py(weight: Float): Float =
            if (maxW == minW) h / 2f else vPad + (1f - (weight - minW) / (maxW - minW)) * (h - 2 * vPad)

        // Líneas guía horizontales (mín, medio, máx).
        for (f in listOf(0f, 0.5f, 1f)) {
            val y = vPad + f * (h - 2 * vPad)
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
        }

        if (points.size == 1) {
            drawCircle(line, radius = 7f, center = Offset(w / 2f, h / 2f))
            return@Canvas
        }

        val linePath = Path()
        val areaPath = Path().apply { moveTo(px(points.first().day), h - vPad) }
        points.forEachIndexed { i, p ->
            val x = px(p.day)
            val y = py(p.weight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            areaPath.lineTo(x, y)
        }
        areaPath.lineTo(px(points.last().day), h - vPad)
        areaPath.close()

        drawPath(areaPath, brush = fill)
        drawPath(linePath, color = line, style = Stroke(width = 5f))
        points.forEach { p ->
            drawCircle(line, radius = 5.5f, center = Offset(px(p.day), py(p.weight)))
        }
    }
}

// ----------------------------------------------------------------- Constancia

@Composable
private fun ConsistencySection(
    weeks: List<Statistics.WeekCount>,
    trained: Set<LocalDate>,
    history: List<SessionRecord>
) {
    Column {
        SectionTitle("Constancia")
        Spacer(Modifier.height(10.dp))

        val today = LocalDate.now()
        val thisWeek = Statistics.workoutsBetween(history, Statistics.weekStart(today), today)
        val thisMonth = Statistics.workoutsBetween(history, today.withDayOfMonth(1), today)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("$thisWeek", "esta semana", Modifier.weight(1f))
            StatTile("$thisMonth", "este mes", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Entrenos por semana",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        WeeklyBars(weeks)

        Spacer(Modifier.height(20.dp))
        Text(
            "Últimas semanas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Heatmap(trained)
    }
}

@Composable
private fun WeeklyBars(weeks: List<Statistics.WeekCount>) {
    val maxCount = (weeks.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val barColor = Brush.verticalGradient(listOf(BrandOrange, BrandMagenta))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        weeks.forEach { wc ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${wc.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (wc.count > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val ratio = wc.count.toFloat() / maxCount
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((90 * ratio).dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                if (wc.count > 0) barColor
                                else Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${wc.weekStart.dayOfMonth}/${wc.weekStart.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Mapa de calor estilo calendario: 13 semanas (columnas) x 7 días (filas). */
@Composable
private fun Heatmap(trained: Set<LocalDate>, weeks: Int = 13) {
    val today = LocalDate.now()
    val gridStart = Statistics.weekStart(today).minusWeeks((weeks - 1).toLong())
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (col in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..6) {
                    val date = gridStart.plusDays((col * 7 + row).toLong())
                    val color = when {
                        date.isAfter(today) -> Color.Transparent
                        date in trained -> on
                        else -> off
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(on)
        )
        Text(
            "  día entrenado",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------------- Records

@Composable
private fun RecordsSection(
    records: List<Statistics.ExerciseRecord>,
    longest: SessionRecord?,
    productive: SessionRecord?
) {
    Column {
        SectionTitle("Records personales")
        Spacer(Modifier.height(10.dp))

        if (records.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Peso máximo por ejercicio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    records.take(8).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                r.exercise,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${fmtWeight(r.weight)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "día ${r.day}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (longest != null || productive != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                longest?.let {
                    RecordTile(
                        emoji = "⏱️",
                        value = formatDurationLong(it.durationSeconds.toLong()),
                        label = "sesión más larga · día ${it.dayNumber}",
                        modifier = Modifier.weight(1f)
                    )
                }
                productive?.let {
                    RecordTile(
                        emoji = "💪",
                        value = "${it.totalSets} series",
                        label = "sesión más productiva · día ${it.dayNumber}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --------------------------------------------------------------------- Comunes

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

/** Formatea un peso: entero si es redondo (50), con un decimal si no (52.5). */
private fun fmtWeight(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString()
    else "%.1f".format(w)

private fun formatDurationLong(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    return when {
        h > 0 -> "${h} h ${m} min"
        m > 0 -> "$m min"
        else -> "$s s"
    }
}
