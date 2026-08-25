@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.LoggedExercise
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Qué has hecho en un entrenamiento libre, al darle a finalizar.
 *
 * Sale con los ejercicios del día ya puestos, para que en el caso normal solo haya que
 * escribir pesos. Cada fila deja **elegir otro ejercicio de la lista o escribir el tuyo**,
 * porque entrenando libre es habitual salirse del plan.
 *
 * Lo primero que se ve es el tiempo que ya está guardado: quien no quiera apuntar nada no
 * tiene que sentir que ha perdido el entreno. Apuntar es opcional de verdad, y con el teclado
 * abierto se dice con todas las letras.
 */
@Composable
fun FreeSessionLog(
    day: TrainingDay?,
    elapsedMs: Long,
    onSave: (List<LoggedExercise>) -> Unit,
    onSkip: () -> Unit
) {
    // Precargado con el día del plan; si no hay, una fila en blanco para empezar.
    val filas = remember {
        mutableStateListOf<LoggedExercise>().apply {
            val delDia = day?.template?.exercises.orEmpty()
            val unaSerie = listOf(SetLog())
            if (delDia.isEmpty()) add(LoggedExercise("", sets = unaSerie))
            else delDia.forEach { add(LoggedExercise(name = it.name, sets = unaSerie)) }
        }
    }

    // Para el desplegable: los del día primero y el resto del plan después, sin repetir.
    val sugerencias = remember {
        val delDia = day?.template?.exercises.orEmpty().map { it.name }
        (delDia + PlanData.active.exerciseNames).distinct()
    }

    // Qué unidad pide cada ejercicio conocido: a una plancha se le apuntan segundos, y a una
    // dominada no se le piden kilos. Lo que se escriba a mano se trata como fuerza.
    val tipos = remember {
        PlanData.days
            .flatMap { it.template.exercises }
            .associate { it.name to (if (it.withoutWeight) ExerciseKind.BODYWEIGHT else it.kind) }
            .toMutableMap()
            .also { mapa ->
                day?.template?.exercises?.forEach { e ->
                    mapa[e.name] = if (e.withoutWeight) ExerciseKind.BODYWEIGHT else e.kind
                }
            }
    }

    val apuntados = filas.count { it.name.isNotBlank() && it.setsOrSingle.isNotEmpty() }
    val tecladoAbierto = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // Qué fila se está escribiendo: la de al lado se atenúa para que el pulgar sepa dónde está.
    var enfocada by remember { mutableStateOf(-1) }
    val foco = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Esta pantalla tampoco vive dentro de un Scaffold: los márgenes del sistema se
            // piden aquí, o el titular acaba debajo del reloj.
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Space.screen, end = Space.screen, top = Space.block,
                // Hueco para la barra fija: sin esto la última fila se queda medio tapada
                // por el botón de guardar y parece cortada.
                bottom = if (tecladoAbierto) 120.dp else 150.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Text(
                    "ENTRENO TERMINADO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
                Text("¿Qué has hecho?", style = MaterialTheme.typography.displaySmall)
                // Escribiendo, la cabecera se encoge: el sitio lo necesitan las filas.
                if (!tecladoAbierto) {
                    Spacer(Modifier.height(Space.x3))
                    TiempoGuardado(elapsedMs)
                    Spacer(Modifier.height(Space.x3))
                    Text(
                        if (filas.size == 1 && filas[0].name.isBlank()) {
                            "Hoy no había ejercicios apuntados en el plan, así que empiezas en " +
                                "blanco: escribe los que hayas hecho, o guarda solo el tiempo."
                        } else {
                            "Apunta lo que quieras y déjate el resto. Cada serie lleva sus " +
                                "kilos y sus repeticiones, y el más alto se queda como tu " +
                                "peso de ese ejercicio."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(filas.size) { i ->
                FilaEjercicio(
                    fila = filas[i],
                    sugerencias = sugerencias,
                    kind = tipos[filas[i].name] ?: ExerciseKind.STRENGTH,
                    // Con otra fila en marcha, esta baja de tono en vez de desaparecer.
                    atenuada = enfocada >= 0 && enfocada != i,
                    onFocus = { tiene -> if (tiene) enfocada = i else if (enfocada == i) enfocada = -1 },
                    onChange = { filas[i] = it },
                    onRemove = { if (filas.size > 1) filas.removeAt(i) }
                )
            }

            item { AñadirEjercicio { filas.add(LoggedExercise("", sets = listOf(SetLog()))) } }
        }

        // Las acciones, fijas abajo: con el teclado abierto y ocho filas, buscarlas al final
        // de la lista era un scroll a ciegas.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
                .padding(bottom = if (tecladoAbierto) Space.x2 else Space.x4)
        ) {
            if (tecladoAbierto) {
                // Escribiendo, la barra se encoge a una sola acción: la salida de "no apuntar
                // nada" ya no toca, pero sí hace falta decir que se puede guardar a medias.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$apuntados de ${filas.size} apuntados",
                        style = LocalAppTextStyles.current.tabular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // Saltar de fila sin buscar el scroll con el teclado tapando media pantalla.
                    // En cuerpo pequeño a propósito: es un atajo, no compite con Guardar.
                    TextButton(onClick = { foco.moveFocus(FocusDirection.Next) }) {
                        Text("Siguiente ejercicio ›", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(Space.x1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Puedes guardar aunque falten cosas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Space.x3))
                    Button(
                        onClick = { onSave(filas.toList()) },
                        shape = CircleShape,
                        modifier = Modifier.height(48.dp)
                    ) { Text("Guardar") }
                }
            } else {
                Button(
                    onClick = { onSave(filas.toList()) },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Guardar y terminar") }
                Subrayado("Terminar sin apuntar nada", onSkip)
            }
        }
    }
}

/** El tiempo ya está a salvo antes de escribir nada: se enseña primero y con cifras tabulares. */
@Composable
private fun TiempoGuardado(elapsedMs: Long) {
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(app.surface1)
            .padding(Space.x4)
    ) {
        Text(
            "TIEMPO DEL ENTRENO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x1))
        Text(
            reloj(elapsedMs),
            style = LocalAppTextStyles.current.tabular.copy(fontSize = 32.sp),
            color = app.work
        )
        Spacer(Modifier.height(Space.x1))
        Text(
            "Ya está guardado, apuntes lo que apuntes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaEjercicio(
    fila: LoggedExercise,
    sugerencias: List<String>,
    kind: ExerciseKind,
    atenuada: Boolean,
    onFocus: (Boolean) -> Unit,
    onChange: (LoggedExercise) -> Unit,
    onRemove: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    var enFoco by remember { mutableStateOf(false) }
    val coincidencias = remember(fila.name, sugerencias) {
        if (fila.name.isBlank()) sugerencias
        else sugerencias.filter { it.contains(fila.name, ignoreCase = true) }
    }
    // Lo escrito a mano no se pierde en el desplegable: se ofrece añadirlo tal cual.
    val puedeCrear = fila.name.isNotBlank() && sugerencias.none { it.equals(fila.name, true) }
    val mostrar = abierto && (coincidencias.isNotEmpty() || puedeCrear)

    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (atenuada) 0.55f else 1f)
            .onFocusChanged { enFoco = it.hasFocus; onFocus(it.hasFocus) }
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (enFoco) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, forma)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = Space.x3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Editable y con lista a la vez: se elige de los que ya hay, o se escribe uno nuevo.
            ExposedDropdownMenuBox(
                expanded = mostrar,
                onExpandedChange = { abierto = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextFieldSinBorde(
                    value = fila.name,
                    onValueChange = { onChange(fila.copy(name = it)); abierto = true },
                    label = "Ejercicio",
                    // Dos líneas: "Prensa de piernas (recorrido corto)" no cabe en una, y
                    // cortarlo obliga a adivinar cuál de los dos parecidos es.
                    modifier = Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrar) }
                )
                ExposedDropdownMenu(expanded = mostrar, onDismissRequest = { abierto = false }) {
                    coincidencias.take(8).forEach { nombre ->
                        DropdownMenuItem(
                            text = { Text(nombre) },
                            onClick = { onChange(fila.copy(name = nombre)); abierto = false }
                        )
                    }
                    if (puedeCrear) {
                        DropdownMenuItem(
                            text = { Text("Añadir «${fila.name}»") },
                            onClick = { abierto = false }
                        )
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar ${fila.name.ifBlank { "este ejercicio" }}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(Space.x3))
        // Una línea por serie: entrenando a tu aire la primera sale con 10, la segunda con 12
        // y la tercera con 11, y hasta ahora solo cabía un número por ejercicio.
        // Dos campos cortos, no dos campos anchos: nadie escribe "ciento veinte kilos".
        // A 150 % de texto el segundo baja solo en vez de estrujarse.
        val series = fila.sets.ifEmpty { listOf(SetLog()) }
        series.forEachIndexed { i, serie ->
            fun cambiar(nueva: SetLog) =
                onChange(fila.copy(sets = series.toMutableList().also { it[i] = nueva }))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${i + 1}ª",
                    style = LocalAppTextStyles.current.tabular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(30.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.x2),
                    verticalArrangement = Arrangement.spacedBy(Space.x2),
                    modifier = Modifier.weight(1f)
                ) {
                    if (kind == ExerciseKind.STRENGTH) {
                        CampoCorto(
                            value = serie.weight,
                            onValueChange = { cambiar(serie.copy(weight = it)) },
                            label = "Peso de la serie ${i + 1}",
                            unidad = "kg",
                            decimal = true
                        )
                    }
                    CampoCorto(
                        value = serie.reps,
                        onValueChange = { cambiar(serie.copy(reps = it)) },
                        label = if (kind == ExerciseKind.TIME) "Tiempo de la serie ${i + 1}"
                        else "Repeticiones de la serie ${i + 1}",
                        unidad = if (kind == ExerciseKind.TIME) "s" else "reps",
                        decimal = false
                    )
                }
                // La primera serie no se puede quitar: es la fila del ejercicio.
                if (series.size > 1) {
                    IconButton(
                        onClick = { onChange(fila.copy(sets = series.filterIndexed { j, _ -> j != i })) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Quitar la serie ${i + 1}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.x2))
        }
        TextButton(onClick = { onChange(fila.copy(sets = series + SetLog())) }) {
            Text("+ Añadir serie", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Campo de 96 dp con la unidad de sufijo: la unidad se lee, no se escribe. */
@Composable
private fun CampoCorto(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unidad: String,
    decimal: Boolean
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // Sin etiqueta propia: en un campo tan corto, "Peso" y "kg" se pisan y la etiqueta
        // acaba partida en dos líneas. La unidad ya dice qué se pide —kg es el peso, reps son
        // las repeticiones, s son segundos—, así que hace de etiqueta y de unidad a la vez.
        // La unidad va de icono final y no de `suffix` a propósito: el sufijo de M3 solo
        // aparece al enfocar el campo, y aquí tiene que verse también vacío. Un campo vacío
        // no es un error, es un dato que no has apuntado.
        trailingIcon = {
            Text(
                unidad,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraSmall,
        textStyle = LocalAppTextStyles.current.tabular,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        // El nombre del dato no se pinta, pero sí se dice: quien va con lector de pantalla
        // necesita saber que este campo es el peso y aquel las repeticiones.
        modifier = Modifier
            // El ancho crece con el tamaño del texto del sistema: a 96 dp fijos, con el texto
            // al 150 % un "100" se sale del campo y solo se leen dos cifras.
            .width(96.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.6f))
            .semantics { contentDescription = label }
    )
}

@Composable
private fun OutlinedTextFieldSinBorde(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable () -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        maxLines = 2,
        shape = MaterialTheme.shapes.extraSmall,
        trailingIcon = trailingIcon,
        modifier = modifier
    )
}

/** Añadir un ejercicio de más: borde discontinuo, porque es un hueco por rellenar. */
@Composable
private fun AñadirEjercicio(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.primary)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val guiones = with(density) { floatArrayOf(9.dp.toPx(), 7.dp.toPx()) }
                drawRoundRect(
                    color = outline,
                    style = Stroke(
                        width = with(density) { 1.5.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(guiones, 0f)
                    ),
                    cornerRadius = CornerRadius(with(density) { 16.dp.toPx() })
                )
            }
            .clickable(onClick = onClick)
            .padding(Space.x4)
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "Añadir otro ejercicio",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun reloj(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
