@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import com.marc.gymplan100.GymApp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R
import com.marc.gymplan100.notify.RestReminder
import com.marc.gymplan100.update.Updates
import com.marc.gymplan100.data.Phase
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch

/**
 * Inicio.
 *
 * Jerarquía invertida respecto al diseño anterior: el protagonista es **lo que toca hoy**, no el
 * marcador "X de N". El progreso del plan queda como una sola lectura (una cifra, un porcentaje
 * y una barra) en vez de las cuatro formas de decir lo mismo que había antes.
 */
@Composable
fun HomeScreen(
    viewModel: PlanViewModel,
    onOpenPhase: (Int) -> Unit,
    onOpenDay: (Int) -> Unit,
    onResumeSession: (Int) -> Unit,
    onOpenSpecial: () -> Unit,
    onFreeWorkout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlans: () -> Unit = {}
) {
    val progress by viewModel.progress.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val done = progress.completedDays.count { it in 1..PlanData.TOTAL_DAYS }
    val total = PlanData.TOTAL_DAYS
    val nextDay = viewModel.nextDay()
    val nextTemplate = PlanData.dayByNumber(nextDay)?.template
    val currentPhase = PlanData.dayByNumber(nextDay)?.phase

    // Estado de optimización de batería: se reevalúa al volver a la pantalla.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBattery = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val showBatteryHint = !progress.batteryHintDismissed && !ignoringBattery

    // Sin permiso de alarmas exactas el aviso del descanso llega tarde, y en la version de
    // Google Play no se puede dar por hecho: alli el permiso que se concedia solo no existe.
    var puedeExactas by remember { mutableStateOf(RestReminder.canScheduleExact(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                puedeExactas = RestReminder.canScheduleExact(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    // La portada vive en su propio Scaffold y sus márgenes salen de ahí (los PaddingValues del
    // Scaffold): con la barra de abajo, el de la barra del sistema ya lo pone el Scaffold de la
    // app y pedirlo a mano (WindowInsets.systemBars) lo contaba dos veces. Van en el
    // contentPadding y no en la pantalla para que la lista siga pasando por debajo al bajar.
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Space.screen,
            end = Space.screen,
            top = inner.calculateTopPadding() + Space.x3,
            bottom = inner.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_icon),
                    contentDescription = stringResource(R.string.ini_home_logo),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.size(Space.x3))
                Text(
                    text = "Building My Future",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                LocalOpenHelp.current?.let { abrir ->
                    IconButton(
                        onClick = abrir,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = stringResource(R.string.ini_home_help),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(Space.x2))
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.ini_home_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            // El plan y la fase, en una línea: con planes propios saber cuál sigues importa.
            // Y debajo, la puerta a los demás: había quien pedía «un plan de fuerza, no de
            // hipertrofia» sin saber que existe, porque la lista vivía dentro de Ajustes.
            Column {
                val cabecera = if (activePlan.builtin) {
                    stringResource(R.string.ini_home_builtin_plan, total)
                } else {
                    activePlan.name.uppercase()
                }
                Text(
                    currentPhase?.let {
                        // Una fase sin nombre propio se llama «Fase 1»: no repetirlo dos veces.
                        val nombre = it.name.trim()
                        val generico = nombre.isEmpty() ||
                            Regex("""(fase|phase)\s*${it.number}""", RegexOption.IGNORE_CASE).matches(nombre)
                        stringResource(
                            R.string.ini_home_plan_phase, cabecera, it.number,
                            if (generico) "" else nombre.uppercase()
                        ).trimEnd()
                    } ?: cabecera,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Debajo y no al lado: a la derecha partía el nombre de la fase con el texto
                // del sistema grande.
                Text(
                    stringResource(R.string.ini_home_other_plans),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onOpenPlans)
                        .padding(vertical = Space.x2)
                )
            }
        }

        // El aviso de version nueva. En la variante de Play no pinta nada: alli avisa la
        // propia tienda, y la app ni siquiera lleva el actualizador dentro.
        item { Updates.Banner() }

        if (!puedeExactas) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Space.x4)) {
                        Text(
                            stringResource(R.string.ini_home_exact_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(Space.x1))
                        Text(
                            stringResource(R.string.ini_home_exact_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Space.x3))
                        Button(
                            onClick = { RestReminder.openExactAlarmSettings(context) },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                        ) { Text(stringResource(R.string.ini_home_exact_button), maxLines = 1) }
                    }
                }
            }
        }

        if (showBatteryHint) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Space.x4)) {
                        Text(stringResource(R.string.ini_home_battery_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Space.x1))
                        Text(
                            stringResource(R.string.ini_home_battery_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Space.x3))
                        // Etiquetas cortas a proposito: los dos botones se reparten el ancho
                        // a medias, y con el texto del sistema al 150 % "Entendido" no cabia
                        // en su mitad y se partia a media palabra ("Entendid / o").
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                            Button(
                                onClick = { openBatterySettings(context) },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.ini_home_battery_adjust), maxLines = 1) }
                            OutlinedButton(
                                onClick = { viewModel.dismissBatteryHint() },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.ini_home_battery_ok), maxLines = 1) }
                        }
                    }
                }
            }
        }

        item {
            val session = activeSession
            if (session != null) {
                HeroEnCurso(
                    dayNumber = session.dayNumber,
                    subtitle = when {
                        session.extra -> stringResource(R.string.ini_home_extra_running)
                        session.special -> stringResource(R.string.ini_home_special_running)
                        else -> PlanData.dayByNumber(session.dayNumber)?.template?.title.orEmpty()
                    },
                    onResume = { onResumeSession(session.dayNumber) }
                )
            } else if (done >= total && total > 0) {
                // Plan acabado: seguir diciendo "hoy te toca" el último día sería empujar a
                // repetir sin querer. Aquí se enseña el final y se manda a elegir qué sigue.
                HeroTerminado(
                    planName = activePlan.name,
                    total = total,
                    finishedAt = progress.finishedAt,
                    weeks = progress.startedAt.takeIf { it > 0L }?.let { ini ->
                        val fin = progress.finishedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                        ((fin - ini) / (7L * 24 * 60 * 60 * 1000)).toInt() + 1
                    },
                    onSeePlans = onOpenPlans,
                    onSeeSummary = { viewModel.showPlanSummaryAgain() }
                )
            } else {
                HeroHoy(
                    dayNumber = nextDay,
                    subtitle = nextTemplate?.let {
                        stringResource(
                            R.string.ini_title_dot_detail,
                            it.title,
                            pluralStringResource(R.plurals.ini_exercises, it.exercises.size, it.exercises.size)
                        )
                    }.orEmpty(),
                    everStarted = done > 0,
                    onStart = { onOpenDay(nextDay) },
                    onSeeDay = { onOpenDay(nextDay) }
                )
            }
        }

        // Con el plan terminado la barra marcaría 100 % y repetiría lo que ya dice el héroe,
        // que además lleva la fecha de fin. Se calla.
        if (done < total || total == 0) {
            item { ProgresoDelPlan(done = done, total = total) }
        }

        // A un toque, sin pasar por el menú de especiales: hay quien no quiere rutina, solo
        // apuntar lo que hace. Con una sesión en marcha no se ofrece otra.
        if (activeSession == null) {
            item { FilaEntrenoLibre(onClick = onFreeWorkout) }
        }

        item { FilaEspeciales(onClick = onOpenSpecial) }

        item {
            Text(
                stringResource(R.string.ini_home_phases),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.x2)
            )
        }

        items(PlanData.phases, key = { it.number }) { phase ->
            PhaseRow(
                phase = phase,
                completed = progress.completedInPhase(phase.number),
                total = PlanData.daysOfPhase(phase.number).size,
                onClick = { onOpenPhase(phase.number) }
            )
        }
    }
    }
}

