@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.FlowRow
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/*
 * Componentes del rediseño de agosto de 2026 (Claude Design), tal cual vinieron en el
 * handoff: los cuatro que comparten la bienvenida, el cierre de plan, la portada y la
 * lista de planes. Se mantienen juntos a propósito: son el vocabulario común de esas
 * pantallas, y separarlos haría que cada una se inventara el suyo.
 */

/**
 * AnswerChip · chip de respuesta del asistente de bienvenida.
 *
 * Alto mínimo 52 dp, radio píldora, texto 16 sp. Elegido = primary relleno;
 * sin elegir = borde 1.5 dp outline.
 * SIEMPRE dentro de un FlowRow: al 150 % baja de línea, nunca se corta.
 */
@Composable
fun AnswerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) cs.primary else Color.Transparent,
        contentColor = if (selected) cs.onPrimary else cs.onSurface,
        border = if (selected) null else BorderStroke(1.5.dp, cs.outline),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.W700 else FontWeight.W400,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 13.dp),
        )
    }
}

/** Grupo de respuestas: FlowRow, nunca Row con scroll horizontal. */
@Composable
fun AnswerGroup(
    question: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(question, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(11.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
            verticalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            options.forEach { AnswerChip(it, it == selected, { onSelect(it) }) }
        }
    }
}

/**
 * DoneBadge · distintivo reutilizable de estado del plan.
 * Portada, Mis planes y cierre de plan usan el MISMO componente.
 * El check va en `positive`; nunca el degradado de marca (reservado a lo que está en marcha).
 */
enum class PlanBadge { Terminado, VueltaEnCurso, Activo, SinEmpezar }

@Composable
fun DoneBadge(kind: PlanBadge, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    val bg = when (kind) {
        PlanBadge.Terminado -> cs.surfaceVariant
        PlanBadge.VueltaEnCurso, PlanBadge.Activo -> cs.primaryContainer
        PlanBadge.SinEmpezar -> Color.Transparent
    }
    val fg = when (kind) {
        PlanBadge.Terminado -> cs.onSurface
        PlanBadge.VueltaEnCurso, PlanBadge.Activo -> cs.onPrimaryContainer
        PlanBadge.SinEmpezar -> cs.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = if (kind == PlanBadge.SinEmpezar) BorderStroke(1.5.dp, cs.outlineVariant) else null,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            if (kind == PlanBadge.Terminado) {
                Icon(Icons.Filled.Check, null, tint = app.positive, modifier = Modifier.size(15.dp))
            }
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            )
        }
    }
}

/** Color del raíl de 5 dp a la izquierda de la tarjeta de plan: es lo que se lee sin leer. */
@Composable
fun planRailColor(kind: PlanBadge): Color = when (kind) {
    PlanBadge.Activo, PlanBadge.VueltaEnCurso -> MaterialTheme.colorScheme.primary
    PlanBadge.Terminado -> LocalAppColors.current.positive
    PlanBadge.SinEmpezar -> MaterialTheme.colorScheme.outlineVariant
}

/**
 * MotiveCard · propuesta de plan con su motivo.
 *
 * El motivo va a 17 sp: es el protagonista, no el nombre del plan. La gracia no es
 * que la app acierte, es que la persona pueda decir "eso no es lo mío" con criterio.
 *
 * `featured = true` → borde 1.5 dp primary, fondo surface1, botón relleno.
 * `featured = false` → sin borde, botón con borde. Misma estructura: se lee igual.
 */
@Composable
fun MotiveCard(
    name: String,
    meta: String,               // "12 días · 3 por semana"
    motive: String,
    actionLabel: String,        // "Empezar con este" / "Seguir con este"
    onAction: () -> Unit,
    featured: Boolean,
    overline: String? = null,   // "EL QUE VIENE DESPUÉS"
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(if (featured) 24.dp else 16.dp),
        color = if (featured) app.surface1 else cs.surface,
        border = if (featured) BorderStroke(1.5.dp, cs.primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(if (featured) 20.dp else 18.dp)) {
            overline?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = app.warmup)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (featured) 26.sp else 20.sp,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(meta, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(motive, style = MaterialTheme.typography.bodyLarge)   // 17 sp, nunca letra pequeña
            Spacer(Modifier.height(18.dp))
            if (featured) {
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary),
                ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, cs.outline),
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.min),
                ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
            }
        }
    }
}

/**
 * Fila de decisión del cierre de plan (segundo peso de la jerarquía).
 * Título + explicación debajo + chevrón. Sin altura fija: el contenido manda.
 */
@Composable
fun DecisionRow(
    title: String,
    explanation: String,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.min)
            // Toda la fila es el botón, no solo el chevrón: el título y su explicación son
            // parte de lo que se está eligiendo, y con el pulgar se apunta al texto.
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 2.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(explanation, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = cs.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = cs.outline)
    }
    HorizontalDivider(color = cs.outlineVariant)
}

/**
 * Salida de tercer peso: texto subrayado en `onSurfaceVariant`.
 *
 * Es la que dice "déjalo así" o "no me apuntes nada": tiene que estar a la vista y no pedir
 * el turno. Un botón con borde la haría competir con la acción de verdad; un gris sin subrayar
 * la escondería.
 */
@Composable
fun Subrayado(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = TextDecoration.Underline
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
