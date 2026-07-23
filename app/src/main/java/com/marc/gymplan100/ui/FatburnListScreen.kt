@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.EjercicioCatalogo

/** Catálogo de la Rutina Quema Grasa: cada ejercicio con su progreso semanal (p. ej. 1/2). */
@Composable
fun FatburnListScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    // Se observa el historial para que el progreso X/Y se refresque al volver de una sesión.
    val history by viewModel.history.collectAsState()
    val exercises = viewModel.specialWorkouts.quemaGrasa?.ejercicios ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quema Grasa") },
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
            item {
                Text(
                    "Elige un ejercicio. El número indica cuántas veces lo has hecho esta semana " +
                        "respecto a lo recomendado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(exercises, key = { it.id }) { ex ->
                // history se lee arriba para forzar recomposición; el conteo lo hace el ViewModel.
                @Suppress("UNUSED_EXPRESSION") history
                FatburnRow(
                    exercise = ex,
                    count = viewModel.exerciseFrequency(ex).count,
                    onClick = { onOpenExercise(ex.id) }
                )
            }
        }
    }
}

@Composable
private fun FatburnRow(
    exercise: EjercicioCatalogo,
    count: Int,
    onClick: () -> Unit
) {
    val freq = exercise.frecuencia_semanal
    val reached = freq.max > 0 && count >= freq.max
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(exercise.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Recomendado ${freq.min}-${freq.max}/semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$count/${freq.max}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (reached) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}
