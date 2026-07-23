@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.isBodyweightScheme
import com.marc.gymplan100.data.secondsPerSetFromScheme
import com.marc.gymplan100.data.setCountFromScheme
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

    // Reloj que avanza cada medio segundo para refrescar cronómetros.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffectTick { now = it }

    val s = session
    if (s == null || s.dayNumber != dayNumber) {
        // No hay sesión activa para este día: ofrecer empezar.
        StartPrompt(
            day = day,
            onStart = { viewModel.startSession(dayNumber) },
            onStartSpecial = { viewModel.startSpecialSession(dayNumber) },
            onExit = onExit
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Día ${s.dayNumber} · ${day.template.title}")
                        Text(
                            formatClock(now - s.startMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showQuitDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Salir")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = inner.calculateTopPadding())
        ) {
            when (s.phase) {
                SessionPhase.WARMUP -> WarmupContent(s = s, day = day, now = now, viewModel = viewModel)
                SessionPhase.WORKING -> WorkingContent(s = s, day = day, viewModel = viewModel)
                SessionPhase.TIMED_SET -> TimedSetContent(s = s, day = day, now = now, viewModel = viewModel)
                SessionPhase.RESTING -> RestingContent(s = s, day = day, now = now, viewModel = viewModel)
                SessionPhase.FREE -> FreeContent(
                    s = s,
                    day = day,
                    now = now,
                    onFinish = { viewModel.finishSession(); onExit() }
                )
                SessionPhase.FINISHED -> FinishedContent(
                    s = s,
                    now = now,
                    onFinish = { viewModel.finishSession(); onExit() }
                )
            }
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("¿Salir del entrenamiento?") },
            text = { Text("Puedes reanudarlo más tarde desde la pantalla principal, o descartarlo.") },
            confirmButton = {
                TextButton(onClick = { showQuitDialog = false; onExit() }) {
                    Text("Reanudar luego")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuitDialog = false
                    viewModel.cancelSession()
                    onExit()
                }) { Text("Descartar") }
            }
        )
    }
}

