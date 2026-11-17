@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.marc.gymplan100.ui

import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import com.marc.gymplan100.plataforma.formatearFecha
import com.marc.gymplan100.plataforma.plataforma
import com.marc.gymplan100.plataforma.platformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.marc.gymplan100.data.Contenido
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.shared.*
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.PlanProblem
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.PlanSource
import com.marc.gymplan100.data.TrainingPlan
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.launch

/**
 * Convierte el motivo por el que no vale un plan en un mensaje en el idioma de la app. Lo usan
 * esta pantalla y el editor; fuera de Compose (el aviso llega en un callback), en una corrutina.
 */
suspend fun PlanProblem.text(): String = when (this) {
    PlanProblem.EmptyFile -> getString(Res.string.plan_error_empty_file)
    PlanProblem.InvalidJson -> getString(Res.string.plan_error_invalid_json)
    PlanProblem.CannotOpen -> getString(Res.string.plan_error_cannot_open)
    PlanProblem.NotAPlanFile -> getString(Res.string.plan_error_not_a_plan)
    PlanProblem.NoName -> getString(Res.string.plan_error_no_name)
    PlanProblem.NoDays -> getString(Res.string.plan_error_no_days)
    is PlanProblem.PhaseWithoutDays -> getString(Res.string.plan_error_phase_without_days, phase)
    is PlanProblem.DayWithoutExercises ->
        getString(Res.string.plan_error_day_without_exercises, day, phase)
    is PlanProblem.TooManyExercises ->
        getString(Res.string.plan_error_too_many_exercises, day, count, max)
    is PlanProblem.ExerciseWithoutName -> getString(Res.string.plan_error_exercise_without_name, day)
    is PlanProblem.ExerciseWithoutScheme ->
        getString(Res.string.plan_error_exercise_without_scheme, exercise, day)
    is PlanProblem.TooManyDays -> getString(Res.string.plan_error_too_many_days, total, max)
    PlanProblem.ReservedId -> getString(Res.string.plan_error_reserved_id)
    PlanProblem.MarkdownNoTitle -> getString(Res.string.plan_error_markdown_no_title)
    PlanProblem.MarkdownNoDays -> getString(Res.string.plan_error_markdown_no_days)
    is PlanProblem.Text -> message
}

@Composable
fun PlansScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onEditPlan: () -> Unit = {}
) {
    val plans by viewModel.plans.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val planProgress by viewModel.planProgress.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = platformContext()

    // Nombres del plan de ejemplo del editor, en el idioma de la app.
    val nombreFaseEjemplo = stringResource(Res.string.mot_draft_phase)
    val nombreDiaEjemplo = stringResource(Res.string.mot_draft_day)

    var showFormat by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<TrainingPlan?>(null) }

    // Se abre el selector con todos los archivos a propósito: muchos gestores no etiquetan
    // los .json como application/json y filtrar por tipo los dejaría en gris, sin explicación.
    val picker = plataforma.rememberAbrirTexto(listOf("*/*")) { texto ->
        viewModel.importPlan(texto) { result ->
            scope.launch {
                when (result) {
                    is PlanImport.Ok -> snackbar.showSnackbar(
                        getPluralString(
                            Res.plurals.plan_imported,
                            result.plan.totalDays,
                            Contenido.t(result.plan.name),
                            result.plan.totalDays
                        )
                    )
                    is PlanImport.Error -> snackbar.showSnackbar(result.problem.text())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.plan_list_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.plan_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Text(
                    stringResource(Res.string.plan_list_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Space.x4)
                ) {
                    Text(stringResource(Res.string.plan_add_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.x3))
                    Button(
                        onClick = {
                            viewModel.startDraft(null, nombreFaseEjemplo, nombreDiaEjemplo)
                            onEditPlan()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) { Text(stringResource(Res.string.plan_create)) }
                    Spacer(Modifier.height(Space.x2))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                        OutlinedButton(
                            onClick = { picker() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text(stringResource(Res.string.plan_import)) }
                        OutlinedButton(
                            onClick = { showFormat = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text(stringResource(Res.string.plan_see_format)) }
                    }
                }
            }

            items(plans, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    isActive = plan.id == activePlan.id,
                    progress = planProgress[plan.id] ?: ProgressState(),
                    canSwitch = activeSession == null,
                    onSeeSummary = { viewModel.showPlanSummaryAgain(); onBack() },
                    onActivate = {
                        viewModel.activatePlan(plan.id)
                        scope.launch {
                            snackbar.showSnackbar(getString(Res.string.plan_now_following, Contenido.t(plan.name)))
                        }
                    },
                    onEdit = {
                        viewModel.startDraft(plan.id)
                        onEditPlan()
                    },
                    onDelete = { planToDelete = plan }
                )
            }

            if (activeSession != null) {
                item {
                    Text(
                        activeSession!!.let {
                            // Una rutina especial o un extra llevan el número del día que toca,
                            // pero no son ese día (el libre sí cuenta como su día).
                            if (it.isRoutine || it.extra) {
                                stringResource(Res.string.plan_workout_half_done)
                            } else {
                                stringResource(Res.string.plan_session_half_done, it.dayNumber)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(Space.x4)
                    )
                }
            }
        }
    }

    if (showFormat) {
        FormatDialog(onDismiss = { showFormat = false })
    }

    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text(stringResource(Res.string.plan_delete_title, Contenido.t(plan.name))) },
            text = {
                Text(stringResource(Res.string.plan_delete_text))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(plan.id)
                    planToDelete = null
                    scope.launch { snackbar.showSnackbar(getString(Res.string.plan_deleted)) }
                }) { Text(stringResource(Res.string.plan_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) { Text(stringResource(Res.string.plan_cancel)) }
            }
        )
    }
}

