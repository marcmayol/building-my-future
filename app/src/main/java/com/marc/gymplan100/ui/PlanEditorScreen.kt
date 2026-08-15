@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.DiaDto
import com.marc.gymplan100.data.EjercicioDto
import com.marc.gymplan100.data.FaseDto
import com.marc.gymplan100.data.PlanDto
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.launch

// --- Utilidades de lista: editar sin mutar, que es como viajan los planes ---

private fun <T> List<T>.replacedAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

private fun <T> List<T>.removedAt(index: Int): List<T> =
    toMutableList().also { it.removeAt(index) }

/** Mueve un elemento [delta] posiciones; si se sale de la lista, la deja igual. */
private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (target !in indices) return this
    return toMutableList().also { it.add(target, it.removeAt(index)) }
}

// --- Validación por nodo -----------------------------------------------------

/**
 * Los problemas del plan, colocados en la fila que los tiene.
 *
 * La validación de [com.marc.gymplan100.data.PlanCodec] devuelve un único mensaje, que sirve
 * para rechazar un archivo pero no para editar: aquí hace falta saber QUÉ fila falla para
 * pintarlo donde el usuario lo va a mirar. Las reglas son las mismas.
 */
private data class EditorErrors(
    val byExercise: Map<Triple<Int, Int, Int>, String> = emptyMap(),
    val byDay: Map<Pair<Int, Int>, String> = emptyMap(),
    val byPhase: Map<Int, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = byExercise.isEmpty() && byDay.isEmpty() && byPhase.isEmpty()
}

private fun problemsOf(dto: PlanDto): EditorErrors {
    val exercises = mutableMapOf<Triple<Int, Int, Int>, String>()
    val days = mutableMapOf<Pair<Int, Int>, String>()
    val phases = mutableMapOf<Int, String>()

    dto.fases.forEachIndexed { p, fase ->
        val phaseLabel = fase.nombre.trim().ifEmpty { "Fase ${p + 1}" }
        if (fase.dias.isEmpty()) {
            phases[p] = "«$phaseLabel» no tiene ningún día."
            return@forEachIndexed
        }
        fase.dias.forEachIndexed { d, dia ->
            val dayLabel = dia.dia.trim().ifEmpty { dia.titulo.trim().ifEmpty { "Día ${d + 1}" } }
            if (dia.ejercicios.isEmpty()) {
                days[p to d] = "«$dayLabel» no tiene ejercicios."
                phases.putIfAbsent(p, "«$dayLabel» no tiene ejercicios.")
                return@forEachIndexed
            }
            dia.ejercicios.forEachIndexed { e, ej ->
                val name = ej.nombre.trim()
                val problem = when {
                    name.isEmpty() -> "Este ejercicio no tiene nombre."
                    ej.esquema.trim().isEmpty() ->
                        "Dime las repeticiones: 4 x 8, por ejemplo."
                    else -> null
                }
                if (problem != null) {
                    exercises[Triple(p, d, e)] = problem
                    val short = if (name.isEmpty()) "Hay un ejercicio sin nombre en «$dayLabel»"
                    else "«$name» ($dayLabel) no dice las series"
                    days.putIfAbsent(p to d, problem)
                    phases.putIfAbsent(p, short)
                }
            }
        }
    }
    return EditorErrors(exercises, days, phases)
}

// --- Navegación interna ------------------------------------------------------

private sealed interface EditorLevel {
    data object Plan : EditorLevel
    data class Phase(val phase: Int) : EditorLevel
    data class Day(val phase: Int, val day: Int) : EditorLevel
}

/**
 * Editor de planes, en cuatro niveles.
 *
 * El formulario anidado anterior metía plan, fases, días y ejercicios en un solo scroll
 * larguísimo. Ahora cada nivel tiene su pantalla, su scroll corto y su acción; los errores se
 * pintan en la fila que falla y suben resumidos al nivel de arriba, así que "Guardar" nunca es
 * la primera vez que te enteras de que algo falta.
 */
