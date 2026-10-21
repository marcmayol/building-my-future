package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.data.CatalogEntry
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Las alternativas de la ficha, para HACERLAS y no solo leerlas.
 *
 * Antes la ficha decía «si no tienes pec deck, haz aperturas con mancuernas en banco» y ahí se
 * quedaba: la serie se apuntaba igual como pec deck, con los kilos de las mancuernas mezclados
 * con los de una máquina que no se había tocado. Ahora cada alternativa que la app conoce sale
 * con su dibujo y un botón; al tocarlo, el ejercicio de hoy pasa a ser ese, con su dibujo, su
 * ficha y sus propios pesos.
 *
 * [planName] es el del plan y [currentName] el que se está haciendo (iguales si no hay cambio).
 * [onSwap] recibe el nuevo nombre —o null para volver al del plan— y si hay que recordarlo.
 */
@Composable
fun SwapSection(
    planName: String,
    currentName: String,
    alternatives: List<String>,
    remembered: Boolean,
    onSwap: (name: String?, remember: Boolean) -> Unit
) {
    var recordar by remember { mutableStateOf(true) }
    var buscando by remember { mutableStateOf(false) }
    val cambiado = currentName != planName

    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        if (cambiado) {
            Text(
                "Hoy haces «$currentName» en lugar de «$planName»" +
                    if (remembered) ", y así seguirá los próximos días." else ".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(onClick = { onSwap(null, false) }, shape = CircleShape) {
                Text("Volver al del plan", maxLines = 1)
            }
        }

        // Solo las que son un ejercicio del catálogo llevan botón; el resto son consejos
        // («hazlas sentado») y se leen como siempre.
        val conocidas = remember(alternatives, currentName) {
            alternatives.map { it to ExerciseCatalog.matchAlternative(it) }
                .distinctBy { it.second?.slug ?: it.first }
                .filter { it.second?.nombre != currentName }
        }
        conocidas.forEach { (texto, entrada) ->
            if (entrada == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Text(texto, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                AlternativeRow(
                    title = texto.trimEnd('.'),
                    entry = entrada,
                    onPick = { onSwap(entrada.nombre, recordar) }
                )
            }
        }

        TextButton(onClick = { buscando = !buscando }) {
            Text(if (buscando) "Cerrar el buscador" else "He hecho otro ejercicio…", maxLines = 1)
        }
        if (buscando) {
            SwapSearch(currentName = currentName, onPick = { onSwap(it, recordar) })
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { recordar = !recordar }
        ) {
            Checkbox(checked = recordar, onCheckedChange = { recordar = it })
            Text(
                "Recordarlo para los próximos días",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Una alternativa del catálogo: su dibujo, lo que dice la ficha y el botón para hacerla. */
@Composable
private fun AlternativeRow(title: String, entry: CatalogEntry, onPick: () -> Unit) {
    val context = LocalContext.current
    val dibujo = ExerciseImages.forName(context, entry.nombre, LocalIsFemale.current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPick)
            .padding(vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        if (dibujo != null) {
            Image(
                painter = painterResource(dibujo),
                contentDescription = entry.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            )
        } else {
            Box(Modifier.size(width = 84.dp, height = 56.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onPick, modifier = Modifier.heightIn(min = Touch.min)) {
            Text("La hago", maxLines = 1)
        }
    }
}

/**
 * Buscar en el catálogo entero, o escribir lo que sea. Sin escribir nada propone los del mismo
 * grupo muscular, que es casi siempre lo que se busca: si no hay pec deck, otro de pecho.
 */
@Composable
private fun SwapSearch(currentName: String, onPick: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    val grupo = remember(currentName) {
        ExerciseImages.slugFor(currentName)?.let { ExerciseCatalog.forSlug(it)?.grupo }
    }
    val resultados = remember(texto, grupo) {
        val q = ExerciseCatalog.tokens(texto)
        if (q.isEmpty()) {
            ExerciseCatalog.all.filter { it.grupo == grupo && it.nombre != currentName }.take(12)
        } else {
            ExerciseCatalog.all.filter { e -> ExerciseCatalog.tokens(e.nombre).let { t -> q.all { p -> t.any { it.startsWith(p) } } } }
                .take(20)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Buscar o escribir el ejercicio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (texto.isBlank() && grupo != null && resultados.isNotEmpty()) {
            Text(
                "Otros de ${ExerciseCatalog.groupLabel(grupo).lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        resultados.forEach { e ->
            AlternativeRow(title = e.nombre, entry = e, onPick = { onPick(e.nombre) })
        }
        val escrito = texto.trim()
        if (escrito.isNotEmpty() && resultados.none { it.nombre.equals(escrito, ignoreCase = true) }) {
            // Lo que no está en el catálogo también vale: se apunta con su nombre, solo que
            // sin dibujo ni ficha (la ficha ofrecerá buscarlo en vídeo).
            TextButton(onClick = { onPick(escrito) }) {
                Text("Apuntarlo como «$escrito»", maxLines = 2)
            }
        }
    }
}
