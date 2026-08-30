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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.marc.gymplan100.GymApp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R
import com.marc.gymplan100.notify.RestReminder
import com.marc.gymplan100.update.Updates
import com.marc.gymplan100.data.Achievements
import com.marc.gymplan100.data.Phase
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.contar
import com.marc.gymplan100.data.palabra
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
    onOpenAchievements: () -> Unit,
    onOpenWeights: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenStats: () -> Unit,
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


    // La portada no vive dentro de un Scaffold, así que los márgenes del sistema se piden
    // aquí. Iban a ojo (44 dp) y la barra de estado de este móvil mide 48,8: el logo estaba
    // ya medio metido debajo, y con una isla o un notch más alto sería peor. Van en el
    // contentPadding y no en la pantalla para que la lista siga pasando por debajo al bajar.
    val sistema = WindowInsets.systemBars.asPaddingValues()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Space.screen,
            end = Space.screen,
            top = sistema.calculateTopPadding() + Space.x3,
            bottom = sistema.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Space.x3)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_icon),
                    contentDescription = "Logo Building My Future",
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
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            // El plan y la fase, en una línea: con planes propios saber cuál sigues importa.
            Text(
                buildString {
                    append(if (activePlan.builtin) "PLAN DE $total DÍAS" else activePlan.name.uppercase())
                    currentPhase?.let { append(" · FASE ${it.number} ${it.name.uppercase()}") }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            "El descanso puede llegar tarde",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(Space.x1))
                        Text(
                            "Para que el aviso suene justo a los 90 segundos, y no cuando al " +
                                "sistema le venga bien, hay que darle permiso para alarmas exactas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Space.x3))
                        Button(
                            onClick = { RestReminder.openExactAlarmSettings(context) },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
                        ) { Text("Dar permiso", maxLines = 1) }
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
                        Text("Para que suenen los avisos", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Space.x1))
                        Text(
                            "Desactiva el ahorro de batería para esta app. Así el aviso del " +
                                "descanso sonará puntual aunque tengas la pantalla apagada.",
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
                            ) { Text("Ajustar", maxLines = 1) }
                            OutlinedButton(
                                onClick = { viewModel.dismissBatteryHint() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Vale", maxLines = 1) }
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
                        session.extra -> "Entrenamiento extra · no cuenta día"
                        session.special -> "Sesión libre en marcha"
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
                        "${it.title} · " + contar(it.exercises.size, "ejercicio", "ejercicios")
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

        item { FilaEspeciales(onClick = onOpenSpecial) }

        item {
            Column {
                AccesoFila(
                    icon = R.drawable.ic_logros,
                    label = "Logros",
                    trailing = "${Achievements.unlockedIds(progress).size}/${Achievements.all.size}",
                    onClick = onOpenAchievements
                )
                AccesoFila(R.drawable.ic_pesos, "Mis pesos", onClick = onOpenWeights)
                AccesoFila(R.drawable.ic_resultados, "Resultados", onClick = onOpenResults)
                AccesoFila(
                    R.drawable.ic_estadisticas, "Estadísticas",
                    onClick = onOpenStats, divider = false
                )
            }
        }

        item {
            Text(
                "FASES DEL PLAN",
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
            "EN CURSO · TOCA PARA REANUDAR",
            style = MaterialTheme.typography.labelMedium,
            color = onHero
        )
        Spacer(Modifier.height(Space.x2))
        Text("Día $dayNumber", style = styles.displayCard, color = onHero)
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
        ) { Text("Reanudar") }
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
            DoneBadge(kind = PlanBadge.Terminado, label = "Plan terminado")
            Spacer(Modifier.height(Space.x3))
            Text(planName, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Space.x1))
            Text(
                "Los $total días, hechos. Elige qué sigue cuando quieras.",
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
            ) { Text("Elegir el siguiente") }
            TextButton(onClick = onSeeSummary, modifier = Modifier.fillMaxWidth()) {
                Text("Ver el resumen del plan")
            }
            if (finishedAt > 0L) {
                Spacer(Modifier.height(Space.x1))
                Text(
                    buildString {
                        append("Terminado el ")
                        append(
                            SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
                                .format(Date(finishedAt))
                        )
                        if (weeks != null) {
                            append(", en $weeks ")
                            append(if (weeks == 1) "semana" else "semanas")
                        }
                        append(".")
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
            if (everStarted) "HOY TE TOCA" else "PLAN NUEVO · EMPIEZA CUANDO QUIERAS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(Space.x2))
        Text(
            "Día $dayNumber",
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
        ) { Text("Empezar día $dayNumber") }
        Spacer(Modifier.height(Space.x2))
        Text(
            "Ver el día antes de empezar",
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
                "$done de $total días",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text("$percent %", style = MaterialTheme.typography.titleMedium)
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
                "Aún no hay progreso: la barra se llena a partir de hoy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        Text(
            "Entrenamientos especiales",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            "no cuentan día",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Acceso como fila con icono lineal: los emoji se quedan en Logros, donde son contenido. */
@Composable
private fun AccesoFila(
    icon: Int,
    label: String,
    trailing: String? = null,
    divider: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = Touch.primary)
                .padding(vertical = Space.x3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(Space.x4))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (trailing != null) {
                Text(
                    trailing,
                    style = LocalAppTextStyles.current.tabular,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
            "${phase.range} · ${phase.weeks}",
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
