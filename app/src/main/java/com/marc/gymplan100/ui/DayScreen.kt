@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.repsFromScheme

@Composable
fun DayScreen(
    dayNumber: Int,
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onStartSession: (Int) -> Unit,
    onStartFreeSession: (Int) -> Unit
) {
    val day = PlanData.dayByNumber(dayNumber) ?: PlanData.days.first()
    val template = day.template
    val progress by viewModel.progress.collectAsState()
    val completed = day.number in progress.completedDays
    // Ejercicio cuya ficha ("¿Cómo se hace?" + mapa muscular) se está mostrando, o null.
    var guideFor by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Día ${day.number}") },
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
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fase ${day.phase.number}: ${day.phase.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = { onStartSession(day.number) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Empezar entrenamiento guiado")
                }
            }

            item {
                // Alternativa al guiado: cronómetro libre (sin series ni pesos) que SÍ cuenta
                // como el día del plan. Para cuando entrenas a tu aire pero quieres registrar el día.
                OutlinedButton(
                    onClick = { onStartFreeSession(day.number) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Entrenar libre · cuenta como día")
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Calentamiento",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            template.warmup,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            itemsIndexed(template.exercises) { index, exercise ->
                val log = progress.logs["${day.number}-$index"]
                ExerciseCard(
                    exercise = exercise,
                    // Peso: lo guardado o, si no, el último conocido de esa máquina.
                    weight = log?.weight.orEmpty().ifEmpty { viewModel.exerciseWeight(exercise.name) },
                    // Reps: las guardadas o, si no, las indicadas en el plan.
                    reps = log?.reps.orEmpty().ifEmpty { repsFromScheme(exercise.scheme) },
                    done = log?.done ?: false,
                    onWeight = { viewModel.setLog(day.number, index, weight = it) },
                    onReps = { viewModel.setLog(day.number, index, reps = it) },
                    onDone = { viewModel.setLog(day.number, index, done = it) },
                    onShowGuide = { guideFor = exercise }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Vuelta a la calma",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(template.cooldown, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                if (completed) {
                    OutlinedButton(
                        onClick = { viewModel.toggleDay(day.number) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text("  Día completado (toca para deshacer)")
                    }
                } else {
                    Button(
                        onClick = { viewModel.toggleDay(day.number) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text("  Marcar día como completado")
                    }
                }
            }
        }

        guideFor?.let { ex ->
            ExerciseGuideSheet(
                exerciseName = ex.name,
                scheme = ex.scheme,
                onDismiss = { guideFor = null }
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    weight: String,
    reps: String,
    done: Boolean,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onDone: (Boolean) -> Unit,
    onShowGuide: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val imageRes = ExerciseImages.forName(LocalContext.current, exercise.name, LocalIsFemale.current)
            if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        exercise.scheme,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (exercise.note.isNotEmpty()) {
                        Text(
                            exercise.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Checkbox(checked = done, onCheckedChange = onDone)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = onWeight,
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = onReps,
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onShowGuide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Text("  ¿Cómo se hace? · músculos y técnica")
            }
        }
    }
}
