package com.marc.gymplan100.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Un texto que, si no cabe, encoge la letra en vez de partir una palabra por la mitad.
 *
 * Con el texto del sistema al 150 % en un móvil de 360 dp salían cosas como «DESC/ANSO» en la
 * cabecera o «Finish exercis» en un botón. Puede ocupar hasta [maxLines] líneas, pero siempre
 * cortando entre palabras; si ni así cabe (de ancho o de alto), baja la letra un 10 % y vuelve
 * a medir.
 */
@Composable
fun TextoQueCabe(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    // Lo que obliga a volver a medir desde el tamaño normal. Por defecto, el propio texto; una
    // cuenta atrás pasa otra cosa, o cada segundo volvería a empezar y parpadearía.
    medirDeNuevoCon: Any? = text,
) {
    val escala = LocalDensity.current.fontScale
    val ancho = LocalConfiguration.current.screenWidthDp
    var factor by remember(medirDeNuevoCon, escala, ancho) { mutableFloatStateOf(1f) }
    var cabe by remember(medirDeNuevoCon, escala, ancho) { mutableStateOf(false) }
    val medido = factor
    Text(
        text,
        style = if (style.fontSize.isSp) style.copy(fontSize = style.fontSize * medido) else style,
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        textAlign = textAlign,
        // No se pinta mientras no quepa: así no se ve el corte un instante.
        modifier = modifier.drawWithContent { if (cabe) drawContent() },
        onTextLayout = { layout ->
            if (!cabeEntero(text, layout) && style.fontSize.isSp && medido > FACTOR_MINIMO) {
                if (factor == medido) factor = (medido * 0.9f).coerceAtLeast(FACTOR_MINIMO)
            } else {
                cabe = true
            }
        }
    )
}

/** Cabe si no desborda y ningún salto de línea cae en mitad de una palabra. */
private fun cabeEntero(text: String, layout: TextLayoutResult): Boolean {
    if (layout.hasVisualOverflow) return false
    for (linea in 0 until layout.lineCount - 1) {
        val fin = layout.getLineEnd(linea)
        if (fin in 1 until text.length && !text[fin - 1].isWhitespace() && !text[fin].isWhitespace()) {
            return false
        }
    }
    return true
}

/** Ni con la letra del sistema al máximo baja de la mitad: antes, mejor cortado que ilegible. */
private const val FACTOR_MINIMO = 0.5f
