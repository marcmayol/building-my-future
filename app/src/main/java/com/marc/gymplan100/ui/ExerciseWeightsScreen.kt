@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.ui.theme.Space

/**
 * El peso que usas en cada ejercicio, para no tener que acordarte.
 *
 * Una fila por ejercicio con el nombre y su campo: sin tarjetas, el bloque de color ya separa
 * una fila de la siguiente.
 */
@Composable
fun ExerciseWeightsScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis pesos", style = MaterialTheme.typography.headlineSmall)
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
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            item {
                Text(
                    "Se actualiza solo cuando registras peso en una sesión, y puedes editarlo " +
                        "aquí. Así no tienes que acordarte de qué peso usabas.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
            }

            items(PlanData.exerciseNames, key = { it }) { name ->
                ExerciseWeightRow(
                    name = name,
                    initialWeight = viewModel.exerciseWeight(name),
                    onWeightChange = { viewModel.setExerciseWeight(name, it) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseWeightRow(
    name: String,
    initialWeight: String,
    onWeightChange: (String) -> Unit
) {
    var weight by remember(name) { mutableStateOf(initialWeight) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Menos aire arriba y abajo que en otros bloques: el campo de texto ya trae el suyo,
            // y con el padding completo cada fila ocupaba media pantalla.
            .padding(horizontal = Space.x4, vertical = Space.x2)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Space.x3))
            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it
                    onWeightChange(it)
                },
                label = { Text("kg") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(112.dp)
            )
        }
    }
}