@Composable
fun PlanEditorScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit
) {
    val draft by viewModel.draft.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf<EditorLevel>(EditorLevel.Plan) }
    var editingExercise by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    LaunchedEffect(draft == null) { if (draft == null) onBack() }
    val plan = draft ?: return
    val errors = remember(plan) { problemsOf(plan) }

    // Si se borra la fase o el día que estabas editando, se vuelve a un nivel que exista.
    val current = level
    if (current is EditorLevel.Phase && current.phase !in plan.fases.indices) {
        level = EditorLevel.Plan
        return
    }
    if (current is EditorLevel.Day) {
        val fase = plan.fases.getOrNull(current.phase)
        if (fase == null || current.day !in fase.dias.indices) {
            level = EditorLevel.Plan
            return
        }
    }

    Scaffold(
        topBar = {
            when (val l = level) {
                EditorLevel.Plan -> EditorTopBar(
                    overline = null,
                    title = if (plan.id == null) "Nuevo plan" else "Editar plan",
                    onBack = { viewModel.discardDraft() }
                )
                is EditorLevel.Phase -> EditorTopBar(
                    overline = "FASE ${l.phase + 1} DE ${plan.fases.size}",
                    title = plan.fases[l.phase].nombre.ifBlank { "Fase ${l.phase + 1}" },
                    onBack = { level = EditorLevel.Plan }
                )
                is EditorLevel.Day -> {
                    val fase = plan.fases[l.phase]
                    val dia = fase.dias[l.day]
                    EditorTopBar(
                        // Sin repetir "FASE" cuando la fase ya se llama "Fase 1".
                        overline = "DÍA ${l.day + 1} · ${fase.nombre.ifBlank { "FASE ${l.phase + 1}" }}"
                            .uppercase(),
                        title = dia.titulo.ifBlank { dia.dia.ifBlank { "Día ${l.day + 1}" } },
                        onBack = { level = EditorLevel.Phase(l.phase) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        val padding = PaddingValues(
            start = Space.screen,
            end = Space.screen,
            top = inner.calculateTopPadding() + Space.x2,
            bottom = 32.dp
        )
        when (val l = level) {
            EditorLevel.Plan -> PlanLevel(
                plan = plan,
                errors = errors,
                padding = padding,
                onEdit = viewModel::editDraft,
                onOpenPhase = { level = EditorLevel.Phase(it) },
                onSave = {
                    viewModel.saveDraft { result ->
                        if (result is PlanImport.Error) {
                            scope.launch { snackbar.showSnackbar(result.message) }
                        }
                    }
                }
            )
            is EditorLevel.Phase -> PhaseLevel(
                plan = plan,
                phaseIndex = l.phase,
                errors = errors,
                padding = padding,
                onEdit = viewModel::editDraft,
                onOpenDay = { level = EditorLevel.Day(l.phase, it) },
                onDeletePhase = {
                    viewModel.editDraft { it.copy(fases = it.fases.removedAt(l.phase)) }
                    level = EditorLevel.Plan
                },
                onDone = { level = EditorLevel.Plan }
            )
            is EditorLevel.Day -> DayLevel(
                plan = plan,
                phaseIndex = l.phase,
                dayIndex = l.day,
                errors = errors,
                padding = padding,
                onEdit = viewModel::editDraft,
                onOpenExercise = { editingExercise = Triple(l.phase, l.day, it) },
                onDone = { level = EditorLevel.Phase(l.phase) }
            )
        }
    }

    editingExercise?.let { (p, d, e) ->
        val ejercicio = plan.fases.getOrNull(p)?.dias?.getOrNull(d)?.ejercicios?.getOrNull(e)
        if (ejercicio == null) {
            editingExercise = null
        } else {
            ExerciseSheet(
                ejercicio = ejercicio,
                onChange = { nuevo ->
                    viewModel.editDraft { dto ->
                        val fase = dto.fases[p]
                        val dia = fase.dias[d]
                        dto.copy(
                            fases = dto.fases.replacedAt(
                                p,
                                fase.copy(
                                    dias = fase.dias.replacedAt(
                                        d, dia.copy(ejercicios = dia.ejercicios.replacedAt(e, nuevo))
                                    )
                                )
                            )
                        )
                    }
                },
                onRemove = {
                    viewModel.editDraft { dto ->
                        val fase = dto.fases[p]
                        val dia = fase.dias[d]
                        dto.copy(
                            fases = dto.fases.replacedAt(
                                p,
                                fase.copy(
                                    dias = fase.dias.replacedAt(
                                        d, dia.copy(ejercicios = dia.ejercicios.removedAt(e))
                                    )
                                )
                            )
                        )
                    }
                    editingExercise = null
                },
                onDismiss = { editingExercise = null }
            )
        }
    }
}

// --- Nivel 1: el plan --------------------------------------------------------

@Composable
private fun PlanLevel(
    plan: PlanDto,
    errors: EditorErrors,
    padding: PaddingValues,
    onEdit: ((PlanDto) -> PlanDto) -> Unit,
    onOpenPhase: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val styles = LocalAppTextStyles.current
    val totalDays = plan.fases.sumOf { it.dias.size * it.semanas.coerceAtLeast(1) }

    LazyColumn(
        // Con el teclado abierto la lista se encoge en vez de quedar tapada: si no, el campo
        // que estás escribiendo se queda debajo de las teclas.
        modifier = Modifier.fillMaxWidth().imePadding(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            // El recuento vive arriba y se recalcula solo: nada asume 100 días ni 4 fases.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = Space.x4, vertical = Space.x3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Con lo escrito salen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (totalDays == 1) "1 día" else "$totalDays días",
                    style = styles.tabular.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        item {
            EditorField(
                label = "NOMBRE DEL PLAN",
                value = plan.nombre,
                placeholder = "Mi vuelta al gimnasio",
                onValue = { v -> onEdit { it.copy(nombre = v) } }
            )
        }
        item {
            EditorField(
                label = "DESCRIPCIÓN · OPCIONAL",
                value = plan.descripcion,
                placeholder = "Para qué es este plan",
                singleLine = false,
                onValue = { v -> onEdit { it.copy(descripcion = v) } }
            )
        }

        item {
            LabelCaps("FASES · ${plan.fases.size}", top = Space.x3)
        }

        itemsIndexedCompat(plan.fases) { index, fase ->
            NavRow(
                leading = "${index + 1}",
                title = fase.nombre.ifBlank { "Fase ${index + 1}" },
                subtitle = "${fase.semanas} ${if (fase.semanas == 1) "semana" else "semanas"} · " +
                    "${fase.dias.size} ${if (fase.dias.size == 1) "día" else "días"}",
                error = errors.byPhase[index],
                canUp = index > 0,
                canDown = index < plan.fases.lastIndex,
                onMove = { delta -> onEdit { it.copy(fases = it.fases.moved(index, delta)) } },
                onClick = { onOpenPhase(index) }
            )
        }

        item {
            DashedAddButton("+ Añadir una fase") {
                onEdit {
                    it.copy(
                        fases = it.fases + FaseDto(
                            nombre = "Fase ${it.fases.size + 1}",
                            semanas = 4,
                            dias = listOf(DiaDto(ejercicios = listOf(EjercicioDto())))
                        )
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.x2))
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primary)
            ) { Text("Guardar el plan") }
            Spacer(Modifier.height(Space.x2))
            Text(
                if (errors.isEmpty) "Todo listo: se guarda y ya puedes seguirlo."
                else "Arregla lo que está marcado en rojo y vuelve a guardar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Nivel 2: una fase -------------------------------------------------------

@Composable
private fun PhaseLevel(
    plan: PlanDto,
    phaseIndex: Int,
    errors: EditorErrors,
    padding: PaddingValues,
    onEdit: ((PlanDto) -> PlanDto) -> Unit,
    onOpenDay: (Int) -> Unit,
    onDeletePhase: () -> Unit,
    onDone: () -> Unit,
) {
    val fase = plan.fases[phaseIndex]
    val styles = LocalAppTextStyles.current

    fun editPhase(transform: (FaseDto) -> FaseDto) =
        onEdit { it.copy(fases = it.fases.replacedAt(phaseIndex, transform(it.fases[phaseIndex]))) }

    LazyColumn(
        // Con el teclado abierto la lista se encoge en vez de quedar tapada: si no, el campo
        // que estás escribiendo se queda debajo de las teclas.
        modifier = Modifier.fillMaxWidth().imePadding(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            EditorField(
                label = "NOMBRE",
                value = fase.nombre,
                placeholder = "Base, Carga, Descarga…",
                onValue = { v -> editPhase { it.copy(nombre = v) } }
            )
        }

        item {
            LabelCaps("SEMANAS")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(Space.x2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(
                    "−",
                    { editPhase { it.copy(semanas = (it.semanas - 1).coerceAtLeast(1)) } },
                    enabled = fase.semanas > 1,
                    size = 52.dp
                )
                Text(
                    "${fase.semanas}",
                    style = styles.displayCard,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                StepperButton(
                    "+",
                    { editPhase { it.copy(semanas = (it.semanas + 1).coerceAtMost(52)) } },
                    enabled = fase.semanas < 52,
                    size = 52.dp
                )
            }
            Spacer(Modifier.height(Space.x1))
            Text(
                "Los días de esta fase se repiten ${fase.semanas} " +
                    if (fase.semanas == 1) "semana." else "semanas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            EditorField(
                label = "PROGRESIÓN · CÓMO SUBIR",
                value = fase.progresion,
                placeholder = "Sube 2,5 kg cuando acabes las 4 series sin fallar.",
                singleLine = false,
                onValue = { v -> editPhase { it.copy(progresion = v) } }
            )
        }

        item { LabelCaps("DÍAS DE LA FASE · ${fase.dias.size}", top = Space.x3) }

        itemsIndexedCompat(fase.dias) { index, dia ->
            NavRow(
                leading = "${index + 1}",
                title = dia.titulo.ifBlank { dia.dia.ifBlank { "Día ${index + 1}" } },
                subtitle = buildString {
                    append("${dia.ejercicios.size} ")
                    append(if (dia.ejercicios.size == 1) "ejercicio" else "ejercicios")
                    if (dia.calentamiento.isNotBlank()) append(" · ${dia.calentamiento}")
                },
                error = errors.byDay[phaseIndex to index],
                canUp = index > 0,
                canDown = index < fase.dias.lastIndex,
                onMove = { delta -> editPhase { it.copy(dias = it.dias.moved(index, delta)) } },
                onClick = { onOpenDay(index) }
            )
        }

        item {
            DashedAddButton("+ Añadir un día") {
                editPhase { it.copy(dias = it.dias + DiaDto(ejercicios = listOf(EjercicioDto()))) }
            }
        }

        item {
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                OutlinedButton(
                    onClick = onDeletePhase,
                    enabled = plan.fases.size > 1,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Touch.primary)
                ) { Text("Borrar fase") }
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Touch.primary)
                ) { Text("Hecho") }
            }
            if (plan.fases.size == 1) {
                Spacer(Modifier.height(Space.x2))
                Text(
                    "No se puede borrar: un plan necesita al menos una fase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Nivel 3: un día ---------------------------------------------------------

@Composable
private fun DayLevel(
    plan: PlanDto,
    phaseIndex: Int,
    dayIndex: Int,
    errors: EditorErrors,
    padding: PaddingValues,
    onEdit: ((PlanDto) -> PlanDto) -> Unit,
    onOpenExercise: (Int) -> Unit,
    onDone: () -> Unit,
) {
    val fase = plan.fases[phaseIndex]
    val dia = fase.dias[dayIndex]
    val app = LocalAppColors.current

    fun editDay(transform: (DiaDto) -> DiaDto) = onEdit {
        it.copy(
            fases = it.fases.replacedAt(
                phaseIndex,
                fase.copy(dias = fase.dias.replacedAt(dayIndex, transform(fase.dias[dayIndex])))
            )
        )
    }

    LazyColumn(
        // Con el teclado abierto la lista se encoge en vez de quedar tapada: si no, el campo
        // que estás escribiendo se queda debajo de las teclas.
        modifier = Modifier.fillMaxWidth().imePadding(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            EditorField(
                label = "DÍA DE LA SEMANA",
                value = dia.dia,
                placeholder = "Lunes",
                onValue = { v -> editDay { it.copy(dia = v) } }
            )
        }
        item {
            EditorField(
                label = "TÍTULO DEL ENTRENO",
                value = dia.titulo,
                placeholder = "Empuje · pecho y hombro",
                onValue = { v -> editDay { it.copy(titulo = v) } }
            )
        }
        item {
            EditorField(
                label = "CALENTAMIENTO",
                value = dia.calentamiento,
                placeholder = "5 min de bici suave",
                onValue = { v -> editDay { it.copy(calentamiento = v) } }
            )
        }

        item { LabelCaps("EJERCICIOS · ${dia.ejercicios.size}", top = Space.x3) }

        itemsIndexedCompat(dia.ejercicios) { index, ejercicio ->
            val problem = errors.byExercise[Triple(phaseIndex, dayIndex, index)]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (problem != null) app.rest.copy(alpha = 0.10f) else Color.Transparent
                    )
                    .clickable { onOpenExercise(index) }
                    .padding(vertical = Space.x3, horizontal = Space.x2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Asa de reordenar: mover con los cursores es más fiable con una mano que
                    // arrastrar, y no se pelea con el scroll.
                    Text(
                        "⠿",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(Space.x3))
                    Text(
                        ejercicio.nombre.ifBlank { "Ejercicio sin nombre" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (ejercicio.nombre.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SchemeChip(ejercicio.esquema, isError = problem != null)
                    IconButton(
                        onClick = {
                            editDay { it.copy(ejercicios = it.ejercicios.moved(index, -1)) }
                        },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir") }
                    IconButton(
                        onClick = {
                            editDay { it.copy(ejercicios = it.ejercicios.moved(index, 1)) }
                        },
                        enabled = index < dia.ejercicios.lastIndex,
                        modifier = Modifier.size(32.dp)
                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar") }
                }
                if (problem != null) {
                    Spacer(Modifier.height(Space.x1))
                    Text(
                        problem,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        item {
            DashedAddButton("+ Añadir ejercicio") {
                editDay { it.copy(ejercicios = it.ejercicios + EjercicioDto()) }
            }
        }

        item {
            Spacer(Modifier.height(Space.x2))
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primary)
            ) { Text("Hecho") }
        }
    }
}

// --- Nivel 4: la hoja del ejercicio -----------------------------------------

/**
 * Series y repeticiones con botones, y el esquema se compone solo ("4 x 8", "3 x 30 s").
 *
 * Si el esquema no encaja en ese patrón (un circuito "3 vueltas", por ejemplo) se edita como
 * texto libre: el formato admite cualquier cosa y no vamos a destruir lo que ya funciona.
 */
@Composable
private fun ExerciseSheet(
    ejercicio: EjercicioDto,
    onChange: (EjercicioDto) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val parsed = remember(ejercicio.esquema) { parseScheme(ejercicio.esquema) }
    var freeForm by remember(ejercicio.esquema) { mutableStateOf(parsed == null) }
    val inCatalog = remember(ejercicio.nombre) {
        com.marc.gymplan100.data.ExerciseGuides.forName(ejercicio.nombre) != null
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.block)
        ) {
            EditorField(
                label = "EJERCICIO",
                value = ejercicio.nombre,
                placeholder = "Press de banca",
                onValue = { onChange(ejercicio.copy(nombre = it)) }
            )
            if (ejercicio.nombre.isNotBlank()) {
                Spacer(Modifier.height(Space.x2))
                Text(
                    if (inCatalog) "Está en el catálogo: usará su ilustración y su ficha."
                    else "No está en el catálogo: no tendrá ilustración ni ficha, solo el vídeo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Space.x4))
            if (freeForm) {
                EditorField(
                    label = "SERIES",
                    value = ejercicio.esquema,
                    placeholder = "3 vueltas",
                    onValue = { onChange(ejercicio.copy(esquema = it)) }
                )
                Spacer(Modifier.height(Space.x2))
                Text(
                    "Escríbelo como quieras: \"4 x 8\", \"3 x 30 s\", \"3 vueltas\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scheme = parsed!!
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x4)) {
                    NumberStepper(
                        label = "SERIES",
                        value = scheme.sets,
                        onValue = { onChange(ejercicio.copy(esquema = scheme.copy(sets = it).text())) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberStepper(
                        label = if (scheme.seconds) "SEGUNDOS" else "REPS",
                        value = scheme.reps,
                        onValue = { onChange(ejercicio.copy(esquema = scheme.copy(reps = it).text())) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Space.x3))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    FilterChip(
                        selected = !scheme.seconds,
                        onClick = {
                            onChange(ejercicio.copy(esquema = scheme.copy(seconds = false).text()))
                        },
                        label = { Text("Reps") }
                    )
                    FilterChip(
                        selected = scheme.seconds,
                        onClick = {
                            onChange(ejercicio.copy(esquema = scheme.copy(seconds = true).text()))
                        },
                        label = { Text("Segundos") }
                    )
                }
            }
            Spacer(Modifier.height(Space.x2))
            Text(
                if (freeForm) "Usar series y repeticiones" else "Escribir las series a mano",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable {
                        if (freeForm && parseScheme(ejercicio.esquema) == null) {
                            onChange(ejercicio.copy(esquema = "3 x 12"))
                        }
                        freeForm = !freeForm
                    }
                    .padding(vertical = Space.x2)
            )

            Spacer(Modifier.height(Space.x4))
            EditorField(
                label = "NOTA · OPCIONAL",
                value = ejercicio.nota,
                placeholder = "Controla la bajada",
                onValue = { onChange(ejercicio.copy(nota = it)) }
            )

            Spacer(Modifier.height(Space.block))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Touch.primary)
                ) { Text("Quitar") }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Touch.primary)
                ) { Text("Hecho") }
            }
        }
    }
}

private data class ParsedScheme(val sets: Int, val reps: Int, val seconds: Boolean) {
    fun text(): String = if (seconds) "$sets x $reps s" else "$sets x $reps"
}

/** "4 x 8" / "3 x 30 s" -> partes editables. Null si el esquema es libre. */
private fun parseScheme(raw: String): ParsedScheme? {
    val m = Regex("""^\s*(\d+)\s*[xX×]\s*(\d+)\s*(s|seg|segundos)?\s*\.?\s*$""").find(raw)
        ?: return null
    return ParsedScheme(
        sets = m.groupValues[1].toInt().coerceIn(1, 20),
        reps = m.groupValues[2].toInt().coerceIn(1, 600),
        seconds = m.groupValues[3].isNotEmpty()
    )
}

// --- Piezas comunes ----------------------------------------------------------

@Composable
private fun EditorTopBar(overline: String?, title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                if (overline != null) {
                    Text(
                        overline,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun LabelCaps(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top, bottom = Space.x2)
    )
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    Column {
        LabelCaps(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            placeholder = if (placeholder.isBlank()) null else {
                { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            singleLine = singleLine,
            shape = MaterialTheme.shapes.extraSmall,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Fila que lleva a otro nivel, con su error debajo si lo tiene. */
@Composable
private fun NavRow(
    leading: String,
    title: String,
    subtitle: String,
    error: String?,
    canUp: Boolean,
    canDown: Boolean,
    onMove: (Int) -> Unit,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = Space.x3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                leading,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Space.x3)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (error != null) {
                    Spacer(Modifier.height(Space.x1))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = { onMove(-1) }, enabled = canUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir")
            }
            IconButton(onClick = { onMove(1) }, enabled = canDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar")
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SchemeChip(scheme: String, isError: Boolean) {
    val app = LocalAppColors.current
    val text = scheme.ifBlank { "? " }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (isError) Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape)
                else Modifier
            )
            .padding(horizontal = Space.x3, vertical = Space.x1)
    ) {
        Text(
            text,
            style = LocalAppTextStyles.current.tabular,
            color = if (isError) MaterialTheme.colorScheme.error else app.warmup
        )
    }
}

@Composable
private fun DashedAddButton(text: String, onClick: () -> Unit) {
    // Borde continuo pero tenue: Compose no tiene borde discontinuo de serie y no compensa
    // dibujarlo a mano solo para esto.
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.primary)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Space.x2))
        Text(text.removePrefix("+ "))
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles = LocalAppTextStyles.current
    Column(modifier = modifier) {
        LabelCaps(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("−", { onValue((value - 1).coerceAtLeast(1)) }, enabled = value > 1, size = 44.dp)
            Text(
                "$value",
                style = styles.tabular.copy(
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            StepperButton("+", { onValue(value + 1) }, size = 44.dp)
        }
    }
}

/** `itemsIndexed` con la firma que usa este archivo, para no repetir el import largo. */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<T>,
    crossinline itemContent: @Composable (Int, T) -> Unit
) {
    items(items.size) { index -> itemContent(index, items[index]) }
}
