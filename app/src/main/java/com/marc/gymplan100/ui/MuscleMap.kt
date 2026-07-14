package com.marc.gymplan100.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.marc.gymplan100.data.MuscleMapData

/**
 * Mapa muscular: cuerpo de frente (izquierda) y de espalda (derecha), resaltando en el color
 * de la app los grupos trabajados por el ejercicio. Los trazados vienen de [MuscleMapData]:
 * la frente está en x 0..724 y la espalda ya viene en x 724..1448 (cuadrado 1448x1448),
 * así que se dibujan tal cual sin desplazar.
 *
 * La cabeza y el pelo del set se dibujan mal con el rasterizador de Compose (salen deformes,
 * como una "cara" en la espalda), así que se omiten y en su lugar se dibuja un óvalo de cráneo
 * limpio en la posición real de la cabeza (calculada a partir de esos mismos trazados).
 *
 * Los trazados SVG se parsean una sola vez (caché perezosa) porque son ~160 y no cambian.
 */
private fun parse(map: Map<String, List<String>>): Map<String, List<Path>> =
    map.mapValues { (_, list) -> list.map { PathParser().parsePathString(it).toPath() } }

private val frontPaths: Map<String, List<Path>> by lazy { parse(MuscleMapData.front) }
private val backPaths: Map<String, List<Path>> by lazy { parse(MuscleMapData.back) }

// Cabeza/pelo: se omiten (Compose los deforma) y se sustituyen por un óvalo.
private val NO_DRAW = setOf("head", "hair")
// Además del óvalo, el cuello se rellena pero sin líneas internas (si no, "cara" rara).
private val NO_SEPARATOR = setOf("head", "hair", "neck")

// Óvalo del cráneo (viewBox): topLeft y tamaño, deducidos de la caja real de cabeza+pelo.
private val frontHead = Offset(303f, 97f) to Size(122f, 146f)
private val backHead = Offset(1022f, 96f) to Size(124f, 138f)

@Composable
fun MuscleMap(
    highlighted: Set<String>,
    bodyColor: Color,
    highlightColor: Color,
    separatorColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val scale = size.width / 1448f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawBody(frontPaths, highlighted, bodyColor, highlightColor, separatorColor)
            drawBody(backPaths, highlighted, bodyColor, highlightColor, separatorColor)
            // Cabezas limpias por encima del cuello.
            drawOval(bodyColor, topLeft = frontHead.first, size = frontHead.second)
            drawOval(bodyColor, topLeft = backHead.first, size = backHead.second)
        }
    }
}

private fun DrawScope.drawBody(
    paths: Map<String, List<Path>>,
    highlighted: Set<String>,
    bodyColor: Color,
    highlightColor: Color,
    separatorColor: Color
) {
    // 1) Relleno del cuerpo (músculos no trabajados).
    paths.forEach { (slug, list) ->
        if (slug in NO_DRAW || slug in highlighted) return@forEach
        list.forEach { drawPath(it, color = bodyColor) }
    }
    // 2) Relleno de los músculos trabajados, por encima.
    paths.forEach { (slug, list) ->
        if (slug in NO_DRAW || slug !in highlighted) return@forEach
        list.forEach { drawPath(it, color = highlightColor) }
    }
    // 3) Líneas de separación entre músculos (aspecto segmentado).
    paths.forEach { (slug, list) ->
        if (slug in NO_SEPARATOR) return@forEach
        list.forEach { drawPath(it, color = separatorColor, style = Stroke(width = 2f)) }
    }
}
