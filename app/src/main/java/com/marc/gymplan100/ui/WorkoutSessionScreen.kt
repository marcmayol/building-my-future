@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.data.isBodyweightScheme
import com.marc.gymplan100.data.secondsPerSetFromScheme
import com.marc.gymplan100.data.setCountFromScheme
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.delay

@Composable
fun WorkoutSessionScreen(
    dayNumber: Int,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val session by viewModel.activeSession.collectAsState()
    val day = PlanData.dayByNumber(dayNumber) ?: PlanData.days.first()
    var showQuitDialog by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()

    // Reloj que avanza cada medio segundo para refrescar cronómetros.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffectTick { now = it }

    val s = session
    if (s == null || s.dayNumber != dayNumber) {
        StartPrompt(
            day = day,
            onStart = { viewModel.startSession(dayNumber) },
            onStartSpecial = { viewModel.startSpecialSession(dayNumber) },
            onExit = onExit
        )
        return
    }

    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val pos = order.indexOf(s.exerciseIndex).coerceAtLeast(0)
    val dayContext = "${pos + 1} de ${order.size} · Día ${s.dayNumber}"

    when (s.phase) {
        SessionPhase.WARMUP -> WarmupContent(s, day, now, dark, viewModel) { showQuitDialog = true }
        SessionPhase.WORKING -> WorkingContent(s, day, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.TIMED_SET -> TimedSetContent(s, day, now, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.RESTING -> RestingContent(s, day, now, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.FREE -> FreeContent(
            s = s, day = day, now = now, dark = dark,
            onFinish = { viewModel.finishSession(); onExit() },
            onExit = { showQuitDialog = true }
        )
        SessionPhase.FINISHED -> FinishedContent(
            s = s, day = day, now = now, dark = dark,
            onFinish = { viewModel.finishSession(); onExit() },
            onExit = { showQuitDialog = true }
        )
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("¿Dejas el entreno a medias?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Puedes reanudarlo cuando quieras: se guarda por dónde ibas, series y " +
                        "pesos incluidos."
                )
            },
            confirmButton = {
                Button(onClick = { showQuitDialog = false; onExit() }) { Text("Reanudar luego") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuitDialog = false
                    viewModel.cancelSession()
                    onExit()
                }) { Text("Descartar el entreno") }
            }
        )
    }
}

// ─── Calentamiento ────────────────────────────────────────────────────────────

