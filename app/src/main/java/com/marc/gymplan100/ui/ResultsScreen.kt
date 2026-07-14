@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionRecord

@Composable
fun ResultsScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val completed = progress.completedDays.filter { it in 1..PlanData.TOTAL_DAYS }.sorted()
    val extras = history.filter { it.extra }.sortedByDescending { it.endMillis }

    // Estado de la conexión con Google Health (Health Connect).
    val healthGranted by viewModel.healthGranted.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshHealthPermissions() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.healthAvailable) {
                item {
                    HealthConnectCard(
                        connected = healthGranted,
                        onConnect = { permissionLauncher.launch(viewModel.healthPermissions) }
                    )
                }
            }

            if (completed.isEmpty() && extras.isEmpty()) {
                item {
                    Text(
                        "Aún no has completado ningún día. Cuando termines tu primer entrenamiento, " +
                            "aquí verás cuánto tardaste y con qué peso hiciste cada ejercicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(completed, key = { it }) { dayNumber ->
                val record = history
                    .filter { it.dayNumber == dayNumber && !it.extra }
                    .maxByOrNull { it.endMillis }
                ResultCard(dayNumber, record, progress)
            }

            if (extras.isNotEmpty()) {
                item {
                    Text(
                        "Entrenamientos extra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(extras, key = { it.startMillis }) { rec ->
                    ExtraCard(rec)
                }
            }
        }
    }
}

@Composable
private fun HealthConnectCard(connected: Boolean, onConnect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (connected) Icons.Filled.CheckCircle else Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (connected) "Conectado con Google Health" else "Google Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(12.dp))
                Button(onClick = onConnect) {
                    Text("Conectar con Google Health")
                }
            }
        }
    }
}

@Composable
private fun ExtraCard(record: SessionRecord) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(0.dp))
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        "Entrenamiento extra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatDate(record.endMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(formatDuration(record.durationSeconds)) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("EEE d MMM · HH:mm", java.util.Locale("es", "ES"))
    return fmt.format(java.util.Date(millis)).replaceFirstChar { it.uppercase() }
}

@Composable
private fun ResultCard(dayNumber: Int, record: SessionRecord?, progress: ProgressState) {
    val day = PlanData.dayByNumber(dayNumber) ?: return
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Día $dayNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        day.template.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (record.special) {
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(Icons.Filled.Star, contentDescription = null)
                                },
                                label = { Text("Especial") },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary,
                                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(formatDuration(record.durationSeconds)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (weight.isNotBlank()) "$weight kg" else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (weight.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (record != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${record.totalSets} series · descanso total ${formatDuration(record.totalRestSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
