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
 *  - Militar: cuenta por rutina COMPLETA (solo sesiones que llegaron al último paso).
 *  - Quema grasa: cuenta por ejercicio individual.
 *  - El aviso nunca bloquea: solo informa si el contador ya alcanzó [FrecuenciaSemanal.max].
 */
object WeeklyFrequency {

    /** Resultado de comprobar la frecuencia de una rutina o ejercicio esta semana. */
    data class Status(
        val count: Int,
        val max: Int,
        /** Ya se alcanzó (o superó) la recomendación semanal: procede mostrar el aviso. */
        val reached: Boolean
    )

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** ¿La sesión cae en la misma semana natural que [today]? */
    private fun inCurrentWeek(record: SessionRecord, today: LocalDate, zone: ZoneId): Boolean =
        Statistics.weekStart(dateOf(record.endMillis, zone)) == Statistics.weekStart(today)

    /** Sesiones de la Rutina Militar COMPLETADAS esta semana. */
    fun militarySessionsThisWeek(
        history: List<SessionRecord>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = history.count {
        it.routineId == SpecialWorkoutsLoader.MILITAR_ID &&
            it.routineCompleted &&
            inCurrentWeek(it, today, zone)
    }

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