@Composable
private fun WarmupContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val elapsed = s.warmupElapsed(now)
    val remaining = s.warmupTargetSeconds - elapsed
    val done = remaining <= 0
    val totalSets = day.template.exercises.sumOf { setCountFromScheme(it.scheme) }

    SessionShell(
        tint = SessionTint.WARMUP,
        dark = dark,
        stateLabel = "Calentamiento",
        context = "Día ${s.dayNumber}",
        onExit = onExit,
        secondaries = {
            SessionSecondary("−1 min", { viewModel.adjustWarmup(-60) })
            SessionSecondary(
                if (s.warmupPaused) "Seguir" else "Pausa",
                { if (s.warmupPaused) viewModel.resumeWarmup() else viewModel.pauseWarmup() }
            )
            SessionSecondary("+1 min", { viewModel.adjustWarmup(60) })
        },
        contextCard = {
            SessionCard(label = "Hoy toca", dark = dark) {
                Text(
                    day.template.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Space.x1))
                Text(
                    "${day.template.exercises.size} ejercicios · $totalSets series",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        primary = {
            SessionPrimary(
                text = "Empezar ejercicios",
                onClick = { viewModel.endWarmup() },
                dark = dark,
                tint = SessionTint.WARMUP,
                alert = done
            )
        }
    ) {
        Countdown(
            text = if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = when {
                s.warmupPaused -> "en pausa · ${day.template.warmup}"
                !done -> "restante · ${day.template.warmup}"
                else -> "de más · cuando quieras, empezamos"
            },
            captionColor = tintAccent(SessionTint.WARMUP)
        )
    }
}

// ─── Serie ────────────────────────────────────────────────────────────────────

@Composable
private fun WorkingContent(
    s: ActiveSession,
    day: TrainingDay,
    dark: Boolean,
    dayContext: String,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val pos = order.indexOf(s.exerciseIndex).coerceAtLeast(0)
    val hasNext = pos < order.lastIndex

    val timedSecs = secondsPerSetFromScheme(exercise.scheme)
    val bodyweight = isBodyweightScheme(exercise.scheme)

    var weight by remember(s.exerciseIndex, s.setNumber) {
        mutableStateOf(viewModel.suggestedWeight(s))
    }
    var showGuide by remember { mutableStateOf(false) }
    var showDayPlan by remember { mutableStateOf(false) }
    var guideFromPlan by remember { mutableStateOf<com.marc.gymplan100.data.Exercise?>(null) }

    val context = LocalContext.current
    val female = LocalIsFemale.current
    val imageRes = ExerciseImages.forName(context, exercise.name, female)

    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = "Serie ${s.setNumber} de $totalSets",
        context = dayContext,
        onExit = onExit,
        secondaries = {
            if (hasNext) {
                // Tras un cambio, el rótulo deja claro que se puede seguir saltando.
                SessionSecondary(
                    if (s.occupiedSkips > 0) "Otra ocupada" else "Máquina ocupada",
                    { viewModel.skipExercise() }
                )
            }
            SessionSecondary("Cómo se hace", { showGuide = true })
            // "Plan" y no "Ejercicios": con el texto del sistema grande la palabra larga no
            // cabía en su tercio y se partía a media palabra ("Ejercicio/s").
            SessionSecondary("Plan", { showDayPlan = true })
        },
        contextCard = {
            if (timedSecs == null && !bodyweight) {
                WeightBlock(
                    value = weight,
                    onValue = { weight = it },
                    dark = dark,
                    exerciseInCatalog = imageRes != null
                )
            }
        },
        primary = {
            when {
                timedSecs != null -> SessionPrimary(
                    text = "Empezar serie · ${formatSecs(timedSecs)}",
                    onClick = { viewModel.startTimedSet() },
                    dark = dark, tint = SessionTint.WORK
                )
                bodyweight -> SessionPrimary(
                    text = "Vuelta hecha",
                    onClick = { viewModel.completeSet("") },
                    dark = dark, tint = SessionTint.WORK
                )
                else -> SessionPrimary(
                    text = "Serie hecha",
                    onClick = { viewModel.completeSet(weight) },
                    dark = dark, tint = SessionTint.WORK
                )
            }
        }
    ) {
        Text(
            exercise.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        Spacer(Modifier.height(Space.x1))
        Text(
            buildString {
                append(exercise.scheme)
                if (exercise.note.isNotBlank()) append(" · ${exercise.note}")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = tintAccent(SessionTint.WORK)
        )
        if (imageRes != null) {
            Spacer(Modifier.height(Space.x4))
            Image(
                painter = painterResource(imageRes),
                contentDescription = exercise.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            )
        } else {
            // Un plan propio puede traer ejercicios que no están en el catálogo: se dice con
            // palabras en vez de dejar un hueco.
            Spacer(Modifier.height(Space.x4))
            SessionCard(label = "Sin ilustración", dark = dark) {
                Text(
                    "Este ejercicio no está en el catálogo de la app, así que no tengo " +
                        "ilustración ni ficha. Te lo busco en el vídeo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showGuide) {
        ExerciseGuideSheet(
            exerciseName = exercise.name,
            scheme = exercise.scheme,
            onDismiss = { showGuide = false }
        )
    }
    if (showDayPlan) {
        DayPlanSheet(
            day = day,
            session = s,
            onExerciseClick = { ex -> showDayPlan = false; guideFromPlan = ex },
            onDismiss = { showDayPlan = false }
        )
    }
    guideFromPlan?.let { ex ->
        ExerciseGuideSheet(
            exerciseName = ex.name,
            scheme = ex.scheme,
            onDismiss = { guideFromPlan = null }
        )
    }
}

/**
 * El peso de la serie, con botones en vez de teclado.
 *
 * De pie, con una mano y a veces con la máquina esperando, el teclado numérico era lo peor de
 * la pantalla. Ahora se sube y baja de 2,5 en 2,5 (manteniendo pulsado corre), hay una rueda
 * para saltos grandes y el teclado queda como escape tocando la cifra.
 */
@Composable
private fun WeightBlock(
    value: String,
    onValue: (String) -> Unit,
    dark: Boolean,
    exerciseInCatalog: Boolean,
) {
    val styles = LocalAppTextStyles.current
    var showWheel by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    val kg = parseKg(value)

    SessionCard(label = "Peso de esta serie", dark = dark) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("−", { onValue(formatKg((kg ?: 0.0) - 2.5)) }, enabled = (kg ?: 0.0) >= 2.5)
            // El valor va en un Text con weight: dentro de un Box se salía de su hueco y se
            // comía el círculo de la izquierda.
            Text(
                text = buildAnnotatedString {
                    withStyle(styles.dataMedium.toSpanStyle()) {
                        append(if (kg != null) formatKg(kg) else "—")
                    }
                    if (kg != null) {
                        withStyle(
                            MaterialTheme.typography.titleMedium.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) { append(" kg") }
                    }
                },
                color = if (kg != null) MaterialTheme.colorScheme.onSurface
                // Sin peso todavía, el guion es una pista, no un dato: va apagado para que no
                // se lea como un elemento gráfico.
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Space.x2)
                    .clickable { typing = true }
            )
            StepperButton("+", { onValue(formatKg((kg ?: 0.0) + 2.5)) })
        }
        Spacer(Modifier.height(Space.x2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (kg != null) "Pasos de 2,5 kg · el mismo que la serie anterior"
                else if (exerciseInCatalog) "Primera vez con este: pon el peso que uses hoy"
                else "Primera vez con este ejercicio: pon el peso que uses hoy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showWheel = true }) { Text("Rueda") }
        }
    }

    if (showWheel) {
        val steps = remember { (0..120).map { it * 2.5 } }
        val current = steps.indexOfFirst { it >= (kg ?: 0.0) }.coerceAtLeast(0)
        ModalBottomSheet(onDismissRequest = { showWheel = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screen)
                    .padding(bottom = Space.block)
            ) {
                Text(
                    "Peso de esta serie",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(Space.x3))
                WheelPicker(
                    items = steps.map { "${formatKg(it)} kg" },
                    selectedIndex = current,
                    onSelectedIndexChange = { onValue(formatKg(steps[it])) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.x3))
                Button(
                    onClick = { showWheel = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) { Text("Hecho") }
            }
        }
    }

    if (typing) {
        AlertDialog(
            onDismissRequest = { typing = false },
            title = { Text("Escribe el peso") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { typing = false }) { Text("Hecho") } }
        )
    }
}

// ─── Serie por tiempo ─────────────────────────────────────────────────────────

@Composable
private fun TimedSetContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    dayContext: String,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val elapsed = s.timedElapsed(now)
    val remaining = s.timedTargetSeconds - elapsed
    val done = remaining <= 0
    var showGuide by remember { mutableStateOf(false) }

    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = "Serie ${s.setNumber} de $totalSets · por tiempo",
        context = dayContext,
        onExit = onExit,
        secondaries = {
            SessionSecondary("−10 s", { viewModel.adjustTimedSet(-10) })
            SessionSecondary(
                if (s.timedPaused) "Seguir" else "Pausa",
                { if (s.timedPaused) viewModel.resumeTimedSet() else viewModel.pauseTimedSet() }
            )
            SessionSecondary("+10 s", { viewModel.adjustTimedSet(10) })
        },
        primary = {
            SessionPrimary(
                text = "Serie hecha",
                onClick = { viewModel.completeSet("") },
                dark = dark,
                tint = SessionTint.WORK,
                alert = done
            )
        }
    ) {
        Countdown(
            text = if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = when {
                s.timedPaused -> "en pausa"
                !done -> "aguanta"
                else -> "¡tiempo!"
            },
            captionColor = tintAccent(SessionTint.WARMUP)
        )
        Spacer(Modifier.height(Space.block))
        Text(
            exercise.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Space.x1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append(exercise.scheme)
                    if (exercise.note.isNotBlank()) append(" · ${exercise.note}")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showGuide = true }) { Text("Cómo se hace") }
        }
    }

    if (showGuide) {
        ExerciseGuideSheet(
            exerciseName = exercise.name,
            scheme = exercise.scheme,
            onDismiss = { showGuide = false }
        )
    }
}

