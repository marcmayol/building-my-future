package com.marc.gymplan100.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.marc.gymplan100.MainActivity
import com.marc.gymplan100.R
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.FreeSessionEngine
import com.marc.gymplan100.data.FreeStep
import com.marc.gymplan100.data.ProgressRepository
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.wear.WearBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Programa una alarma del sistema para avisar (sonido propio llamativo + vibración + notificación)
 * cuando termina el descanso, incluso con la pantalla apagada, en segundo plano o con música.
 *
 * Además, mientras una fase con temporizador está en marcha, muestra una notificación PERSISTENTE
 * con cuenta atrás visible en la pantalla de bloqueo y un botón "Saltar" que termina la fase sin
 * desbloquear. La cuenta atrás la dibuja el propio Android (chronometer countdown), por lo que
 * avanza sola con la app dormida y no hace falta un servicio en primer plano.
 */
object RestReminder {

    private const val CHANNEL_ID = "rest_alert"
    /** Canal silencioso (LOW) para la cuenta atrás persistente: visible pero sin sonar ni saltar. */
    private const val CHANNEL_TIMER = "rest_timer"
    private const val NOTIF_ID = 1001
    /** Notificación persistente de la cuenta atrás en curso (distinta del aviso de fin). */
    private const val NOTIF_ID_ONGOING = 1002
    private const val REQUEST_CODE = 2001
    private const val REQUEST_CODE_SKIP = 2002
    private const val REQUEST_CODE_OPEN = 2003
    const val EXTRA_KIND = "alert_kind"

