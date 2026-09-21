package com.marc.gymplan100.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

private const val SOLICITUD_NOTIFICACIONES = 1

class MainActivity : ComponentActivity() {

    /** True mientras el reloj está en modo ambiente (muñeca bajada): la app sigue visible, atenuada. */
    private val ambient = mutableStateOf(false)
    /** Se incrementa en cada actualización de ambiente (~1/min) para refrescar la cuenta atrás. */
    private val ambientTick = mutableIntStateOf(0)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            ambient.value = true
            ambientTick.intValue++
        }

        override fun onUpdateAmbient() {
            ambientTick.intValue++
        }

        override fun onExitAmbient() {
            ambient.value = false
        }
    }

    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activa el modo always-on: al bajar la muñeca la app se queda visible (atenuada)
        // en vez de cerrarse y volver al watch face.
        lifecycle.addObserver(ambientObserver)
        // El chip de la esfera es una notificación: sin este permiso no aparecería.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                SOLICITUD_NOTIFICACIONES
            )
        }
        setContent { WearApp(ambient, ambientTick) }
    }
}

/** Estado de la sesión recibido del móvil (espejo de lo que publica WearBridge). */
data class WearState(
    val active: Boolean = false,
    val phase: String = "",
    val exercise: String = "",
    val setNumber: Int = 0,
    val totalSets: Int = 0,
    val primaryLabel: String = "",
    val canSwap: Boolean = false,
    /** Etiqueta del botón de cambio de ejercicio (la cocina el móvil según el contexto). */
    val swapLabel: String = "Máquina ocupada",
    val paused: Boolean = false,
    val endTime: Long = 0L,
    /** Inicio de la sesión (epoch ms) cuando es cronómetro libre; 0 en las sesiones guiadas. */
    val startTime: Long = 0L,
    /** Siguiente día pendiente (solo cuando no hay sesión): rotula el botón "Empezar entreno". */
    val nextDay: Int = 0
)

@Composable
fun WearApp(ambient: State<Boolean>, ambientTick: State<Int>) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(WearState()) }
    val isAmbient = ambient.value

    // El chip de la esfera lo mantiene StateListenerService, pero también se refresca aquí:
    // así aparece aunque la sesión ya estuviera en marcha antes de abrir la app del reloj.
    DisposableDataLayer(onState = {
        state = it
        OngoingSession.update(context, it)
    })

    MaterialTheme {
        // Lista con escala en vez de una Column fija: Play rechazó la 2.12 porque en un reloj
        // redondo el último botón quedaba fuera del círculo y, con la fuente del sistema
        // grande, el título se cortaba por los lados. Así el contenido hace scroll (con la
        // corona también), lo que cae en los bordes se encoge y nada queda inalcanzable.
        val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
        // La lista se centra en el primer elemento, que con la fuente grande deja el título
        // pegado al borde de arriba. Arriba del todo, y otra vez en cada serie o ejercicio
        // nuevo: lo primero que hay que ver es qué toca y el botón de «Serie hecha».
        LaunchedEffect(state.active, state.exercise, state.setNumber) {
            listState.scrollBy(-100_000f)
        }
        val config = LocalConfiguration.current
        // Margen lateral en % de la pantalla (las pautas de Wear): en redonda la cuerda se
        // estrecha arriba y abajo, así que el texto necesita bastante más aire que los chips.
        val width = config.screenWidthDp.dp
        val round = config.isScreenRound
        val textPadding = if (round) width * 0.12f else width * 0.04f
        val chipPadding = if (round) width * 0.10f else width * 0.02f
        Scaffold(
            modifier = Modifier.background(Color.Black),
            positionIndicator = { if (!isAmbient) PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                autoCentering = null,
                contentPadding = PaddingValues(
                    top = config.screenHeightDp.dp * 0.15f,
                    bottom = config.screenHeightDp.dp * 0.25f
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!state.active) {
                    item {
                        Text(
                            text = "Sin entrenamiento en curso",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = textPadding),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.title3,
                            color = if (isAmbient) Color.Gray else MaterialTheme.colors.onBackground
                        )
                    }
                    if (!isAmbient) {
                        item {
                            val label = if (state.nextDay > 0) "Empezar · Día ${state.nextDay}"
                            else "Empezar entreno"
                            WearChip(label, chipPadding, primary = true) { sendCommand(context, CMD_START) }
                        }
                    }
                } else {
                    sessionItems(
                        state = state,
                        isAmbient = isAmbient,
                        ambientTick = ambientTick.value,
                        textPadding = textPadding,
                        chipPadding = chipPadding,
                        onPrimary = { sendCommand(context, CMD_PRIMARY) },
                        onSwap = { sendCommand(context, CMD_SWAP) }
                    )
                }
            }
        }
    }
}