@Composable
private fun WorkingContent(
    s: ActiveSession,
    day: com.marc.gymplan100.data.TrainingDay,
    viewModel: PlanViewModel
) {
    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val pos = order.indexOf(s.exerciseIndex).coerceAtLeast(0)
    val hasNext = pos < order.lastIndex

    // Si el ejercicio se mide por tiempo (planchas, isométricos) no pedimos peso:
    // se hará con una cuenta atrás que avisa al terminar la serie.
    val timedSecs = secondsPerSetFromScheme(exercise.scheme)
    // Circuitos de peso corporal (por vueltas): tampoco hay kilos que registrar.
    val bodyweight = isBodyweightScheme(exercise.scheme)

    // Peso por serie: precargado con el sugerido (serie anterior) y editable.
    var weight by remember(s.exerciseIndex, s.setNumber) {
        mutableStateOf(viewModel.suggestedWeight(s))
    }
    var showGuide by remember { mutableStateOf(false) }
    // Se leen aquí (contexto @Composable) porque dentro del LazyColumn no se puede.
    val context = LocalContext.current
    val female = LocalIsFemale.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Ejercicio ${pos + 1} de ${order.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Serie ${s.setNumber} de $totalSets  ·  ${exercise.scheme}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        val imageRes = ExerciseImages.forName(context, exercise.name, female)
        if (imageRes != null) {
            item {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                )
            }
        }
        if (timedSecs != null) {
            item {
                Button(
                    onClick = { viewModel.startTimedSet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Empezar serie · ${formatSecs(timedSecs)}")
                }
            }
        } else if (bodyweight) {
            // Peso corporal: nada de kilos, solo confirmar la vuelta hecha.
            item {
                Button(
                    onClick = { viewModel.completeSet("") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Vuelta hecha")
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso de esta serie (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = { viewModel.completeSet(weight) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Serie hecha")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { showGuide = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Text("  ¿Cómo se hace? · máquina y alternativas")
            }
        }
        if (hasNext) {
            item {
                // Tras un cambio, el rótulo deja claro que se puede seguir saltando si la
                // nueva máquina también está pillada (el ejercicio se pospone al final).
                val skipLabel = if (s.occupiedSkips > 0)
                    "Esta máquina también está ocupada · siguiente"
                else
                    "Máquina ocupada · cambiar por el siguiente"
                OutlinedButton(
                    onClick = { viewModel.skipExercise() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    Text("  $skipLabel")
                }
            }
        }
        item {
            // Vista rápida de los ejercicios del día, en su orden actual.
            Spacer(Modifier.height(4.dp))
            Text(
                "Ejercicios del día",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        itemsIndexed(order) { i, exIndex ->
            val ex = day.template.exercises[exIndex]
            val done = s.completedSets.count { it.exerciseIndex == exIndex } >=
                setCountFromScheme(ex.scheme)
            val state = when {
                exIndex == s.exerciseIndex -> "ahora"
                done -> "hecho"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${i + 1}. ${ex.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (exIndex == s.exerciseIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (exIndex == s.exerciseIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.isNotEmpty()) {
                    Text(
                        state,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
}

@Composable
private fun TimedSetContent(
    s: ActiveSession,
    day: com.marc.gymplan100.data.TrainingDay,
    now: Long,
    viewModel: PlanViewModel
) {
    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val elapsed = s.timedElapsed(now)
    val remaining = s.timedTargetSeconds - elapsed
    val done = remaining <= 0
    var showGuide by remember { mutableStateOf(false) }

    // El aviso al terminar la serie lo lanza la notificación del sistema (RestReminder),
    // para que suene también con la pantalla apagada o la app en segundo plano.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistChip(onClick = {}, label = { Text("${exercise.name} · serie ${s.setNumber} de $totalSets") })
        Spacer(Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            val fraction = if (s.timedTargetSeconds <= 0) 0f
            else (elapsed.toFloat() / s.timedTargetSeconds).coerceIn(0f, 1f)
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!done) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    when {
                        s.timedPaused -> "en pausa"
                        !done -> "aguanta"
                        else -> "¡tiempo!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (s.timedPaused || done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.adjustTimedSet(-10) }) { Text("-10 s") }
            OutlinedButton(
                onClick = {
                    if (s.timedPaused) viewModel.resumeTimedSet() else viewModel.pauseTimedSet()
                }
            ) {
                if (s.timedPaused) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reanudar")
                    Text(" Reanudar")
                } else {
                    PauseGlyph(tint = MaterialTheme.colorScheme.primary)
                    Text(" Pausar")
                }
            }
            OutlinedButton(onClick = { viewModel.adjustTimedSet(10) }) { Text("+10 s") }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.completeSet("") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Text("  Serie hecha")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showGuide = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Text("  ¿Cómo se hace?")
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

@Composable
private fun WarmupContent(
    s: ActiveSession,
    day: com.marc.gymplan100.data.TrainingDay,
    now: Long,
    viewModel: PlanViewModel
) {
    val elapsed = s.warmupElapsed(now)
    val remaining = s.warmupTargetSeconds - elapsed
    val done = remaining <= 0

    // El aviso al terminar lo lanza la notificación del sistema (RestReminder),
    // para que suene también con la pantalla apagada o la app en segundo plano.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistChip(onClick = {}, label = { Text("Calentamiento") })
        Spacer(Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            val fraction = if (s.warmupTargetSeconds <= 0) 0f
            else (elapsed.toFloat() / s.warmupTargetSeconds).coerceIn(0f, 1f)
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!done) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    when {
                        s.warmupPaused -> "en pausa"
                        !done -> "restante"
                        else -> "de más"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (s.warmupPaused || done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.adjustWarmup(-60) }) { Text("-1 min") }
            OutlinedButton(
                onClick = {
                    if (s.warmupPaused) viewModel.resumeWarmup() else viewModel.pauseWarmup()
                }
            ) {
                if (s.warmupPaused) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reanudar")
                    Text(" Reanudar")
                } else {
                    PauseGlyph(tint = MaterialTheme.colorScheme.primary)
                    Text(" Pausar")
                }
            }
            OutlinedButton(onClick = { viewModel.adjustWarmup(60) }) { Text("+1 min") }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            day.template.warmup,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.endWarmup() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Text("  Empezar ejercicios")
        }
    }
}

@Composable
private fun RestingContent(
    s: ActiveSession,
    day: com.marc.gymplan100.data.TrainingDay,
    now: Long,
    viewModel: PlanViewModel
) {
    val elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0)
    val remaining = s.restTargetSeconds - elapsed

    // El aviso al terminar el descanso lo lanza una notificación del sistema
    // (RestReminder), para que suene también con la pantalla apagada.

    // ¿Qué viene después?
    val exercise = day.template.exercises[s.exerciseIndex]
    val totalSets = setCountFromScheme(exercise.scheme)
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    // El siguiente ejercicio real: la próxima serie del mismo ejercicio, o el
    // siguiente del orden si esta era la última serie. Null = sesión casi acabada.
    val nextExercise = if (s.setNumber < totalSets) {
        exercise
    } else {
        val pos = order.indexOf(s.exerciseIndex)
        order.getOrNull(pos + 1)?.let { day.template.exercises.getOrNull(it) }
    }
    val nextIsNewExercise = nextExercise != null && nextExercise !== exercise
    val nextLabel = when {
        s.setNumber < totalSets ->
            "Luego: ${exercise.name} · serie ${s.setNumber + 1} de $totalSets"
        nextExercise != null -> "Luego: ${nextExercise.name}"
        else -> "Última serie completada"
    }

    var showNextGuide by remember { mutableStateOf(false) }
    // Plan completo del día, consultable durante el descanso.
    var showDayPlan by remember { mutableStateOf(false) }
    // Ejercicio elegido desde el plan del día para ver su ficha.
    var guideFromPlan by remember { mutableStateOf<com.marc.gymplan100.data.Exercise?>(null) }

    // El siguiente no lleva kilos que preparar: por tiempo (planchas) o circuito corporal.
    val nextIsTimed = nextExercise != null && secondsPerSetFromScheme(nextExercise.scheme) != null
    val nextIsBodyweight = nextExercise != null && isBodyweightScheme(nextExercise.scheme)
    // Peso preparado para la próxima serie: precargado con la sugerencia y editable.
    var plannedWeight by remember(s.restStartMillis) {
        mutableStateOf(s.plannedWeight.ifBlank { viewModel.plannedWeightSuggestion(s) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistChip(
            onClick = {},
            label = {
                Text(if (s.restBetweenExercises) "Descanso entre ejercicios" else "Descanso entre series")
            }
        )
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            val fraction = if (s.restTargetSeconds <= 0) 0f
            else (elapsed.toFloat() / s.restTargetSeconds).coerceIn(0f, 1f)
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (remaining >= 0) formatSecs(remaining) else "+${formatSecs(-remaining)}",
                    style = MaterialTheme.typography.displayMedium,
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
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.adjustRest(-30) }) { Text("-30 s") }
            OutlinedButton(onClick = { viewModel.adjustRest(30) }) { Text("+30 s") }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            nextLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (nextExercise != null && !nextIsTimed && !nextIsBodyweight) {
            Spacer(Modifier.height(12.dp))
            // Aprovecha el descanso para dejar la máquina preparada con el peso adecuado:
            // se precarga con la sugerencia y lo que dejes aquí precarga la siguiente serie.
            OutlinedTextField(
                value = plannedWeight,
                onValueChange = { plannedWeight = it; viewModel.setPlannedWeight(it) },
                label = {
                    Text(
                        if (nextIsNewExercise) "Peso para preparar la máquina (kg)"
                        else "Peso de la siguiente serie (kg)"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (nextExercise != null) {
            Spacer(Modifier.height(12.dp))
            // Aprovechar el descanso para repasar el siguiente ejercicio:
            // técnica, máquina, alternativas y vídeo en YouTube. Útil sobre todo
            // cuando toca uno nuevo y hay que ir buscando la máquina.
            OutlinedButton(
                onClick = { showNextGuide = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Text(
                    if (nextIsNewExercise) "  Prepara el siguiente · busca la máquina"
                    else "  Repasa cómo se hace"
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Ver el entreno entero sin salir del descanso: qué queda, qué está hecho.
        OutlinedButton(
            onClick = { showDayPlan = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Text("  Plan de hoy")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.endRest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Text("  Continuar")
        }
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
            // Se cierra el plan antes de abrir la ficha para no anidar dos paneles.
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

@Composable
private fun FreeContent(
    s: ActiveSession,
    day: com.marc.gymplan100.data.TrainingDay,
    now: Long,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistChip(
            onClick = {},
            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
            label = { Text(if (s.extra) "Entrenamiento extra" else "Entrenamiento especial") }
        )
        Spacer(Modifier.height(28.dp))
        Text(
            formatClock(now - s.startMillis),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "en marcha",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (s.extra)
                "Entreno extra (bonus). No cuenta como día del plan. El tiempo corre hasta que pulses Finalizar."
            else
                "Sesión libre guiada · Día ${day.number}. El tiempo corre hasta que pulses Finalizar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Text("  Finalizar entrenamiento")
        }
    }
}

@Composable
private fun FinishedContent(
    s: ActiveSession,
    now: Long,
    onFinish: () -> Unit
) {
    val totalRest = s.completedSets.sumOf { it.restSeconds }
    val sets = s.completedSets.size
    val avgRest = if (sets > 0) totalRest / sets else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "¡Entrenamiento completado!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(20.dp))
        SummaryRow("Tiempo total en el gimnasio", formatClock(now - s.startMillis))
        SummaryRow("Series realizadas", "$sets")
        SummaryRow("Descanso total", formatSecs(totalRest))
        SummaryRow("Descanso medio por serie", formatSecs(avgRest))
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Finalizar y guardar")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StartPrompt(
    day: com.marc.gymplan100.data.TrainingDay,
    onStart: () -> Unit,
    onStartSpecial: () -> Unit,
    onExit: () -> Unit
) {
    var showSpecialConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Día ${day.number}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            day.template.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${day.template.exercises.size} ejercicios · calentamiento: ${day.template.warmup}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Empezar entrenamiento")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showSpecialConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Filled.Star, contentDescription = null)
            Text("  Entrenamiento especial (con guía)")
        }
        Spacer(Modifier.height(8.dp))
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
                        "pulses \"Finalizar entrenamiento\". Se guardará como el entreno del día ${day.number}."
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

/** Glifo de pausa (dos barras) del tamaño de un icono, para reaprovechar sin librería extendida. */
@Composable
private fun PauseGlyph(tint: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.size(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 14.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint)
            )
        }
    }
}

/** Bucle que actualiza un reloj cada 500 ms mientras la pantalla está activa. */
@Composable
private fun LaunchedEffectTick(onTick: (Long) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            onTick(System.currentTimeMillis())
            delay(500)
        }
    }
}

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

private fun formatSecs(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