// ─── Descanso ─────────────────────────────────────────────────────────────────

@Composable
private fun RestingContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    dayContext: String,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0)
    val remaining = s.restTargetSeconds - elapsed
    val done = remaining <= 0

    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val nextExercise = if (s.setNumber < totalSets) exercise else {
        val pos = order.indexOf(s.exerciseIndex)
        order.getOrNull(pos + 1)?.let { day.template.exercises.getOrNull(it) }
    }
    val nextIsNewExercise = nextExercise != null && nextExercise !== exercise
    val nextSetNumber = if (s.setNumber < totalSets) s.setNumber + 1 else 1
    val nextTotalSets = nextExercise?.let { setCountFromScheme(it.scheme) } ?: 0

    val nextIsTimed = nextExercise != null && secondsPerSetFromScheme(nextExercise.scheme) != null
    val nextIsBodyweight = nextExercise != null && isBodyweightScheme(nextExercise.scheme)
    var plannedWeight by remember(s.restStartMillis) {
        mutableStateOf(s.plannedWeight.ifBlank { viewModel.plannedWeightSuggestion(s) })
    }

    var showNextGuide by remember { mutableStateOf(false) }
    var showDayPlan by remember { mutableStateOf(false) }
    var guideFromPlan by remember { mutableStateOf<com.marc.gymplan100.data.Exercise?>(null) }

    SessionShell(
        tint = SessionTint.REST,
        dark = dark,
        stateLabel = if (s.restBetweenExercises) "Descanso · cambio de máquina" else "Descanso",
        context = dayContext,
        onExit = onExit,
        secondaries = {
            SessionSecondary("−30 s", { viewModel.adjustRest(-30) })
            // "Plan" y no "Ejercicios": con el texto del sistema grande la palabra larga no
            // cabía en su tercio y se partía a media palabra ("Ejercicio/s").
            SessionSecondary("Plan", { showDayPlan = true })
            SessionSecondary("+30 s", { viewModel.adjustRest(30) })
        },
        contextCard = {
            if (nextExercise != null) {
                SessionCard(
                    label = if (nextIsNewExercise) "Después · deja la máquina lista" else "Después",
                    dark = dark
                ) {
                    Text(
                        nextExercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Space.x1))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Serie $nextSetNumber de $nextTotalSets · ${nextExercise.scheme}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showNextGuide = true }) {
                            Text(if (nextIsNewExercise) "Buscar la máquina" else "Repasar")
                        }
                    }
                    if (!nextIsTimed && !nextIsBodyweight) {
                        Spacer(Modifier.height(Space.x3))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val kg = parseKg(plannedWeight)
                            StepperButton(
                                "−",
                                {
                                    plannedWeight = formatKg((kg ?: 0.0) - 2.5)
                                    viewModel.setPlannedWeight(plannedWeight)
                                },
                                enabled = (kg ?: 0.0) >= 2.5,
                                size = Touch.min
                            )
                            Text(
                                if (kg != null) "${formatKg(kg)} kg" else "— kg",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = Space.x2)
                            )
                            StepperButton(
                                "+",
                                {
                                    plannedWeight = formatKg((kg ?: 0.0) + 2.5)
                                    viewModel.setPlannedWeight(plannedWeight)
                                },
                                size = Touch.min
                            )
                        }
                    }
                }
            }
        },
        primary = {
            SessionPrimary(
                text = "Empezar ya",
                onClick = { viewModel.endRest() },
                dark = dark,
                tint = SessionTint.REST,
                alert = done
            )
        }
    ) {
        // El hueco de arriba lo llena la ilustración de lo que viene: descansando es justo
        // cuando da tiempo a mirar el movimiento. Va con weight para que se encoja sola —o
        // desaparezca— antes que empujar la cuenta atrás fuera del alcance del pulgar.
        val nextImageRes = nextExercise?.let {
            ExerciseImages.forName(LocalContext.current, it.name, LocalIsFemale.current)
        }
        if (nextImageRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = Space.x4),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(nextImageRes),
                    contentDescription = nextExercise.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                )
            }
        }
        Countdown(
            text = if (remaining >= 0) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = if (remaining >= 0) "restante de ${s.restTargetSeconds} s"
            else "de más · cuando quieras",
            captionColor = tintAccent(SessionTint.REST)
        )
        Spacer(Modifier.height(Space.x4))
        SessionProgress(
            fraction = if (s.restTargetSeconds <= 0) 1f
            else elapsed.toFloat() / s.restTargetSeconds,
            tint = SessionTint.REST
        )
    }

    if (showNextGuide && nextExercise != null) {
        ExerciseGuideSheet(
            exerciseName = nextExercise.name,
            scheme = nextExercise.scheme,
            onDismiss = { showNextGuide = false }
        )
    }
    if (showDayPlan) {
        DayPlanSheet(
            day = day,
            session = s,
            onExerciseClick = { ex -> showDayPlan = false; guideFromPlan = ex },
            onDismiss = { showDayPlan = false }
        )
    }
    guideFromPlan?.let { ex ->
        ExerciseGuideSheet(
            exerciseName = ex.name,
            scheme = ex.scheme,
            onDismiss = { guideFromPlan = null }
        )
    }
}

