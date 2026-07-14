package com.marc.gymplan100.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.marc.gymplan100.data.ProgressRepository
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.notify.RestReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Recibe las órdenes que el reloj (Pixel Watch) envía a [WearBridge.PATH_COMMAND] y las aplica
 * sobre la sesión en curso, aunque la app del móvil esté cerrada (el sistema arranca este
 * servicio al llegar el mensaje).
 *
 * Replica el patrón de SkipActionReceiver: lee la sesión activa del repositorio, aplica la
 * transición pura de SessionEngine, la guarda y reprograma la alarma/notificación. Después
 * vuelve a publicar el estado para que el reloj se refresque.
 */
class WearCommandListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearBridge.PATH_COMMAND) return
        val command = String(event.data)
        val appContext = applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            val repo = ProgressRepository(appContext)
            val now = System.currentTimeMillis()

            // Empezar entreno desde el reloj: crea la sesión del siguiente día pendiente. Marca
            // watchControlled para que, al finalizar, el móvil no estime calorías (las pone el pulso).
            if (command == WearBridge.CMD_START) {
                if (repo.activeSession.first() != null) return@launch // ya hay una en curso
                val day = SessionEngine.nextDay(repo.progress.first().completedDays)
                val session = SessionEngine.startSession(day, now, watchControlled = true)
                repo.saveActiveSession(session)
                RestReminder.syncForSession(appContext, session)
                WearBridge.publishState(appContext, session)
                return@launch
            }

            val session = repo.activeSession.first() ?: run {
                // Sin sesión: refresca el reloj con el siguiente día pendiente por si se desincronizó.
                WearBridge.publishState(
                    appContext, null,
                    nextDay = SessionEngine.nextDay(repo.progress.first().completedDays)
                )
                return@launch
            }

            val next = when (command) {
                WearBridge.CMD_PRIMARY -> when (session.phase) {
                    // Peso vacío: desde el reloj solo se marca "serie hecha"; el peso se anota
                    // en el móvil. El móvil conserva igualmente los pesos de referencia.
                    SessionPhase.WARMUP -> SessionEngine.endWarmup(session)
                    SessionPhase.WORKING -> SessionEngine.completeSet(session, "", now)
                    SessionPhase.TIMED_SET -> SessionEngine.completeSet(session, "", now)
                    SessionPhase.RESTING -> SessionEngine.endRest(session, now)
                    else -> session
                }
                WearBridge.CMD_SWAP -> SessionEngine.skipExercise(session)
                else -> session
            }

            // Cualquier orden desde el reloj confirma que se está usando: marca la sesión para
            // que el cálculo de calorías al finalizar sea automático (sin estimación).
            val marked = if (next.watchControlled) next else next.copy(watchControlled = true)

            if (marked !== session) {
                repo.saveActiveSession(marked)
                RestReminder.syncForSession(appContext, marked)
            }
            // Refresca el reloj con el estado resultante (haya cambiado o no).
            WearBridge.publishState(appContext, marked)
        }
    }
}
