package com.marc.gymplan100.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Conteo de sesiones registradas en la semana natural en curso (lunes-domingo, zona del
 * dispositivo) para el sistema de avisos de frecuencia de los entrenamientos especiales.
 *
 * Todo son funciones puras (se inyectan [today] y [zone]) para poder testearlas de forma
 * determinista. Reutiliza [Statistics.weekStart] para el lunes de la semana.
 *
 * Reglas (de `entrenamientos_especiales.json`):
 *  - Rutinas de secuencia fija (Militar, Altura y Postura): cuentan por rutina COMPLETA (solo
 *    sesiones que llegaron al último paso, marcadas con `routineCompleted`).
 *  - Militar: aviso semanal (frecuencia_semanal.max). Altura y Postura: aviso DIARIO
 *    (periodicidad_aviso="diaria", frecuencia_diaria.max) porque está pensada para hacerse a diario.
 *  - Quema grasa: cuenta por ejercicio individual (semanal).
 *  - El aviso nunca bloquea: solo informa si el contador ya alcanzó el máximo aplicable.
 */
object WeeklyFrequency {

    /** Resultado de comprobar la frecuencia de una rutina o ejercicio. */
    data class Status(
        val count: Int,
        val max: Int,
        /** Ya se alcanzó (o superó) la recomendación: procede mostrar el aviso. */
        val reached: Boolean
    )

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** ¿La sesión cae en la misma semana natural que [today]? */
    private fun inCurrentWeek(record: SessionRecord, today: LocalDate, zone: ZoneId): Boolean =
        Statistics.weekStart(dateOf(record.endMillis, zone)) == Statistics.weekStart(today)

    /** Sesiones COMPLETADAS de una rutina (secuencia fija) esta semana. */
    fun routineSessionsThisWeek(
        history: List<SessionRecord>,
        routineId: String,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = history.count {
        it.routineId == routineId && it.routineCompleted && inCurrentWeek(it, today, zone)
    }

    /** Sesiones COMPLETADAS de una rutina (secuencia fija) HOY. */
    fun routineSessionsToday(
        history: List<SessionRecord>,
        routineId: String,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = history.count {
        it.routineId == routineId && it.routineCompleted && dateOf(it.endMillis, zone) == today
    }

    /**
     * Estado de frecuencia de una rutina de secuencia fija. Si [daily] es true el contador es por
     * DÍA (frente a [dailyMax]); si no, por semana natural (frente a [frecuencia].max). El máximo
     * se lee siempre del JSON, nunca se asume.
     */
    fun routineStatus(
        history: List<SessionRecord>,
        routineId: String,
        frecuencia: FrecuenciaSemanal,
        daily: Boolean = false,
        dailyMax: Int = 0,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Status {
        return if (daily) {
            val count = routineSessionsToday(history, routineId, today, zone)
            Status(count, dailyMax, dailyMax > 0 && count >= dailyMax)
        } else {
            val count = routineSessionsThisWeek(history, routineId, today, zone)
            Status(count, frecuencia.max, frecuencia.max > 0 && count >= frecuencia.max)
        }
    }

    /** Sesiones de la Rutina Militar COMPLETADAS esta semana. */
    fun militarySessionsThisWeek(
        history: List<SessionRecord>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = routineSessionsThisWeek(history, SpecialWorkoutsLoader.MILITAR_ID, today, zone)

    /** Sesiones registradas esta semana de un ejercicio concreto de quema grasa. */
    fun exerciseSessionsThisWeek(
        history: List<SessionRecord>,
        exerciseId: String,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = history.count {
        it.exerciseId == exerciseId && inCurrentWeek(it, today, zone)
    }

    /** Estado de frecuencia de la Rutina Militar (para decidir si avisar antes de empezar). */
    fun militaryStatus(
        history: List<SessionRecord>,
        frecuencia: FrecuenciaSemanal,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Status {
        val count = militarySessionsThisWeek(history, today, zone)
        return Status(count, frecuencia.max, frecuencia.max > 0 && count >= frecuencia.max)
    }

    /** Estado de frecuencia de un ejercicio de quema grasa. */
    fun exerciseStatus(
        history: List<SessionRecord>,
        exercise: EjercicioCatalogo,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Status {
        val count = exerciseSessionsThisWeek(history, exercise.id, today, zone)
        val max = exercise.frecuencia_semanal.max
        return Status(count, max, max > 0 && count >= max)
    }
}
