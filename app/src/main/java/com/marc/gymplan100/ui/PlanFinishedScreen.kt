@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.Statistics
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cierre de un plan terminado, según el rediseño de agosto de 2026.
 *
 * **Dos mitades con un separador explícito**: mirar atrás (qué has hecho) y mirar adelante
 * (qué sigue). Sin el separador, una mitad se come a la otra.
 *
 * Los números salen del historial de sesiones de ese plan, así que son reales. Si el plan se
 * completó marcando los días a mano no hay ninguno, y entonces **se dice con palabras y en
 * tono de constatación**, nunca de error: marcar a mano es una forma legítima de usar la app.
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

    val inicio = remember(progress.startedAt, history) {
        progress.startedAt.takeIf { it > 0L } ?: history.minByOrNull { it.startMillis }?.startMillis
    }
    val minutos = remember(history) { history.sumOf { it.durationSeconds } / 60 }
    val series = remember(history) { history.sumOf { it.totalSets } }
    val semanas = remember(inicio, progress.finishedAt) {
        val fin = progress.finishedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        inicio?.let { ((fin - it) / TimeUnit.DAYS.toMillis(7)).toInt() + 1 }
    }
    val racha = remember(progress.completedDays) { Statistics.bestStreak(progress.completedDays) }
    val subidas = remember(progress) {
        Statistics.weightProgression(progress)
            .mapNotNull { (nombre, puntos) ->
                if (puntos.size < 2) return@mapNotNull null
                val ini = puntos.first().weight
                val fin = puntos.last().weight
                if (fin > ini) Triple(nombre, ini, fin) else null
            }
            .sortedByDescending { (_, ini, fin) -> fin - ini }
            .take(4)          // el techo son cuatro: Estadísticas está a dos toques
    }
    val siguiente = remember(plan.id) { viewModel.nextPlanSuggestion() }
    val ultimaFase = plan.phases.lastOrNull()?.takeIf { plan.phases.size > 1 }
    val sinDatos = history.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cinta de degradado: el único sitio donde aparece la marca en esta pantalla, junto
        // al bloque de días. Terminado no es "en marcha", así que el degradado no se reparte.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(app.brandGradient)
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = Space.screen, end = Space.screen, top = Space.block, bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            // ─── Mirar atrás ────────────────────────────────────────────────────────
            item {
                DoneBadge(
                    kind = if (progress.rounds > 0) PlanBadge.VueltaEnCurso else PlanBadge.Terminado,
                    label = if (progress.rounds > 0) "Vuelta ${progress.rounds + 1} terminada"
                    else "Plan terminado"
                )
                Spacer(Modifier.height(Space.x3))
                Text(
                    plan.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(app.brandGradient)
                        .padding(Space.screen)
                ) {
                    val onHero = Color(0xFF3D0B02)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${plan.totalDays}",
                            style = styles.tabular.copy(fontSize = 60.sp),
                            color = onHero
                        )
                        Spacer(Modifier.size(Space.x2))
                        Text(
                            "de ${plan.totalDays} días",
                            style = MaterialTheme.typography.titleMedium,
                            color = onHero,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    if (semanas != null && inicio != null) {
                        Spacer(Modifier.height(Space.x1))
                        Text(
                            "en $semanas semanas · empezaste el ${fecha(inicio)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = onHero
                        )
                    }
                }
            }

            if (sinDatos) {
                item { SinRegistro() }
            } else {
                // Tres cifras en fila: apiladas competían entre ellas. Al 150 % el FlowRow
                // las baja de línea solo.
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)
                    ) {
                        Cifra("Entrenado", tiempo(minutos), Modifier.weight(1f))
                        Cifra("Series", "$series", Modifier.weight(1f))
                        Cifra("Mejor racha", "$racha d", Modifier.weight(1f), app.streak)
                    }
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
                        Text("${kg(ini)} → ${kg(fin)} kg", style = styles.tabular, color = app.streak)
                    }
                }
            }

            // ─── El separador: aquí se cambia de mirar atrás a mirar adelante ───────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.x3),
                    modifier = Modifier.padding(top = Space.block, bottom = Space.x1)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "¿Y AHORA QUÉ?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }

            // ─── Mirar adelante: cuatro salidas en tres pesos ───────────────────────
            if (siguiente != null) {
                item {
                    MotiveCard(
                        name = siguiente.plan.name,
                        meta = "${siguiente.plan.totalDays} días · " +
                            "${siguiente.plan.daysPerWeek} por semana",
                        motive = siguiente.reason,
                        actionLabel = "Seguir con este",
                        onAction = {
                            viewModel.dismissPlanFinished()
                            viewModel.activatePlan(siguiente.plan.id)
                        },
                        featured = true,
                        overline = "El que viene después"
                    )
                }
            }

            // La de la fase va primera de las filas porque es la que necesita explicación.
            if (ultimaFase != null) {
                item {
                    DecisionRow(
                        title = "Repetir solo «${ultimaFase.name}»",
                        explanation = "La última fase son " +
                            "${PlanData.daysOfPhase(ultimaFase.number).size} días: repetirla " +
                            "mantiene el nivel al que has llegado sin volver al principio.",
                        onClick = { viewModel.repeatLastPhase() }
                    )
                }
            }
            item {
                DecisionRow(
                    title = "Empezarlo de cero",
                    explanation = "Los ${plan.totalDays} días otra vez, como " +
                        "${progress.rounds + 2}ª vuelta.",
                    onClick = { viewModel.startNewRound() }
                )
            }
            item {
                DecisionRow(
                    title = "Elegir otro plan",
                    explanation = "Los doce, con el asistente si quieres que te ayude.",
                    onClick = { viewModel.dismissPlanFinished(); onSeePlans() }
                )
            }

            item {
                Spacer(Modifier.height(Space.x2))
                TextButton(
                    onClick = { viewModel.dismissPlanFinished(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Ahora no, déjalo así",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Una de las tres cifras del resumen. */
@Composable
private fun Cifra(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) {
        Text(valor, style = LocalAppTextStyles.current.tabular.copy(fontSize = 22.sp), color = color)
        Spacer(Modifier.height(Space.x1))
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Días marcados a mano: no hay tiempo, ni series, ni pesos. Ocupa el sitio de las tres cifras
 * en vez de dejar el hueco, y lo cuenta sin dramatismo: sin color de error ni icono de alerta.
 */
@Composable
private fun SinRegistro() {
    val app = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4),
        horizontalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = app.positive,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                "Marcaste los días a mano, sin usar el entrenamiento guiado, así que de este " +
                    "plan no queda registro de tiempo, series ni pesos.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(Space.x2))
            Text(
                "Los días están hechos, y eso es lo que cuenta. Si quieres que el próximo deje " +
                    "datos, entrena con el guiado o con el libre.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun fecha(millis: Long): String =
    SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-ES")).format(Date(millis))

private fun tiempo(minutos: Int): String {
    val h = minutos / 60
    val m = minutos % 60
    return if (h > 0) "$h h $m" else "$m min"
}

private fun kg(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
