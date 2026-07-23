@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.Protocolo

/**
 * Ficha de un ejercicio de quema grasa: elegir protocolo, ver notas de forma y empezar.
 * Antes de arrancar, si ya se cumplió la frecuencia semanal, muestra el aviso no bloqueante.
 */
@Composable
fun FatburnExerciseScreen(
    exerciseId: String,
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onStarted: () -> Unit
) {
    val exercise = viewModel.specialWorkouts.ejercicio(exerciseId)
    if (exercise == null) { onBack(); return }

    var selected by remember { mutableStateOf(exercise.protocolos.firstOrNull()) }
    var warning by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.nombre) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercise.calentamiento_obligatorio_min > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ Calienta al menos ${exercise.calentamiento_obligatorio_min} min antes.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text("Elige protocolo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(exercise.protocolos.size) { i ->
                val p = exercise.protocolos[i]
                ProtocolRow(
                    protocol = p,
                    selected = selected?.nombre == p.nombre,
                    onSelect = { selected = p }
                )
            }

            if (exercise.progresiones.isNotEmpty()) {
                item {
                    Column {
                        Text("Progresiones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        exercise.progresiones.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (exercise.notas_forma.isNotBlank()) {
                item {
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Técnica", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(exercise.notas_forma, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val status = viewModel.exerciseFrequency(exercise)
                        if (status.reached) warning = exercise.aviso_frecuencia
                        else {
                            selected?.let { viewModel.startFatburnSession(exercise, it); onStarted() }
                        }
                    },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Empezar")
                }
            }
        }
    }

    warning?.let { text ->
        FrequencyWarningDialog(
            text = text,
            onContinue = {
                warning = null
                selected?.let { viewModel.startFatburnSession(exercise, it); onStarted() }
            },
            onCancel = { warning = null }
        )
    }
}

@Composable
private fun ProtocolRow(
    protocol: Protocolo,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onSelect)
                Text(protocol.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                protocolSummary(protocol),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            if (protocol.nota.isNotBlank()) {
                Text(
                    protocol.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** Resumen legible del protocolo para la tarjeta de selección. */
fun protocolSummary(p: Protocolo): String = when {
    p.esIntervalos -> {
        val trabajo = if (p.trabajo_seg > 0) "${p.trabajo_seg}s"
        else "${p.trabajo_seg_min}-${p.trabajo_seg_max}s"
        val descanso = if (p.descanso_seg > 0) "${p.descanso_seg}s"
        else "${p.descanso_seg_min}-${p.descanso_seg_max}s"
        "$trabajo trabajo · $descanso descanso · ${p.numRondas} rondas"
    }
    p.esSeries -> "${p.numSeries} series · ${p.repsLabel} reps"
    p.esTiempoUnico -> "Aguanta ${p.duracion_inicial_seg}s (progresa cada semana)"
    else -> p.repsLabel.ifBlank { "Protocolo" }
}