/** Botón a todo el ancho que admite dos líneas: con la fuente grande una sola no cabe. */
@Composable
private fun WearChip(label: String, padding: Dp, primary: Boolean, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth().padding(horizontal = padding),
        label = {
            Text(
                label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = onClick,
        colors = if (primary) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors()
    )
}

private fun ScalingLazyListScope.sessionItems(
    state: WearState,
    isAmbient: Boolean,
    ambientTick: Int,
    textPadding: Dp,
    chipPadding: Dp,
    onPrimary: () -> Unit,
    onSwap: () -> Unit
) {
    item {
        // En ambiente atenuamos a gris (evita consumo y quemado de pantalla); interactivo, color pleno.
        val titleColor = if (isAmbient) Color(0xFFCCCCCC) else MaterialTheme.colors.onBackground
        val subColor = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = textPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.exercise,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.title3,
                color = titleColor
            )
            if (state.totalSets > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Serie ${state.setNumber} de ${state.totalSets}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    color = subColor
                )
            }

            // Cuenta atrás de la fase actual (descanso, calentamiento o serie por tiempo).
            if (state.endTime > 0L && !state.paused) {
                Spacer(Modifier.height(4.dp))
                Countdown(endTime = state.endTime, isAmbient = isAmbient, ambientTick = ambientTick, color = titleColor)
            } else if (state.startTime > 0L) {
                // Cronómetro libre: aquí no hay plan que seguir, lo único que importa es el tiempo.
                Spacer(Modifier.height(4.dp))
                Elapsed(startTime = state.startTime, isAmbient = isAmbient, ambientTick = ambientTick, color = titleColor)
            } else if (state.paused) {
                Spacer(Modifier.height(4.dp))
                Text("En pausa", style = MaterialTheme.typography.title2, color = titleColor)
            }
        }
    }

    // En modo ambiente no hay táctil: ocultamos los botones (se ven al tocar/levantar la muñeca).
    if (!isAmbient) {
        if (state.primaryLabel.isNotBlank()) {
            item { WearChip(state.primaryLabel, chipPadding, primary = true, onClick = onPrimary) }
        }
        if (state.canSwap) {
            item { WearChip(state.swapLabel, chipPadding, primary = false, onClick = onSwap) }
        }
    }
}

/** Muestra los segundos restantes hasta [endTime], refrescando cada segundo (o por tick en ambiente). */
@Composable
private fun Countdown(endTime: Long, isAmbient: Boolean, ambientTick: Int, color: Color) {
    // ambientTick fuerza el recálculo en cada actualización de ambiente (~1/min).
    var remaining by remember(endTime, ambientTick) {
        mutableStateOf(((endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0))
    }
    // El tic por segundo solo corre en modo interactivo; en ambiente el sistema no ejecuta corrutinas.
    LaunchedEffect(endTime, isAmbient) {
        if (isAmbient) return@LaunchedEffect
        while (true) {
            remaining = ((endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            if (remaining <= 0) break
            delay(500)
        }
    }
    val mm = remaining / 60
    val ss = remaining % 60
    Text(
        text = "%d:%02d".format(mm, ss),
        style = MaterialTheme.typography.display3,
        color = color
    )
}

/** Tiempo transcurrido desde [startTime], para las sesiones de cronómetro libre. */
@Composable
private fun Elapsed(startTime: Long, isAmbient: Boolean, ambientTick: Int, color: Color) {
    var elapsed by remember(startTime, ambientTick) {
        mutableStateOf(((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(0))
    }
    LaunchedEffect(startTime, isAmbient) {
        if (isAmbient) return@LaunchedEffect
        while (true) {
            elapsed = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(0)
            delay(500)
        }
    }
    val hh = elapsed / 3600
    val mm = (elapsed % 3600) / 60
    val ss = elapsed % 60
    Text(
        text = if (hh > 0) "%d:%02d:%02d".format(hh, mm, ss) else "%d:%02d".format(mm, ss),
        style = MaterialTheme.typography.display3,
        color = color
    )
}

const val PATH_STATE = "/gym/state"
const val PATH_COMMAND = "/gym/command"
const val PATH_ALERT = "/gym/alert"
const val CMD_PRIMARY = "primary"
const val CMD_SWAP = "swap"
const val CMD_START = "start"

// Tipos de aviso que manda el móvil (espejo de RestReminder.KIND_*).
const val KIND_BETWEEN_SETS = 0
const val KIND_BETWEEN_EXERCISES = 1
const val KIND_WARMUP = 2
const val KIND_TIMED_SET = 3
