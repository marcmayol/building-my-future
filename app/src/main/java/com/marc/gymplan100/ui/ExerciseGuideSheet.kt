@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.marc.gymplan100.data.ExerciseAliases
import com.marc.gymplan100.data.ExerciseGuides
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.ui.theme.LocalAppColors
import java.net.URLEncoder

/**
 * Panel deslizable con la ficha del ejercicio: imagen, técnica, errores y alternativas.
 * Se abre desde la pantalla de la serie para consultar en mitad del entreno.
 */
@Composable
fun ExerciseGuideSheet(
    exerciseName: String,
    scheme: String,
    onDismiss: () -> Unit,
    /** Para que el consejo de cuando no hay ficha encaje: a un bloque de cardio no se le
     *  dice que controle el peso y no arquee la espalda. */
    kind: ExerciseKind = ExerciseKind.STRENGTH,
    /**
     * Guarda a que ejercicio del catalogo se parece este (null lo desvincula). Si no se pasa,
     * la ficha es de solo lectura: hay sitios donde ensenarla sin poder tocar nada.
     */
    onAlias: ((String?) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val guide = ExerciseGuides.forName(LocalContext.current, exerciseName)
    val imageRes = ExerciseImages.forName(LocalContext.current, exerciseName, LocalIsFemale.current)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(exerciseName, style = MaterialTheme.typography.headlineMedium)
            Text(
                // El esquema y el músculo principal en una línea: es lo que se mira de reojo
                // cuando ya sabes hacer el ejercicio y solo quieres confirmar.
                buildString {
                    append(scheme)
                    guide?.muscles?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
                        ?.let { append(" · ${it.lowercase()}") }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Los circuitos primero: forName devuelve la imagen del primer movimiento, así
            // que preguntar "¿hay imagen?" daba siempre que sí y la lista de movimientos no
            // llegaba a pintarse nunca. Un circuito se enseña entero o no se enseña.
            if (ExerciseImages.isCompound(exerciseName)) {
                CompoundMoveList(exerciseName)
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = exerciseName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                )
            }

            // Vídeo: abre una búsqueda en YouTube del movimiento (devuelve shorts y tutoriales).
            // Más robusto que un enlace fijo (los vídeos se borran/privatizan). Va al final de
            // la ficha, salvo cuando no hay ficha: entonces es la única salida y sube arriba.
            val context = LocalContext.current
            val videoQuery = guide?.videoQuery?.takeIf { it.isNotBlank() } ?: "$exerciseName técnica"
            val abrirVideo = {
                val url = "https://www.youtube.com/results?search_query=" +
                    URLEncoder.encode(videoQuery, "UTF-8")
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                Unit
            }

            if (guide == null) {
                OutlinedButton(onClick = abrirVideo, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Buscarlo en YouTube")
                }
                Text(
                    // Con planes propios este caso es habitual (el ejercicio no está en el
                    // catálogo de la app), y entonces tampoco hay ilustración a la que mirar.
                    when {
                        kind == ExerciseKind.CARDIO ->
                            "Un bloque de cardio no tiene técnica que mirar: elige la máquina " +
                                "que quieras (cinta, bici, elíptica o remo) y mantén el ritmo " +
                                "que pide el plan de principio a fin."
                        imageRes != null ->
                            "Todavía no hay ficha para este ejercicio. Fíjate en la imagen y " +
                                "haz el movimiento controlando el peso, con la espalda neutra " +
                                "y sin impulso."
                        else ->
                            "Este ejercicio no está en el catálogo de la app, así que no hay " +
                                "ficha ni ilustración. El vídeo de aquí arriba te lo busca en " +
                                "YouTube; hazlo controlando el peso, con la espalda neutra y " +
                                "sin impulso."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Un plan que te traes llama a las cosas como quiere. En vez de pedirte que
                // escribas musculos y tecnica a mano —que no lo haria nadie—, basta con decir
                // a cual se parece: hereda dibujo, ficha, musculos y sitio en el mapa.
                if (onAlias != null && kind != ExerciseKind.CARDIO) {
                    Spacer(Modifier.height(8.dp))
                    SelectorDeParecido(exerciseName, onAlias)
                }
                Spacer(Modifier.height(8.dp))
                return@Column
            }

            GuideSection("Músculos que trabaja") {
                Text(guide.muscles, style = MaterialTheme.typography.bodyMedium)
                val targets = MuscleTargets.forName(exerciseName)
                if (targets != null) {
                    // Principal y secundario usan los tokens de estado: el mismo código de
                    // color que la sesión (magenta = lo que más trabaja, ámbar = lo que ayuda).
                    val primaryColor = LocalAppColors.current.rest
                    val secondaryColor = LocalAppColors.current.warmup
                    Spacer(Modifier.height(4.dp))
                    MuscleMap(
                        primary = targets.primary,
                        secondary = targets.secondary,
                        bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        separatorColor = MaterialTheme.colorScheme.surface
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Frente",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Espalda",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (targets.secondary.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(primaryColor)
                            Text(
                                "  Principal    ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LegendDot(secondaryColor)
                            Text(
                                "  Secundario",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            GuideSection("Cómo se hace") {
                Text(guide.howTo, style = MaterialTheme.typography.bodyMedium)
            }

            // Estas dos se consultan de vez en cuando, no en cada serie: van plegadas para que
            // "Cómo se hace" no quede enterrado.
            // Solo si hay algo que contar: una sección plegable vacía es una promesa rota.
            if (guide.mistakes.isNotEmpty()) {
                CollapsibleSection("Errores típicos") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        guide.mistakes.forEach { Bullet(it) }
                    }
                }
            }

            if (guide.alternatives.isNotEmpty()) {
                CollapsibleSection("Si está ocupada o no la tienes") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        guide.alternatives.forEach { Bullet(it) }
                    }
                }
            }

            OutlinedButton(onClick = abrirVideo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  Ver vídeo en YouTube")
            }
        }
    }
}

/** Sección plegable: el título siempre visible y el contenido a un toque. */
@Composable
private fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (open) {
            content()
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun GuideSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * "Se parece a...": lista buscable del catalogo para adoptar un ejercicio desconocido.
 *
 * Con buscador porque el catalogo pasa de ochenta entradas y sin filtrar seria una lista
 * infinita en la que nadie encuentra nada.
 */
@Composable
private fun SelectorDeParecido(exerciseName: String, onAlias: (String?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    val actual = ExerciseAliases.aliasOf(exerciseName)

    if (actual != null) {
        Text(
            "Ahora se comporta como \u00ab$actual\u00bb.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { abierto = true }, shape = CircleShape) {
            Text(if (actual == null) "Se parece a\u2026" else "Cambiar", maxLines = 1)
        }
        if (actual != null) {
            OutlinedButton(onClick = { onAlias(null) }, shape = CircleShape) {
                Text("Quitar", maxLines = 1)
            }
        }
    }

    if (abierto) {
        val todos = remember { ExerciseImages.catalogNames() }
        val filtrados = remember(busqueda, todos) {
            if (busqueda.isBlank()) todos
            else todos.filter { it.contains(busqueda.trim(), ignoreCase = true) }
        }
        ModalBottomSheet(onDismissRequest = { abierto = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("\u00bfA cu\u00e1l se parece?", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                filtrados.take(40).forEach { nombre ->
                    TextButton(
                        onClick = { onAlias(nombre); abierto = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(nombre, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (filtrados.isEmpty()) {
                    Text(
                        "Nada con ese nombre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
