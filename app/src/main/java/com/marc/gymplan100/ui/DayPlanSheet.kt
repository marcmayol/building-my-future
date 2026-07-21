@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.data.setCountFromScheme

/**
 * Panel deslizable con el plan completo del día, pensado para consultarlo durante
 * el descanso: qué queda, qué se ha hecho y cuántas series lleva cada ejercicio.
 * Tocar un ejercicio abre su ficha ("¿Cómo se hace?").
 */
@Composable
fun DayPlanSheet(
    day: TrainingDay,
    session: ActiveSession,
    onExerciseClick: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // El orden real de la sesión (puede haber cambiado por máquinas ocupadas).
    val order = session.order.ifEmpty { day.template.exercises.indices.toList() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Plan de hoy · Día ${day.number}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                day.template.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (day.template.warmup.isNotBlank()) {
                Text(
                    "Calentamiento · ${day.template.warmup}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            order.forEachIndexed { pos, exIndex ->
                val ex = day.template.exercises.getOrNull(exIndex) ?: return@forEachIndexed
                val total = setCountFromScheme(ex.scheme)
                val doneSets = session.completedSets.count { it.exerciseIndex == exIndex }
                PlanRow(
                    position = pos + 1,
                    exercise = ex,
                    doneSets = doneSets,
                    totalSets = total,
                    isCurrent = exIndex == session.exerciseIndex,
                    isDone = doneSets >= total,
                    onClick = { onExerciseClick(ex) }
                )
            }

            if (day.template.cooldown.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Vuelta a la calma · ${day.template.cooldown}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Toca un ejercicio para ver cómo se hace",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanRow(
    position: Int,
    exercise: Exercise,
    doneSets: Int,
    totalSets: Int,
    isCurrent: Boolean,
    isDone: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val bubbleColor = when {
        isCurrent -> accent
        isDone -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleContent = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimary
        isDone -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bubbleColor)
        ) {
            when {
                isCurrent -> Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "En curso",
                    tint = bubbleContent,
                    modifier = Modifier.size(18.dp)
                )
                isDone -> Icon(
                    Icons.Filled.Check,
                    contentDescription = "Hecho",
                    tint = bubbleContent,
                    modifier = Modifier.size(18.dp)
                )
                else -> Text(
                    "$position",
                    style = MaterialTheme.typography.labelMedium,
                    color = bubbleContent
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isDone && !isCurrent) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${exercise.scheme}  ·  $doneSets/$totalSets series",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.Info,
            contentDescription = "Cómo se hace",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
