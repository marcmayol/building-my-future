package com.marc.gymplan100.plataforma

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.PlatformContext
import com.marc.gymplan100.shared.*
import kotlinx.datetime.LocalDate

/*
 * Los nombres de siempre (RestReminder, WearBridge, TrainingReminder) para la lógica común:
 * reenvían a [plataforma]. Así el ViewModel y las pantallas apenas cambian.
 */

object RestReminder {
    const val KIND_BETWEEN_SETS = 0
    const val KIND_BETWEEN_EXERCISES = 1
    const val KIND_WARMUP = 2
    const val KIND_TIMED_SET = 3

    fun schedule(context: PlatformContext, triggerAtMillis: Long, kind: Int, dayNumber: Int) =
        plataforma.programarDescanso(triggerAtMillis, kind, dayNumber)

    fun cancel(context: PlatformContext) = plataforma.cancelarDescanso()

    fun syncForSession(context: PlatformContext, session: ActiveSession?) =
        plataforma.sincronizarDescanso(session)

    fun canScheduleExact(context: PlatformContext): Boolean = plataforma.puedeAvisarExacto()

    fun openExactAlarmSettings(context: PlatformContext) = plataforma.abrirAjustesAvisoExacto()
}

object WearBridge {
    fun publishState(context: PlatformContext, session: ActiveSession?, nextDay: Int = 0) =
        plataforma.publicarEnReloj(session, nextDay)
}

object TrainingReminder {
    val DEFAULT_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5)
    const val DEFAULT_MINUTE = 18 * 60

    fun isEnabled(context: PlatformContext): Boolean = plataforma.recordatorio.activo()
    fun days(context: PlatformContext): Set<Int> = plataforma.recordatorio.dias()
    fun minuteOfDay(context: PlatformContext): Int = plataforma.recordatorio.minutoDelDia()
    fun pausedUntil(context: PlatformContext): LocalDate? = plataforma.recordatorio.pausaHasta()
    fun save(context: PlatformContext, enabled: Boolean, days: Set<Int>, minuteOfDay: Int) =
        plataforma.recordatorio.guardar(enabled, days, minuteOfDay)
    fun setPause(context: PlatformContext, until: LocalDate?) = plataforma.recordatorio.pausar(until)

    fun formatTime(minuteOfDay: Int): String =
        "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"
}

/** Días de la semana (1 = lunes, ISO) con su inicial, para el selector del recordatorio. */
val WEEKDAY_LABELS = listOf(
    1 to Res.string.ini_weekday_mon,
    2 to Res.string.ini_weekday_tue,
    3 to Res.string.ini_weekday_wed,
    4 to Res.string.ini_weekday_thu,
    5 to Res.string.ini_weekday_fri,
    6 to Res.string.ini_weekday_sat,
    7 to Res.string.ini_weekday_sun,
)
