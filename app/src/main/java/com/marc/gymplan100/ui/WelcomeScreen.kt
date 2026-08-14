@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Bienvenida: ayuda a elegir plan la primera vez que se abre la app.
 *
 * Cuatro preguntas y una recomendación con su porqué. **Las respuestas no se guardan**: solo
 * sirven para decidir qué proponer y se olvidan al salir de aquí. Lo único que queda es el plan
 * que se active.
 *
 * El asistente nunca es obligatorio: en todos los pasos están a la vista el catálogo entero y
 * el editor, porque quien ya sabe lo que quiere no tiene por qué contestar nada.
 */
@Composable
fun WelcomeScreen(
    viewModel: PlanViewModel,
    onSeePlans: () -> Unit,
    onCreatePlan: () -> Unit,
    onDone: () -> Unit
) {
    val plans by viewModel.plans.collectAsState()

    var goal by remember { mutableStateOf<PlanGoal?>(null) }
    var shape by remember { mutableStateOf<PlanAdvisor.Shape?>(null) }
    var days by remember { mutableStateOf<PlanAdvisor.Days?>(null) }
    var place by remember { mutableStateOf<PlanAdvisor.Place?>(null) }
    var preguntando by remember { mutableStateOf(false) }

    val respuestas = if (goal != null && shape != null && days != null && place != null) {
        PlanAdvisor.Answers(goal!!, shape!!, days!!, place!!)
    } else null
    val resultado = respuestas?.let { PlanAdvisor.recommend(plans, it) }

    LazyColumn(
        // Opaca y a pantalla completa: se dibuja encima del resto de la app, que todavía no
        // tiene nada que enseñar porque no hay plan elegido.
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Space.screen, end = Space.screen, top = 64.dp, bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            Text(
                "BUILDING MY FUTURE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
            Text(
                if (preguntando) "Cuéntame cuatro cosas" else "Vamos a elegir tu plan",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(Space.x2))
            Text(
                if (preguntando) {
                    "No se guarda nada de esto: solo sirve para proponerte un plan."
                } else {
                    "Hay doce planes dentro. Puedo recomendarte uno en cuatro preguntas, " +
                        "o los miras tú mismo."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
        }

        if (!preguntando) {
            item {
                Button(
                    onClick = { preguntando = true },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Ayúdame a elegir") }
            }
            item {
                OutlinedButton(
                    onClick = onSeePlans,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Ver los planes") }
            }
            item {
                TextButton(onClick = onCreatePlan, modifier = Modifier.fillMaxWidth()) {
                    Text("Crear el mío")
                }
            }
        }

        if (preguntando) {
        item {
            Pregunta("¿Qué buscas?") {
                Opcion("Perder grasa", goal == PlanGoal.LOSE_FAT) { goal = PlanGoal.LOSE_FAT }
                Opcion("Ganar músculo", goal == PlanGoal.MUSCLE) { goal = PlanGoal.MUSCLE }
                Opcion("Levantar más", goal == PlanGoal.STRENGTH) { goal = PlanGoal.STRENGTH }
                Opcion("Mantenerme", goal == PlanGoal.MAINTAIN) { goal = PlanGoal.MAINTAIN }
                Opcion("Moverme mejor", goal == PlanGoal.MOBILITY) { goal = PlanGoal.MOBILITY }
            }
        }
        item {
            Pregunta("¿Cómo estás ahora?") {
                Opcion("Parado hace tiempo", shape == PlanAdvisor.Shape.STOPPED) {
                    shape = PlanAdvisor.Shape.STOPPED
                }
                Opcion("De vez en cuando", shape == PlanAdvisor.Shape.SOMETIMES) {
                    shape = PlanAdvisor.Shape.SOMETIMES
                }
                Opcion("Entreno regular", shape == PlanAdvisor.Shape.REGULAR) {
                    shape = PlanAdvisor.Shape.REGULAR
                }
            }
        }
        item {
            Pregunta("¿Cuántos días por semana?") {
                PlanAdvisor.Days.entries.forEach { d ->
                    Opcion("${d.n} días", days == d) { days = d }
                }
            }
        }
        item {
            Pregunta("¿Dónde entrenas?") {
                Opcion("En el gimnasio", place == PlanAdvisor.Place.GYM) {
                    place = PlanAdvisor.Place.GYM
                }
                Opcion("En casa, sin material", place == PlanAdvisor.Place.HOME) {
                    place = PlanAdvisor.Place.HOME
                }
            }
        }

        if (resultado != null) {
            val mejor = resultado.best
            item {
                Spacer(Modifier.height(Space.x2))
                Text(
                    if (mejor != null) "TE PROPONGO" else "NO TENGO NADA QUE ENCAJE",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalAppColors.current.work
                )
            }
            if (mejor != null) {
                item {
                    PlanPropuesto(
                        nombre = mejor.plan.name,
                        detalle = "${mejor.plan.totalDays} días · " +
                            "${mejor.plan.daysPerWeek} por semana",
                        motivo = mejor.reason,
                        destacado = true,
                        onElegir = {
                            viewModel.activatePlan(mejor.plan.id)
                            onDone()
                        }
                    )
                }
                resultado.alternatives.forEach { alt ->
                    item {
                        PlanPropuesto(
                            nombre = alt.plan.name,
                            detalle = "${alt.plan.totalDays} días · " +
                                "${alt.plan.daysPerWeek} por semana",
                            motivo = alt.reason,
                            destacado = false,
                            onElegir = {
                                viewModel.activatePlan(alt.plan.id)
                                onDone()
                            }
                        )
                    }
                }
                if (resultado.addOns.isNotEmpty()) {
                    item {
                        Text(
                            "Y si te sobra un día, encima de cualquiera de estos puedes montar " +
                                resultado.addOns.joinToString(", ") { it.name } + ".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Space.x2)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Con lo que me has dicho no hay ninguno que encaje del todo. Míralos " +
                            "todos y elige, o créate uno a tu medida.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Spacer(Modifier.height(Space.x2))
                OutlinedButton(
                    onClick = onSeePlans,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Ver los doce planes") }
            }
            item {
                TextButton(onClick = onCreatePlan, modifier = Modifier.fillMaxWidth()) {
                    Text("Mejor me creo el mío")
                }
            }
        }
        }
    }
}

@Composable
private fun Pregunta(titulo: String, opciones: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = Space.x2)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.x2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) { opciones() }
    }
}

@Composable
private fun Opcion(texto: String, elegida: Boolean, onClick: () -> Unit) {
    val app = LocalAppColors.current
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = if (elegida) app.work else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (elegida) app.work.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (elegida) 1.5.dp else 0.dp,
                color = if (elegida) app.work else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x4, vertical = Space.x3)
    )
}

@Composable
private fun PlanPropuesto(
    nombre: String,
    detalle: String,
    motivo: String,
    destacado: Boolean,
    onElegir: () -> Unit
) {
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (destacado) app.work.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(Space.x4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.x1))
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x2))
        Text(motivo, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Space.x3))
        Button(
            onClick = onElegir,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
        ) { Text("Empezar con este") }
    }
}
