@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.contar
import com.marc.gymplan100.data.palabra
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space

/**
 * Los días de una fase.
 *
 * Mismo lenguaje que el resto del rediseño: versalitas para las secciones, el nombre de la fase
 * en grande y una fila por día donde el estado manda. El día que toca va teñido de naranja, igual
 * que el ejercicio en curso durante la sesión, para que al entrar se vea sin leer nada.
 */
@Composable
fun PhaseScreen(
    phaseNumber: Int,
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onOpenDay: (Int) -> Unit
) {
    val phase = PlanData.phases.firstOrNull { it.number == phaseNumber } ?: PlanData.phases.first()
    val days = PlanData.daysOfPhase(phaseNumber)
    val progress by viewModel.progress.collectAsState()
    val app = LocalAppColors.current
    val hechos = days.count { it.number in progress.completedDays }
    val siguiente = viewModel.nextDay()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Fase ${phase.number}", style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            item {
                Text(
                    "${phase.range.uppercase()} · ${phase.weeks.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x1))
                Text(phase.name, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(Space.x3))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$hechos de ${days.size} " + palabra(days.size, "día", "días"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // El porcentaje, no un segundo "1/6": la misma lectura que en Inicio, y
                    // repetir la cifra al lado era decir lo mismo dos veces.
                    Text(
                        "${(hechos * 100f / days.size).toInt()} %",
                        style = LocalAppTextStyles.current.tabular,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(Space.x2))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                ) {
                    if (hechos > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    fraction = (hechos.toFloat() / days.size).coerceIn(0f, 1f)
                                )
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(app.brandGradient)
                        )
                    }
                }
            }

            item { PhaseSection("OBJETIVO", phase.description) }
            item { PhaseSection("PROGRESIÓN", phase.progression) }

            item {
                Text(
                    "DÍAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.x2)
                )
            }

            items(days, key = { it.number }) { day ->
                DayRow(
                    day = day,
                    completed = day.number in progress.completedDays,
                    isNext = day.number == siguiente,
                    week = PlanData.weekWithinPhase(day),
                    onClick = { onOpenDay(day.number) }
                )
            }
        }
    }
}

/** Objetivo y progresión: versalitas y texto, sin tarjeta de color. */
@Composable
private fun PhaseSection(label: String, text: String) {
    if (text.isBlank()) return
    Column(modifier = Modifier.padding(top = Space.x2)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x1))
        // Los textos del plan venían pensados para ir detrás de "Progresión:" en la misma línea,
        // así que empiezan en minúscula. Sueltos bajo su versalita necesitan mayúscula inicial.
        Text(
            text.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DayRow(
    day: TrainingDay,
    completed: Boolean,
    isNext: Boolean,
    week: Int,
    onClick: () -> Unit
) {
    val app = LocalAppColors.current
    // Un día solo puede estar en un estado: hecho, el que toca, o pendiente.
    val bubbleColor = when {
        completed -> app.work
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isNext && !completed) app.work.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bubbleColor)
                    .then(
                        if (isNext && !completed) {
                            Modifier.border(2.dp, app.work, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Día hecho",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "${day.number}",
                        style = LocalAppTextStyles.current.tabular,
                        color = if (isNext) app.work
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.size(Space.x3))
            Column(modifier = Modifier.weight(1f)) {
                // La versalita va encima del titular, como en el resto de la app. A la derecha
                // de la fila se le comía el subtítulo en cuanto el texto del sistema crecía.
                if (isNext && !completed) {
                    Text(
                        "HOY TOCA",
                        style = MaterialTheme.typography.labelMedium,
                        color = app.work
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text("Día ${day.number}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${day.template.title} · semana $week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
