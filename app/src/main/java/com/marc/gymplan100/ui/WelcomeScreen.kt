@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Bienvenida: ayuda a elegir plan la primera vez que se abre la app.
 *
 * Cuatro preguntas **en una sola pantalla** (cuatro toques no justifican cuatro transiciones)
 * y una recomendación con su porqué. Las respuestas **no se guardan en ninguna parte**: entran
 * en [PlanAdvisor], sale una propuesta y se olvidan. Se dice aquí mismo, en un bloque y no en
 * letra pequeña, porque es verdad.
 *
 * El asistente es un atajo, nunca un peaje: el catálogo y el editor están a la vista en todos
 * los pasos.
 */
@Composable
fun WelcomeScreen(
    viewModel: PlanViewModel,
    onSeePlans: () -> Unit,
    onCreatePlan: () -> Unit,
    onDone: () -> Unit
) {
    val plans by viewModel.plans.collectAsState()
    val app = LocalAppColors.current

    var goal by remember { mutableStateOf<PlanGoal?>(null) }
    var shape by remember { mutableStateOf<PlanAdvisor.Shape?>(null) }
    var days by remember { mutableStateOf<PlanAdvisor.Days?>(null) }
    var place by remember { mutableStateOf<PlanAdvisor.Place?>(null) }
    var preguntando by remember { mutableStateOf(false) }

    val completas = goal != null && shape != null && days != null && place != null
    val resultado = if (completas) {
        PlanAdvisor.recommend(plans, PlanAdvisor.Answers(goal!!, shape!!, days!!, place!!))
    } else null

    val lista = rememberLazyListState()
    // Al contestar la cuarta, el resultado se busca solo: nadie tiene que adivinar que hay
    // algo más abajo. Se para justo en la versalita del resultado (cabecera, privacidad y las
    // cuatro preguntas van antes), no al final de todo: bajar hasta el fondo dejaba la
    // propuesta cortada por arriba.
    LaunchedEffect(completas) { if (completas) lista.animateScrollToItem(PRIMER_RESULTADO) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Sin esto el titular sube por debajo del reloj al hacer scroll: esta pantalla se
            // dibuja encima del NavHost y no hereda los márgenes del sistema de nadie.
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Space.screen, end = Space.screen,
                top = Space.block,
                // Sitio para que la última pregunta pueda subir por encima de la barra fija:
                // si no, los chips de días se quedan medio tapados y parecen cortados.
                bottom = if (preguntando && !completas) 132.dp else Space.block
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(app.brandGradient)
                    )
                    Spacer(Modifier.size(Space.x3))
                    Text(
                        "BUILDING MY FUTURE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(Space.block))
                Text(
                    if (preguntando) "Cuéntame cuatro cosas" else "Vamos a elegir tu plan",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp)
                )
                Spacer(Modifier.height(Space.x3))
                Text(
                    if (preguntando) {
                        "No se guarda nada de esto: solo sirve para proponerte un plan."
                    } else {
                        "Hay doce planes dentro. Puedo recomendarte uno en cuatro preguntas, " +
                            "o los miras tú mismo."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!preguntando) {
                // Las tres salidas, en tres pesos: recomendar es un atajo, no la única puerta.
                item {
                    Spacer(Modifier.height(Space.x2))
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
                item { Subrayado("Crear el mío", onCreatePlan) }
            }

            if (preguntando) {
                item { BloquePrivacidad() }

                item {
                    AnswerGroup(
                        question = "¿Qué buscas?",
                        options = OBJETIVOS.map { it.second },
                        selected = OBJETIVOS.firstOrNull { it.first == goal }?.second,
                        onSelect = { etiqueta ->
                            goal = OBJETIVOS.first { it.second == etiqueta }.first
                        }
                    )
                }
                item {
                    AnswerGroup(
                        question = "¿Cómo estás ahora?",
                        options = FORMA.map { it.second },
                        selected = FORMA.firstOrNull { it.first == shape }?.second,
                        onSelect = { etiqueta -> shape = FORMA.first { it.second == etiqueta }.first }
                    )
                }
                item {
                    AnswerGroup(
                        question = "¿Cuántos días por semana?",
                        options = PlanAdvisor.Days.entries.map { "${it.n} días" },
                        selected = days?.let { "${it.n} días" },
                        onSelect = { etiqueta ->
                            days = PlanAdvisor.Days.entries.first { "${it.n} días" == etiqueta }
                        }
                    )
                }
                item {
                    AnswerGroup(
                        question = "¿Dónde entrenas?",
                        options = LUGAR.map { it.second },
                        selected = LUGAR.firstOrNull { it.first == place }?.second,
                        onSelect = { etiqueta -> place = LUGAR.first { it.second == etiqueta }.first }
                    )
                }

                if (resultado != null) {
                    val mejor = resultado.best
                    if (mejor != null) {
                        item {
                            Spacer(Modifier.height(Space.x2))
                            Text(
                                "TE PROPONGO",
                                style = MaterialTheme.typography.labelMedium,
                                color = app.work
                            )
                        }
                        item {
                            MotiveCard(
                                name = mejor.plan.name,
                                meta = "${mejor.plan.totalDays} días · " +
                                    "${mejor.plan.daysPerWeek} por semana",
                                motive = mejor.reason,
                                actionLabel = "Empezar con este",
                                onAction = { viewModel.activatePlan(mejor.plan.id); onDone() },
                                featured = true
                            )
                        }
                        if (resultado.alternatives.isNotEmpty()) {
                            item {
                                Text(
                                    "TAMBIÉN TE PEGAN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Space.x2)
                                )
                            }
                            items(resultado.alternatives.size) { i ->
                                val alt = resultado.alternatives[i]
                                MotiveCard(
                                    name = alt.plan.name,
                                    meta = "${alt.plan.totalDays} días · " +
                                        "${alt.plan.daysPerWeek} por semana",
                                    motive = alt.reason,
                                    actionLabel = "Empezar con este",
                                    onAction = { viewModel.activatePlan(alt.plan.id); onDone() },
                                    featured = false
                                )
                            }
                        }
                        if (resultado.addOns.isNotEmpty()) {
                            item {
                                Text(
                                    "Y si te sobra un día, encima de cualquiera de estos puedes " +
                                        "montar " +
                                        resultado.addOns.joinToString(" o ") { it.name } + ".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        item { NadaEncaja(resultado) }
                        // Sin plan principal que encaje, los bloques extra sí son la respuesta
                        // útil: quien pide moverse mejor no tiene un plan de movilidad de 12
                        // semanas, pero sí un bloque de movilidad que puede empezar hoy.
                        if (resultado.addOns.isNotEmpty()) {
                            item {
                                Text(
                                    "LO QUE SÍ TENGO DE ESTO",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Space.x2)
                                )
                            }
                            items(resultado.addOns.size) { i ->
                                val extra = resultado.addOns[i]
                                MotiveCard(
                                    name = extra.name,
                                    meta = "${extra.totalDays} días · " +
                                        "${extra.daysPerWeek} por semana",
                                    motive = "Es un bloque corto: se hace solo o encima de " +
                                        "cualquier plan, sin que te cambie la semana.",
                                    actionLabel = "Empezar con este",
                                    onAction = { viewModel.activatePlan(extra.id); onDone() },
                                    featured = i == 0
                                )
                            }
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
                    item { Subrayado("Mejor me creo el mío", onCreatePlan) }
                }
            }
        }

        // Barra fija: mientras faltan respuestas, el botón está inhabilitado **con el motivo
        // escrito al lado**, nunca solo gris.
        if (preguntando && !completas) {
            val falta = when {
                goal == null -> "qué buscas"
                shape == null -> "cómo estás ahora"
                days == null -> "cuántos días"
                else -> "dónde entrenas"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = Space.screen)
                    .padding(bottom = Space.x4)
            ) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                ) { Text("Ver qué me propones") }
                Spacer(Modifier.height(Space.x2))
                Text(
                    "Te falta una: $falta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Cabecera, bloque de privacidad y las cuatro preguntas: el resultado empieza en el sexto. */
private const val PRIMER_RESULTADO = 6

private val OBJETIVOS = listOf(
    PlanGoal.LOSE_FAT to "Perder grasa",
    PlanGoal.MUSCLE to "Ganar músculo",
    PlanGoal.STRENGTH to "Levantar más",
    PlanGoal.MAINTAIN to "Mantenerme",
    PlanGoal.MOBILITY to "Moverme mejor",
)

private val FORMA = listOf(
    PlanAdvisor.Shape.STOPPED to "Parado hace tiempo",
    PlanAdvisor.Shape.SOMETIMES to "De vez en cuando",
    PlanAdvisor.Shape.REGULAR to "Entreno regular",
)

private val LUGAR = listOf(
    PlanAdvisor.Place.GYM to "En el gimnasio",
    PlanAdvisor.Place.HOME to "En casa, sin material",
)

/**
 * La promesa de privacidad, como bloque y no como letra pequeña: preguntar por el cuerpo de
 * alguien y no explicar qué haces con la respuesta no está bien. Y es verdad, así que se ve.
 */
@Composable
private fun BloquePrivacidad() {
    val app = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Space.x4),
        horizontalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = app.warmup,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "Se calcula en el móvil y se descarta. No hay cuentas ni servidor.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
        )
    }
}

/** Ninguna propuesta encaja: se dice el porqué concreto y qué es lo más cercano. */
@Composable
private fun NadaEncaja(resultado: PlanAdvisor.Result) {
    val outline = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val guiones = with(density) { floatArrayOf(9.dp.toPx(), 7.dp.toPx()) }
                drawRoundRect(
                    color = outline,
                    style = Stroke(
                        width = with(density) { 1.5.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(guiones, 0f)
                    ),
                    cornerRadius = CornerRadius(with(density) { 16.dp.toPx() })
                )
            }
            .padding(Space.x4)
    ) {
        Text(
            "NO TENGO NADA QUE ENCAJE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x3))
        Text(resultado.whyNothing, style = MaterialTheme.typography.bodyLarge)
        resultado.closest?.let { cercano ->
            Spacer(Modifier.height(Space.x4))
            Text(
                "LO MÁS CERCANO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
            Text(cercano.plan.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.x1))
            Text(
                cercano.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
