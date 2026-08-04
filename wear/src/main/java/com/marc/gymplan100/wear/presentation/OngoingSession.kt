package com.marc.gymplan100.wear.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.marc.gymplan100.wear.R

/**
 * Chip de "entrenamiento en curso" en la esfera del reloj (Ongoing Activity).
 *
 * Problema que resuelve: en el gimnasio, si la app del reloj se va a segundo plano (o el reloj
 * vuelve al watch face), había que buscarla en la lista de apps para volver a la sesión. Con la
 * Ongoing Activity, Wear OS pinta un icono en la propia esfera mientras haya sesión: un toque y
 * estás otra vez en los botones.
 *
 * Es una notificación *ongoing* corriente a la que se le "aplica" una [OngoingActivity]; no hace
 * falta un Foreground Service porque el reloj no calcula nada — el móvil sigue siendo el cerebro
 * y solo empuja estado. Quien la mantiene al día es [StateListenerService], que el sistema
 * despierta en cada cambio aunque la app del reloj esté cerrada.
 */
object OngoingSession {

    private const val CHANNEL_ID = "sesion_en_curso"
    private const val NOTIFICATION_ID = 4001

    /** Refleja [state] en la esfera: crea o actualiza el chip, y lo retira si ya no hay sesión. */
    fun update(context: Context, state: WearState) {
        if (!state.active) {
            clear(context)
            return
        }

        crearCanal(context)

        val abrir = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = state.exercise.ifBlank { "Entrenamiento" }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ongoing)
            .setContentTitle(titulo)
            .setContentText(textoDeApoyo(state))
            .setContentIntent(abrir)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_ongoing)
            .setTouchIntent(abrir)
            .setStatus(estado(state))
            .build()
            .apply(context)

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    /** Retira el chip de la esfera (fin de la sesión o app sin sesión activa). */
    fun clear(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    /**
     * Lo que se lee en el chip. Con cuenta atrás en marcha la dibuja el propio sistema
     * ([Status.TimerPart]), así que corre al segundo sin que la app esté despierta.
     */
    private fun estado(state: WearState): Status {
        val builder = Status.Builder()
        val corriendo = state.endTime > 0L && !state.paused
        if (corriendo) {
            // Dos plantillas: la larga si cabe, y el reloj solo cuando el espacio es justo.
            builder.addTemplate("#ejercicio# · #cuenta#")
            builder.addTemplate("#cuenta#")
            builder.addPart("cuenta", Status.TimerPart(state.endTime))
        } else {
            builder.addTemplate("#ejercicio# · #serie#")
            builder.addTemplate("#ejercicio#")
            builder.addPart("serie", Status.TextPart(textoDeApoyo(state)))
        }
        builder.addPart("ejercicio", Status.TextPart(state.exercise.ifBlank { "Entrenamiento" }))
        return builder.build()
    }

    /** Serie en curso, o el aviso de pausa si el temporizador está parado. */
    private fun textoDeApoyo(state: WearState): String = when {
        state.paused -> "En pausa"
        state.totalSets > 0 -> "Serie ${state.setNumber} de ${state.totalSets}"
        else -> "En curso"
    }

    private fun crearCanal(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val canal = NotificationChannel(
            CHANNEL_ID,
            "Entrenamiento en curso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Acceso rápido a la sesión desde la esfera del reloj."
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(canal)
    }
}
