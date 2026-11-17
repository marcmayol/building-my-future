@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import com.marc.gymplan100.plataforma.anchoPantallaDp
import com.marc.gymplan100.plataforma.fmt
import com.marc.gymplan100.data.ahoraMs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.mutableFloatStateOf
import com.marc.gymplan100.shared.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.Contenido
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
    var now by remember { mutableStateOf(ahoraMs()) }
    LaunchedEffect(Unit) {
        while (true) { now = ahoraMs(); delay(500) }
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
        isFixed -> rutina?.nombre?.let(Contenido::t) ?: stringResource(Res.string.ses_routine)
        else -> viewModel.specialWorkouts.ejercicio(s.exerciseId ?: "")?.nombre?.let(Contenido::t) ?: stringResource(Res.string.ses_fatburn_short)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { confirmExit = true }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.ses_exit))
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
                    Text("  " + stringResource(Res.string.ses_finish_save))
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(Res.string.ses_routine_exit_title)) },
            text = { Text(stringResource(Res.string.ses_routine_exit_text)) },
            confirmButton = {
                Button(onClick = { confirmExit = false; viewModel.cancelSession(); onExit() }) {
                    Text(stringResource(Res.string.ses_exit_no_save))
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(Res.string.ses_keep_going)) } }
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

    // Etiqueta, no botón: un AssistChip con onClick vacío se ilumina al tocarlo y promete
    // algo que no pasa. Aquí solo dice por dónde vas.
    Etiqueta(stringResource(Res.string.ses_step_of, s.stepIndex + 1, s.totalUnits))
    if (hasSeries) {
        Text(
            stringResource(Res.string.ses_set_of, s.setNumber, paso.numSeries),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Text(Contenido.t(nombre), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (paso.notas.isNotBlank()) NoteCard(Contenido.t(paso.notas))

    when (s.phase) {
        SessionPhase.TIMED_SET -> {
            // El objetivo de la sesión, no el del paso: «±10 s» (adjustTimedSet) lo cambia ahí y es
            // el que usan el aviso y el reloj. El motor lo fija desde el paso al empezar la serie.
            CountdownCircle(elapsed = s.timedElapsed(now), target = s.timedTargetSeconds)
            TimerButtons(s, viewModel)
            PrimaryAdvance(if (hasSeries) stringResource(Res.string.ses_set_done) else stringResource(Res.string.ses_next_step)) {
                viewModel.completeFixedSerie("")
            }
            if (canEndEarly) EndExerciseButton { viewModel.completeFixedSerie("", endExercise = true) }
        }
        SessionPhase.WORKING -> {
            RepsTarget(paso)
            // Alternativa (solo en el paso de burpees de la militar): jumping jacks por tiempo.
            val alternativa = paso.alternativa
            if (alternativa != null && !usingAlt) {
                OutlinedButton(
                    onClick = { viewModel.chooseAlternative() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.ses_instead, Contenido.t(alternativa.nombre))) }
            }
            var reps by remember(s.stepIndex, s.setNumber) { mutableStateOf("") }
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text(stringResource(Res.string.ses_reps_done)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAdvance(if (hasSeries) stringResource(Res.string.ses_set_done) else stringResource(Res.string.ses_step_done)) {
                viewModel.completeFixedSerie(reps)
            }
            if (canEndEarly) EndExerciseButton { viewModel.completeFixedSerie(reps, endExercise = true) }
        }
        SessionPhase.RESTING -> {
            Text(
                if (s.restBetweenExercises) stringResource(Res.string.ses_rest_next_exercise) else stringResource(Res.string.ses_rest_between_sets),
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
            PrimaryAdvance(stringResource(Res.string.ses_skip_rest)) { viewModel.endRoutineRest() }
        }
        SessionPhase.FINISHED -> FinishedNote(stringResource(Res.string.ses_routine_done))
        else -> {}
    }
}

@Composable
private fun EndExerciseButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.ses_end_exercise))
    }
}

// -------------------------------------------------------------- Quema grasa

