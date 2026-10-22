package com.marc.gymplan100.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * «Saltar»: qué hacer con un ejercicio que ahora no toca.
 *
 * Son tres cosas distintas y antes solo existía la primera. Con la máquina ocupada se hace
 * luego; sin la máquina se hace otro; y si no se va a hacer, se tacha, como en la hoja de
 * papel. Sin esa última, quien no tenía prensa en casa marcaba «Serie hecha» tres veces para
 * poder pasar al siguiente, y esas series falsas acababan en su historial.
 *
 * [doneSets] son las series ya hechas de este ejercicio: con alguna hecha, tacharlo es dejarlo
 * ahí, y se dice así.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipExerciseSheet(
    exerciseName: String,
    doneSets: Int,
    canPostpone: Boolean,
    onPostpone: () -> Unit,
    onSwap: () -> Unit,
    onDrop: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            Text(
                exerciseName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("¿Qué hacemos con él?", style = MaterialTheme.typography.headlineMedium)
            if (canPostpone) {
                SkipOption(
                    title = "Máquina ocupada",
                    detail = "Lo haces al final y sigues con el siguiente.",
                    onClick = { onDismiss(); onPostpone() }
                )
            }
            SkipOption(
                title = "Hacer otro en su lugar",
                detail = "Las alternativas de la ficha, por si no tienes esta máquina.",
                onClick = { onDismiss(); onSwap() }
            )
            SkipOption(
                title = if (doneSets > 0) "No hago más series" else "No lo hago hoy",
                detail = if (doneSets > 0)
                    "Lo dejas aquí: las series hechas quedan apuntadas y sigues con el siguiente."
                else "Se tacha de hoy y no cuenta como hecho. Sigues con el siguiente.",
                onClick = { onDismiss(); onDrop() }
            )
        }
    }
}

@Composable
private fun SkipOption(title: String, detail: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.primary)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(Space.x4),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