// ─── Sesión libre / extra ─────────────────────────────────────────────────────

@Composable
private fun FreeContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = if (s.extra) "Entrenamiento extra" else "Entrenamiento libre",
        context = if (s.extra) "no cuenta día" else "Día ${s.dayNumber}",
        onExit = onExit,
        primary = {
            SessionPrimary(
                text = "Finalizar y guardar",
                onClick = onFinish,
                dark = dark,
                tint = SessionTint.WORK
            )
        }
    ) {
        Countdown(
            text = formatClock(now - s.startMillis),
            caption = "en marcha",
            captionColor = tintAccent(SessionTint.WORK)
        )
        Spacer(Modifier.height(Space.block))
        Text(
            if (s.extra)
                "Un bonus: se guarda en tu historial pero no cuenta como día del plan."
            else
                "Sesión libre del día ${day.number}. El tiempo corre hasta que pulses finalizar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Fin ──────────────────────────────────────────────────────────────────────

@Composable
private fun FinishedContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val styles = LocalAppTextStyles.current
    val appColors = LocalAppColors.current
    val sets = s.completedSets.size
    val totalRest = s.completedSets.sumOf { it.restSeconds }
    val minutes = ((now - s.startMillis) / 60000).toInt().coerceAtLeast(0)

    // Peso final por ejercicio, en el orden en que se hicieron.
    val weights = s.completedSets
        .filter { it.weight.isNotBlank() }
        .groupBy { it.exerciseIndex }
        .map { (idx, sets) ->
            (day.template.exercises.getOrNull(idx)?.name ?: "Ejercicio") to sets.last().weight
        }

    SessionShell(
        tint = SessionTint.NEUTRAL,
        dark = dark,
        stateLabel = "Día ${s.dayNumber} completado",
        context = "",
        onExit = onExit,
        contextCard = {
            Text(
                "Se guarda también en Google Health.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Space.x3)
            )
        },
        primary = {
            SessionPrimary(
                text = "Guardar el día",
                onClick = onFinish,
                dark = dark,
                tint = SessionTint.NEUTRAL,
                gradient = appColors.brandGradient
            )
        }
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                day.template.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Space.x4))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                ResultTile("$minutes", "min", "duración", Modifier.weight(1f), dark)
                ResultTile("$sets", "", "series", Modifier.weight(1f), dark)
            }
            Spacer(Modifier.height(Space.x3))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                ResultTile(formatSecs(totalRest), "", "descanso total", Modifier.weight(1f), dark)
                ResultTile(
                    formatSecs(if (sets > 0) totalRest / sets else 0), "",
                    "media por serie", Modifier.weight(1f), dark
                )
            }
            if (weights.isNotEmpty()) {
                Spacer(Modifier.height(Space.x3))
                SessionCard(label = "Pesos de hoy", dark = dark) {
                    weights.forEach { (name, kg) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Space.x1)
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$kg kg",
                                style = styles.tabular,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultTile(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    dark: Boolean
) {
    val styles = LocalAppTextStyles.current
    val bg = if (dark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = styles.displayCard,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unit.isNotBlank()) {
                Text(
                    " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Antes de empezar ─────────────────────────────────────────────────────────

@Composable
private fun StartPrompt(
    day: TrainingDay,
    onStart: () -> Unit,
    onStartSpecial: () -> Unit,
    onExit: () -> Unit
) {
    var showSpecialConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space.screen),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Día ${day.number}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x2))
        Text(
            day.template.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Space.x2))
        Text(
            "${day.template.exercises.size} ejercicios · calentamiento: ${day.template.warmup}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.block))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) { Text("Empezar entrenamiento") }
        Spacer(Modifier.height(Space.x3))
        OutlinedButton(
            onClick = { showSpecialConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) {
            Icon(Icons.Filled.Star, contentDescription = null)
            Spacer(Modifier.width(Space.x2))
            // Sin el "(con guía)": cabía en una sola línea solo a tamaño de texto normal, y
            // partida dejaba la estrella descolgada. Lo que es ya lo cuenta el diálogo, y así
            // se llama igual que en el reloj.
            Text("Entrenamiento especial")
        }
        Spacer(Modifier.height(Space.x2))
        TextButton(onClick = onExit) { Text("Volver") }
    }

    if (showSpecialConfirm) {
        AlertDialog(
            onDismissRequest = { showSpecialConfirm = false },
            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
            title = { Text("¿Entrenamiento especial?") },
            text = {
                Text(
                    "Este modo es para cuando entrenas guiado (p. ej. con tu tío). " +
                        "No habrá series ni pesos: solo un cronómetro contando el tiempo hasta que " +
                        "pulses \"Finalizar\". Se guardará como el entreno del día ${day.number}."
                )
            },
            confirmButton = {
                Button(onClick = { showSpecialConfirm = false; onStartSpecial() }) {
                    Text("Sí, empezar especial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpecialConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

// ─── Utilidades ───────────────────────────────────────────────────────────────

/** Bucle que actualiza un reloj cada 500 ms mientras la pantalla está activa. */
@Composable
private fun LaunchedEffectTick(onTick: (Long) -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            onTick(System.currentTimeMillis())
            delay(500)
        }
    }
}

/** "12,5" o "12.5" -> 12.5. Null si no hay número. */
fun parseKg(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

/** 12.5 -> "12,5"; 40.0 -> "40". Coma decimal, que es como se escribe aquí. */
fun formatKg(value: Double): String {
    val v = value.coerceIn(0.0, 500.0)
    return if (v % 1.0 == 0.0) v.toInt().toString()
    else "%.1f".format(v).replace('.', ',')
}

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun formatSecs(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
