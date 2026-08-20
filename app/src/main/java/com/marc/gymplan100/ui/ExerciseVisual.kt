package com.marc.gymplan100.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space

/*
 * Cómo se ilustra un ejercicio. Un ejercicio normal es un dibujo; un circuito son tres, y
 * durante meses se enseñaba solo el primero: "Circuito core (plancha, rodillas, plancha
 * lateral)" aparecía como una plancha a secas y no había manera de saber qué tocaba hacer.
 * Aquí se pintan todos los movimientos, en orden y con su dosis.
 */

/**
 * La ilustración de un ejercicio: el dibujo si es un movimiento suelto, y la lista de
 * movimientos si es un circuito o una superserie. No pinta nada si no hay con qué.
 *
 * [imageHeight] es el alto del dibujo cuando es un ejercicio suelto; los movimientos de un
 * compuesto usan miniaturas fijas, que van en fila con su texto al lado.
 */
@Composable
fun ExerciseVisual(
    exerciseName: String,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 140.dp,
) {
    val female = LocalIsFemale.current
    val context = LocalContext.current

    if (ExerciseImages.isCompound(exerciseName)) {
        CompoundMoveList(exerciseName, modifier)
        return
    }
    val imageRes = ExerciseImages.forName(context, exerciseName, female) ?: return
    Image(
        painter = painterResource(imageRes),
        contentDescription = exerciseName,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    )
}

/**
 * Los movimientos de un compuesto, uno por fila: miniatura, número de orden, nombre y cuánto
 * toca. En vertical y no en tira horizontal porque lo que hay que entender es el orden, y
 * tres dibujos repartidos a lo ancho del móvil se quedan en tres manchas.
 */
@Composable
fun CompoundMoveList(exerciseName: String, modifier: Modifier = Modifier) {
    val moves = ExerciseImages.circuitMoves(
        LocalContext.current, exerciseName, LocalIsFemale.current
    )
    if (moves.isEmpty()) return
    val app = LocalAppColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.x2)
    ) {
        moves.forEachIndexed { index, move ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.x3)
            ) {
                Image(
                    painter = painterResource(move.drawable),
                    contentDescription = move.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 116.dp, height = 78.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(app.warmup)
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Spacer(Modifier.width(Space.x2))
                        Text(
                            move.label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (move.dose != null) {
                        Text(
                            move.dose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
            }
        }
    }
}
