@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.Statistics
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cierre de un plan terminado: qué has hecho y qué puedes hacer ahora.
 *
 * Sale una sola vez, al completar el último día. Los números salen del historial de sesiones
 * de ese plan (que no se borra nunca), así que son reales: no hay ningún dato guardado a
 * propósito para esta pantalla. La fecha de inicio se deduce del primer entreno registrado.
 */
@Composable
fun PlanFinishedScreen(
    viewModel: PlanViewModel,
    onSeePlans: () -> Unit,
    onDismiss: () -> Unit
) {
    val plan by viewModel.activePlan.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val app = LocalAppColors.current
    val styles = LocalAppTextStyles.current

    // La fecha guardada al empezar manda; el primer entreno es el respaldo para los planes
    // que ya estaban en marcha antes de que se guardara.
    val primera = remember(progress.startedAt, history) {
        progress.startedAt.takeIf { it > 0L } ?: history.minByOrNull { it.startMillis }?.startMillis
    }
    val minutos = remember(history) { history.sumOf { it.durationSeconds } / 60 }
    val series = remember(history) { history.sumOf { it.totalSets } }
    val semanas = remember(primera, progress.finishedAt) {
        val fin = progress.finishedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        primera?.let { ((fin - it) / TimeUnit.DAYS.toMillis(7)).toInt() + 1 }
    }
    val mejorRacha = remember(progress.completedDays) { Statistics.bestStreak(progress.completedDays) }
    // Pesos que subieron: solo los que tienen dos puntos o más, y solo los que mejoraron.
    val subidas = remember(progress) {
        Statistics.weightProgression(progress)
            .mapNotNull { (nombre, puntos) ->
                if (puntos.size < 2) return@mapNotNull null
                val ini = puntos.first().weight
                val fin = puntos.last().weight
                if (fin > ini) Triple(nombre, ini, fin) else null
            }
            .sortedByDescending { (_, ini, fin) -> fin - ini }
            .take(4)
    }
    val siguiente = remember(plan.id) { viewModel.nextPlanSuggestion() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Space.screen, end = Space.screen, top = 56.dp, bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            Text(
                if (progress.rounds > 0) "VUELTA ${progress.rounds + 1} TERMINADA" else "PLAN TERMINADO",
                style = MaterialTheme.typography.labelMedium,
                color = app.work
            )
            Spacer(Modifier.height(Space.x2))
            Text(plan.name, style = MaterialTheme.typography.displaySmall)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(app.brandGradient)
                    .padding(Space.x4)
            ) {
                val onHero = Color(0xFF3D0B02)
                Text(
                    "${plan.totalDays} de ${plan.totalDays} días",
                    style = MaterialTheme.typography.headlineMedium,
                    color = onHero
                )
                if (semanas != null && primera != null) {
                    Spacer(Modifier.height(Space.x1))
                    Text(
                        "en $semanas semanas · empezaste el ${fecha(primera)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onHero
                    )
                }
            }
        }

        if (minutos > 0 || series > 0 || mejorRacha > 1) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Space.x4)
                ) {
                    if (minutos > 0) Dato("Tiempo entrenado", tiempo(minutos), styles.tabular)
                    if (series > 0) Dato("Series hechas", "$series", styles.tabular)
                    if (mejorRacha > 1) Dato("Mejor racha", "$mejorRacha días", styles.tabular)
                }
            }
        }

        // Sin sesiones guardadas no hay tiempo, ni series, ni pesos que enseñar. Se dice, en
        // vez de dejar el hueco: marcar los días a mano es una forma legítima de usar la app,
        // pero entonces no hay nada que resumir.
        if (history.isEmpty()) {
            item {
                Text(
                    "Marcaste los días a mano, sin usar el entrenamiento guiado, así que de " +
                        "este plan no queda registro de tiempo, series ni pesos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (subidas.isNotEmpty()) {
            item {
                Text(
                    "LO QUE HA SUBIDO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.x2)
                )
            }
            items(subidas.size) { i ->
                val (nombre, ini, fin) = subidas[i]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${kg(ini)} → ${kg(fin)} kg",
                        style = styles.tabular,
                        color = app.work
                    )
                }
            }
        }

        item {
            Text(
                "¿Y AHORA QUÉ?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.block)
            )
        }

        if (siguiente != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(app.work.copy(alpha = 0.12f))
                        .padding(Space.x4)
                ) {
                    Text(siguiente.plan.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(Space.x1))
                    Text(
                        "${siguiente.plan.totalDays} días · ${siguiente.plan.daysPerWeek} por semana",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Space.x2))
                    Text(siguiente.reason, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(Space.x3))
                    Button(
                        onClick = {
                            viewModel.dismissPlanFinished()
                            viewModel.activatePlan(siguiente.plan.id)
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                    ) { Text("Seguir con este") }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.startNewRound() },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
            ) { Text("Otra vuelta a ${plan.name}") }
        }
        item {
            OutlinedButton(
                onClick = { viewModel.dismissPlanFinished(); onSeePlans() },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
            ) { Text("Ver todos los planes") }
        }
        item {
            TextButton(
                onClick = { viewModel.dismissPlanFinished(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ahora no, déjalo así") }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, estilo: androidx.compose.ui.text.TextStyle) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(valor, style = estilo, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun fecha(millis: Long): String =
    SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-ES")).format(Date(millis))

private fun tiempo(minutos: Int): String {
    val h = minutos / 60
    val m = minutos % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

private fun kg(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
