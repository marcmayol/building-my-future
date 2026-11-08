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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.texto
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Bienvenida: ayuda a elegir plan la primera vez que se abre la app.
 *
 * Cinco preguntas **en una sola pantalla** (cinco toques no justifican cinco transiciones)
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
    var time by remember { mutableStateOf<PlanAdvisor.SessionTime?>(null) }
    var preguntando by remember { mutableStateOf(false) }

    val completas = goal != null && shape != null && days != null && place != null && time != null
    val resultado = if (completas) {
        PlanAdvisor.recommend(plans, PlanAdvisor.Answers(goal!!, shape!!, days!!, place!!, time))
    } else null

    // Etiquetas de las respuestas en el idioma en uso. Solo se comparan en memoria para saber
    // cuál está marcada; no se guardan.
    val objetivos = OBJETIVOS.map { it.first to stringResource(it.second) }
    val forma = FORMA.map { it.first to stringResource(it.second) }
    val tiempo = TIEMPO.map { it.first to stringResource(it.second) }
    val lugar = LUGAR.map { it.first to stringResource(it.second) }
    val dias = PlanAdvisor.Days.entries.map {
        it to pluralStringResource(R.plurals.ayu_welcome_n_dias, it.n, it.n)
    }
    val empezarConEste = stringResource(R.string.ayu_welcome_empezar_con_este)

    val lista = rememberLazyListState()
    // Al contestar la cuarta, el resultado se busca solo: nadie tiene que adivinar que hay
    // algo más abajo. Se para justo en la versalita del resultado (cabecera, privacidad y las
    // cinco preguntas van antes), no al final de todo: bajar hasta el fondo dejaba la
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
                    stringResource(
                        if (preguntando) R.string.ayu_welcome_titulo_preguntando
                        else R.string.ayu_welcome_titulo
                    ),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp)
                )
                Spacer(Modifier.height(Space.x3))
                Text(
                    stringResource(
                        if (preguntando) R.string.ayu_welcome_sub_preguntando
                        else R.string.ayu_welcome_sub
                    ),
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
                    ) { Text(stringResource(R.string.ayu_welcome_ayudame)) }
                }
                item {
                    OutlinedButton(
                        onClick = onSeePlans,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                    ) { Text(stringResource(R.string.ayu_welcome_ver_planes)) }
                }
                item { Subrayado(stringResource(R.string.ayu_welcome_crear), onCreatePlan) }
                // Hay quien ya sabe lo que hace y no quiere plan: entra y usa el entreno libre
                // de la portada, que no depende de ninguna rutina.
                item { Subrayado(stringResource(R.string.ayu_welcome_solo_apuntar), onDone) }
            }

            if (preguntando) {
                item { BloquePrivacidad() }

                item {
                    AnswerGroup(
                        question = stringResource(R.string.ayu_welcome_q_objetivo),
                        options = objetivos.map { it.second },
                        selected = objetivos.firstOrNull { it.first == goal }?.second,
                        onSelect = { etiqueta ->
                            goal = objetivos.first { it.second == etiqueta }.first
                        }
                    )
                }
                item {
                    AnswerGroup(
                        question = stringResource(R.string.ayu_welcome_q_forma),
                        options = forma.map { it.second },
                        selected = forma.firstOrNull { it.first == shape }?.second,
                        onSelect = { etiqueta -> shape = forma.first { it.second == etiqueta }.first }
                    )
                }
                item {
                    AnswerGroup(
                        question = stringResource(R.string.ayu_welcome_q_dias),
                        options = dias.map { it.second },
                        selected = dias.firstOrNull { it.first == days }?.second,
                        onSelect = { etiqueta -> days = dias.first { it.second == etiqueta }.first }
                    )
                }
                item {
                    // El rato que hay cada día pesa tanto como los días: un plan de una hora
                    // no le sirve a quien tiene cuarenta minutos antes de ir a trabajar.
                    AnswerGroup(
                        question = stringResource(R.string.ayu_welcome_q_tiempo),
                        options = tiempo.map { it.second },
                        selected = tiempo.firstOrNull { it.first == time }?.second,
                        onSelect = { etiqueta -> time = tiempo.first { it.second == etiqueta }.first }
                    )
                }
                item {
                    AnswerGroup(
                        question = stringResource(R.string.ayu_welcome_q_lugar),
                        options = lugar.map { it.second },
                        selected = lugar.firstOrNull { it.first == place }?.second,
                        onSelect = { etiqueta -> place = lugar.first { it.second == etiqueta }.first }
                    )
                }

                if (resultado != null) {
                    val mejor = resultado.best
                    if (mejor != null) {
                        item {
                            Spacer(Modifier.height(Space.x2))
                            Text(
                                stringResource(R.string.ayu_welcome_te_propongo),
                                style = MaterialTheme.typography.labelMedium,
                                color = app.work
                            )
                        }
                        item {
                            MotiveCard(
                                name = mejor.plan.name,
                                meta = planMeta(mejor.plan.totalDays, mejor.plan.daysPerWeek),
                                motive = mejor.reason.texto(),
                                actionLabel = empezarConEste,
                                onAction = { viewModel.activatePlan(mejor.plan.id); onDone() },
                                featured = true
                            )
                        }
                        if (resultado.alternatives.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.ayu_welcome_tambien),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Space.x2)
                                )
                            }
                            items(resultado.alternatives.size) { i ->
                                val alt = resultado.alternatives[i]
                                MotiveCard(
                                    name = alt.plan.name,
                                    meta = planMeta(alt.plan.totalDays, alt.plan.daysPerWeek),
                                    motive = alt.reason.texto(),
                                    actionLabel = empezarConEste,
                                    onAction = { viewModel.activatePlan(alt.plan.id); onDone() },
                                    featured = false
                                )
                            }
                        }
                        if (resultado.addOns.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(
                                        R.string.ayu_welcome_addons,
                                        resultado.addOns.joinToString(
                                            stringResource(R.string.ayu_welcome_o)
                                        ) { it.name }
                                    ),
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
                                    stringResource(R.string.ayu_welcome_lo_que_si),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Space.x2)
                                )
                            }
                            items(resultado.addOns.size) { i ->
                                val extra = resultado.addOns[i]
                                MotiveCard(
                                    name = extra.name,
                                    meta = planMeta(extra.totalDays, extra.daysPerWeek),
                                    motive = stringResource(R.string.ayu_welcome_bloque_corto),
                                    actionLabel = empezarConEste,
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
                        ) { Text(stringResource(R.string.ayu_welcome_ver_todos)) }
                    }
                    item { Subrayado(stringResource(R.string.ayu_welcome_mejor_creo), onCreatePlan) }
                }
            }
        }

        // Barra fija: mientras faltan respuestas, el botón está inhabilitado **con el motivo
        // escrito al lado**, nunca solo gris.
        if (preguntando && !completas) {
            val falta = stringResource(
                when {
                    goal == null -> R.string.ayu_welcome_falta_objetivo
                    shape == null -> R.string.ayu_welcome_falta_forma
                    days == null -> R.string.ayu_welcome_falta_dias
                    time == null -> R.string.ayu_welcome_falta_tiempo
                    else -> R.string.ayu_welcome_falta_lugar
                }
            )
            // «Te falta una» solo si de verdad es una; con varias, se cuentan.
            val cuantas = listOf(goal, shape, days, time, place).count { it == null }
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
                ) { Text(stringResource(R.string.ayu_welcome_ver_propuesta)) }
                Spacer(Modifier.height(Space.x2))
                Text(
                    pluralStringResource(R.plurals.ayu_welcome_te_faltan, cuantas, cuantas, falta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** «120 días · 3 por semana», en el idioma en uso. */
@Composable
private fun planMeta(totalDays: Int, daysPerWeek: Int): String = stringResource(
    R.string.ayu_welcome_meta,
    pluralStringResource(R.plurals.ayu_welcome_n_dias, totalDays, totalDays),
    daysPerWeek
)

/** Cabecera, bloque de privacidad y las cinco preguntas: el resultado empieza en el séptimo. */
private const val PRIMER_RESULTADO = 7

private val OBJETIVOS = listOf(
    PlanGoal.LOSE_FAT to R.string.ayu_welcome_obj_perder,
    PlanGoal.MUSCLE to R.string.ayu_welcome_obj_musculo,
    PlanGoal.STRENGTH to R.string.ayu_welcome_obj_fuerza,
    PlanGoal.MAINTAIN to R.string.ayu_welcome_obj_mantener,
    PlanGoal.MOBILITY to R.string.ayu_welcome_obj_movilidad,
)

private val FORMA = listOf(
    PlanAdvisor.Shape.LIMITED to R.string.ayu_welcome_forma_limitado,
    PlanAdvisor.Shape.STOPPED to R.string.ayu_welcome_forma_parado,
    PlanAdvisor.Shape.SOMETIMES to R.string.ayu_welcome_forma_aveces,
    PlanAdvisor.Shape.REGULAR to R.string.ayu_welcome_forma_regular,
)

private val TIEMPO = listOf(
    PlanAdvisor.SessionTime.MIN30 to R.string.ayu_welcome_tiempo_30,
    PlanAdvisor.SessionTime.MIN45 to R.string.ayu_welcome_tiempo_45,
    PlanAdvisor.SessionTime.MIN60 to R.string.ayu_welcome_tiempo_60,
    PlanAdvisor.SessionTime.MIN90 to R.string.ayu_welcome_tiempo_90,
)

private val LUGAR = listOf(
    PlanAdvisor.Place.GYM to R.string.ayu_welcome_lugar_gym,
    PlanAdvisor.Place.HOME to R.string.ayu_welcome_lugar_casa,
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
            stringResource(R.string.ayu_welcome_privacidad),
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
            stringResource(R.string.ayu_welcome_nada_encaja),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x3))
        Text(resultado.whyNothing?.texto().orEmpty(), style = MaterialTheme.typography.bodyLarge)
        resultado.closest?.let { cercano ->
            Spacer(Modifier.height(Space.x4))
            Text(
                stringResource(R.string.ayu_welcome_lo_mas_cercano),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
            Text(cercano.plan.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.x1))
            Text(
                cercano.reason.texto(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
