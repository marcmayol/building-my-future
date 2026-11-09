package com.marc.gymplan100.notify

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marc.gymplan100.MainActivity
import com.marc.gymplan100.R
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.data.ProgressRepository
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.ui.theme.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Avisos de "hoy toca gimnasio".
 *
 * El plan va por días numerados y no por fechas, y así se queda: el día 47 es el día 47 aunque
 * lo hagas un martes o un sábado. Lo que faltaba no era un calendario, era que la app supiera
 * **cuándo sueles ir** y te lo recordara. Con esto, "100 días" pasa a ser "100 días de
 * entrenamiento" repartidos como tú entrenes, sin que el plan dependa del calendario.
 *
 * No avisa si ya has entrenado hoy: el recordatorio es para que no se te pase, no para dar la
 * lata cuando vienes de la ducha.
 */
object TrainingReminder {

    private const val PREFS = "gym_recordatorios"
    private const val KEY_ON = "activo"
    private const val KEY_DAYS = "dias"
    private const val KEY_MINUTE = "minuto"
    private const val KEY_PAUSE = "pausa_hasta"
    private const val CHANNEL_ID = "training_reminder"
    private const val NOTIF_ID = 5100
    private const val REQUEST = 5101

    /** Por defecto, de lunes a viernes a las 18:00, que es el plan de serie. */
    val DEFAULT_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5)
    const val DEFAULT_MINUTE = 18 * 60

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** ¿Están puestos los avisos? */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ON, false)

    /** Días de la semana elegidos (1 = lunes … 7 = domingo). */
    fun days(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_DAYS, null)
            ?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: DEFAULT_DAYS

    /** Hora del aviso, en minutos desde medianoche. */
    fun minuteOfDay(context: Context): Int = prefs(context).getInt(KEY_MINUTE, DEFAULT_MINUTE)

    /** "18:30", para enseñarlo. */
    fun formatTime(minuteOfDay: Int): String =
        "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    /** Guarda la configuración y deja programado (o quitado) el próximo aviso. */
    fun save(context: Context, enabled: Boolean, days: Set<Int>, minuteOfDay: Int) {
        prefs(context).edit()
            .putBoolean(KEY_ON, enabled)
            .putStringSet(KEY_DAYS, days.map { it.toString() }.toSet())
            .putInt(KEY_MINUTE, minuteOfDay)
            .apply {
                // Apagar el recordatorio también quita la pausa: al volver a encenderlo, que
                // avise como siempre y no arrastre unas vacaciones de hace meses.
                if (!enabled) remove(KEY_PAUSE)
            }
            .apply()
        reschedule(context)
    }

    /**
     * Último día en silencio de la pausa («Pausar hasta el…»), o null si no hay pausa.
     *
     * Es para las vacaciones: apagar el aviso ya se podía, pero luego se olvida encenderlo y la
     * app se queda muda justo al volver a la rutina. Con fecha de vuelta no hay que acordarse
     * de nada. Una pausa ya pasada se borra aquí mismo y no vuelve a salir.
     */
    fun pausedUntil(context: Context, today: LocalDate = LocalDate.now()): LocalDate? {
        val guardada = prefs(context).getString(KEY_PAUSE, null) ?: return null
        val fecha = runCatching { LocalDate.parse(guardada) }.getOrNull()
        if (fecha == null || fecha.isBefore(today)) {
            prefs(context).edit().remove(KEY_PAUSE).apply()
            return null
        }
        return fecha
    }

    /** Pausa los avisos hasta [until] incluido (null la quita) y reprograma el próximo. */
    fun setPause(context: Context, until: LocalDate?) {
        prefs(context).edit().apply {
            if (until == null) remove(KEY_PAUSE) else putString(KEY_PAUSE, until.toString())
        }.apply()
        reschedule(context)
    }

    /** ¿Hoy cae dentro de la pausa? */
    fun isPausedOn(pausedUntil: LocalDate?, day: LocalDate): Boolean =
        pausedUntil != null && !day.isAfter(pausedUntil)

    /**
     * Cuándo cae el próximo aviso, o null si no hay ninguno que programar.
     *
     * Se busca el siguiente de los días elegidos a partir de [from]; si hoy es uno de ellos pero
     * la hora ya pasó, se salta a la semana que viene. Con una pausa vigente ([pausedUntil] es
     * su último día en silencio) la búsqueda empieza el día de vuelta, a cualquier hora: así no
     * queda ninguna alarma programada en mitad de las vacaciones. Una pausa ya pasada no cuenta.
     * Función pura para poder probarla.
     */
    fun nextTrigger(
        from: LocalDateTime,
        days: Set<Int>,
        minuteOfDay: Int,
        pausedUntil: LocalDate? = null,
    ): LocalDateTime? {
        if (days.isEmpty()) return null
        val hora = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val enPausa = isPausedOn(pausedUntil, from.toLocalDate())
        val desde = if (enPausa) pausedUntil!!.plusDays(1) else from.toLocalDate()
        for (salto in 0..7) {
            val fecha = desde.plusDays(salto.toLong())
            if (fecha.dayOfWeek.value !in days) continue
            val cuando = LocalDateTime.of(fecha, hora)
            if (enPausa || cuando.isAfter(from)) return cuando
        }
        return null
    }

    /** El aviso en milisegundos, en la zona del móvil (con su cambio de hora). */
    fun triggerMillis(trigger: LocalDateTime, zone: ZoneId): Long =
        trigger.atZone(zone).toInstant().toEpochMilli()

    /** Reprograma el próximo aviso: al cambiar los ajustes, al terminar un entreno y al arrancar. */
    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST,
            Intent(context, TrainingAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (!isEnabled(context)) return
        val proximo = nextTrigger(
            LocalDateTime.now(), days(context), minuteOfDay(context), pausedUntil(context)
        ) ?: return
        val millis = triggerMillis(proximo, ZoneId.systemDefault())
        // Un recordatorio no necesita ser exacto al segundo: `setWindow` deja que el sistema lo
        // agrupe con otros y no gasta el permiso de alarmas exactas, que aqui no hace falta.
        am.setWindow(AlarmManager.RTC_WAKEUP, millis, 15 * 60 * 1000L, pi)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        // Se crea siempre, aunque ya exista: con un canal existente Android solo actualiza el
        // nombre y la descripción, y así siguen el idioma elegido en la app.
        val textos = AppLanguage.wrap(context)
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                textos.getString(R.string.ini_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = textos.getString(R.string.ini_reminder_channel_desc)
            }
        )
    }

    /** Enseña el aviso de hoy con el día que toca. */
    fun notifyToday(context: Context, dayNumber: Int, title: String) {
        ensureChannel(context)
        val abrir = PendingIntent.getActivity(
            context, REQUEST,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // El contexto del receiver no sabe el idioma elegido en la app: se le pide a AppLanguage.
        val textos = AppLanguage.wrap(context)
        val cuerpo = textos.getString(R.string.ini_day_n_title, dayNumber, title)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(textos.getString(R.string.ini_reminder_title))
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        }
    }
}

