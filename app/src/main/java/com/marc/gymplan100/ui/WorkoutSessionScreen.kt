@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.setsSummary
import com.marc.gymplan100.data.contar
import com.marc.gymplan100.data.palabra
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.data.REPS_STEP
import com.marc.gymplan100.data.WEIGHT_STEP
import com.marc.gymplan100.data.formatKg
import com.marc.gymplan100.data.isBodyweightScheme
import com.marc.gymplan100.data.parseKg
import com.marc.gymplan100.data.parseReps
import com.marc.gymplan100.data.repsRangeFromScheme
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
    // Apuntando lo hecho al terminar un entreno libre (el paso previo a guardarlo).
    var apuntando by remember { mutableStateOf(false) }
    var tiempoLibre by remember { mutableStateOf(0L) }
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
        SessionPhase.FREE -> if (apuntando) {
            // Entrenando libre el cronómetro solo guardaba el tiempo: antes de cerrar, se
            // ofrece apuntar qué se ha hecho, con los ejercicios del día ya puestos.
            FreeSessionLog(
                day = day,
                elapsedMs = tiempoLibre,
                onSave = { apuntado -> viewModel.finishFreeSession(apuntado); onExit() },
                onSkip = { viewModel.finishSession(); onExit() }
            )
        } else {
            FreeContent(
                s = s, day = day, now = now, dark = dark,
                // El cronómetro se para al pulsar finalizar: lo que se enseña mientras se
                // apunta es el tiempo entrenado, no el rato que se ha tardado en escribirlo.
                onFinish = { tiempoLibre = now - s.startMillis; apuntando = true },
                onExit = { showQuitDialog = true }
            )
        }
        SessionPhase.FINISHED -> FinishedContent(
            s = s, day = day, now = now, dark = dark, viewModel = viewModel,
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
                    contar(day.template.exercises.size, "ejercicio", "ejercicios") + " · " +
                        contar(totalSets, "serie", "series"),
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
        // El calentamiento no tiene dibujo propio (es "6 min de lo que quieras"), así que el
        // hueco lo llena con qué se empieza: da tiempo a ir buscando la máquina.
        day.template.exercises.firstOrNull()?.let {
            SessionIllustration(it.name, label = "Empezamos con ${it.name.lowercase()}")
        }
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

/**
 * La ilustración de un ejercicio ocupando el aire que sobra en una pantalla de cronómetro.
 *
 * Va con `weight`, como la del descanso: se encoge sola —o desaparece— antes que empujar la
 * cuenta atrás y los botones fuera del alcance del pulgar. Con [label], una línea encima que
 * dice qué se está mirando.
 */
@Composable
private fun ColumnScope.SessionIllustration(exerciseName: String, label: String? = null) {
    if (!ExerciseImages.hasVisual(exerciseName)) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(bottom = Space.x4),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(bottom = Space.x2)
            )
        }
        ExerciseVisual(exerciseName, imageHeight = 150.dp)
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
    // El propio ejercicio dice si lleva kilos: una dominada y un press de banca se escriben
    // igual ("3 x 8-12") y solo en uno tiene sentido preguntarlos.
    val bodyweight = exercise.withoutWeight

    var weight by rememberSaveable(s.exerciseIndex, s.setNumber) {
        mutableStateOf(viewModel.suggestedWeight(s))
    }
    // Solo se persiste lo que el usuario toca: si no, cada serie reescribiría en disco el
    // peso que la propia app acaba de sugerir.
    var weightTouched by rememberSaveable(s.exerciseIndex, s.setNumber) { mutableStateOf(false) }

    // Repeticiones: se preguntan cuando el ejercicio se cuenta por ellas. Una plancha va por
    // segundos y un circuito por vueltas, y ahí el contador solo estorbaría.
    val repsRange = repsRangeFromScheme(exercise.scheme)
    var reps by rememberSaveable(s.exerciseIndex, s.setNumber) {
        mutableStateOf(viewModel.suggestedReps(s))
    }
    var repsTouched by rememberSaveable(s.exerciseIndex, s.setNumber) { mutableStateOf(false) }
    LaunchedEffect(reps, repsTouched, s.exerciseIndex, s.setNumber) {
        if (!repsTouched) return@LaunchedEffect
        delay(500)
        viewModel.setSetReps(reps)
    }

    // Serie de aproximación: no gasta serie del plan ni cuenta para los kilos movidos, la
    // fuerza estimada o la progresión. Se apaga sola al pasar de serie.
    // La cuenta de series hechas entra en la clave a proposito: una aproximacion no cambia el
    // numero de serie, asi que sin ella la casilla se quedaria marcada y la serie buena se
    // apuntaria como calentamiento sin que nadie lo notara.
    var esCalentamiento by rememberSaveable(s.exerciseIndex, s.setNumber, s.completedSets.size) {
        mutableStateOf(false)
    }

    // Superserie: con quién va encadenado este ejercicio.
    val grupo = com.marc.gymplan100.data.SessionEngine.supersetMembers(s.dayNumber, s.exerciseIndex)
    val posEnGrupo = grupo.indexOf(s.exerciseIndex)
    val enSuperserie = grupo.size > 1
    val siguienteDelGrupo = if (enSuperserie && posEnGrupo < grupo.lastIndex) {
        day.template.exercises.getOrNull(grupo[posEnGrupo + 1])?.name
    } else null

    // Qué toca hoy y por qué. Un número que sube solo sin explicación se lee como un fallo de
    // la app, así que el motivo va escrito debajo del peso.
    val progression = remember(s.exerciseIndex, s.setNumber, s.completedSets.size) {
        viewModel.progressionFor(s)
    }

    // De dónde sale el número que se está viendo. Antes ponía "Pasos de 0,5 kg · el mismo que
    // la serie anterior": media frase explicaba un botón y la otra media un dato, y juntas no
    // se entendía ninguna de las dos.
    val yaHayoSerieHecha = s.completedSets.any { it.exerciseIndex == s.exerciseIndex }
    val pesoHint = when {
        progression != null -> progression.reason
        yaHayoSerieHecha -> "El mismo peso que la serie anterior."
        parseKg(weight) != null -> "El peso con el que lo dejaste la última vez."
        else -> "Primera vez con este ejercicio: pon el peso que uses hoy."
    }

    // La propuesta se guarda en cuanto se ve, no solo si se toca: el reloj y la notificación
    // cierran la serie leyendo lo persistido, y si la propuesta viviera solo en la pantalla,
    // marcar desde la muñeca apuntaría un peso distinto del que se está mirando.
    LaunchedEffect(s.exerciseIndex, s.setNumber) {
        viewModel.setSetWeight(weight)
        viewModel.setSetReps(reps)
    }

    // El peso baja a disco medio segundo después del último toque. Con pasos de medio kilo y
    // el botón acelerando, guardar en cada toque serían decenas de escrituras por segundo;
    // así una ráfaga entera es una sola. Lo que se ve en pantalla cambia al instante igual.
    LaunchedEffect(weight, weightTouched, s.exerciseIndex, s.setNumber) {
        if (!weightTouched) return@LaunchedEffect
        delay(500)
        viewModel.setSetWeight(weight)
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
            if (timedSecs == null) {
                if (!bodyweight) {
                    WeightBlock(
                        value = weight,
                        onValue = { weight = it; weightTouched = true },
                        dark = dark,
                        reps = if (repsRange != null) reps else null,
                        onReps = { reps = it; repsTouched = true },
                        hint = pesoHint,
                        warmup = esCalentamiento,
                        onWarmup = { esCalentamiento = it }
                    )
                } else if (repsRange != null) {
                    // Una dominada no lleva kilos pero sí repeticiones, y son justo las que
                    // marcan si se progresa.
                    SessionCard(label = "Repeticiones de esta serie", dark = dark) {
                        RepsRow(
                            value = reps,
                            onValue = { reps = it; repsTouched = true },
                            hint = progression?.reason.orEmpty()
                                .ifBlank { "El plan pide ${exercise.scheme}" }
                        )
                    }
                }
            }
        },
        primary = {
            when {
                timedSecs != null -> SessionPrimary(
                    text = "Empezar serie · ${formatSecs(timedSecs)}",
                    onClick = { viewModel.startTimedSet() },
                    dark = dark, tint = SessionTint.WORK
                )
                // Un circuito se cuenta por vueltas; unas dominadas, por series.
                bodyweight && repsRange == null -> SessionPrimary(
                    text = "Vuelta hecha",
                    onClick = { viewModel.completeSet("") },
                    dark = dark, tint = SessionTint.WORK
                )
                bodyweight -> SessionPrimary(
                    text = if (esCalentamiento) "Aproximación hecha" else "Serie hecha",
                    onClick = { viewModel.completeSet("", reps, esCalentamiento) },
                    dark = dark, tint = SessionTint.WORK
                )
                else -> SessionPrimary(
                    text = if (esCalentamiento) "Aproximación hecha" else "Serie hecha",
                    onClick = { viewModel.completeSet(weight, reps, esCalentamiento) },
                    dark = dark, tint = SessionTint.WORK
                )
            }
        }
    ) {
        Text(
            ExerciseImages.headlineName(exercise.name),
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
        if (enSuperserie) {
            Spacer(Modifier.height(Space.x1))
            Text(
                if (siguienteDelGrupo != null)
                    "SUPERSERIE · sin descanso, encadenas con ${ExerciseImages.headlineName(siguienteDelGrupo)}"
                else "SUPERSERIE · al acabar esta, a descansar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (imageRes != null) {
            Spacer(Modifier.height(Space.x4))
            // Un circuito son varios movimientos: en mitad de la serie es justo cuando hace
            // falta ver los tres y en qué orden, no solo la plancha del principio.
            ExerciseVisual(exercise.name, imageHeight = 150.dp)
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
            onDismiss = { guideFromPlan = null },
            onAlias = { viewModel.setExerciseAlias(ex.name, it) }
        )
    }
}

/**
 * Las repeticiones de la serie, con el mismo par de botones que el peso.
 *
 * Vienen puestas de fábrica: el plan dice cuántas tocan y marcar "serie hecha" es decir que se
 * han hecho. Solo hay que tocarlas el día que no salen —ocho en vez de doce—, que es justo el
 * día que interesa apuntar bien para saber si se avanza o se está atascado.
 */
@Composable
private fun RepsRow(value: String, onValue: (String) -> Unit, hint: String) {
    val n = parseReps(value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Repeticiones",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hint.isNotBlank()) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        StepperButton(
            "−",
            { onValue(((n ?: 1) - REPS_STEP).coerceAtLeast(1).toString()) },
            enabled = (n ?: 0) > 1,
            fastRepeat = true,
            size = Touch.min,
        )
        Text(
            text = n?.toString() ?: "—",
            style = MaterialTheme.typography.headlineMedium,
            color = if (n != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 52.dp)
                .padding(horizontal = Space.x2)
        )
        StepperButton(
            "+",
            { onValue(((n ?: 0) + REPS_STEP).coerceAtMost(999).toString()) },
            fastRepeat = true,
            size = Touch.min,
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
    reps: String? = null,
    onReps: (String) -> Unit = {},
    /**
     * De dónde sale el número que se ve: el mismo de la serie anterior, el de la última vez o
     * la propuesta de hoy con su motivo. Es una frase, no una instrucción de uso: cómo van los
     * botones ya lo enseñan los propios botones.
     */
    hint: String,
    warmup: Boolean = false,
    onWarmup: (Boolean) -> Unit = {},
) {
    val styles = LocalAppTextStyles.current
    var showWheel by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    val kg = parseKg(value)

    // Con repeticiones dentro, la tarjeta ya no habla solo de kilos.
    SessionCard(label = if (reps != null) "Esta serie" else "Peso de esta serie", dark = dark) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                "−",
                { onValue(formatKg((kg ?: 0.0) - WEIGHT_STEP)) },
                enabled = (kg ?: 0.0) >= WEIGHT_STEP,
                fastRepeat = true,
            )
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
            StepperButton(
                "+",
                { onValue(formatKg((kg ?: 0.0) + WEIGHT_STEP)) },
                fastRepeat = true,
            )
        }
        Spacer(Modifier.height(Space.x2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showWheel = true }) { Text("Rueda") }
        }
        // Aproximar antes de la serie buena es normal y hasta ahora no había dónde apuntarlo:
        // o se contaba como serie de trabajo (y ensuciaba los kilos movidos y la progresión)
        // o se perdía. Marcada aquí, se guarda pero no cuenta.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = warmup, onCheckedChange = onWarmup)
            Text(
                "Serie de calentamiento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onWarmup(!warmup) }
            )
        }
        if (reps != null) {
            Spacer(Modifier.height(Space.x3))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(Modifier.height(Space.x2))
            RepsRow(value = reps, onValue = onReps, hint = "")
        }
    }

    if (showWheel) {
        val steps = remember { (0..600).map { it * WEIGHT_STEP } }
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
        // Aguantando una plancha, mirar la postura del dibujo es justo lo que hace falta; y
        // sin él la pantalla del cronómetro era una cifra en mitad de la nada.
        SessionIllustration(exercise.name)
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
    // Igual que en el motor: una aproximacion se apunta pero no avanza el plan, asi que la
    // serie que viene sale de contar el trabajo hecho y no de sumarle uno al contador.
    val trabajoHecho = s.completedSets.count { it.exerciseIndex == s.exerciseIndex && !it.warmup }
    val nextSetNumber = if (trabajoHecho < totalSets) trabajoHecho + 1 else 1
    val nextTotalSets = nextExercise?.let { setCountFromScheme(it.scheme) } ?: 0

    val nextIsTimed = nextExercise != null && secondsPerSetFromScheme(nextExercise.scheme) != null
    val nextIsBodyweight = nextExercise != null && nextExercise.withoutWeight
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
            // El esfuerzo se pregunta aqui y no en la serie: justo al acabar es cuando sabes si
            // te has quedado corto, y la pantalla de la serie ya tiene bastante. En palabras y
            // no en numeros porque "RIR 2" no lo dice nadie en un gimnasio.
            val ultima = s.completedSets.lastOrNull()
            if (ultima != null && !ultima.warmup) {
                SessionCard(label = "¿Cómo ha ido?", dark = dark) {
                    // "Fallo" y no "Al limite": con el texto del sistema al 150 % la etiqueta
                    // larga no cabia en su tercio y se quedaba en "Al". Ademas es la palabra
                    // que se usa en el gimnasio.
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                        listOf(
                            "Fácil" to "3",
                            "Justo" to "1",
                            "Fallo" to "0"
                        ).forEach { (etiqueta, valor) ->
                            FilterChip(
                                selected = ultima.rir == valor,
                                onClick = {
                                    viewModel.setLastSetRir(if (ultima.rir == valor) "" else valor)
                                },
                                label = { Text(etiqueta, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.x2))
            }
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
                                    plannedWeight = formatKg((kg ?: 0.0) - WEIGHT_STEP)
                                    viewModel.setPlannedWeight(plannedWeight)
                                },
                                enabled = (kg ?: 0.0) >= WEIGHT_STEP,
                                fastRepeat = true,
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
                                    plannedWeight = formatKg((kg ?: 0.0) + WEIGHT_STEP)
                                    viewModel.setPlannedWeight(plannedWeight)
                                },
                                fastRepeat = true,
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
        // Y en ese mismo hueco cabe el mapa muscular, que aquí sí tiene sentido: en la serie
        // no cambia nada de lo que vas a hacer en los próximos cuarenta segundos, pero
        // descansando hay tiempo muerto y una pantalla con poco trabajo. Se alterna con la
        // ilustración en vez de ponerse al lado: dos dibujos compiten por la misma mirada.
        val nextTargets = nextExercise?.let { MuscleTargets.forName(it.name) }
        var verMusculos by rememberSaveable(nextExercise?.name) { mutableStateOf(false) }
        val mostrarMapa = nextTargets != null && (verMusculos || nextImageRes == null)
        if (nextImageRes != null || nextTargets != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = Space.x2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (mostrarMapa && nextTargets != null) {
                    MuscleMap(
                        primary = nextTargets.primary,
                        secondary = nextTargets.secondary,
                        bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        primaryColor = tintAccent(SessionTint.REST),
                        secondaryColor = LocalAppColors.current.warmup,
                        separatorColor = Color.Transparent,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 230.dp)
                    )
                } else if (nextImageRes != null) {
                    Image(
                        painter = painterResource(nextImageRes),
                        contentDescription = nextExercise?.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            // weight con fill=false: si el hueco no da para la imagen Y el
                            // boton de debajo, encoge la imagen en vez de taparlo.
                            .weight(1f, fill = false)
                            .heightIn(max = 230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                    )
                }
                if (nextTargets != null && nextImageRes != null) {
                    TextButton(onClick = { verMusculos = !verMusculos }) {
                        Text(if (verMusculos) "Ver el ejercicio" else "Ver los músculos")
                    }
                }
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
            onDismiss = { showNextGuide = false },
            onAlias = { viewModel.setExerciseAlias(nextExercise.name, it) }
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
            onDismiss = { guideFromPlan = null },
            onAlias = { viewModel.setExerciseAlias(ex.name, it) }
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
    viewModel: PlanViewModel,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val styles = LocalAppTextStyles.current
    val appColors = LocalAppColors.current
    val sets = s.completedSets.size
    val totalRest = s.completedSets.sumOf { it.restSeconds }
    val minutes = ((now - s.startMillis) / 60000).toInt().coerceAtLeast(0)

    // Lo que ha dado de sí cada ejercicio, serie a serie y en el orden en que se hizo: hoy la
    // primera de bíceps fueron 10 kg x 12, la segunda 12 x 12 y la tercera 11 x 10, y las tres
    // se leen. Entran también las series sin kilos: unas dominadas se cuentan por repeticiones.
    val weights = s.completedSets
        .filter { it.weight.isNotBlank() || it.reps.isNotBlank() }
        .groupBy { it.exerciseIndex }
        .map { (idx, seriesDelEjercicio) ->
            val nombre = day.template.exercises.getOrNull(idx)?.name ?: "Ejercicio"
            nombre to setsSummary(
                seriesDelEjercicio.sortedBy { it.setNumber }.map { SetLog(it.weight, it.reps) }
            )
        }
        .filter { it.second.isNotBlank() }

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
            // Las calorias se calculaban para mandarlas a Google Health y no se ensenaban en
            // ningun sitio, que es justo lo primero que uno mira al terminar. Es una estimacion
            // por formula (peso y minutos), no una medicion: se dice tal cual.
            val kcal = viewModel.estimatedKcal(((now - s.startMillis) / 1000).toInt())
            Spacer(Modifier.height(Space.x3))
            if (kcal != null) {
                SessionCard(label = "Calorías", dark = dark) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "~$kcal",
                            style = styles.displayCard,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            " kcal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        if (viewModel.usesWatch)
                            "Estimadas por tu peso y el tiempo. Las de verdad las mide tu reloj: " +
                                "míralas en Google Health."
                        else "Estimadas por tu peso y el tiempo que ha durado, no medidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SessionCard(label = "Calorías", dark = dark) {
                    Text(
                        "Pon tu peso en Ajustes y las calcularé al acabar cada entreno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (weights.isNotEmpty()) {
                Spacer(Modifier.height(Space.x3))
                // Ya no son solo los pesos: cada ejercicio trae sus series con repeticiones.
                SessionCard(label = "Series de hoy", dark = dark) {
                    // El nombre arriba y el desglose debajo, no en la misma línea: "10 · 12 ·
                    // 11 kg" al lado de "Curl de bíceps con mancuernas" no cabe, y menos con
                    // el texto del sistema al 150 %.
                    weights.forEach { (name, resumen) ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = Space.x1)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                resumen,
                                style = styles.tabular,
                                color = appColors.work
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
