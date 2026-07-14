package com.marc.gymplan100.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

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
    /** Siguiente día pendiente (solo cuando no hay sesión): rotula el botón "Empezar entreno". */
    val nextDay: Int = 0
)

@Composable
fun WearApp(ambient: State<Boolean>, ambientTick: State<Int>) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(WearState()) }
    val isAmbient = ambient.value

    DisposableDataLayer(onState = { state = it })

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!state.active) {
                Text(
                    text = "Sin entrenamiento en curso",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = if (isAmbient) Color.Gray else MaterialTheme.colors.onBackground
                )
                if (!isAmbient) {
                    Spacer(Modifier.height(10.dp))
                    val label = if (state.nextDay > 0) "Empezar · Día ${state.nextDay}"
                    else "Empezar entreno"
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(label, textAlign = TextAlign.Center) },
                        onClick = { sendCommand(context, CMD_START) },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
            } else {
                SessionContent(
                    state = state,
                    isAmbient = isAmbient,
                    ambientTick = ambientTick.value,
                    onPrimary = { sendCommand(context, CMD_PRIMARY) },
                    onSwap = { sendCommand(context, CMD_SWAP) }
                )
            }
        }
    }
}

@Composable
private fun SessionContent(
    state: WearState,
    isAmbient: Boolean,
    ambientTick: Int,
    onPrimary: () -> Unit,
    onSwap: () -> Unit
) {
    // En ambiente atenuamos a gris (evita consumo y quemado de pantalla); interactivo, color pleno.
    val titleColor = if (isAmbient) Color(0xFFCCCCCC) else MaterialTheme.colors.onBackground
    val subColor = if (isAmbient) Color.Gray else MaterialTheme.colors.onSurfaceVariant

    Text(
        text = state.exercise,
        textAlign = TextAlign.Center,
        maxLines = 2,
        style = MaterialTheme.typography.title3,
        color = titleColor
    )
    if (state.totalSets > 0) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Serie ${state.setNumber} de ${state.totalSets}",
            style = MaterialTheme.typography.caption1,
            color = subColor
        )
    }

    // Cuenta atrás de la fase actual (descanso, calentamiento o serie por tiempo).
    if (state.endTime > 0L && !state.paused) {
        Spacer(Modifier.height(4.dp))
        Countdown(endTime = state.endTime, isAmbient = isAmbient, ambientTick = ambientTick, color = titleColor)
    } else if (state.paused) {
        Spacer(Modifier.height(4.dp))
        Text("En pausa", style = MaterialTheme.typography.title2, color = titleColor)
    }

    // En modo ambiente no hay táctil: ocultamos los botones (se ven al tocar/levantar la muñeca).
    if (!isAmbient) {
        Spacer(Modifier.height(12.dp))
        if (state.primaryLabel.isNotBlank()) {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(state.primaryLabel, textAlign = TextAlign.Center) },
                onClick = onPrimary,
                colors = ChipDefaults.primaryChipColors()
            )
        }
        if (state.canSwap) {
            Spacer(Modifier.height(6.dp))
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(state.swapLabel, textAlign = TextAlign.Center) },
                onClick = onSwap,
                colors = ChipDefaults.secondaryChipColors()
            )
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

const val PATH_STATE = "/gym/state"
const val PATH_COMMAND = "/gym/command"
const val CMD_PRIMARY = "primary"
const val CMD_SWAP = "swap"
const val CMD_START = "start"
