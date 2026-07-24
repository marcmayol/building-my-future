@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.PasoMilitar
import com.marc.gymplan100.data.Protocolo
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SpecialWorkoutsLoader
import kotlinx.coroutines.delay

/**
 * Ejecución guiada de una rutina especial (militar o quema grasa). Observa la sesión activa del
 * ViewModel y reutiliza los mismos temporizadores/notificaciones que la sesión normal.
 */
@Composable
fun RoutineSessionScreen(
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val session by viewModel.activeSession.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(500) }
    }
    var confirmExit by remember { mutableStateOf(false) }

    val s = session
    if (s == null || !s.isRoutine) {
        // No hay rutina en curso (se finalizó o se canceló): vuelve.
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val rutina = viewModel.specialWorkouts.rutina(s.routineId ?: "")
    val isFixed = rutina?.esSecuenciaFija == true
    val title = when {
        isFixed -> rutina?.nombre ?: "Rutina"
        else -> viewModel.specialWorkouts.ejercicio(s.exerciseId ?: "")?.nombre ?: "Quema Grasa"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { confirmExit = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Salir")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                formatClock(now - s.startMillis),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isFixed) FixedSequenceContent(s, now, viewModel)
            else FatburnContent(s, now, viewModel)

            if (s.phase == SessionPhase.FINISHED) {
                Button(
                    onClick = { viewModel.finishSession(); onExit() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Finalizar y guardar")
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Salir de la rutina?") },
            text = { Text("Si sales ahora, esta sesión no se guardará.") },
            confirmButton = {
                Button(onClick = { confirmExit = false; viewModel.cancelSession(); onExit() }) {
                    Text("Salir sin guardar")
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir") } }
        )
    }
}

// --------------------------------------------------------- Secuencia fija

/** Ejecución de una rutina de secuencia fija: Militar (1 serie/paso) o Altura y Postura (2-3 series). */
@Composable
private fun FixedSequenceContent(s: ActiveSession, now: Long, viewModel: PlanViewModel) {
    val rutina = viewModel.specialWorkouts.rutina(s.routineId ?: "") ?: return
    val paso = rutina.pasosOrdenados.getOrNull(s.stepIndex) ?: return
    val usingAlt = s.useAlternative && paso.alternativa != null
    val nombre = if (usingAlt) paso.alternativa!!.nombre else paso.nombre
    val hasSeries = paso.numSeries > 1
    // Se puede terminar el ejercicio antes (elegir 2 de 3) una vez hechas las series mínimas.
    val canEndEarly = hasSeries && s.setNumber >= paso.minSeries && s.setNumber < paso.numSeries

    AssistChip(onClick = {}, label = { Text("Paso ${s.stepIndex + 1} de ${s.totalUnits}") })
    if (hasSeries) {
        Text(
            "Serie ${s.setNumber} de ${paso.numSeries}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Text(nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (paso.notas.isNotBlank()) NoteCard(paso.notas)

    when (s.phase) {
        SessionPhase.TIMED_SET -> {
            val target = if (usingAlt) paso.alternativa!!.duracion_seg else paso.objetivoSeg
            CountdownCircle(elapsed = s.timedElapsed(now), target = target)
            TimerButtons(s, viewModel)
            PrimaryAdvance(if (hasSeries) "Serie hecha" else "Siguiente paso") {
                viewModel.completeFixedSerie("")
            }
            if (canEndEarly) EndExerciseButton { viewModel.completeFixedSerie("", endExercise = true) }
        }
        SessionPhase.WORKING -> {
            RepsTarget(paso)
            // Alternativa (solo en el paso de burpees de la militar): jumping jacks por tiempo.
            if (paso.alternativa != null && !usingAlt) {
                OutlinedButton(
                    onClick = { viewModel.chooseAlternative() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(paso.alternativa.nombre + " en su lugar") }
            }
            var reps by remember(s.stepIndex, s.setNumber) { mutableStateOf("") }
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Repeticiones hechas") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAdvance(if (hasSeries) "Serie hecha" else "Paso hecho") {
                viewModel.completeFixedSerie(reps)
            }
            if (canEndEarly) EndExerciseButton { viewModel.completeFixedSerie(reps, endExercise = true) }
        }
        SessionPhase.RESTING -> {
            Text(
                if (s.restBetweenExercises) "Descanso · siguiente ejercicio" else "Descanso entre series",
                style = MaterialTheme.typography.titleMedium
            )
            CountdownCircle(
                elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0),
                target = s.restTargetSeconds
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.adjustRest(-15) }) { Text("-15 s") }
                OutlinedButton(onClick = { viewModel.adjustRest(15) }) { Text("+15 s") }
            }
            PrimaryAdvance("Saltar descanso") { viewModel.endRoutineRest() }
        }
        SessionPhase.FINISHED -> FinishedNote("¡Rutina completada!")
        else -> {}
    }
}

@Composable
private fun EndExerciseButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Terminar ejercicio · pasar al siguiente")
    }
}

// -------------------------------------------------------------- Quema grasa

@Composable
private fun FatburnContent(s: ActiveSession, now: Long, viewModel: PlanViewModel) {
    val exercise = viewModel.specialWorkouts.ejercicio(s.exerciseId ?: "") ?: return
    val protocol = exercise.protocolos.firstOrNull { it.nombre == s.protocolName } ?: return
    val unitLabel = when {
        protocol.esSeries -> "Serie ${s.stepIndex + 1} de ${s.totalUnits}"
        s.totalUnits > 1 -> "Ronda ${s.stepIndex + 1} de ${s.totalUnits}"
        else -> protocol.nombre
    }
    AssistChip(onClick = {}, label = { Text(unitLabel) })

    when (s.phase) {
        SessionPhase.TIMED_SET -> {
            CountdownCircle(elapsed = s.timedElapsed(now), target = s.timedTargetSeconds)
            TimerButtons(s, viewModel)
            PrimaryAdvance(if (protocol.esIntervalos) "Ronda hecha" else "Hecho") {
                viewModel.completeFatburnUnit("")
            }
        }
        SessionPhase.WORKING -> {
            if (protocol.repsLabel.isNotBlank()) {
                Text(
                    "Objetivo: ${protocol.repsLabel} reps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (exercise.notas_forma.isNotBlank()) NoteCard(exercise.notas_forma)
            var reps by remember(s.stepIndex) { mutableStateOf("") }
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Repeticiones hechas") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAdvance("Serie hecha") { viewModel.completeFatburnUnit(reps) }
        }
        SessionPhase.RESTING -> {
            Text("Descanso", style = MaterialTheme.typography.titleMedium)
            CountdownCircle(
                elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0),
                target = s.restTargetSeconds
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.adjustRest(-15) }) { Text("-15 s") }
                OutlinedButton(onClick = { viewModel.adjustRest(15) }) { Text("+15 s") }
            }
            PrimaryAdvance("Saltar descanso") { viewModel.endRoutineRest() }
        }
        SessionPhase.FINISHED -> FinishedNote("¡Ejercicio completado!")
        else -> {}
    }
}

// ------------------------------------------------------------- Componentes

@Composable
private fun CountdownCircle(elapsed: Int, target: Int) {
    val remaining = target - elapsed
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        val fraction = if (target <= 0) 0f else (elapsed.toFloat() / target).coerceIn(0f, 1f)
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (remaining >= 0) formatSecs(remaining) else "+${formatSecs(-remaining)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (remaining >= 0) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.primary
            )
            Text(
                if (remaining >= 0) "restante" else "de más",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerButtons(s: ActiveSession, viewModel: PlanViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { viewModel.adjustTimedSet(-10) }) { Text("-10 s") }
        if (s.timedPaused) {
            OutlinedButton(onClick = { viewModel.resumeTimedSet() }) { Text("Reanudar") }
        } else {
            OutlinedButton(onClick = { viewModel.pauseTimedSet() }) { Text("Pausa") }
        }
        OutlinedButton(onClick = { viewModel.adjustTimedSet(10) }) { Text("+10 s") }
    }
}

@Composable
private fun RepsTarget(paso: PasoMilitar) {
    val target = when {
        paso.reps.equals("AMRAP", ignoreCase = true) -> "Máximas repeticiones con buena forma (AMRAP)"
        paso.repsObjetivo.isNotBlank() -> "Objetivo: ${paso.repsObjetivo} reps"
        else -> ""
    }
    if (target.isNotBlank()) {
        Text(target, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PrimaryAdvance(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        Text("  $label")
    }
}

@Composable
private fun NoteCard(text: String) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FinishedNote(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun formatSecs(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun formatClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val m = total / 60
    val sec = total % 60
    return "%02d:%02d".format(m, sec)
}
