@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.marc.gymplan100.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.texto
import com.marc.gymplan100.data.ExpressPlan
import com.marc.gymplan100.data.RestDefaults
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.FreeStep
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.review.Valoracion
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.setsSummary
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
    // Apuntando lo hecho en un entreno libre: sobre la marcha o al terminar (null = cronómetro).
    // rememberSaveable: si Android mata la app con esta pantalla abierta, se vuelve a ella.
    var apuntando by rememberSaveable { mutableStateOf<ModoApunte?>(null) }
    var tiempoLibre by rememberSaveable { mutableStateOf(0L) }
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current

    // Terminar de entrenar es el unico momento en que tiene sentido preguntar por la app: se
    // acaba de usar para lo que sirve. Quien decide si hoy toca —y si toca alguna vez— es
    // Valoracion; aqui solo se le dice que el entreno ha terminado.
    fun cierraElEntreno() {
        Valoracion.trasEntrenar(context)
        onExit()
    }

    // Reloj que avanza cada medio segundo para refrescar cronómetros.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffectTick { now = it }

    val s = session
    if (s == null || s.dayNumber != dayNumber) {
        StartPrompt(
            day = day,
            other = s,
            estimatedMinutes = remember(dayNumber) { viewModel.estimatedMinutes(dayNumber) },
            expressFor = { minutos -> viewModel.expressPlan(dayNumber, minutos) },
            onStart = { express -> viewModel.startSession(dayNumber, express) },
            onStartSpecial = { viewModel.startSpecialSession(dayNumber) },
            onExit = onExit
        )
        return
    }

    // El botón de atrás del sistema NO tira el entreno: se sale a Inicio con todo guardado y
    // se dice, porque al volver a la ficha del día los pesos de hoy todavía no están (se
    // apuntan al terminar) y parecía que se había borrado todo. El apunte del entreno libre
    // tiene su propio atrás, que gana a este por estar más adentro.
    BackHandler {
        Toast.makeText(
            context,
            context.getString(R.string.ses_back_toast),
            Toast.LENGTH_LONG
        ).show()
        onExit()
    }

    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val pos = order.indexOf(s.exerciseIndex).coerceAtLeast(0)
    val dayContext = stringResource(R.string.ses_day_context, pos + 1, order.size, s.dayNumber)

    when (s.phase) {
        SessionPhase.WARMUP -> WarmupContent(s, day, now, dark, viewModel) { showQuitDialog = true }
        SessionPhase.WORKING -> WorkingContent(s, day, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.TIMED_SET -> TimedSetContent(s, day, now, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.RESTING -> RestingContent(s, day, now, dark, dayContext, viewModel) { showQuitDialog = true }
        SessionPhase.FREE -> when (val modo = apuntando) {
            // Entrenando libre se puede ir apuntando sobre la marcha (borrador en la sesión) y,
            // al finalizar, se repasa lo apuntado o, si no hay nada, se pide entonces.
            // Sin plan pero guiado: calentar, serie, descanso (ver FreeSessionEngine).
            null -> when (s.freeStep) {
                FreeStep.WARMUP -> FreeWarmupContent(s, day, now, dark, viewModel) { showQuitDialog = true }
                FreeStep.REST -> FreeRestContent(
                    s = s, now = now, dark = dark, viewModel = viewModel,
                    onLog = { apuntando = ModoApunte.DURANTE },
                    onExit = { showQuitDialog = true }
                )
                FreeStep.WORK -> FreeContent(
                    s = s, now = now, dark = dark, viewModel = viewModel,
                    onLog = { apuntando = ModoApunte.DURANTE },
                    // El cronómetro se para al pulsar finalizar: lo que se enseña mientras se
                    // apunta es el tiempo entrenado, no el rato que se ha tardado en escribirlo.
                    onFinish = { tiempoLibre = now - s.startMillis; apuntando = ModoApunte.AL_TERMINAR },
                    onExit = { showQuitDialog = true }
                )
            }
            else -> FreeSessionLog(
                day = day,
                elapsedMs = tiempoLibre,
                modo = modo,
                inicial = s.freeLog,
                onSave = { apuntado ->
                    if (modo == ModoApunte.DURANTE) {
                        viewModel.saveFreeLog(apuntado); apuntando = null
                    } else {
                        viewModel.finishFreeSession(apuntado); cierraElEntreno()
                    }
                },
                onBack = { apuntado -> viewModel.saveFreeLog(apuntado); apuntando = null },
                onDraft = { apuntado -> viewModel.saveFreeLog(apuntado) },
                onSkip = { viewModel.finishSession(); cierraElEntreno() },
                pesoCorporal = viewModel.profile.collectAsState().value.bodyweightExercises,
                onPesoCorporal = { nombre, on -> viewModel.setBodyweight(nombre, on) }
            )
        }
        SessionPhase.FINISHED -> FinishedContent(
            s = s, day = day, now = now, dark = dark, viewModel = viewModel,
            onFinish = { viewModel.finishSession(); cierraElEntreno() },
            onExit = { showQuitDialog = true }
        )
    }

    if (showQuitDialog) {
        // Terminar antes de tiempo solo tiene sentido si ya hay algo apuntado. En el cronómetro
        // libre y en el resumen final no se ofrece: ahí ya está el botón de terminar.
        val seriesHechas = s.completedSets.count { !it.warmup }
        val puedeTerminar = seriesHechas > 0 &&
            s.phase != SessionPhase.FREE && s.phase != SessionPhase.FINISHED
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text(stringResource(R.string.ses_quit_title), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    if (puedeTerminar)
                        stringResource(
                            R.string.ses_quit_text_finish,
                            pluralStringResource(R.plurals.ses_n_sets, seriesHechas, seriesHechas),
                            s.dayNumber
                        )
                    else
                        stringResource(R.string.ses_quit_text_resume)
                )
            },
            // Tres salidas no caben en los dos huecos del diálogo: van apiladas, de la que
            // guarda más a la que no guarda nada.
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Space.x1)
                ) {
                    if (puedeTerminar) {
                        Button(
                            onClick = {
                                showQuitDialog = false
                                viewModel.finishSession()
                                cierraElEntreno()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                        ) { Text(stringResource(R.string.ses_finish_and_save)) }
                        OutlinedButton(
                            onClick = { showQuitDialog = false; onExit() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.ses_resume_later)) }
                    } else {
                        Button(
                            onClick = { showQuitDialog = false; onExit() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.ses_resume_later)) }
                    }
                    TextButton(
                        onClick = {
                            showQuitDialog = false
                            viewModel.cancelSession()
                            onExit()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.ses_discard_workout)) }
                }
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
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val deHoy = order.mapNotNull { s.exerciseAt(it) }
    val totalSets = deHoy.sumOf { s.setsFor(it) }

    SessionShell(
        tint = SessionTint.WARMUP,
        dark = dark,
        stateLabel = stringResource(R.string.ses_warmup),
        context = stringResource(R.string.ses_day_n, s.dayNumber),
        onExit = onExit,
        secondaries = {
            SessionSecondary("−1 min", { viewModel.adjustWarmup(-60) })
            SessionSecondary(
                if (s.warmupPaused) stringResource(R.string.ses_keep_going) else stringResource(R.string.ses_pause),
                { if (s.warmupPaused) viewModel.resumeWarmup() else viewModel.pauseWarmup() }
            )
            SessionSecondary("+1 min", { viewModel.adjustWarmup(60) })
        },
        contextCard = {
            SessionCard(label = stringResource(R.string.ses_today), dark = dark) {
                Text(
                    day.template.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Space.x1))
                Text(
                    (if (s.isExpress) stringResource(R.string.ses_express) + " · " else "") +
                        pluralStringResource(R.plurals.ses_n_exercises, deHoy.size, deHoy.size) + " · " +
                        pluralStringResource(R.plurals.ses_n_sets, totalSets, totalSets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        primary = {
            SessionPrimary(
                text = stringResource(R.string.ses_start_exercises),
                onClick = { viewModel.endWarmup() },
                dark = dark,
                tint = SessionTint.WARMUP,
                alert = done
            )
        }
    ) {
        // El calentamiento no tiene dibujo propio (es "6 min de lo que quieras"), así que el
        // hueco lo llena con qué se empieza: da tiempo a ir buscando la máquina.
        deHoy.firstOrNull()?.let {
            SessionIllustration(it.name, label = stringResource(R.string.ses_starting_with, it.name.lowercase()))
        }
        Countdown(
            text = if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = when {
                s.warmupPaused -> stringResource(R.string.ses_paused_dot, day.template.warmup)
                !done -> stringResource(R.string.ses_remaining_dot, day.template.warmup)
                else -> stringResource(R.string.ses_warmup_over)
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
    // El de hoy: si se cambió por otro, manda el que se está haciendo (nombre, dibujo, pesos).
    val exercise = s.exerciseAt(s.exerciseIndex) ?: day.template.exercises[s.exerciseIndex]
    val delPlan = s.planNameAt(s.exerciseIndex) ?: exercise.name
    val totalSets = s.setsFor(exercise)
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
        s.exerciseAt(grupo[posEnGrupo + 1])?.name
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
        progression != null -> progression.reason.texto()
        yaHayoSerieHecha -> stringResource(R.string.ses_hint_same_weight)
        parseKg(weight) != null -> stringResource(R.string.ses_hint_last_weight)
        else -> stringResource(R.string.ses_hint_first_time)
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
    var showSkip by remember { mutableStateOf(false) }
    var guideFromPlan by remember { mutableStateOf<com.marc.gymplan100.data.Exercise?>(null) }

    val context = LocalContext.current
    val female = LocalIsFemale.current
    val imageRes = ExerciseImages.forName(context, exercise.name, female)

    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = stringResource(R.string.ses_set_of, s.setNumber, totalSets),
        context = dayContext,
        onExit = onExit,
        secondaries = {
            // «Saltar» y no «Máquina ocupada»: hay más razones para no hacer un ejercicio
            // ahora (no tienes la máquina, ya has tenido bastante) y todas se eligen ahí.
            SessionSecondary(stringResource(R.string.ses_skip), { showSkip = true })
            SessionSecondary(stringResource(R.string.ses_how_to), { showGuide = true })
            // "Plan" y no "Ejercicios": con el texto del sistema grande la palabra larga no
            // cabía en su tercio y se partía a media palabra ("Ejercicio/s").
            SessionSecondary(stringResource(R.string.ses_plan), { showDayPlan = true })
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
                    SessionCard(label = stringResource(R.string.ses_reps_this_set), dark = dark) {
                        RepsRow(
                            value = reps,
                            onValue = { reps = it; repsTouched = true },
                            hint = progression?.reason?.texto().orEmpty()
                                .ifBlank { stringResource(R.string.ses_plan_asks, exercise.scheme) }
                        )
                    }
                }
            }
        },
        primary = {
            when {
                timedSecs != null -> SessionPrimary(
                    text = stringResource(R.string.ses_start_set, formatSecs(timedSecs)),
                    onClick = { viewModel.startTimedSet() },
                    dark = dark, tint = SessionTint.WORK
                )
                // Un circuito se cuenta por vueltas; unas dominadas, por series.
                bodyweight && repsRange == null -> SessionPrimary(
                    text = stringResource(R.string.ses_lap_done),
                    onClick = { viewModel.completeSet("") },
                    dark = dark, tint = SessionTint.WORK
                )
                bodyweight -> SessionPrimary(
                    text = if (esCalentamiento) stringResource(R.string.ses_warmup_set_done) else stringResource(R.string.ses_set_done),
                    onClick = { viewModel.completeSet("", reps, esCalentamiento) },
                    dark = dark, tint = SessionTint.WORK
                )
                else -> SessionPrimary(
                    text = if (esCalentamiento) stringResource(R.string.ses_warmup_set_done) else stringResource(R.string.ses_set_done),
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
        if (delPlan != exercise.name) {
            Spacer(Modifier.height(Space.x1))
            Text(
                stringResource(R.string.ses_instead_of, ExerciseImages.headlineName(delPlan)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enSuperserie) {
            Spacer(Modifier.height(Space.x1))
            Text(
                if (siguienteDelGrupo != null)
                    stringResource(R.string.ses_superset_chain, ExerciseImages.headlineName(siguienteDelGrupo))
                else stringResource(R.string.ses_superset_last),
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
            SessionCard(label = stringResource(R.string.ses_no_illustration), dark = dark) {
                Text(
                    stringResource(R.string.ses_no_illustration_text),
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
            onDismiss = { showGuide = false },
            // Desde la serie, las alternativas se pueden hacer: con un toque el ejercicio de
            // hoy pasa a ser ese, con su dibujo y sus pesos.
            onSwap = { nombre, recordar ->
                viewModel.swapExercise(s.exerciseIndex, nombre, recordar)
                showGuide = false
            },
            planName = delPlan,
            swapRemembered = viewModel.isSwapRemembered(delPlan)
        )
    }
    if (showSkip) {
        SkipExerciseSheet(
            exerciseName = exercise.name,
            doneSets = s.completedSets.count { it.exerciseIndex == s.exerciseIndex && !it.warmup },
            canPostpone = hasNext,
            onPostpone = { viewModel.skipExercise() },
            onSwap = { showGuide = true },
            onDrop = { viewModel.dropExercise() },
            onDismiss = { showSkip = false }
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
                stringResource(R.string.ses_reps),
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
    /** Sin él no sale la casilla de calentamiento (el libre apunta la serie ya hecha). */
    onWarmup: ((Boolean) -> Unit)? = null,
) {
    val styles = LocalAppTextStyles.current
    var showWheel by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    val kg = parseKg(value)

    // Con repeticiones dentro, la tarjeta ya no habla solo de kilos.
    SessionCard(label = if (reps != null) stringResource(R.string.ses_this_set) else stringResource(R.string.ses_weight_this_set), dark = dark) {
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
            TextButton(onClick = { showWheel = true }) { Text(stringResource(R.string.ses_wheel)) }
        }
        // Aproximar antes de la serie buena es normal y hasta ahora no había dónde apuntarlo:
        // o se contaba como serie de trabajo (y ensuciaba los kilos movidos y la progresión)
        // o se perdía. Marcada aquí, se guarda pero no cuenta.
        if (onWarmup != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = warmup, onCheckedChange = onWarmup)
            Text(
                stringResource(R.string.ses_warmup_set),
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
                    stringResource(R.string.ses_weight_this_set),
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
                ) { Text(stringResource(R.string.ses_done)) }
            }
        }
    }

    if (typing) {
        AlertDialog(
            onDismissRequest = { typing = false },
            title = { Text(stringResource(R.string.ses_type_weight)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { typing = false }) { Text(stringResource(R.string.ses_done)) } }
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
    val exercise = s.exerciseAt(s.exerciseIndex) ?: day.template.exercises[s.exerciseIndex]
    val totalSets = s.setsFor(exercise)
    val elapsed = s.timedElapsed(now)
    val remaining = s.timedTargetSeconds - elapsed
    val done = remaining <= 0
    var showGuide by remember { mutableStateOf(false) }

    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = stringResource(R.string.ses_set_of_timed, s.setNumber, totalSets),
        context = dayContext,
        onExit = onExit,
        secondaries = {
            SessionSecondary("−10 s", { viewModel.adjustTimedSet(-10) })
            SessionSecondary(
                if (s.timedPaused) stringResource(R.string.ses_keep_going) else stringResource(R.string.ses_pause),
                { if (s.timedPaused) viewModel.resumeTimedSet() else viewModel.pauseTimedSet() }
            )
            SessionSecondary("+10 s", { viewModel.adjustTimedSet(10) })
        },
        primary = {
            SessionPrimary(
                text = stringResource(R.string.ses_set_done),
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
                s.timedPaused -> stringResource(R.string.ses_paused)
                !done -> stringResource(R.string.ses_hold)
                else -> stringResource(R.string.ses_time_up)
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
            TextButton(onClick = { showGuide = true }) { Text(stringResource(R.string.ses_how_to)) }
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

    val exercise = s.exerciseAt(s.exerciseIndex) ?: day.template.exercises[s.exerciseIndex]
    val totalSets = s.setsFor(exercise)
    val order = s.order.ifEmpty { day.template.exercises.indices.toList() }
    val nextIndex = if (s.setNumber < totalSets) s.exerciseIndex else {
        val pos = order.indexOf(s.exerciseIndex)
        order.getOrNull(pos + 1)
    }
    val nextExercise = if (nextIndex == s.exerciseIndex) exercise else nextIndex?.let { s.exerciseAt(it) }
    val nextIsNewExercise = nextExercise != null && nextExercise !== exercise
    // Igual que en el motor: una aproximacion se apunta pero no avanza el plan, asi que la
    // serie que viene sale de contar el trabajo hecho y no de sumarle uno al contador.
    val trabajoHecho = s.completedSets.count { it.exerciseIndex == s.exerciseIndex && !it.warmup }
    val nextSetNumber = if (trabajoHecho < totalSets) trabajoHecho + 1 else 1
    val nextTotalSets = nextExercise?.let { s.setsFor(it) } ?: 0

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
        stateLabel = if (s.restBetweenExercises) stringResource(R.string.ses_rest_change_machine) else stringResource(R.string.ses_rest),
        context = dayContext,
        onExit = onExit,
        secondaries = {
            SessionSecondary("−30 s", { viewModel.adjustRest(-30) })
            // "Plan" y no "Ejercicios": con el texto del sistema grande la palabra larga no
            // cabía en su tercio y se partía a media palabra ("Ejercicio/s").
            SessionSecondary(stringResource(R.string.ses_plan), { showDayPlan = true })
            SessionSecondary("+30 s", { viewModel.adjustRest(30) })
        },
        contextCard = {
            // El esfuerzo se pregunta aqui y no en la serie: justo al acabar es cuando sabes si
            // te has quedado corto, y la pantalla de la serie ya tiene bastante. En palabras y
            // no en numeros porque "RIR 2" no lo dice nadie en un gimnasio.
            val ultima = s.completedSets.lastOrNull()
            if (ultima != null && !ultima.warmup) {
                SessionCard(label = stringResource(R.string.ses_how_was_it), dark = dark) {
                    // "Fallo" y no "Al limite": con el texto del sistema al 150 % la etiqueta
                    // larga no cabia en su tercio y se quedaba en "Al". Ademas es la palabra
                    // que se usa en el gimnasio.
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                        listOf(
                            stringResource(R.string.ses_effort_easy) to "3",
                            stringResource(R.string.ses_effort_hard) to "1",
                            stringResource(R.string.ses_effort_failure) to "0"
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
                    label = if (nextIsNewExercise) stringResource(R.string.ses_next_setup) else stringResource(R.string.ses_next),
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
                            stringResource(R.string.ses_set_of_scheme, nextSetNumber, nextTotalSets, nextExercise.scheme),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showNextGuide = true }) {
                            Text(if (nextIsNewExercise) stringResource(R.string.ses_find_machine) else stringResource(R.string.ses_review))
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
                text = stringResource(R.string.ses_start_now),
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
                        Text(if (verMusculos) stringResource(R.string.ses_see_exercise) else stringResource(R.string.ses_see_muscles))
                    }
                }
            }
        }
        // De dónde sale el descanso, en pocas palabras: tres minutos sin explicación parecen
        // un fallo de la cuenta atrás, y noventa segundos tras una serie de cinco, también.
        val motivo = when (s.restSource) {
            RestDefaults.Source.YOURS.name -> " · " + stringResource(R.string.ses_rest_src_yours)
            RestDefaults.Source.PLAN.name -> " · " + stringResource(R.string.ses_rest_src_plan)
            RestDefaults.Source.HEAVY.name -> " · " + stringResource(R.string.ses_rest_src_heavy)
            else -> ""
        } + when (RestDefaults.forEffort(s.completedSets.lastOrNull()?.rir.orEmpty())) {
            // Lo que ha sumado «¿Cómo ha ido?»: si no se dice, el tiempo cambia sin motivo.
            30 -> " · " + stringResource(R.string.ses_rest_plus_hard)
            60 -> " · " + stringResource(R.string.ses_rest_plus_failure)
            else -> ""
        }
        Countdown(
            text = if (remaining >= 0) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = if (remaining >= 0) stringResource(R.string.ses_rest_left_of, formatSecs(s.restTargetSeconds)) + motivo
            else stringResource(R.string.ses_over_whenever),
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
        val delPlanSiguiente = nextIndex?.let { s.planNameAt(it) } ?: nextExercise.name
        ExerciseGuideSheet(
            exerciseName = nextExercise.name,
            scheme = nextExercise.scheme,
            onDismiss = { showNextGuide = false },
            onAlias = { viewModel.setExerciseAlias(nextExercise.name, it) },
            // Buscando la máquina es cuando se ve que está cogida: se puede cambiar ya.
            onSwap = if (nextIndex == null) null else { nombre, recordar ->
                viewModel.swapExercise(nextIndex, nombre, recordar)
                showNextGuide = false
            },
            planName = delPlanSiguiente,
            swapRemembered = viewModel.isSwapRemembered(delPlanSiguiente)
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

/** Lo apuntado en un libre, a la vista y en el orden en que se hizo (los últimos cuatro). */
@Composable
private fun FreeLoggedCard(s: ActiveSession, dark: Boolean) {
    val apuntados = s.freeLog.filter { it.name.isNotBlank() && it.setsOrSingle.isNotEmpty() }
    if (apuntados.isEmpty()) return
    SessionCard(label = stringResource(R.string.ses_logged_label, pluralStringResource(R.plurals.ses_n_exercises, apuntados.size, apuntados.size)), dark = dark) {
        if (apuntados.size > 4) {
            Text(
                pluralStringResource(R.plurals.ses_and_n_earlier, apuntados.size - 4, apuntados.size - 4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        apuntados.takeLast(4).forEach { e ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = Space.x1)) {
                Text(
                    e.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Space.x2))
                Text(
                    setsSummary(e.setsOrSingle, stringResource(R.string.mot_warmup_short)),
                    style = LocalAppTextStyles.current.tabular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Libre: el calentamiento, con su cuenta atrás y un «Ya he calentado» para saltarlo. */
@Composable
private fun FreeWarmupContent(
    s: ActiveSession,
    day: TrainingDay,
    now: Long,
    dark: Boolean,
    viewModel: PlanViewModel,
    onExit: () -> Unit
) {
    val remaining = s.warmupTargetSeconds - s.warmupElapsed(now)
    val done = remaining <= 0
    // En un extra no hay día del plan que mirar: el calentamiento es el de cada uno.
    val tuManera = stringResource(R.string.ses_your_way)
    val queCalentar = if (s.extra) tuManera else day.template.warmup.ifBlank { tuManera }
    SessionShell(
        tint = SessionTint.WARMUP,
        dark = dark,
        stateLabel = stringResource(R.string.ses_warmup),
        // Corto: con el texto grande, uno largo partía el estado («DESCANS/O»).
        context = if (s.extra) stringResource(R.string.ses_extra) else stringResource(R.string.ses_day_n, s.dayNumber),
        onExit = onExit,
        secondaries = {
            SessionSecondary("−1 min", { viewModel.freeAdjustWarmup(-60) })
            SessionSecondary("+1 min", { viewModel.freeAdjustWarmup(60) })
        },
        primary = {
            SessionPrimary(
                text = stringResource(R.string.ses_warmed_up),
                onClick = { viewModel.freeEndWarmup() },
                dark = dark,
                tint = SessionTint.WARMUP,
                alert = done
            )
        }
    ) {
        Text(
            stringResource(R.string.ses_free_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.block))
        Countdown(
            text = if (!done) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = if (!done) stringResource(R.string.ses_warmup_left, queCalentar) else stringResource(R.string.ses_warmup_over),
            captionColor = tintAccent(SessionTint.WARMUP)
        )
    }
}

/** Libre: la serie. Un botón grande y el cronómetro total; lo demás se pregunta descansando. */
@Composable
private fun FreeContent(
    s: ActiveSession,
    now: Long,
    dark: Boolean,
    viewModel: PlanViewModel,
    onLog: () -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val actual = s.freeLog.getOrNull(s.freeCurrent)
    val hechas = actual?.sets?.count { !it.warmup } ?: 0
    SessionShell(
        tint = SessionTint.WORK,
        dark = dark,
        stateLabel = when {
            actual != null -> stringResource(R.string.ses_set_n, hechas + 1)
            s.extra -> stringResource(R.string.ses_extra_workout)
            else -> stringResource(R.string.ses_free_workout)
        },
        context = if (s.extra) stringResource(R.string.ses_not_a_day) else stringResource(R.string.ses_day_n, s.dayNumber),
        onExit = onExit,
        contextCard = { FreeLoggedCard(s, dark) },
        secondaries = {
            if (actual != null) SessionSecondary(stringResource(R.string.ses_finish_exercise), { viewModel.freeFinishExercise() })
            SessionSecondary(stringResource(R.string.ses_log_or_edit), onLog)
            SessionSecondary(stringResource(R.string.ses_finish), onFinish)
        },
        primary = {
            SessionPrimary(
                text = stringResource(R.string.ses_set_finished),
                onClick = { viewModel.freeSetDone() },
                dark = dark,
                tint = SessionTint.WORK
            )
        }
    ) {
        if (actual != null && actual.name.isNotBlank()) {
            SessionIllustration(actual.name)
            Text(
                ExerciseImages.headlineName(actual.name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            actual.sets.lastOrNull()?.let { ultima ->
                Text(
                    stringResource(R.string.ses_previous, setsSummary(listOf(ultima), stringResource(R.string.mot_warmup_short))),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tintAccent(SessionTint.WORK)
                )
            }
            Spacer(Modifier.height(Space.block))
        }
        Countdown(
            text = formatClock(now - s.startMillis),
            caption = stringResource(R.string.ses_training),
            captionColor = tintAccent(SessionTint.WORK)
        )
        if (actual == null) {
            Spacer(Modifier.height(Space.x3))
            Text(
                if (s.freeLog.none { it.name.isNotBlank() })
                    stringResource(R.string.ses_free_first_set)
                else stringResource(R.string.ses_free_next_exercise),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Libre: el descanso. La cuenta atrás va sola y, mientras, se apunta la serie: qué ejercicio
 * era (solo en la primera), cómo ha ido y sus kilos y repeticiones, que ya vienen escritos con
 * los de la anterior.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FreeRestContent(
    s: ActiveSession,
    now: Long,
    dark: Boolean,
    viewModel: PlanViewModel,
    onLog: () -> Unit,
    onExit: () -> Unit
) {
    val elapsed = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0)
    val remaining = s.restTargetSeconds - elapsed
    val done = remaining <= 0
    val actual = s.freeLog.getOrNull(s.freeCurrent)
    val ultima = actual?.sets?.lastOrNull()
    val sinNombre = actual != null && actual.name.isBlank()
    var buscando by remember { mutableStateOf(false) }
    // Peso corporal: o lo dice el plan (unas dominadas) o lo has marcado tú.
    val progress by viewModel.progress.collectAsState()
    val pesoCorporalDelPlan = remember(actual?.name) {
        val nombre = actual?.name.orEmpty()
        nombre.isNotBlank() && PlanData.days.asSequence()
            .flatMap { it.template.exercises }
            .firstOrNull { it.name == nombre }?.withoutWeight == true
    }
    val perfil by viewModel.profile.collectAsState()
    val conTuPeso = pesoCorporalDelPlan || actual?.name?.trim() in perfil.bodyweightExercises

    // Kilos y repeticiones de la serie recién hecha: se ven al instante y bajan a disco medio
    // segundo después del último toque, como en el guiado. Al ponerle nombre al ejercicio se
    // rellenan con los de la última vez, y por eso dependen también del nombre.
    var weight by remember(s.freeCurrent, actual?.sets?.size, actual?.name) {
        mutableStateOf(ultima?.weight.orEmpty())
    }
    var reps by remember(s.freeCurrent, actual?.sets?.size, actual?.name) {
        mutableStateOf(ultima?.reps.orEmpty())
    }
    LaunchedEffect(weight, reps) {
        if (ultima == null || (weight == ultima.weight && reps == ultima.reps)) return@LaunchedEffect
        delay(500)
        viewModel.freeEditLastSet(weight, reps)
    }

    val porEsfuerzo = when (RestDefaults.forEffort(ultima?.rir.orEmpty())) {
        30 -> " · " + stringResource(R.string.ses_rest_plus_hard)
        60 -> " · " + stringResource(R.string.ses_rest_plus_failure)
        else -> ""
    }
    SessionShell(
        tint = SessionTint.REST,
        dark = dark,
        stateLabel = if (s.restBetweenExercises) stringResource(R.string.ses_rest_change_exercise) else stringResource(R.string.ses_rest),
        // Corto: con el texto grande, uno largo partía el estado («DESCANS/O»).
        context = if (s.extra) stringResource(R.string.ses_extra) else stringResource(R.string.ses_day_n, s.dayNumber),
        onExit = onExit,
        secondaries = {
            SessionSecondary("−30 s", { viewModel.freeAdjustRest(-30) })
            if (actual != null && !s.restBetweenExercises) {
                SessionSecondary(stringResource(R.string.ses_finish_exercise), { viewModel.freeFinishExercise() })
            } else {
                SessionSecondary(stringResource(R.string.ses_log_or_edit), onLog)
            }
            SessionSecondary("+30 s", { viewModel.freeAdjustRest(30) })
        },
        contextCard = {
            when {
                sinNombre -> SessionCard(label = stringResource(R.string.ses_which_exercise), dark = dark) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x1)
                    ) {
                        // Cuatro y no más: con el texto grande cada uno ocupa una línea entera.
                        viewModel.freeSuggestions(s).take(4).forEach { nombre ->
                            // Una píldora propia y no un FilterChip: el chip tiene alto fijo y
                            // con el texto grande cortaba el nombre a media palabra.
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .heightIn(min = Touch.min)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.freeNameCurrent(nombre) }
                                    .padding(horizontal = Space.x3, vertical = Space.x2)
                            ) {
                                Text(
                                    nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(onClick = { buscando = true }) { Text(stringResource(R.string.ses_search_other), maxLines = 1) }
                }
                actual != null && !s.restBetweenExercises -> {
                    SessionCard(label = stringResource(R.string.ses_how_was_it), dark = dark) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                            listOf(stringResource(R.string.ses_effort_easy) to "3", stringResource(R.string.ses_effort_hard) to "1", stringResource(R.string.ses_effort_failure) to "0").forEach { (etiqueta, valor) ->
                                FilterChip(
                                    selected = ultima?.rir == valor,
                                    onClick = { viewModel.freeSetLastRir(if (ultima?.rir == valor) "" else valor) },
                                    label = { Text(etiqueta, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.x2))
                    val hint = when {
                        (actual.sets.size) > 1 -> stringResource(R.string.ses_hint_prev_set)
                        weight.isNotBlank() || reps.isNotBlank() -> stringResource(R.string.ses_hint_last_time)
                        else -> if (conTuPeso) stringResource(R.string.ses_hint_log_reps) else stringResource(R.string.ses_hint_log_weight)
                    }
                    if (conTuPeso) {
                        // Con tu peso no hay kilos que apuntar: solo las repeticiones.
                        SessionCard(label = stringResource(R.string.ses_this_set_bodyweight), dark = dark) {
                            RepsRow(value = reps, onValue = { reps = it }, hint = hint)
                        }
                    } else {
                        WeightBlock(
                            value = weight,
                            onValue = { weight = it },
                            dark = dark,
                            reps = reps,
                            onReps = { reps = it },
                            hint = hint
                        )
                    }
                    // Los del plan ya saben si llevan kilos; el resto lo decides tú, y se recuerda.
                    if (!pesoCorporalDelPlan) {
                        Spacer(Modifier.height(Space.x1))
                        FilterChip(
                            selected = conTuPeso,
                            onClick = {
                                if (!conTuPeso) weight = ""
                                viewModel.setBodyweight(actual.name, !conTuPeso)
                            },
                            label = { Text(stringResource(R.string.ses_bodyweight_chip), maxLines = 2) }
                        )
                    }
                }
                else -> FreeLoggedCard(s, dark)
            }
        },
        primary = {
            SessionPrimary(
                text = if (s.restBetweenExercises) stringResource(R.string.ses_next_exercise) else stringResource(R.string.ses_next_set),
                onClick = { viewModel.freeEndRest() },
                dark = dark,
                tint = SessionTint.REST,
                alert = done
            )
        }
    ) {
        if (actual != null && !sinNombre && !s.restBetweenExercises) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    actual.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // Por si se eligió mal: se vuelve a preguntar.
                TextButton(onClick = { buscando = true }) { Text(stringResource(R.string.ses_change), maxLines = 1) }
            }
            Spacer(Modifier.height(Space.x2))
        }
        Countdown(
            text = if (remaining >= 0) formatSecs(remaining) else "+${formatSecs(-remaining)}",
            caption = if (remaining >= 0) stringResource(R.string.ses_rest_left_of, formatSecs(s.restTargetSeconds)) + porEsfuerzo
            else stringResource(R.string.ses_over_whenever),
            captionColor = tintAccent(SessionTint.REST)
        )
        Spacer(Modifier.height(Space.x4))
        SessionProgress(
            fraction = if (s.restTargetSeconds <= 0) 1f else elapsed.toFloat() / s.restTargetSeconds,
            tint = SessionTint.REST
        )
    }

    if (buscando) {
        ModalBottomSheet(
            onDismissRequest = { buscando = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(Space.x2)
            ) {
                Text(stringResource(R.string.ses_which_exercise), style = MaterialTheme.typography.headlineSmall)
                SwapSearch(currentName = actual?.name.orEmpty()) { nombre ->
                    viewModel.freeNameCurrent(nombre)
                    buscando = false
                }
            }
        }
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
            val nombre = s.exerciseAt(idx)?.name ?: stringResource(R.string.ses_exercise)
            nombre to setsSummary(
                seriesDelEjercicio.sortedBy { it.setNumber }.map { SetLog(it.weight, it.reps) },
                stringResource(R.string.mot_warmup_short)
            )
        }
        .filter { it.second.isNotBlank() }

    SessionShell(
        tint = SessionTint.NEUTRAL,
        dark = dark,
        stateLabel = stringResource(R.string.ses_day_completed, s.dayNumber),
        context = "",
        onExit = onExit,
        contextCard = {
            Text(
                stringResource(R.string.ses_saved_google_health),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Space.x3)
            )
        },
        primary = {
            SessionPrimary(
                text = stringResource(R.string.ses_save_day),
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
                ResultTile("$minutes", "min", stringResource(R.string.ses_tile_duration), Modifier.weight(1f), dark)
                ResultTile("$sets", "", stringResource(R.string.ses_tile_sets), Modifier.weight(1f), dark)
            }
            Spacer(Modifier.height(Space.x3))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                ResultTile(formatSecs(totalRest), "", stringResource(R.string.ses_tile_total_rest), Modifier.weight(1f), dark)
                ResultTile(
                    formatSecs(if (sets > 0) totalRest / sets else 0), "",
                    stringResource(R.string.ses_tile_avg_per_set), Modifier.weight(1f), dark
                )
            }
            // Las calorias se calculaban para mandarlas a Google Health y no se ensenaban en
            // ningun sitio, que es justo lo primero que uno mira al terminar. Es una estimacion
            // por formula (peso y minutos), no una medicion: se dice tal cual.
            val kcal = viewModel.estimatedKcal(((now - s.startMillis) / 1000).toInt())
            Spacer(Modifier.height(Space.x3))
            if (kcal != null) {
                SessionCard(label = stringResource(R.string.ses_calories), dark = dark) {
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
                            stringResource(R.string.ses_kcal_watch)
                        else stringResource(R.string.ses_kcal_estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SessionCard(label = stringResource(R.string.ses_calories), dark = dark) {
                    Text(
                        stringResource(R.string.ses_kcal_no_weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (weights.isNotEmpty()) {
                Spacer(Modifier.height(Space.x3))
                // Ya no son solo los pesos: cada ejercicio trae sus series con repeticiones.
                SessionCard(label = stringResource(R.string.ses_todays_sets), dark = dark) {
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

/**
 * Antes de empezar: qué toca, cuánto dura y, si no da tiempo, la versión exprés.
 *
 * [other] es la sesión de OTRO día que haya a medias: empezar esta la descarta, y se avisa
 * antes. [expressFor] recorta el día a los minutos que se le pasen (null = entero ya cabe).
 */
@Composable
private fun StartPrompt(
    day: TrainingDay,
    other: ActiveSession?,
    estimatedMinutes: Int?,
    expressFor: (Int) -> ExpressPlan?,
    onStart: (ExpressPlan?) -> Unit,
    onStartSpecial: () -> Unit,
    onExit: () -> Unit
) {
    var showSpecialConfirm by remember { mutableStateOf(false) }
    // Minutos elegidos para el exprés; null = el día entero.
    var minutos by rememberSaveable(day.number) { mutableStateOf<Int?>(null) }
    // Solo se ofrecen los tiempos que de verdad recortan algo.
    val opciones = listOf(45, 30).filter { estimatedMinutes != null && it < estimatedMinutes }
    val express = remember(minutos, day.number) { minutos?.let(expressFor) }
    val warmupWord = stringResource(R.string.ses_warmup_word)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Space.screen),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.ses_day_n, day.number),
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
            pluralStringResource(R.plurals.ses_n_exercises, day.template.exercises.size, day.template.exercises.size) +
                (estimatedMinutes?.let { " · " + stringResource(R.string.ses_about_min, it) } ?: "") +
                // Hay planes que ya escriben «6 min de calentamiento»: no se repite la palabra.
                // El texto del plan es contenido (en castellano): se mira también la palabra
                // castellana aunque la app esté en inglés.
                " · " + day.template.warmup.let {
                    if (it.contains("calentamiento", ignoreCase = true) ||
                        it.contains(warmupWord, ignoreCase = true)
                    ) it else stringResource(R.string.ses_warmup_colon, it)
                },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (opciones.isNotEmpty()) {
            Spacer(Modifier.height(Space.block))
            Text(
                stringResource(R.string.ses_short_on_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                FilterChip(
                    selected = minutos == null,
                    onClick = { minutos = null },
                    label = { Text(stringResource(R.string.ses_whole), maxLines = 1) }
                )
                opciones.forEach { m ->
                    FilterChip(
                        selected = minutos == m,
                        onClick = { minutos = m },
                        label = { Text("$m min", maxLines = 1) }
                    )
                }
            }
            val m = minutos
            if (m != null) {
                Spacer(Modifier.height(Space.x2))
                Text(
                    describeExpress(day, express, m),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (other != null) {
            Spacer(Modifier.height(Space.x3))
            Text(
                stringResource(R.string.ses_other_day_pending, other.dayNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(Space.block))
        Button(
            onClick = { onStart(express) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) { Text(if (express != null) stringResource(R.string.ses_start_express) else stringResource(R.string.ses_start_workout)) }
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
            Text(stringResource(R.string.ses_special_workout))
        }
        Spacer(Modifier.height(Space.x2))
        TextButton(onClick = onExit) { Text(stringResource(R.string.ses_go_back)) }
    }

    if (showSpecialConfirm) {
        AlertDialog(
            onDismissRequest = { showSpecialConfirm = false },
            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
            title = { Text(stringResource(R.string.ses_special_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.ses_special_confirm_text, day.number)
                )
            },
            confirmButton = {
                Button(onClick = { showSpecialConfirm = false; onStartSpecial() }) {
                    Text(stringResource(R.string.ses_special_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpecialConfirm = false }) { Text(stringResource(R.string.ses_cancel)) }
            }
        )
    }
}

/**
 * El exprés en una frase: qué se hace, qué se queda fuera y cuánto dura. Si se queda fuera
 * algo, se nombra: «te quito dos ejercicios» sin decir cuáles obliga a ir a mirarlo.
 */
@Composable
private fun describeExpress(day: TrainingDay, plan: ExpressPlan?, minutos: Int): String {
    if (plan == null) return stringResource(R.string.ses_express_fits, minutos)
    val total = day.template.exercises.size
    val hechos = plan.order.size
    val cuantos = if (hechos == total) stringResource(R.string.ses_express_all)
    else pluralStringResource(R.plurals.ses_express_some, total, hechos, total)
    val fuera = if (plan.dropped.isEmpty()) "" else
        " " + stringResource(
            R.string.ses_express_left_out,
            plan.dropped.joinToString(", ") { ExerciseImages.headlineName(it) }
        )
    return stringResource(
        R.string.ses_express_summary,
        cuantos,
        pluralStringResource(R.plurals.ses_n_sets, plan.maxSets, plan.maxSets),
        plan.warmupSeconds / 60,
        (plan.estimatedSeconds + 59) / 60
    ) + fuera
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