    /** Tipo de aviso, para elegir el texto de la notificación. */
    const val KIND_BETWEEN_SETS = 0
    const val KIND_BETWEEN_EXERCISES = 1
    const val KIND_WARMUP = 2
    const val KIND_TIMED_SET = 3

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        // El canal antiguo sonaba como notificación normal; lo retiramos.
        runCatching { mgr.deleteNotificationChannel("rest_done") }
        // Los canales se vuelven a crear siempre: si ya existen, Android solo les cambia el
        // nombre y la descripción (sonido e importancia se quedan como estaban), y así siguen
        // el idioma elegido en la app.
        val textos = AppLanguage.wrap(context)
        run {
            val channel = NotificationChannel(
                CHANNEL_ID,
                textos.getString(R.string.ini_rest_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = textos.getString(R.string.ini_rest_channel_desc)
                // El sonido y la vibración los reproducimos nosotros (canal silencioso para no duplicar).
                setSound(null, null)
                enableVibration(false)
            }
            mgr.createNotificationChannel(channel)
        }
        run {
            val timer = NotificationChannel(
                CHANNEL_TIMER,
                textos.getString(R.string.ini_timer_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = textos.getString(R.string.ini_timer_channel_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(timer)
        }
    }

    /**
     * ¿Nos deja el sistema despertar a la hora exacta?
     *
     * Importa más de lo que parece: sin ese permiso el aviso del descanso se programa como
     * inexacto y Android puede retrasarlo varios minutos para agrupar trabajo, que en una app
     * cuyo trabajo es avisar a los noventa segundos lo estropea todo. Hasta ahora nunca fallaba
     * porque el manifiesto pedía USE_EXACT_ALARM, que se concede solo; la versión de Google
     * Play no puede pedirlo (está reservado a despertadores y calendarios), así que ahí sí
     * hace falta preguntar.
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Lleva al ajuste del sistema donde se concede. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            // Algunos fabricantes no traen esa pantalla: se abre la ficha de la app.
            runCatching {
                context.startActivity(
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + context.packageName)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun schedule(context: Context, triggerAtMillis: Long, kind: Int, dayNumber: Int) {
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmPendingIntent(context, kind)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
        // Muestra la cuenta atrás persistente hasta ese mismo instante.
        showOngoing(context, triggerAtMillis, kind, dayNumber)
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(alarmPendingIntent(context, KIND_BETWEEN_SETS))
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIF_ID)
        nm.cancel(NOTIF_ID_ONGOING)
    }

    /**
     * Decide, a partir del estado de la sesión, si debe haber una cuenta atrás en marcha
     * (la programa y la muestra) o no (la cancela). La usa SkipActionReceiver tras aplicar
     * una transición de fase desde la notificación.
     */
    fun syncForSession(context: Context, session: ActiveSession?) {
        if (session == null) {
            cancel(context)
            return
        }
        val now = System.currentTimeMillis()
        when (session.phase) {
            SessionPhase.WARMUP -> {
                if (session.warmupPaused) { cancel(context); return }
                val remaining = (session.warmupTargetSeconds - session.warmupElapsed(now)).coerceAtLeast(0)
                schedule(context, now + remaining * 1000L, KIND_WARMUP, session.dayNumber)
            }
            SessionPhase.TIMED_SET -> {
                if (session.timedPaused) { cancel(context); return }
                val remaining = (session.timedTargetSeconds - session.timedElapsed(now)).coerceAtLeast(0)
                schedule(context, now + remaining * 1000L, KIND_TIMED_SET, session.dayNumber)
            }
            SessionPhase.RESTING -> {
                val kind = if (session.restBetweenExercises) KIND_BETWEEN_EXERCISES else KIND_BETWEEN_SETS
                schedule(context, session.restStartMillis + session.restTargetSeconds * 1000L, kind, session.dayNumber)
            }
            // El libre guiado calienta y descansa igual que el del plan, con sus mismos avisos.
            SessionPhase.FREE -> when (session.freeStep) {
                FreeStep.WARMUP -> schedule(
                    context, session.warmupStartMillis + session.warmupTargetSeconds * 1000L,
                    KIND_WARMUP, session.dayNumber
                )
                FreeStep.REST -> {
                    val kind = if (session.restBetweenExercises) KIND_BETWEEN_EXERCISES else KIND_BETWEEN_SETS
                    schedule(context, session.restStartMillis + session.restTargetSeconds * 1000L, kind, session.dayNumber)
                }
                FreeStep.WORK -> cancel(context)
            }
            else -> cancel(context)
        }
    }

    private fun alarmPendingIntent(context: Context, kind: Int): PendingIntent {
        val intent = Intent(context, RestAlarmReceiver::class.java).apply {
            putExtra(EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Notificación persistente con la cuenta atrás (la dibuja Android) hasta [endTimeMillis],
     * visible en la pantalla de bloqueo, con un botón para terminar la fase y abriendo la sesión
     * al tocar el cuerpo.
     */
    private fun showOngoing(context: Context, endTimeMillis: Long, kind: Int, dayNumber: Int) {
        ensureChannel(context)
        val textos = AppLanguage.wrap(context)
        val title = textos.getString(
            when (kind) {
                KIND_WARMUP -> R.string.ini_rest_ongoing_warmup
                KIND_TIMED_SET -> R.string.ini_rest_ongoing_timed
                KIND_BETWEEN_EXERCISES -> R.string.ini_rest_ongoing_exercises
                else -> R.string.ini_rest_ongoing_sets
            }
        )
        val skipLabel = textos.getString(
            when (kind) {
                KIND_WARMUP -> R.string.ini_action_start_now
                KIND_TIMED_SET -> R.string.ini_action_set_done
                else -> R.string.ini_action_skip_rest
            }
        )
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_SESSION_DAY, dayNumber)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipPi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SKIP,
            Intent(context, SkipActionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(textos.getString(R.string.ini_rest_ongoing_text))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endTimeMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPi)
            .addAction(0, skipLabel, skipPi)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIF_ID_ONGOING, notif)
    }

    fun showRestDone(context: Context, kind: Int) {
        ensureChannel(context)
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val textos = AppLanguage.wrap(context)
        val title = textos.getString(
            when (kind) {
                KIND_WARMUP -> R.string.ini_rest_done_warmup
                KIND_TIMED_SET -> R.string.ini_rest_done_timed
                else -> R.string.ini_rest_done_rest
            }
        )
        val text = textos.getString(
            when (kind) {
                KIND_WARMUP -> R.string.ini_rest_done_text_warmup
                KIND_TIMED_SET -> R.string.ini_rest_done_text_timed
                KIND_BETWEEN_EXERCISES -> R.string.ini_rest_done_text_exercises
                else -> R.string.ini_rest_done_text_sets
            }
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        // La fase con temporizador terminó: retira la cuenta atrás persistente y lanza el aviso de fin.
        nm.cancel(NOTIF_ID_ONGOING)
        nm.notify(NOTIF_ID, notif)
    }

    /**
     * Reproduce el aviso sonoro (por el canal de alarma, bajando la música un instante) y vibra.
     * Llama a [onDone] cuando termina (para soltar el BroadcastReceiver).
     */
    fun playAlert(context: Context, onDone: () -> Unit) {
        vibrate(context)
        val am = context.getSystemService(AudioManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build()
        runCatching { am.requestAudioFocus(focusRequest) }

        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                runCatching { am.abandonAudioFocusRequest(focusRequest) }
                onDone()
            }
        }

        runCatching {
            val afd = context.resources.openRawResourceFd(R.raw.rest_done)
            val mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
            afd.close()
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                runCatching { it.release() }
                finish()
            }
            mp.setOnErrorListener { p, _, _ ->
                runCatching { p.release() }
                finish()
                true
            }
            mp.prepareAsync()
        }.onFailure { finish() }
    }

    private fun vibrate(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 250, 130, 250, 130, 500)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}

/** Recibe la alarma del descanso y lanza el aviso (sonido + vibración + notificación). */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val kind = intent.getIntExtra(RestReminder.EXTRA_KIND, RestReminder.KIND_BETWEEN_SETS)
        RestReminder.showRestDone(context, kind)
        // El móvil suele estar en el bolsillo o en el banco: que también avise la muñeca.
        WearBridge.sendAlert(context, kind)
        RestReminder.playAlert(context) { runCatching { pending.finish() } }
    }
}

/**
 * Recibe el toque del botón "Saltar" de la cuenta atrás persistente: lee la sesión activa,
 * termina la fase actual (calentamiento / serie por tiempo / descanso) y reprograma o retira
 * la notificación según la nueva fase. Todo en segundo plano, sin abrir la app.
 */
class SkipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ProgressRepository(appContext)
                val session = repo.activeSession.first()
                if (session != null) {
                    val now = System.currentTimeMillis()
                    val next = if (session.isRoutine) {
                        // Rutina especial: la transición la calcula su propio motor puro.
                        val data = com.marc.gymplan100.data.SpecialWorkoutsLoader.load(appContext)
                        com.marc.gymplan100.data.SpecialSessionEngine.skipFromNotification(data, session, now)
                    } else when (session.phase) {
                        SessionPhase.WARMUP -> SessionEngine.endWarmup(session)
                        SessionPhase.TIMED_SET -> SessionEngine.completeSet(
                            session, "", now, yourRests = repo.progress.first().yourRests,
                            restBase = repo.profile.first().restBase
                        )
                        SessionPhase.RESTING -> SessionEngine.endRest(session, now)
                        // El libre: «Empezar ya» acaba el calentamiento y «Saltar descanso», el descanso.
                        SessionPhase.FREE -> when (session.freeStep) {
                            FreeStep.WARMUP -> FreeSessionEngine.endWarmup(session)
                            FreeStep.REST -> FreeSessionEngine.endRest(session)
                            FreeStep.WORK -> session
                        }
                        else -> session
                    }
                    if (next !== session) {
                        repo.saveActiveSession(next)
                        RestReminder.syncForSession(appContext, next)
                    }
                }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }
}
