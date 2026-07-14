package com.marc.gymplan100.wear.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

/**
 * Mantiene [WearState] sincronizado con lo que publica el móvil en la Data Layer.
 *
 * Al entrar, lee el estado actual (por si la sesión ya estaba en marcha) y se suscribe a los
 * cambios; al salir, se da de baja. El móvil es la fuente de verdad: el reloj solo refleja.
 */
@Composable
fun DisposableDataLayer(onState: (WearState) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val dataClient = Wearable.getDataClient(context)

        val listener = DataClient.OnDataChangedListener { events ->
            for (event in events) {
                val item = event.dataItem
                if (item.uri.path == PATH_STATE) {
                    onState(parseState(DataMapItem.fromDataItem(item).dataMap))
                }
            }
        }
        dataClient.addListener(listener)

        // Carga el estado actual ya presente en la Data Layer (sesión en curso al abrir el reloj).
        dataClient.dataItems.addOnSuccessListener { buffer ->
            try {
                for (item in buffer) {
                    if (item.uri.path == PATH_STATE) {
                        onState(parseState(DataMapItem.fromDataItem(item).dataMap))
                    }
                }
            } finally {
                buffer.release()
            }
        }

        onDispose { dataClient.removeListener(listener) }
    }
}

private fun parseState(map: DataMap): WearState {
    if (!map.getBoolean("active", false)) {
        return WearState(active = false, nextDay = map.getInt("next_day", 0))
    }
    return WearState(
        active = true,
        phase = map.getString("phase", ""),
        exercise = map.getString("exercise", ""),
        setNumber = map.getInt("set", 0),
        totalSets = map.getInt("total", 0),
        primaryLabel = map.getString("primary", ""),
        canSwap = map.getBoolean("can_swap", false),
        swapLabel = map.getString("swap_label", "Máquina ocupada"),
        paused = map.getBoolean("paused", false),
        endTime = map.getLong("end_time", 0L)
    )
}

/** Envía una orden a TODOS los nodos conectados (el móvil emparejado). */
fun sendCommand(context: Context, command: String) {
    val messageClient = Wearable.getMessageClient(context)
    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
        for (node in nodes) {
            messageClient.sendMessage(node.id, PATH_COMMAND, command.toByteArray())
        }
    }
}