/**
 * Salta a la hora elegida: mira si hoy ya se ha entrenado y, si no, avisa. Luego deja
 * programado el aviso de la próxima vez.
 */
class TrainingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // El proceso puede arrancar aquí: el plan activo se carga a mano.
                PlanStore.load(app)
                val repo = ProgressRepository(app)
                val zona = ZoneId.systemDefault()
                val hoy = LocalDate.now(zona)
                // Una alarma puesta antes de pausar puede saltar igual: en pausa no se avisa.
                val enPausa = TrainingReminder.isPausedOn(TrainingReminder.pausedUntil(app, hoy), hoy)
                val yaEntrenado = repo.history.first().any {
                    Instant.ofEpochMilli(it.endMillis).atZone(zona).toLocalDate() == hoy
                }
                if (!enPausa && !yaEntrenado) {
                    val dia = SessionEngine.nextDay(repo.progress.first().completedDays)
                    val titulo = PlanData.dayByNumber(dia)?.template?.title.orEmpty()
                    TrainingReminder.notifyToday(app, dia, titulo)
                }
                TrainingReminder.reschedule(app)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }
}

/** Las alarmas no sobreviven a un reinicio: se vuelven a poner al encender el móvil. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        TrainingReminder.reschedule(context.applicationContext)
    }
}

/** Lunes a domingo, para pintar los chips: el número del día y el recurso de su inicial. */
val WEEKDAY_LABELS: List<Pair<Int, Int>> = DayOfWeek.entries.map { d ->
    d.value to when (d) {
        DayOfWeek.MONDAY -> R.string.ini_weekday_mon
        DayOfWeek.TUESDAY -> R.string.ini_weekday_tue
        DayOfWeek.WEDNESDAY -> R.string.ini_weekday_wed
        DayOfWeek.THURSDAY -> R.string.ini_weekday_thu
        DayOfWeek.FRIDAY -> R.string.ini_weekday_fri
        DayOfWeek.SATURDAY -> R.string.ini_weekday_sat
        DayOfWeek.SUNDAY -> R.string.ini_weekday_sun
    }
}