@Composable
private fun FatburnContent(s: ActiveSession, now: Long, viewModel: PlanViewModel) {
    val exercise = viewModel.specialWorkouts.ejercicio(s.exerciseId ?: "") ?: return
    val protocol = exercise.protocolos.firstOrNull { it.nombre == s.protocolName } ?: return
    val unitLabel = when {
        protocol.esSeries -> stringResource(Res.string.ses_set_of, s.stepIndex + 1, s.totalUnits)
        s.totalUnits > 1 -> stringResource(Res.string.ses_round_of, s.stepIndex + 1, s.totalUnits)
        else -> Contenido.t(protocol.nombre)
    }
    Etiqueta(unitLabel)

    when (s.phase) {
        SessionPhase.TIMED_SET -> {
            CountdownCircle(elapsed = s.timedElapsed(now), target = s.timedTargetSeconds)
            TimerButtons(s, viewModel)
            PrimaryAdvance(if (protocol.esIntervalos) stringResource(Res.string.ses_round_done) else stringResource(Res.string.ses_done)) {
                viewModel.completeFatburnUnit("")
            }
        }
        SessionPhase.WORKING -> {
            if (protocol.repsLabel.isNotBlank()) {
                Text(
                    stringResource(Res.string.ses_target_reps, Contenido.t(protocol.repsLabel)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (exercise.notas_forma.isNotBlank()) NoteCard(Contenido.t(exercise.notas_forma))
            var reps by remember(s.stepIndex) { mutableStateOf("") }
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text(stringResource(Res.string.ses_reps_done)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAdvance(stringResource(Res.string.ses_set_done)) { viewModel.completeFatburnUnit(reps) }
        }
        SessionPhase.RESTING -> {
            Text(stringResource(Res.string.ses_rest), style = MaterialTheme.typography.titleMedium)
            CountdownCircle(
                elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0),
                target = s.restTargetSeconds
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.adjustRest(-15) }) { Text("-15 s") }
                OutlinedButton(onClick = { viewModel.adjustRest(15) }) { Text("+15 s") }
            }
            PrimaryAdvance(stringResource(Res.string.ses_skip_rest)) { viewModel.endRoutineRest() }
        }
        SessionPhase.FINISHED -> FinishedNote(stringResource(Res.string.ses_exercise_done))
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
                if (remaining >= 0) stringResource(Res.string.ses_remaining) else stringResource(Res.string.ses_over),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * «-10 s / Pausa / +10 s». Con la letra al 150 % en un móvil de 360 dp, «Reanudar» se comía
 * el ancho y «+10 s» acababa en vertical («+ / 1 / 0»). Ahora los laterales ocupan lo que
 * mide su texto, sin partirlo, y el central se queda con el resto. Si aun así el central no
 * cabe (letra al 200 %), encogen los tres a la vez, como en EffortChips: así los laterales
 * le dejan sitio y la fila no se ve desigual.
 */
@Composable
private fun TimerButtons(s: ActiveSession, viewModel: PlanViewModel) {
    val central = if (s.timedPaused) stringResource(Res.string.ses_resume) else stringResource(Res.string.ses_pause)
    val escala = LocalDensity.current.fontScale
    val ancho = anchoPantallaDp()
    // Se vuelve a medir desde el tamaño normal si cambia el texto, la letra o el ancho.
    var factor by remember(central, escala, ancho) { mutableFloatStateOf(1f) }
    var cabe by remember(central, escala, ancho, factor) { mutableStateOf(false) }
    val texto: @Composable (String, ((TextLayoutResult) -> Unit)?) -> Unit = { t, medir ->
        // El estilo que pone el botón a su etiqueta, leído aquí dentro.
        val estilo = LocalTextStyle.current
        Text(
            t,
            maxLines = 1,
            softWrap = false,
            style = if (estilo.fontSize.type == TextUnitType.Sp) estilo.copy(fontSize = estilo.fontSize * factor) else estilo,
            // No se pinta mientras no quepa: evita ver el corte un instante.
            modifier = Modifier.drawWithContent { if (cabe) drawContent() },
            onTextLayout = medir ?: {}
        )
    }
    val relleno = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // Los tres igual de altos.
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        OutlinedButton(onClick = { viewModel.adjustTimedSet(-10) }, contentPadding = relleno, modifier = Modifier.fillMaxHeight()) {
            texto("-10 s", null)
        }
        OutlinedButton(
            onClick = { if (s.timedPaused) viewModel.resumeTimedSet() else viewModel.pauseTimedSet() },
            contentPadding = relleno,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            texto(central) { layout ->
                // Ni con la letra al máximo baja de la mitad: antes, cortado que ilegible.
                // Sin letra en sp el factor no se aplica: bajarlo solo dejaría el texto sin pintar.
                val sp = layout.layoutInput.style.fontSize.type == TextUnitType.Sp
                if (layout.didOverflowWidth && sp && factor > 0.5f) {
                    factor = (factor * 0.9f).coerceAtLeast(0.5f)
                } else {
                    cabe = true
                }
            }
        }
        OutlinedButton(onClick = { viewModel.adjustTimedSet(10) }, contentPadding = relleno, modifier = Modifier.fillMaxHeight()) {
            texto("+10 s", null)
        }
    }
}

@Composable
private fun RepsTarget(paso: PasoMilitar) {
    val target = when {
        paso.reps.equals("AMRAP", ignoreCase = true) -> stringResource(Res.string.ses_amrap)
        paso.repsObjetivo.isNotBlank() -> stringResource(Res.string.ses_target_reps, Contenido.t(paso.repsObjetivo))
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
    return "%d:%02d".fmt(s / 60, s % 60)
}

private fun formatClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val m = total / 60
    val sec = total % 60
    return "%02d:%02d".fmt(m, sec)
}

/** Píldora informativa: la forma de un chip sin fingir que se puede pulsar. */
@Composable
private fun Etiqueta(texto: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