/** Héroe con entreno a medias: el degradado de marca avisa de que hay algo abierto. */
@Composable
private fun HeroEnCurso(
    dayNumber: Int,
    subtitle: String,
    onResume: () -> Unit
) {
    val app = LocalAppColors.current
    val styles = LocalAppTextStyles.current
    val onHero = Color(0xFF3D0B02)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(app.work, app.rest)))
            .clickable(onClick = onResume)
            .padding(Space.screen)
    ) {
        Text(
            stringResource(R.string.ini_home_in_progress),
            style = MaterialTheme.typography.labelMedium,
            color = onHero
        )
        Spacer(Modifier.height(Space.x2))
        Text(stringResource(R.string.ini_day_n, dayNumber), style = styles.displayCard, color = onHero)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(Space.x1))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = onHero)
        }
        Spacer(Modifier.height(Space.x4))
        Button(
            onClick = onResume,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C0410),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) { Text(stringResource(R.string.ini_home_resume)) }
    }
}

/**
 * Héroe de un plan acabado. Sustituye al de "hoy te toca": con todos los días hechos, seguir
 * ofreciendo el último era empujar a repetirlo sin querer.
 *
 * **Sin el degradado de marca**: el degradado significa *en marcha*, y terminado es reposo.
 * Lleva solo una cinta de 5 dp arriba, para no competir con el héroe de "entreno en curso".
 */
