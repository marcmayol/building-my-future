package com.marc.gymplan100

import com.marc.gymplan100.data.EjercicioCatalogo
import com.marc.gymplan100.data.FrecuenciaSemanal
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.Statistics
import com.marc.gymplan100.data.WeeklyFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Tests de la lógica de frecuencia semanal (funciones puras, semana lunes-domingo). */
class WeeklyFrequencyTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")
    // Miércoles arbitrario; la semana natural se deriva con Statistics.weekStart (lunes).
    private val today: LocalDate = LocalDate.of(2026, 7, 22)
    private val monday: LocalDate = Statistics.weekStart(today)       // lunes de esta semana
    private val lastSunday: LocalDate = monday.minusDays(1)           // domingo de la semana pasada

    private fun millisAt(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun militar(date: LocalDate, completed: Boolean) = SessionRecord(
        dayNumber = 0,
        startMillis = millisAt(date),
        endMillis = millisAt(date),
        totalSets = 13,
        totalRestSeconds = 0,
        routineId = "militar_basica",
        routineCompleted = completed
    )

    private fun quema(date: LocalDate, exerciseId: String) = SessionRecord(
        dayNumber = 0,
        startMillis = millisAt(date),
        endMillis = millisAt(date),
        totalSets = 1,
        totalRestSeconds = 0,
        routineId = "quema_grasa",
        exerciseId = exerciseId
    )

    private val freq3 = FrecuenciaSemanal(min = 3, max = 3)

    @Test fun `sesion de la semana pasada no cuenta`() {
        val history = listOf(militar(lastSunday, completed = true))
        val status = WeeklyFrequency.militaryStatus(history, freq3, today, zone)
        assertEquals(0, status.count)
        assertFalse(status.reached)
    }

    @Test fun `el domingo de esta semana si cuenta`() {
        val sundayThisWeek = monday.plusDays(6)
        val history = listOf(militar(sundayThisWeek, completed = true))
        val status = WeeklyFrequency.militaryStatus(history, freq3, sundayThisWeek, zone)
        assertEquals(1, status.count)
    }

    @Test fun `militar abandonada no cuenta para la frecuencia`() {
        val history = listOf(
            militar(monday, completed = true),
            militar(monday.plusDays(1), completed = false), // abandonada
            militar(monday.plusDays(2), completed = true)
        )
        val status = WeeklyFrequency.militaryStatus(history, freq3, today, zone)
        assertEquals(2, status.count)
        assertFalse(status.reached)
    }

    @Test fun `alcanzar el maximo dispara el aviso`() {
        val history = listOf(
            militar(monday, completed = true),
            militar(monday.plusDays(1), completed = true),
            militar(monday.plusDays(2), completed = true)
        )
        val status = WeeklyFrequency.militaryStatus(history, freq3, today, zone)
        assertEquals(3, status.count)
        assertTrue(status.reached)
    }

    @Test fun `por debajo del maximo no avisa`() {
        val history = listOf(militar(monday, completed = true), militar(monday.plusDays(1), completed = true))
        assertFalse(WeeklyFrequency.militaryStatus(history, freq3, today, zone).reached)
    }

    @Test fun `los ejercicios de quema grasa cuentan por separado`() {
        val burpees = EjercicioCatalogo(id = "burpees_hiit", frecuencia_semanal = FrecuenciaSemanal(1, 2))
        val cuerda = EjercicioCatalogo(id = "saltar_cuerda", frecuencia_semanal = FrecuenciaSemanal(2, 3))
        val history = listOf(
            quema(monday, "burpees_hiit"),
            quema(monday.plusDays(1), "burpees_hiit"),
            quema(monday.plusDays(2), "saltar_cuerda")
        )
        val burpeesStatus = WeeklyFrequency.exerciseStatus(history, burpees, today, zone)
        val cuerdaStatus = WeeklyFrequency.exerciseStatus(history, cuerda, today, zone)
        assertEquals(2, burpeesStatus.count)
        assertTrue(burpeesStatus.reached)      // 2/2
        assertEquals(1, cuerdaStatus.count)
        assertFalse(cuerdaStatus.reached)      // 1/3
    }

    @Test fun `la militar no cuenta como ejercicio de quema grasa`() {
        val burpees = EjercicioCatalogo(id = "burpees_hiit", frecuencia_semanal = FrecuenciaSemanal(1, 2))
        val history = listOf(militar(monday, completed = true))
        assertEquals(0, WeeklyFrequency.exerciseSessionsThisWeek(history, "burpees_hiit", today, zone))
        assertEquals(0, WeeklyFrequency.exerciseStatus(history, burpees, today, zone).count)
    }
}
