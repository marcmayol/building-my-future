package com.marc.gymplan100.wear.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Recibe del móvil el aviso de "fase terminada" y lo convierte en vibración en la muñeca.
 *
 * Es un servicio y no parte de la pantalla a propósito: en el gimnasio la app del reloj suele
 * estar en ambiente o cerrada, y el sistema despierta este listener igualmente. Solo vibra —
 * el sonido, la notificación y la lógica siguen viviendo en el móvil.
 */
class AlertListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PATH_ALERT) return
        val kind = event.data.firstOrNull()?.toInt() ?: KIND_BETWEEN_SETS
        vibrate(this, patternFor(kind))
    }
}

/**
 * Patrón por tipo de aviso, para distinguirlos sin mirar el reloj: el fin de un descanso
 * (vuelves a la máquina) llama más que el fin de una serie por tiempo.
 */
internal fun patternFor(kind: Int): LongArray = when (kind) {
    KIND_WARMUP -> longArrayOf(0, 300, 150, 300)
    KIND_TIMED_SET -> longArrayOf(0, 500)
    KIND_BETWEEN_EXERCISES -> longArrayOf(0, 400, 200, 400, 200, 600)
    else -> longArrayOf(0, 250, 130, 250, 130, 500)
}

private fun vibrate(context: Context, pattern: LongArray) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