@Composable
private fun HeroTerminado(
    planName: String,
    total: Int,
    finishedAt: Long,
    weeks: Int?,
    onSeePlans: () -> Unit,
    onSeeSummary: () -> Unit
) {
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(app.surface1)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(app.brandGradient)
        )
        Column(modifier = Modifier.padding(Space.screen)) {
            DoneBadge(kind = PlanBadge.Terminado, label = stringResource(R.string.ini_home_plan_done))
            Spacer(Modifier.height(Space.x3))
            Text(planName, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Space.x1))
            Text(
                pluralStringResource(R.plurals.ini_home_done_body, total, total),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x4))
            Button(
                onClick = onSeePlans,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primary)
            ) { Text(stringResource(R.string.ini_home_pick_next)) }
            TextButton(onClick = onSeeSummary, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ini_home_see_summary))
            }
            if (finishedAt > 0L) {
                Spacer(Modifier.height(Space.x1))
                // La fecha en el idioma con el que se pinta la app, con su patrón por idioma
                // («8 de noviembre» / «November 8»).
                val context = LocalContext.current
                val fecha = SimpleDateFormat(
                    stringResource(R.string.ini_home_date_pattern),
                    AppLanguage.locale(context)
                ).format(Date(finishedAt))
                Text(
                    if (weeks != null) {
                        pluralStringResource(R.plurals.ini_home_finished_on_weeks, weeks, weeks, fecha)
                    } else {
                        stringResource(R.string.ini_home_finished_on, fecha)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Héroe sin entreno: lo que toca hoy, con el gesto de empezar a un dedo. */
@Composable
private fun HeroHoy(
    dayNumber: Int,
    subtitle: String,
    everStarted: Boolean,
    onStart: () -> Unit,
    onSeeDay: () -> Unit
) {
    val styles = LocalAppTextStyles.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(Space.screen)
    ) {
        Text(
            stringResource(if (everStarted) R.string.ini_home_today else R.string.ini_home_new_plan),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(Space.x2))
        Text(
            stringResource(R.string.ini_day_n, dayNumber),
            style = styles.displayCard,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(Space.x1))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(Space.x4))
        Button(
            onClick = onStart,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) { Text(stringResource(R.string.ini_home_start_day, dayNumber)) }
        Spacer(Modifier.height(Space.x2))
        Text(
            stringResource(R.string.ini_home_see_day),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onSeeDay)
                .padding(Space.x2)
        )
    }
}

/** El progreso del plan, en una sola lectura: cifra, porcentaje y cinta. */
@Composable
private fun ProgresoDelPlan(done: Int, total: Int) {
    val app = LocalAppColors.current
    val percent = if (total > 0) done * 100 / total else 0
    Column(modifier = Modifier.padding(top = Space.x2)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                pluralStringResource(R.plurals.ini_home_progress, total, done, total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(stringResource(R.string.ini_percent, percent), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Space.x2))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (done > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (done.toFloat() / total).coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(app.brandGradient)
                )
            }
        }
        if (done == 0) {
            Spacer(Modifier.height(Space.x2))
            Text(
                stringResource(R.string.ini_home_no_progress),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Entreno libre directo: sin plan ni rutina, pero guiado (calentamiento, serie y descanso).
 * Con borde de color para que se distinga de los especiales, que van justo debajo.
 */
@Composable
private fun FilaEntrenoLibre(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x4, vertical = Space.x4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ini_home_free_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ini_home_free_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.ini_home_free_start),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Space.x2)
        )
    }
}

/** Los especiales salen de la tarjeta de progreso: ya no compiten con el botón del día. */
@Composable
private fun FilaEspeciales(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x4, vertical = Space.x4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // La nota a la derecha si cabe y, si no, debajo: con weight a su lado, la letra grande
        // partía el título a media palabra («Entrenamient / os especiales»).
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.ini_home_specials),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = Space.x2)
            )
            Text(
                stringResource(R.string.ini_home_specials_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

/** Una fase del plan. El número de fases y de días es siempre el del plan activo. */
@Composable
private fun PhaseRow(
    phase: Phase,
    completed: Int,
    total: Int,
    onClick: () -> Unit
) {
    val app = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(Space.x4)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${phase.number} · ${phase.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$completed/$total",
                style = LocalAppTextStyles.current.tabular,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(Space.x1))
        Text(
            phaseSpanText(phase),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.x3))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                // El track va translúcido: con `surface` a secas, en claro es blanco puro y la
                // barra vacía llamaba más la atención que la llena.
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        ) {
            if (completed > 0 && total > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (completed.toFloat() / total).coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(app.brandGradient)
                )
            }
        }
    }
}

/** ¿La app está exenta del ahorro de batería? Si no, los avisos pueden llegar tarde. */
private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
