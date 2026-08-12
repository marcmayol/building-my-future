@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.Protocolo
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

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
                title = {
                    Text(exercise.nombre, style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            if (exercise.calentamiento_obligatorio_min > 0) {
                item {
                    // El aviso de calentar usa el amarillo del calentamiento de la sesión.
                    val app = LocalAppColors.current
                    Text(
                        "Calienta al menos ${exercise.calentamiento_obligatorio_min} min antes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = app.warmup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(app.warmup.copy(alpha = 0.12f))
                            .padding(Space.x4)
                    )
                }
            }

            item {
                Text(
                    "ELIGE PROTOCOLO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.x2)
                )
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
                    Column(modifier = Modifier.padding(top = Space.x2)) {
                        Text(
                            "PROGRESIONES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Space.x1))
                        exercise.progresiones.forEach {
                            Text("· $it", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (exercise.notas_forma.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(top = Space.x2)) {
                        Text(
                            "TÉCNICA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Space.x1))
                        Text(exercise.notas_forma, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Spacer(Modifier.height(Space.x2))
                Button(
                    onClick = {
                        val status = viewModel.exerciseFrequency(exercise)
                        if (status.reached) warning = exercise.aviso_frecuencia
                        else {
                            selected?.let { viewModel.startFatburnSession(exercise, it); onStarted() }
                        }
                    },
                    enabled = selected != null,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Space.x2))
                    Text("Empezar")
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
    // El elegido se tiñe de naranja, como todo lo que está activo en la app.
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) app.work.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (selected) Modifier.border(1.dp, app.work, MaterialTheme.shapes.medium)
                else Modifier
            )
            .selectable(selected = selected, onClick = onSelect)
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(Space.x2))
            Text(protocol.nombre, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Space.x1))
        Text(
            protocolSummary(protocol),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (protocol.nota.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                protocol.nota,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
