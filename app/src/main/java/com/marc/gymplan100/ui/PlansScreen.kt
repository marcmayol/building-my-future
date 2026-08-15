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
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.PlanSource
import com.marc.gymplan100.data.TrainingPlan
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ejemplos de los dos formatos, para copiarlos y usarlos de plantilla (a mano o pidiéndoselos
 * a una IA). Markdown va primero porque es el que se escribe sin pelearse con las comas.
 */
private val EJEMPLO_MD = """
# Mi plan de fuerza
Tres días por semana durante ocho semanas.

## Base (4 semanas)
Aprender el movimiento con peso cómodo.
Progresión: sube peso cuando las 10 salgan fáciles.

### Lunes — Empuje
Calentamiento: 6 min bici + movilidad de hombros
- Press de banca — 4 x 8
- Press de hombros — 3 x 10 (sin bloquear el codo)
- Plancha — 3 x 30 s

### Jueves — Tirón
- Jalón al pecho — 4 x 10
- Remo sentado — 4 x 10

## Carga (4 semanas)

### Lunes — Fuerza
- Sentadilla — 5 x 5
- Press de banca — 5 x 5
""".trimIndent()

private val EJEMPLO_JSON = """
{
  "nombre": "Mi plan de fuerza",
  "descripcion": "Tres días por semana, 8 semanas.",
  "fases": [
    {
      "nombre": "Base",
      "semanas": 4,
      "descripcion": "Aprender el movimiento con peso cómodo.",
      "progresion": "Sube peso cuando las 10 repeticiones salgan fáciles.",
      "dias": [
        {
          "dia": "Lunes",
          "titulo": "Empuje",
          "calentamiento": "6 min bici + movilidad de hombros",
          "ejercicios": [
            { "nombre": "Press de banca", "esquema": "4 x 8" },
            { "nombre": "Press de hombros", "esquema": "3 x 10", "nota": "Sin bloquear el codo" },
            { "nombre": "Plancha", "esquema": "3 x 30 s" }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()

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
                        "«${result.plan.name}» importado: ${result.plan.totalDays} días"
                    )
                    is PlanImport.Error -> snackbar.showSnackbar(result.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis planes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                    "Cada plan guarda su propio progreso: cambiar de plan no borra nada.",
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
                    Text("Añadir un plan", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Space.x3))
                    Button(
                        onClick = {
                            viewModel.startDraft(null)
                            onEditPlan()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) { Text("Crear un plan") }
                    Spacer(Modifier.height(Space.x2))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text("Importar") }
                        OutlinedButton(
                            onClick = { showFormat = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Touch.min)
                        ) { Text("Ver formato") }
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
                        scope.launch { snackbar.showSnackbar("Ahora sigues «${plan.name}»") }
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
                        "Tienes el día ${activeSession!!.dayNumber} a medias: termínalo o " +
                            "descártalo antes de cambiar de plan.",
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
            title = { Text("¿Borrar «${plan.name}»?") },
            text = {
                Text(
                    "Se borra el plan y también los días, pesos y entrenos que hayas " +
                        "registrado en él. Esto no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(plan.id)
                    planToDelete = null
                    scope.launch { snackbar.showSnackbar("Plan borrado") }
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) { Text("Cancelar") }
            }
        )
    }
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
    val fechaFin = remember(progress.finishedAt) {
        progress.finishedAt.takeIf { it > 0L }?.let {
            SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES")).format(Date(it))
        }
    }

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
                    PlanBadge.Activo -> "Activo"
                    PlanBadge.Terminado ->
                        if (fechaFin != null) "Terminado el $fechaFin" else "Terminado"
                    PlanBadge.VueltaEnCurso -> "${progress.rounds + 1}ª vuelta en curso"
                    PlanBadge.SinEmpezar -> "Sin empezar"
                }
            )
            if (repitiendo) {
                DoneBadge(
                    kind = PlanBadge.VueltaEnCurso,
                    label = "${progress.rounds + 1}ª vuelta en curso"
                )
            }
            if (isActive && estado != PlanBadge.Activo) {
                DoneBadge(kind = PlanBadge.Activo, label = "Activo")
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
        Text(
            buildString {
                append("${plan.totalDays} días · ")
                append(if (plan.phases.size == 1) "1 fase" else "${plan.phases.size} fases")
                // Sin empezar no se cuentan ceros: un "0 completados" es ruido, no información.
                if (completedDays > 0) {
                    append(if (completedDays == 1) " · 1 completado" else " · $completedDays completados")
                    if (repitiendo) append(" de la ${progress.rounds + 1}ª vuelta")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!plan.builtin && plan.importedAt > 0L) {
            val fecha = remember(plan.importedAt) {
                SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES"))
                    .format(Date(plan.importedAt))
            }
            Text(
                when (plan.source) {
                    PlanSource.EDITOR -> "Creado el $fecha"
                    PlanSource.MARKDOWN -> "Importado el $fecha (Markdown)"
                    else -> "Importado el $fecha"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (plan.builtin) {
            Text(
                "De serie en la app",
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
                Button(onClick = onActivate, enabled = canSwitch) { Text("Seguir este") }
            }
            // El resumen del cierre se puede volver a abrir, pero solo del plan que se está
            // siguiendo: abrirlo de otro obligaría a cambiar de plan por detrás, y eso no es
            // lo que pide quien pulsa "ver resumen".
            if (isActive && terminado) {
                OutlinedButton(onClick = onSeeSummary) { Text("Ver resumen") }
            }
            if (!plan.builtin) {
                OutlinedButton(onClick = onEdit) { Text("Editar") }
                OutlinedButton(onClick = onDelete) { Text("Borrar") }
            }
        }
        if (!isActive && !canSwitch) {
            Spacer(Modifier.height(Space.x2))
            Text(
                "No puedes cambiar con un entreno a medias.",
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
    val ejemplo = if (markdown) EJEMPLO_MD else EJEMPLO_JSON

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formato del plan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Un plan es una lista de fases; cada fase dice cuántas semanas se repiten " +
                        "sus días, y cada día lleva sus ejercicios. Las series van en texto " +
                        "(\"4 x 8\", \"3 x 30 s\", \"3 vueltas\"). Si tu plan no tiene fases, " +
                        "escribe los días sin más y se tratará como una fase única.",
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
            }) { Text("Copiar ejemplo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
