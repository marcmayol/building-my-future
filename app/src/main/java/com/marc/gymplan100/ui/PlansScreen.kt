@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.marc.gymplan100.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R
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
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Convierte el motivo por el que no vale un plan en un mensaje en el idioma de la app. Lo usan
 * esta pantalla y el editor; fuera de Compose (el aviso llega en un callback) va con contexto.
 */
fun PlanProblem.text(context: Context): String = when (this) {
    PlanProblem.EmptyFile -> context.getString(R.string.plan_error_empty_file)
    PlanProblem.InvalidJson -> context.getString(R.string.plan_error_invalid_json)
    PlanProblem.CannotOpen -> context.getString(R.string.plan_error_cannot_open)
    PlanProblem.NotAPlanFile -> context.getString(R.string.plan_error_not_a_plan)
    PlanProblem.NoName -> context.getString(R.string.plan_error_no_name)
    PlanProblem.NoDays -> context.getString(R.string.plan_error_no_days)
    is PlanProblem.PhaseWithoutDays -> context.getString(R.string.plan_error_phase_without_days, phase)
    is PlanProblem.DayWithoutExercises ->
        context.getString(R.string.plan_error_day_without_exercises, day, phase)
    is PlanProblem.TooManyExercises ->
        context.getString(R.string.plan_error_too_many_exercises, day, count, max)
    is PlanProblem.ExerciseWithoutName -> context.getString(R.string.plan_error_exercise_without_name, day)
    is PlanProblem.ExerciseWithoutScheme ->
        context.getString(R.string.plan_error_exercise_without_scheme, exercise, day)
    is PlanProblem.TooManyDays -> context.getString(R.string.plan_error_too_many_days, total, max)
    PlanProblem.ReservedId -> context.getString(R.string.plan_error_reserved_id)
    PlanProblem.MarkdownNoTitle -> context.getString(R.string.plan_error_markdown_no_title)
    PlanProblem.MarkdownNoDays -> context.getString(R.string.plan_error_markdown_no_days)
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
    val context = LocalContext.current

    var showFormat by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<TrainingPlan?>(null) }

    // Se abre el selector con todos los archivos a propósito: muchos gestores no etiquetan
    // los .json como application/json y filtrar por tipo los dejaría en gris, sin explicación.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importPlan(uri) { result ->
            scope.launch {
                when (result) {
                    is PlanImport.Ok -> snackbar.showSnackbar(
                        context.resources.getQuantityString(
                            R.plurals.plan_imported,
                            result.plan.totalDays,
                            result.plan.name,
                            result.plan.totalDays
                        )
                    )
                    is PlanImport.Error -> snackbar.showSnackbar(result.problem.text(context))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_list_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.plan_back))
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
                    stringResource(R.string.plan_list_intro),
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
                    Text(stringResource(R.string.plan_add_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.x3))
                    Button(
                        onClick = {
                            viewModel.startDraft(null)
                            onEditPlan()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) { Text(stringResource(R.string.plan_create)) }
                    Spacer(Modifier.height(Space.x2))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text(stringResource(R.string.plan_import)) }
                        OutlinedButton(
                            onClick = { showFormat = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text(stringResource(R.string.plan_see_format)) }
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
                            snackbar.showSnackbar(context.getString(R.string.plan_now_following, plan.name))
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
                        stringResource(R.string.plan_session_half_done, activeSession!!.dayNumber),
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
            title = { Text(stringResource(R.string.plan_delete_title, plan.name)) },
            text = {
                Text(stringResource(R.string.plan_delete_text))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(plan.id)
                    planToDelete = null
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.plan_deleted)) }
                }) { Text(stringResource(R.string.plan_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) { Text(stringResource(R.string.plan_cancel)) }
            }
        )
    }
}

/** El objetivo del plan en dos palabras, con lo que lo distingue cuando no es obvio. */
@Composable
private fun goalLabel(goal: PlanGoal?): String? = when (goal) {
    PlanGoal.STRENGTH -> stringResource(R.string.plan_goal_strength)
    PlanGoal.MUSCLE -> stringResource(R.string.plan_goal_muscle)
    PlanGoal.LOSE_FAT -> stringResource(R.string.plan_goal_lose_fat)
    PlanGoal.MAINTAIN -> stringResource(R.string.plan_goal_maintain)
    PlanGoal.MOBILITY -> stringResource(R.string.plan_goal_mobility)
    PlanGoal.START -> stringResource(R.string.plan_goal_start)
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
    val context = LocalContext.current
    // Fechas en el idioma con el que se pinta la app, con el patrón que toca en cada uno.
    val locale = AppLanguage.locale(context)
    val shortPattern = stringResource(R.string.plan_date_short_pattern)
    val longPattern = stringResource(R.string.plan_date_long_pattern)

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
    val fechaFin = remember(progress.finishedAt, shortPattern, locale) {
        progress.finishedAt.takeIf { it > 0L }?.let {
            SimpleDateFormat(shortPattern, locale).format(Date(it))
        }
    }
    val vuelta = stringResource(R.string.plan_badge_round, progress.rounds + 1)
    val activo = stringResource(R.string.plan_badge_active)

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
        Text(plan.name, style = MaterialTheme.typography.titleMedium)
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
                        if (fechaFin != null) stringResource(R.string.plan_badge_finished_on, fechaFin)
                        else stringResource(R.string.plan_badge_finished)
                    PlanBadge.VueltaEnCurso -> vuelta
                    PlanBadge.SinEmpezar -> stringResource(R.string.plan_badge_not_started)
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
                plan.description,
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
        val dias = pluralStringResource(R.plurals.plan_days_count, plan.totalDays, plan.totalDays)
        val fases = pluralStringResource(R.plurals.plan_phases_count, plan.phases.size, plan.phases.size)
        val completados = pluralStringResource(R.plurals.plan_completed_count, completedDays, completedDays)
        val completadosVuelta = stringResource(R.string.plan_completed_in_round, completados, progress.rounds + 1)
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
            val fecha = remember(plan.importedAt, longPattern, locale) {
                SimpleDateFormat(longPattern, locale).format(Date(plan.importedAt))
            }
            Text(
                when (plan.source) {
                    PlanSource.EDITOR -> stringResource(R.string.plan_created_on, fecha)
                    PlanSource.MARKDOWN -> stringResource(R.string.plan_imported_on_markdown, fecha)
                    else -> stringResource(R.string.plan_imported_on, fecha)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (plan.builtin) {
            Text(
                stringResource(R.string.plan_builtin),
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
                Button(onClick = onActivate, enabled = canSwitch) { Text(stringResource(R.string.plan_follow_this)) }
            }
            // El resumen del cierre se puede volver a abrir, pero solo del plan que se está
            // siguiendo: abrirlo de otro obligaría a cambiar de plan por detrás, y eso no es
            // lo que pide quien pulsa "ver resumen".
            if (isActive && terminado) {
                OutlinedButton(onClick = onSeeSummary) { Text(stringResource(R.string.plan_see_summary)) }
            }
            if (!plan.builtin) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.plan_edit)) }
                OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.plan_delete)) }
            }
        }
        if (!isActive && !canSwitch) {
            Spacer(Modifier.height(Space.x2))
            Text(
                stringResource(R.string.plan_cannot_switch),
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
        if (markdown) R.string.plan_format_example_markdown else R.string.plan_format_example_json
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_format_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.plan_format_text),
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
            }) { Text(stringResource(R.string.plan_copy_example)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.plan_close)) } }
    )
}
