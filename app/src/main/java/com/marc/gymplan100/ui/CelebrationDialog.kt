package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.marc.gymplan100.data.Achievement
import com.marc.gymplan100.data.Celebration
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/** Texto sobre el degradado de marca, el mismo que usa el héroe de Inicio. */
private val OnHero = Color(0xFF3D0B02)

@Composable
fun CelebrationDialog(
    celebration: Celebration,
    onDismiss: () -> Unit,
    onPlayAnthem: () -> Unit = {}
) {
    val app = LocalAppColors.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Space.block),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(Space.x2))
            Text(
                "¡Día ${celebration.dayNumber} completado!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.x2))
            Text(
                celebration.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.x4))
            // El marcador va con el degradado de marca: es el momento de celebrar, no un dato más.
            // En cifra grande, además, para que no se lea como un segundo botón encima de "¡Seguir!".
            Text(
                "${celebration.totalCompleted} de ${PlanData.TOTAL_DAYS} días",
                style = MaterialTheme.typography.headlineMedium,
                color = OnHero,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(app.brandGradient)
                    .padding(vertical = Space.x4)
            )

            if (celebration.newAchievements.isNotEmpty()) {
                Spacer(Modifier.height(Space.x4))
                Text(
                    if (celebration.newAchievements.size == 1) "LOGRO DESBLOQUEADO"
                    else "LOGROS DESBLOQUEADOS",
                    style = MaterialTheme.typography.labelMedium,
                    color = app.work
                )
                Spacer(Modifier.height(Space.x2))
                celebration.newAchievements.forEach { AchievementRow(it) }
            }

            if (celebration.isFinalVictory) {
                Spacer(Modifier.height(Space.x4))
                Button(
                    onClick = onPlayAnthem,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) { Text("We Are The Champions") }
                Spacer(Modifier.height(Space.x2))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            } else {
                Spacer(Modifier.height(Space.block))
                Button(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.primary)
                ) { Text("¡Seguir!") }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.x1)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        Text(achievement.emoji, style = MaterialTheme.typography.headlineSmall)
        Column {
            Text(achievement.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
