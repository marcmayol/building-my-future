@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.data.LoggedExercise
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Qué has hecho en un entrenamiento libre, al darle a finalizar.
 *
 * Sale con los ejercicios del día ya puestos, para que en el caso normal solo haya que
 * escribir pesos. Cada fila deja **elegir otro ejercicio de la lista o escribir el tuyo**,
 * porque entrenando libre es habitual salirse del plan.
 *
 * Se puede guardar vacío: apuntar es opcional, y el tiempo se guarda igual.
 */
@Composable
fun FreeSessionLog(
    day: TrainingDay?,
    onSave: (List<LoggedExercise>) -> Unit,
    onSkip: () -> Unit
) {
    // Precargado con el día del plan; si no hay, una fila en blanco para empezar.
    val filas = remember {
        mutableStateListOf<LoggedExercise>().apply {
            val delDia = day?.template?.exercises.orEmpty()
            if (delDia.isEmpty()) add(LoggedExercise(""))
            else delDia.forEach { add(LoggedExercise(name = it.name)) }
        }
    }

    // Para el desplegable: los del día primero y el resto del plan después, sin repetir.
    val sugerencias = remember {
        val delDia = day?.template?.exercises.orEmpty().map { it.name }
        (delDia + PlanData.active.exerciseNames).distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Space.screen, end = Space.screen, top = 56.dp, bottom = Space.x4
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Text(
                    "ENTRENO TERMINADO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
                Text("¿Qué has hecho?", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(Space.x2))
                Text(
                    "Apunta lo que quieras y déjate el resto: el tiempo se guarda igual. " +
                        "Los pesos que pongas se quedan como los últimos de cada ejercicio.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
            }

            items(filas.size) { i ->
                FilaEjercicio(
                    fila = filas[i],
                    sugerencias = sugerencias,
                    onChange = { filas[i] = it },
                    onRemove = { if (filas.size > 1) filas.removeAt(i) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { filas.add(LoggedExercise("")) },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Añadir otro ejercicio") }
            }
        }

        // Las acciones, fijas abajo: con el teclado abierto y ocho filas, buscarlas al final
        // de la lista era un scroll a ciegas.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x4)
        ) {
            Button(
                onClick = { onSave(filas.toList()) },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
            ) { Text("Guardar y terminar") }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Terminar sin apuntar nada")
            }
        }
    }
}

@Composable
private fun FilaEjercicio(
    fila: LoggedExercise,
    sugerencias: List<String>,
    onChange: (LoggedExercise) -> Unit,
    onRemove: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val coincidencias = remember(fila.name, sugerencias) {
        if (fila.name.isBlank()) sugerencias
        else sugerencias.filter { it.contains(fila.name, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Editable y con lista a la vez: se elige de los que ya hay, o se escribe uno nuevo.
            ExposedDropdownMenuBox(
                expanded = abierto && coincidencias.isNotEmpty(),
                onExpandedChange = { abierto = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = fila.name,
                    onValueChange = { onChange(fila.copy(name = it)); abierto = true },
                    label = { Text("Ejercicio") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = abierto && coincidencias.isNotEmpty()
                        )
                    },
                    modifier = Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = abierto && coincidencias.isNotEmpty(),
                    onDismissRequest = { abierto = false }
                ) {
                    coincidencias.take(8).forEach { nombre ->
                        DropdownMenuItem(
                            text = { Text(nombre) },
                            onClick = { onChange(fila.copy(name = nombre)); abierto = false }
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Space.x2))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            OutlinedTextField(
                value = fila.weight,
                onValueChange = { onChange(fila.copy(weight = it)) },
                label = { Text("Peso (kg)") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = fila.reps,
                onValueChange = { onChange(fila.copy(reps = it)) },
                label = { Text("Reps") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