/** El objetivo del plan en dos palabras, con lo que lo distingue cuando no es obvio. */
@Composable
private fun goalLabel(goal: PlanGoal?): String? = when (goal) {
    PlanGoal.STRENGTH -> stringResource(Res.string.plan_goal_strength)
    PlanGoal.MUSCLE -> stringResource(Res.string.plan_goal_muscle)
    PlanGoal.LOSE_FAT -> stringResource(Res.string.plan_goal_lose_fat)
    PlanGoal.MAINTAIN -> stringResource(Res.string.plan_goal_maintain)
    PlanGoal.MOBILITY -> stringResource(Res.string.plan_goal_mobility)
    PlanGoal.START -> stringResource(Res.string.plan_goal_start)
    null -> null
}

@Composable
private fun PlanCard(
    plan: TrainingPlan,
    isActive: Boolean,
    progress: ProgressState,
    canSwitch: Boolean,
    onSeeSummary: () -> Unit,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val app = LocalAppColors.current
    val context = platformContext()
    // Fechas en el idioma con el que se pinta la app, con el patrón que toca en cada uno.
    val shortPattern = stringResource(Res.string.plan_date_short_pattern)
    val longPattern = stringResource(Res.string.plan_date_long_pattern)

    val completedDays = progress.completedDays.size
    // Estado del plan: es lo primero que se lee, y va en el raíl y en el distintivo.
    // Terminado y sin empezar mandan sobre "activo": lo que importa es en qué punto está.
    val terminado = progress.everFinished ||
        (plan.totalDays > 0 && completedDays >= plan.totalDays)
    // Se terminó y ya se ha vuelto a empezar: la vuelta en curso se dice aparte, sin borrar
    // el "terminado el…" (haber acabado el plan una vez no se deshace por repetirlo).
    val enMarcha = completedDays in 1 until plan.totalDays
    val repitiendo = terminado && progress.rounds > 0 && enMarcha
    val estado = when {
        terminado -> PlanBadge.Terminado
        completedDays == 0 -> PlanBadge.SinEmpezar
        else -> PlanBadge.Activo
    }
    val fechaFin = remember(progress.finishedAt, shortPattern) {
        progress.finishedAt.takeIf { it > 0L }?.let {
            formatearFecha(it, shortPattern)
        }
    }
    val vuelta = stringResource(Res.string.plan_badge_round, progress.rounds + 1)
    val activo = stringResource(Res.string.plan_badge_active)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Altura intrínseca: sin esto el raíl no sabe cuánto mide la tarjeta y no se pinta.
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                // El plan activo se distingue por el borde, no solo por el chip.
                if (isActive) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.medium
                ) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(planRailColor(estado))
        )
    Column(modifier = Modifier.padding(Space.x4)) {
        Text(Contenido.t(plan.name), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.x2))
        // Los distintivos en FlowRow: un plan terminado que se está repitiendo lleva dos, y
        // con el texto del sistema grande el segundo baja de línea en vez de recortarse.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
            verticalArrangement = Arrangement.spacedBy(Space.x1)
        ) {
            DoneBadge(
                kind = estado,
                label = when (estado) {
                    PlanBadge.Activo -> activo
                    PlanBadge.Terminado ->
                        if (fechaFin != null) stringResource(Res.string.plan_badge_finished_on, fechaFin)
                        else stringResource(Res.string.plan_badge_finished)
                    PlanBadge.VueltaEnCurso -> vuelta
                    PlanBadge.SinEmpezar -> stringResource(Res.string.plan_badge_not_started)
                }
            )
            if (repitiendo) {
                DoneBadge(
                    kind = PlanBadge.VueltaEnCurso,
                    label = vuelta
                )
            }
            if (isActive && estado != PlanBadge.Activo) {
                DoneBadge(kind = PlanBadge.Activo, label = activo)
            }
        }
        if (plan.description.isNotBlank()) {
            Spacer(Modifier.height(Space.x1))
            // Recortada a propósito: los planes de serie traen su descripción entera y, sin
            // tope, cada tarjeta ocupaba una pantalla y la lista no había quien la bajara.
            Text(
                Contenido.t(plan.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(Space.x2))
        // El objetivo delante: «fuerza» y «músculo» se entrenan distinto (pocas
        // repeticiones y descansos largos frente a 8-12), y es lo primero que se elige.
        val goal = goalLabel(plan.goal)
        val dias = pluralStringResource(Res.plurals.plan_days_count, plan.totalDays, plan.totalDays)
        val fases = pluralStringResource(Res.plurals.plan_phases_count, plan.phases.size, plan.phases.size)
        val completados = pluralStringResource(Res.plurals.plan_completed_count, completedDays, completedDays)
        val completadosVuelta = stringResource(Res.string.plan_completed_in_round, completados, progress.rounds + 1)
        Text(
            buildString {
                goal?.let { append("$it · ") }
                append("$dias · $fases")
                // Sin empezar no se cuentan ceros: un "0 completados" es ruido, no información.
                if (completedDays > 0) {
                    append(" · ")
                    append(if (repitiendo) completadosVuelta else completados)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!plan.builtin && plan.importedAt > 0L) {
            val fecha = remember(plan.importedAt, longPattern) {
                formatearFecha(plan.importedAt, longPattern)
            }
            Text(
                when (plan.source) {
                    PlanSource.EDITOR -> stringResource(Res.string.plan_created_on, fecha)
                    PlanSource.MARKDOWN -> stringResource(Res.string.plan_imported_on_markdown, fecha)
                    else -> stringResource(Res.string.plan_imported_on, fecha)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (plan.builtin) {
            Text(
                stringResource(Res.string.plan_builtin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // La barra solo aparece si hay algo que enseñar: vacía no dice nada, y al 100 % repite
        // lo que ya pone el distintivo de terminado.
        if (enMarcha) {
            Spacer(Modifier.height(Space.x3))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (completedDays.toFloat() / plan.totalDays).coerceIn(0f, 1f)
                        )
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(app.brandGradient)
                )
            }
        }

        Spacer(Modifier.height(Space.x3))
        // FlowRow para que con el texto del sistema grande los botones bajen de línea en vez
        // de comprimirse hasta ser ilegibles.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            if (!isActive) {
                Button(onClick = onActivate, enabled = canSwitch) { Text(stringResource(Res.string.plan_follow_this)) }
            }
            // El resumen del cierre se puede volver a abrir, pero solo del plan que se está
            // siguiendo: abrirlo de otro obligaría a cambiar de plan por detrás, y eso no es
            // lo que pide quien pulsa "ver resumen".
            if (isActive && terminado) {
                OutlinedButton(onClick = onSeeSummary) { Text(stringResource(Res.string.plan_see_summary)) }
            }
            if (!plan.builtin) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(Res.string.plan_edit)) }
                OutlinedButton(onClick = onDelete) { Text(stringResource(Res.string.plan_delete)) }
            }
        }
        if (!isActive && !canSwitch) {
            Spacer(Modifier.height(Space.x2))
            Text(
                stringResource(Res.string.plan_cannot_switch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

/** Explica los dos formatos y deja copiar el ejemplo listo para editar. */
@Composable
private fun FormatDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var markdown by remember { mutableStateOf(true) }
    // Ejemplos de los dos formatos, para copiarlos y usarlos de plantilla (a mano o
    // pidiéndoselos a una IA). Markdown va primero porque es el que se escribe sin pelearse
    // con las comas. En inglés el ejemplo usa las claves en inglés, que el lector acepta igual.
    val ejemplo = stringResource(
        if (markdown) Res.string.plan_format_example_markdown else Res.string.plan_format_example_json
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.plan_format_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(Res.string.plan_format_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(Space.x3))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    FilterChip(
                        selected = markdown,
                        onClick = { markdown = true },
                        label = { Text("Markdown") }
                    )
                    FilterChip(
                        selected = !markdown,
                        onClick = { markdown = false },
                        label = { Text("JSON") }
                    )
                }
                Spacer(Modifier.height(Space.x3))
                Text(
                    ejemplo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(ejemplo))
                onDismiss()
            }) { Text(stringResource(Res.string.plan_copy_example)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.plan_close)) } }
    )
}
