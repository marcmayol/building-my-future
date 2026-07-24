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

    // ------------------------------ Altura y Postura (aviso DIARIO, max 7/semana)

    private val alturaId = "altura_postura"
    private val freqAltura = FrecuenciaSemanal(min = 5, max = 7)

    private fun altura(date: LocalDate) = SessionRecord(
        dayNumber = 0,
        startMillis = millisAt(date),
        endMillis = millisAt(date),
        totalSets = 5,
        totalRestSeconds = 0,
        routineId = alturaId,
        routineCompleted = true
    )

    private fun alturaStatus(history: List<SessionRecord>, refDay: LocalDate) =
        WeeklyFrequency.routineStatus(
            history, alturaId, freqAltura, daily = true, dailyMax = 1, today = refDay, zone = zone
        )

    @Test fun `altura no avisa haciendola en dias distintos de la misma semana`() {
        // Lun..Sab hechas; el domingo (7º día, aún sin hacer) no debe avisar: contador diario 0.
        val history = (0..5).map { altura(monday.plusDays(it.toLong())) }
        val sunday = monday.plusDays(6)
        val status = alturaStatus(history, sunday)
        assertEquals(6, WeeklyFrequency.routineSessionsThisWeek(history, alturaId, sunday, zone))
        assertEquals(0, status.count)      // hoy (domingo) aún no se ha hecho
        assertFalse(status.reached)
    }

    @Test fun `altura avisa en la segunda vez del mismo dia`() {
        // Ya hecha una vez hoy: al ir a repetirla el mismo día, el aviso salta (max diario 1).
        val history = listOf(altura(today))
        val status = alturaStatus(history, today)
        assertEquals(1, status.count)
        assertTrue(status.reached)
    }

    @Test fun `altura no avisa el primer intento del dia aunque la semana este llena`() {
        // 7 sesiones en días distintos (semana "llena"), pero hoy es un día nuevo sin sesión.
        val history = (0..6).map { altura(monday.plusDays(it.toLong())) }
        val nextMonday = monday.plusWeeks(1)
        val status = alturaStatus(history, nextMonday)
        assertEquals(0, status.count)
        assertFalse(status.reached)
    }

    @Test fun `altura solo cuenta sesiones completadas`() {
        val incompleta = altura(today).copy(routineCompleted = false)
        val status = alturaStatus(listOf(incompleta), today)
        assertEquals(0, status.count)
        assertFalse(status.reached)
    }

    @Test fun `militar y altura no se mezclan entre si`() {
        // Una militar y una altura, ambas hoy: cada rutina cuenta solo la suya.
        val history = listOf(militar(today, completed = true), altura(today))
        assertEquals(1, WeeklyFrequency.militarySessionsThisWeek(history, today, zone))
        assertEquals(1, WeeklyFrequency.routineSessionsToday(history, alturaId, today, zone))
        assertEquals(1, WeeklyFrequency.routineSessionsToday(history, "militar_basica", today, zone))
        // La altura (diaria) no se ve afectada por la militar.
        assertEquals(1, alturaStatus(history, today).count)
    }
}
