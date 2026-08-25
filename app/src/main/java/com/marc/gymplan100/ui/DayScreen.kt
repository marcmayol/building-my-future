@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.setCountFromScheme
import com.marc.gymplan100.data.contar
import com.marc.gymplan100.data.palabra
import com.marc.gymplan100.data.repsFromScheme
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * El día del plan: lo que toca hoy, para consultarlo o para lanzar el entrenamiento.
 *
 * Sigue los patrones del rediseño: versalitas para las secciones, una tarjeta ligera por
 * ejercicio (la ilustración manda, el resto acompaña) y el enlace de la ficha como texto en
 * vez de un botón a todo lo ancho, que con seis ejercicios llenaba la pantalla de botones.
 */
@Composable
fun DayScreen(
    dayNumber: Int,
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onStartSession: (Int) -> Unit,
    onStartFreeSession: (Int) -> Unit
) {
    val day = PlanData.dayByNumber(dayNumber) ?: PlanData.days.first()
    val template = day.template
    val progress by viewModel.progress.collectAsState()
    val completed = day.number in progress.completedDays
    // Ejercicio cuya ficha se está mostrando, o null.
    var guideFor by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Día ${day.number}", style = MaterialTheme.typography.headlineSmall) },
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
    ) { inner ->
        LazyColumn(
            // Con el teclado abierto la lista se encoge en vez de quedar tapada: si no, el
            // campo que estás escribiendo se queda debajo de las teclas.
            modifier = Modifier.fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Text(
                    "FASE ${day.phase.number} · ${day.phase.name.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x1))
                Text(template.title, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(Space.x1))
                Text(
                    contar(template.exercises.size, "ejercicio", "ejercicios"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = { onStartSession(day.number) },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Space.x2))
                    Text("Empezar entrenamiento")
                }
            }

            item {
                // Alternativa al guiado: cronómetro libre (sin series ni pesos) que SÍ cuenta
                // como el día del plan. Para cuando entrenas a tu aire pero quieres el registro.
                OutlinedButton(
                    onClick = { onStartFreeSession(day.number) },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) { Text("Entrenar libre · cuenta como día") }
            }

            item { SectionBlock("CALENTAMIENTO", template.warmup) }

            itemsIndexed(template.exercises) { index, exercise ->
                val log = progress.logs["${day.number}-$index"]
                val totalSets = setCountFromScheme(exercise.scheme)
                ExerciseCard(
                    exercise = exercise,
                    totalSets = totalSets,
                    // Lo apuntado serie a serie. Si el día viene de antes del desglose, el
                    // peso que hubiera se enseña como el de la primera serie.
                    sets = log?.sets?.ifEmpty { null }
                        ?: listOf(SetLog(weight = log?.weight.orEmpty(), reps = log?.reps.orEmpty())),
                    // Lo último conocido de esa máquina, de pista en los campos vacíos.
                    hint = viewModel.exerciseWeight(exercise.name),
                    // De pista en los campos vacios: lo que pide el plan para hoy.
                    reps = repsFromScheme(exercise.scheme),
                    done = log?.done ?: false,
                    onSetWeight = { serie, kilos ->
                        viewModel.setDaySetWeight(day.number, index, serie, totalSets, kilos)
                    },
                    onSetReps = { serie, r ->
                        viewModel.setDaySetReps(day.number, index, serie, totalSets, r)
                    },
                    onDone = { viewModel.setLog(day.number, index, done = it) },
                    onShowGuide = { guideFor = exercise }
                )
            }

            item { SectionBlock("VUELTA A LA CALMA", template.cooldown) }

            item {
                Spacer(Modifier.height(Space.x1))
                if (completed) {
                    OutlinedButton(
                        onClick = { viewModel.toggleDay(day.number) },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(Space.x2))
                        Text("Día hecho · deshacer")
                    }
                } else {
                    Button(
                        onClick = { viewModel.toggleDay(day.number) },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(Space.x2))
                        // Etiqueta corta a propósito: con el texto del sistema al 130 % la
                        // frase larga se partía en dos líneas y el ✓ quedaba descolgado.
                        Text("Marcar día hecho")
                    }
                }
            }
        }

        guideFor?.let { ex ->
            ExerciseGuideSheet(
                exerciseName = ex.name,
                scheme = ex.scheme,
                onDismiss = { guideFor = null },
                kind = ex.kind
            )
        }
    }
}

