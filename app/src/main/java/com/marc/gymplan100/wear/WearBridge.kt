package com.marc.gymplan100.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.setCountFromScheme

/**
 * Puente con el reloj (Wear OS / Google Pixel Watch).
 *
 * El móvil es el "cerebro": mantiene la lógica de la sesión, los temporizadores y las
 * notificaciones. El reloj es un "mando": muestra el ejercicio/serie actual y envía órdenes.
 *
 * - Móvil -> reloj: [publishState] escribe en la Data Layer un resumen de la sesión en curso
 *   (ya cocinado: nombre del ejercicio, nº de serie, etiqueta del botón, fin de la cuenta atrás).
 *   El reloj solo tiene que pintarlo, sin conocer el plan ni la lógica.
 * - Reloj -> móvil: el reloj envía un mensaje a [PATH_COMMAND] con uno de los [CMD_*]. Lo recibe
 *   [WearCommandListenerService], que aplica la transición con SessionEngine (igual que el botón
 *   "Saltar" de la notificación), aunque la app esté cerrada.
 *
 * Las constantes de paths/keys están duplicadas en el módulo :wear a propósito (no comparten
 * código). Si cambian aquí, hay que cambiarlas también allí.
 */
object WearBridge {

    const val PATH_STATE = "/gym/state"
    const val PATH_COMMAND = "/gym/command"

    // --- Comandos que el reloj puede enviar ---
    /** Acción principal contextual: terminar calentamiento / serie hecha / saltar descanso. */
    const val CMD_PRIMARY = "primary"
    /** Cambiar de ejercicio (máquina ocupada): pospone el actual al final de la cola. */
    const val CMD_SWAP = "swap"
    /** Empezar el entrenamiento del siguiente día pendiente (cuando no hay sesión en curso). */
    const val CMD_START = "start"

    // --- Claves del estado publicado ---
    const val KEY_ACTIVE = "active"          // Boolean: hay una sesión en curso
    const val KEY_PHASE = "phase"            // String: nombre de SessionPhase
    const val KEY_EXERCISE = "exercise"      // String: nombre del ejercicio actual
    const val KEY_SET_NUMBER = "set"         // Int: nº de serie actual
    const val KEY_TOTAL_SETS = "total"       // Int: series totales del ejercicio
    const val KEY_PRIMARY_LABEL = "primary"  // String: etiqueta del botón principal ("" si no hay)
    const val KEY_CAN_SWAP = "can_swap"      // Boolean: se puede cambiar de ejercicio ahora
    const val KEY_SWAP_LABEL = "swap_label"  // String: etiqueta del botón de cambio de ejercicio
    const val KEY_END_TIME = "end_time"      // Long: fin de la cuenta atrás (epoch ms), 0 si no aplica
    const val KEY_PAUSED = "paused"          // Boolean: temporizador en pausa
    const val KEY_DAY = "day"                // Int: día del plan
    const val KEY_NEXT_DAY = "next_day"      // Int: siguiente día pendiente (para el botón "Empezar" sin sesión)
    const val KEY_TS = "ts"                  // Long: marca de tiempo, fuerza la actualización del DataItem

    /** Etiqueta del botón principal del reloj para cada fase (vacía si no hay acción). */
    fun primaryLabel(phase: SessionPhase): String = when (phase) {
        SessionPhase.WARMUP -> "Empezar ya"
        SessionPhase.WORKING -> "Serie hecha"
        SessionPhase.TIMED_SET -> "Serie hecha"
        SessionPhase.RESTING -> "Saltar descanso"
        else -> ""
    }

    /**
     * Publica el estado actual de la sesión en la Data Layer para que lo lea el reloj.
     * Si [session] es null, marca que no hay sesión activa (el reloj muestra "sin entreno").
     */
    fun publishState(context: Context, session: ActiveSession?, nextDay: Int = 0) {
        val req = PutDataMapRequest.create(PATH_STATE)
        val map = req.dataMap
        map.putLong(KEY_TS, System.currentTimeMillis())

        if (session == null || session.phase == SessionPhase.FINISHED) {
            map.putBoolean(KEY_ACTIVE, false)
            // Sin sesión: el reloj muestra un botón para empezar el siguiente día pendiente.
            map.putInt(KEY_NEXT_DAY, nextDay)
        } else {
            val now = System.currentTimeMillis()
            val day = PlanData.dayByNumber(session.dayNumber)
            val exercise = day?.template?.exercises?.getOrNull(session.exerciseIndex)
            val total = exercise?.scheme?.let { setCountFromScheme(it) } ?: 0

            map.putBoolean(KEY_ACTIVE, true)
            map.putString(KEY_PHASE, session.phase.name)
            map.putString(KEY_EXERCISE, exercise?.name ?: "Entrenamiento")
            map.putInt(KEY_SET_NUMBER, session.setNumber)
            map.putInt(KEY_TOTAL_SETS, total)
            map.putString(KEY_PRIMARY_LABEL, primaryLabel(session.phase))
            map.putInt(KEY_DAY, session.dayNumber)
            map.putBoolean(KEY_CAN_SWAP, canSwap(session))
            map.putString(KEY_SWAP_LABEL, swapLabel(session))
            map.putBoolean(KEY_PAUSED, isPaused(session))
            map.putLong(KEY_END_TIME, endTimeMillis(session, now))
        }

        runCatching {
            Wearable.getDataClient(context).putDataItem(req.asPutDataRequest().setUrgent())
        }
    }

    /** Etiqueta del botón de cambio: tras un salto avisa de que se puede seguir saltando. */
    private fun swapLabel(s: ActiveSession): String =
        if (s.occupiedSkips > 0) "También ocupada" else "Máquina ocupada"

    private fun canSwap(s: ActiveSession): Boolean {
        if (s.phase != SessionPhase.WORKING) return false
        val order = s.order.ifEmpty {
            PlanData.dayByNumber(s.dayNumber)?.template?.exercises?.indices?.toList() ?: listOf(0)
        }
        val pos = order.indexOf(s.exerciseIndex)
        return pos in 0 until order.lastIndex
    }

    private fun isPaused(s: ActiveSession): Boolean = when (s.phase) {
        SessionPhase.WARMUP -> s.warmupPaused
        SessionPhase.TIMED_SET -> s.timedPaused
        else -> false
    }

    /** Instante (epoch ms) en que termina la cuenta atrás de la fase actual, o 0 si no aplica. */
    private fun endTimeMillis(s: ActiveSession, now: Long): Long = when (s.phase) {
        SessionPhase.WARMUP ->
            if (s.warmupPaused) 0L
            else now + (s.warmupTargetSeconds - s.warmupElapsed(now)).coerceAtLeast(0) * 1000L
        SessionPhase.TIMED_SET ->
            if (s.timedPaused) 0L
            else now + (s.timedTargetSeconds - s.timedElapsed(now)).coerceAtLeast(0) * 1000L
        SessionPhase.RESTING -> s.restStartMillis + s.restTargetSeconds * 1000L
        else -> 0L
    }
}
