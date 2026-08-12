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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionRecord
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
    viewModel: PlanViewModel,
    onBack: () -> Unit
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
                    Text("Resultados", style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
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
                        Text(if (newestFirst) "Recientes" else "Antiguos")
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
                        "Aún no has completado ningún día. Cuando termines tu primer " +
                            "entrenamiento, aquí verás cuánto tardaste y con qué peso hiciste " +
                            "cada ejercicio.",
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
                        "ENTRENAMIENTOS EXTRA",
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
                if (connected) "Conectado con Google Health" else "Google Health",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(Space.x2))
        Text(
            if (connected)
                "Cada entreno que termines se guardará automáticamente en Google Health, " +
                    "con su duración y el detalle de ejercicios y pesos."
            else
                "Conecta la app para que tus entrenamientos aparezcan en Google Health " +
                    "automáticamente al terminarlos.",
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
            ) { Text("Conectar con Google Health") }
        }
    }
}

@Composable
private fun ExtraBlock(record: SessionRecord) {
    val styles = LocalAppTextStyles.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Entrenamiento extra", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                formatDate(record.endMillis),
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
}

private fun formatDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("EEE d MMM · HH:mm", java.util.Locale("es", "ES"))
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
                        "ESPECIAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = app.work
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text("Día $dayNumber", style = MaterialTheme.typography.titleMedium)
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
            Text(
                "Sesión libre guiada (p. ej. con tu tío). Solo se registró el tiempo total.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            day.template.exercises.forEachIndexed { index, exercise ->
                val log = progress.logs["$dayNumber-$index"]
                val weight = log?.weight.orEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        exercise.name,
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
            }

            if (record != null) {
                Spacer(Modifier.height(Space.x2))
                Text(
                    "${record.totalSets} series · descanso total " +
                        formatDuration(record.totalRestSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
