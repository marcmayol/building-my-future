package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.Space

/**
 * «¿Cómo ha ido?»: fácil, justo o fallo, guardado como RIR (3, 1, 0). Pulsar el elegido lo
 * quita.
 *
 * Cada chip tiene un tercio del ancho, y en un móvil de 360 dp con el texto del sistema
 * grande (o en inglés, «Failure») la palabra no cabe: con `maxLines = 1` se cortaba a media
 * palabra. Aquí la letra de las tres etiquetas se encoge a la vez hasta que caben todas.
 */
@Composable
fun EffortChips(selectedRir: String?, onSelect: (String) -> Unit) {
    val opciones = listOf(
        stringResource(R.string.ses_effort_easy) to "3",
        stringResource(R.string.ses_effort_hard) to "1",
        stringResource(R.string.ses_effort_failure) to "0"
    )
    // Un solo factor para los tres: si cada uno se encogiera por su cuenta, «Easy» saldría
    // más grande que «Failure» y la fila se vería desigual. Se vuelve a medir desde el
    // tamaño normal si cambia la letra del sistema o el ancho.
    val escala = LocalDensity.current.fontScale
    val ancho = LocalConfiguration.current.screenWidthDp
    var factor by remember(opciones, escala, ancho) { mutableFloatStateOf(1f) }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
        opciones.forEach { (etiqueta, valor) ->
            FilterChip(
                selected = selectedRir == valor,
                onClick = { onSelect(if (selectedRir == valor) "" else valor) },
                label = {
                    // El estilo que pone el chip a su etiqueta, leído aquí dentro.
                    val estilo = LocalTextStyle.current
                    // El factor con el que se ha medido esta etiqueta. Si dos desbordan en la
                    // misma pasada, solo la primera lo baja: si no, bajaría dos escalones de golpe.
                    val medido = factor
                    var cabe by remember(etiqueta, medido, escala) { mutableStateOf(false) }
                    Text(
                        etiqueta,
                        maxLines = 1,
                        softWrap = false,
                        style = if (estilo.fontSize.isSp) estilo.copy(fontSize = estilo.fontSize * medido) else estilo,
                        // No se pinta mientras no quepa: evita ver el corte un instante.
                        modifier = Modifier.drawWithContent { if (cabe) drawContent() },
                        onTextLayout = { layout ->
                            if (layout.didOverflowWidth && estilo.fontSize.isSp && medido > MIN_FACTOR) {
                                if (factor == medido) factor = (medido * 0.9f).coerceAtLeast(MIN_FACTOR)
                            } else {
                                cabe = true
                            }
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Ni con la letra del sistema al máximo baja de la mitad: antes, mejor cortado que ilegible. */
private const val MIN_FACTOR = 0.5f

private val TextUnit.isSp get() = type == TextUnitType.Sp
