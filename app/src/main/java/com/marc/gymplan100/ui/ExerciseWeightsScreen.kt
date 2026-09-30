@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.ExerciseGroup
import com.marc.gymplan100.data.ExerciseGroups
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.Statistics
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * El peso que usas en cada ejercicio, para no tener que acordarte.
 *
 * Una fila por ejercicio con el nombre y su campo: sin tarjetas, el bloque de color ya separa
 * una fila de la siguiente. Las filas van agrupadas por músculo en secciones que se pliegan:
 * todas seguidas eran cuarenta, y buscar el curl de bíceps era bajar hasta encontrarlo.
 */
@Composable
fun ExerciseWeightsScreen(
    viewModel: PlanViewModel
) {
    val progress by viewModel.progress.collectAsState()
    val grupos = remember(PlanData.exerciseNames) {
        PlanData.exerciseNames.groupBy { ExerciseGroups.of(it) }.toSortedMap()
    }
    val prs = remember(progress) {
        Statistics.personalRecords(progress).associate { it.exercise to it.weight }
    }
    // Todo plegado de entrada, que es lo que hace la lista corta. Con un solo grupo no hay
    // nada que elegir y se abre solo. Guardado como texto para que sobreviva a girar la
    // pantalla y a que Android mate la app mientras entrenas.
    var abiertosTexto by rememberSaveable {
        mutableStateOf(if (grupos.size == 1) grupos.keys.first().name else "")
    }
    val abiertos = abiertosTexto.split(',').filter { it.isNotBlank() }.toSet()
    fun alternar(grupo: ExerciseGroup) {
        abiertosTexto = (if (grupo.name in abiertos) abiertos - grupo.name else abiertos + grupo.name)
            .joinToString(",")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis pesos", style = MaterialTheme.typography.headlineSmall)
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
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            item {
                Text(
                    "Se actualiza solo cuando registras peso en una sesión, y puedes editarlo " +
                        "aquí. Así no tienes que acordarte de qué peso usabas.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x3))
                // El aviso, porque aquí hay un solo número por ejercicio y las series casi
                // nunca llevan lo mismo: sin decirlo, este "12" parece que fue todo el día.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Space.x4)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Space.x3))
                    // El campo NO es el PR: se pisa en cada sesión, también el día que bajas
                    // carga. Llamarlo récord mentiría justo ese día, así que el récord va aparte.
                    Text(
                        "Aquí se guarda el peso MÁS ALTO de tu última sesión, el que te toca " +
                            "usar: si hoy hiciste 10, 12 y 11 kg, verás 12. Tu PR (peso récord, " +
                            "lo máximo que has movido nunca) sale debajo de cada ejercicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(Space.x2))
            }

            grupos.forEach { (grupo, nombres) ->
                val abierto = grupo.name in abiertos
                item(key = "grupo-${grupo.name}") {
                    GroupHeader(
                        label = grupo.label,
                        count = nombres.size,
                        open = abierto,
                        onClick = { alternar(grupo) }
                    )
                }
                if (abierto) {
                    items(nombres, key = { it }) { name ->
                        ExerciseWeightRow(
                            name = name,
                            pr = prs[name],
                            initialWeight = viewModel.exerciseWeight(name),
                            onWeightChange = { viewModel.setExerciseWeight(name, it) }
                        )
                    }
                }
            }
        }
    }
}

/** Cabecera de un grupo: toda la fila se toca para abrirlo o cerrarlo. */
@Composable
private fun GroupHeader(label: String, count: Int, open: Boolean, onClick: () -> Unit) {
    val giro by animateFloatAsState(if (open) 180f else 0f, label = "flecha")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.primary)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$label, $count ejercicios, " + if (open) "abierto" else "cerrado"
            }
            .padding(horizontal = Space.x2, vertical = Space.x2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = LocalAppTextStyles.current.tabular,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(Space.x2))
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(giro)
        )
    }
}

@Composable
private fun ExerciseWeightRow(
    name: String,
    pr: Float?,
    initialWeight: String,
    onWeightChange: (String) -> Unit
) {
    var weight by remember(name) { mutableStateOf(initialWeight) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Menos aire arriba y abajo que en otros bloques: el campo de texto ya trae el suyo,
            // y con el padding completo cada fila ocupaba media pantalla.
            .padding(horizontal = Space.x4, vertical = Space.x2)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (pr != null && pr > 0f) {
                    Text(
                        "PR ${formatPr(pr)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(Space.x3))
            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it
                    onWeightChange(it)
                },
                label = { Text("kg") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(112.dp)
            )
        }
    }
}

/** Con punto, como el campo de al lado y como Resultados: «57,5» junto a «57.5» despista. */
private fun formatPr(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
