@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.setsSummary
import com.marc.gymplan100.data.weightsSummary
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Lo que ya has hecho: un bloque por día completado con sus pesos, y los extras aparte.
 *
 * Las duraciones y los pesos van en cifras tabulares en vez de en chips: los chips parecían
 * botones y no llevaban a ninguna parte.
 */
@Composable
fun ResultsScreen(
    viewModel: PlanViewModel
) {
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val newestFirst = progress.resultsNewestFirst
    val completed = progress.completedDays
        .filter { it in 1..PlanData.TOTAL_DAYS }
        .sorted()
        .let { if (newestFirst) it.reversed() else it }
    val extras = history.filter { it.extra }
        .sortedByDescending { it.endMillis }
        .let { if (newestFirst) it else it.reversed() }

    // Estado de la conexión con Google Health (Health Connect).
    val healthGranted by viewModel.healthGranted.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshHealthPermissions() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.est_resultados_titulo), style = MaterialTheme.typography.headlineSmall)
                },
                actions = {
                    TextButton(onClick = { viewModel.toggleResultsOrder() }) {
                        Icon(
                            if (newestFirst) Icons.Filled.KeyboardArrowDown
                            else Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Space.x1))
                        Text(if (newestFirst) stringResource(R.string.est_orden_recientes) else stringResource(R.string.est_orden_antiguos))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            if (viewModel.healthAvailable) {
                item {
                    HealthConnectBlock(
                        connected = healthGranted,
                        onConnect = { permissionLauncher.launch(viewModel.healthPermissions) }
                    )
                }
            }

            if (completed.isEmpty() && extras.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.est_resultados_vacio),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(completed, key = { it }) { dayNumber ->
                val record = history
                    .filter { it.dayNumber == dayNumber && !it.extra }
                    .maxByOrNull { it.endMillis }
                ResultBlock(dayNumber, record, progress)
            }

            if (extras.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.est_entrenos_extra_titulo),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.x2)
                    )
                }
                items(extras, key = { it.startMillis }) { rec ->
                    ExtraBlock(rec)
                }
            }
        }
    }
}

@Composable
private fun HealthConnectBlock(connected: Boolean, onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (connected) Icons.Filled.CheckCircle else Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(Space.x2))
            Text(
                if (connected) stringResource(R.string.est_health_conectado) else "Google Health",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(Space.x2))
        Text(
            if (connected)
                stringResource(R.string.est_health_conectado_texto)
            else
                stringResource(R.string.est_health_conectar_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!connected) {
            Spacer(Modifier.height(Space.x3))
            Button(
                onClick = onConnect,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primary)
            ) { Text(stringResource(R.string.est_health_conectar)) }
        }
    }
}

@Composable
private fun ExtraBlock(record: SessionRecord) {
    val styles = LocalAppTextStyles.current
    val apuntado = record.logged.filter { it.setsOrSingle.isNotEmpty() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.est_entreno_extra), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDate(
                        record.endMillis,
                        stringResource(R.string.est_fecha_entreno),
                        AppLanguage.locale(LocalContext.current)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(Space.x2))
            Text(
                formatDuration(record.durationSeconds),
                style = styles.tabular,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Un extra también se apunta al terminar: si escribiste los kilos de cada serie, aquí
        // están, en vez de un bloque que solo dice cuánto duró.
        if (apuntado.isNotEmpty()) {
            Spacer(Modifier.height(Space.x3))
            apuntado.forEach { ejercicio ->
                ExerciseResultRow(
                    name = ejercicio.name,
                    weight = ejercicio.weight,
                    sets = ejercicio.setsOrSingle,
                    conReps = true
                )
            }
        }
    }
}

/** [pattern] sale de los recursos: cada idioma ordena el día y el mes a su manera. */
private fun formatDate(millis: Long, pattern: String, locale: java.util.Locale): String {
    val fmt = java.text.SimpleDateFormat(pattern, locale)
    return fmt.format(java.util.Date(millis)).replaceFirstChar { it.uppercase() }
}

@Composable
private fun ResultBlock(dayNumber: Int, record: SessionRecord?, progress: ProgressState) {
    val day = PlanData.dayByNumber(dayNumber) ?: return
    val app = LocalAppColors.current
    val styles = LocalAppTextStyles.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (record?.special == true) {
                    Text(
                        stringResource(R.string.est_especial),
                        style = MaterialTheme.typography.labelMedium,
                        color = app.work
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(stringResource(R.string.est_dia_titulo, dayNumber), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    day.template.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (record != null) {
                Spacer(Modifier.width(Space.x2))
                Text(
                    formatDuration(record.durationSeconds),
                    style = styles.tabular,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(Space.x3))

        if (record?.special == true) {
            val apuntado = record.logged.filter { it.setsOrSingle.isNotEmpty() }
            if (apuntado.isEmpty()) {
                Text(
                    stringResource(R.string.est_sesion_libre_solo_tiempo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Lo que se apuntó al terminar el entreno libre. Se guardaba desde la v2.3
                // pero no se enseñaba en ningún sitio: el entreno salía como "solo el tiempo"
                // aunque hubieras escrito los kilos de cada serie.
                Text(
                    stringResource(R.string.est_entreno_libre_apuntado),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
                apuntado.forEach { ejercicio ->
                    ExerciseResultRow(
                        name = ejercicio.name,
                        weight = ejercicio.weight,
                        sets = ejercicio.setsOrSingle,
                        conReps = true
                    )
                }
            }
        } else {
            day.template.exercises.forEachIndexed { index, exercise ->
                val log = progress.logs["$dayNumber-$index"]
                val hecho = log?.doneAs?.takeIf { it.isNotBlank() }
                ExerciseResultRow(
                    // Lo que se hizo de verdad, con el del plan entre paréntesis para no perderse.
                    name = if (hecho != null) stringResource(R.string.est_en_lugar_de, hecho, exercise.name)
                    else exercise.name,
                    weight = log?.weight.orEmpty(),
                    sets = log?.filledSets.orEmpty(),
                    conReps = false
                )
            }

            if (record != null) {
                Spacer(Modifier.height(Space.x2))
                Text(
                    stringResource(
                        R.string.est_descanso_total,
                        pluralStringResource(R.plurals.est_series, record.totalSets, record.totalSets),
                        formatDuration(record.totalRestSeconds)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Un ejercicio dentro de la ficha de un día: el nombre con su peso a la derecha —el más alto
 * que se movió— y, debajo, el desglose de las series.
 *
 * El desglose es la razón de ser de esta fila: tres series de bíceps con 10, 12 y 11 salían
 * aquí como un escueto "11 kg", que es justo el dato que no habías hecho.
 */
@Composable
private fun ExerciseResultRow(
    name: String,
    weight: String,
    sets: List<SetLog>,
    conReps: Boolean
) {
    val styles = LocalAppTextStyles.current
    val marcaCal = stringResource(R.string.mot_warmup_short)
    val desglose = if (conReps) setsSummary(sets, marcaCal) else weightsSummary(sets)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Space.x2))
            Text(
                if (weight.isNotBlank()) "$weight kg" else "—",
                style = styles.tabular,
                color = if (weight.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Con una sola serie y sin repeticiones, el desglose repetiría el peso de al lado.
        if (desglose.isNotBlank() && (sets.size > 1 || conReps)) {
            Text(
                desglose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    return when {
        h > 0 -> "${h} h ${m} min"
        m > 0 -> "$m min"
        else -> "$s s"
    }
}