/** Calentamiento y vuelta a la calma: versalitas y texto, sin tarjeta de color. */
@Composable
private fun SectionBlock(label: String, text: String) {
    if (text.isBlank()) return
    Column(modifier = Modifier.padding(top = Space.x2)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x1))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Cuánto crece un campo con el tamaño de texto del sistema. A ancho fijo, con el texto al
 * 150 % un "100" se sale del campo y solo se leen dos cifras.
 */
@Composable
private fun fontScale(): Float = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    totalSets: Int,
    sets: List<SetLog>,
    hint: String,
    reps: String,
    done: Boolean,
    onSetWeight: (Int, String) -> Unit,
    onSetReps: (Int, String) -> Unit,
    onDone: (Boolean) -> Unit,
    onShowGuide: () -> Unit
) {
    val app = LocalAppColors.current
    val styles = LocalAppTextStyles.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        if (ExerciseImages.hasVisual(exercise.name)) {
            ExerciseVisual(exercise.name, imageHeight = 140.dp)
            Spacer(Modifier.height(Space.x3))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(exercise.scheme, style = styles.tabular, color = app.warmup)
                if (exercise.note.isNotEmpty()) {
                    Text(
                        exercise.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Checkbox(checked = done, onCheckedChange = onDone)
        }
        // Sin kilos en lo que no los lleva (dominadas, planchas, cardio) y sin repeticiones en
        // el cardio: el campo vac\u00edo solo invitaba a preguntarse qu\u00e9 poner. Un bloque de cardio
        // no tiene ninguno de los dos, as\u00ed que la fila entera desaparece.
        val pidePeso = !exercise.withoutWeight
        val pideReps = exercise.kind != ExerciseKind.CARDIO
        if (pidePeso || pideReps) {
            Spacer(Modifier.height(Space.x2))
            // Una L\u00cdNEA por serie, con lo suyo junto. Antes los kilos iban serie a serie y las
            // repeticiones eran un \u00fanico campo suelto al final: adem\u00e1s de dejar cajas hu\u00e9rfanas
            // cuando las series no llenaban la fila, contaba que cada serie llev\u00f3 un peso distinto
            // y todas las mismas repeticiones, que es justo lo que no pasa.
            val etiquetaReps = if (exercise.kind == ExerciseKind.TIME) "seg" else "reps"
            repeat(maxOf(totalSets, sets.size)) { i ->
                if (i > 0) Spacer(Modifier.height(Space.x2))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.x2)
                ) {
                    if (pidePeso) {
                        OutlinedTextField(
                            value = sets.getOrNull(i)?.weight.orEmpty(),
                            onValueChange = { onSetWeight(i, it) },
                            label = { Text("${i + 1}\u00aa") },
                            // La pista es el \u00faltimo peso conocido de esa m\u00e1quina: orienta sin
                            // dar por hecho que hoy vas a mover lo mismo.
                            placeholder = if (hint.isBlank()) null else {
                                {
                                    Text(
                                        hint,
                                        style = LocalAppTextStyles.current.tabular,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            // La unidad va de icono final y no de `suffix`: el sufijo de M3 solo
                            // aparece al enfocar el campo, y aqu\u00ed tiene que verse tambi\u00e9n vac\u00edo.
                            trailingIcon = {
                                Text(
                                    "kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraSmall,
                            textStyle = LocalAppTextStyles.current.tabular,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pidePeso && pideReps) {
                        Text(
                            "\u00d7",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pideReps) {
                        OutlinedTextField(
                            value = sets.getOrNull(i)?.reps.orEmpty(),
                            onValueChange = { onSetReps(i, it) },
                            // Sin kilos delante, el n\u00famero de serie tiene que ir aqu\u00ed.
                            label = {
                                Text(
                                    if (pidePeso) etiquetaReps
                                    else "${i + 1}\u00aa \u00b7 $etiquetaReps"
                                )
                            },
                            placeholder = if (reps.isBlank()) null else {
                                {
                                    Text(
                                        reps,
                                        style = LocalAppTextStyles.current.tabular,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraSmall,
                            textStyle = LocalAppTextStyles.current.tabular,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(if (pidePeso) 0.8f else 1f)
                        )
                    }
                }
            }
        }
        // La ficha, como enlace: con seis ejercicios, seis botones anchos eran una pared.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onShowGuide) { Text("Cómo se hace") }
        }
    }
}
