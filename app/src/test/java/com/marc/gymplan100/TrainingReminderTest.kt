package com.marc.gymplan100

import com.marc.gymplan100.notify.TrainingReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * Cuándo cae el próximo "hoy toca gimnasio".
 *
 * El plan sigue yendo por días numerados; esto es solo el calendario de avisos que va por
 * encima. Si la cuenta falla, el aviso llega el día que no toca o no llega nunca.
 */
class TrainingReminderTest {

    // Lunes 24 de agosto de 2026, a las 10:00.
    private val lunes10 = LocalDateTime.of(2026, 8, 24, 10, 0)

    @Test
    fun `si hoy toca y la hora no ha pasado, es hoy`() {
        val next = TrainingReminder.nextTrigger(lunes10, setOf(1), 18 * 60)
        assertEquals(LocalDateTime.of(2026, 8, 24, 18, 0), next)
    }

    @Test
    fun `si hoy toca pero la hora ya paso, se salta a la semana que viene`() {
        val next = TrainingReminder.nextTrigger(lunes10, setOf(1), 9 * 60)
        assertEquals(LocalDateTime.of(2026, 8, 31, 9, 0), next)
    }

    @Test
    fun `con varios dias, coge el mas cercano`() {
        // Lunes por la mañana con avisos lunes/miércoles/viernes a las 7:00: el lunes ya pasó.
        val next = TrainingReminder.nextTrigger(lunes10, setOf(1, 3, 5), 7 * 60)
        assertEquals(LocalDateTime.of(2026, 8, 26, 7, 0), next)
    }

    @Test
    fun `el domingo enlaza con el lunes siguiente`() {
        val domingo = LocalDateTime.of(2026, 8, 30, 20, 0)
        val next = TrainingReminder.nextTrigger(domingo, setOf(1), 18 * 60)
        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), next)
    }

    @Test
    fun `sin dias elegidos no hay nada que programar`() {
        assertNull(TrainingReminder.nextTrigger(lunes10, emptySet(), 18 * 60))
    }

    @Test
    fun `la hora se escribe con dos cifras`() {
        assertEquals("07:05", TrainingReminder.formatTime(7 * 60 + 5))
        assertEquals("18:30", TrainingReminder.formatTime(18 * 60 + 30))
        assertEquals("00:00", TrainingReminder.formatTime(0))
    }

    @Test
    fun `los dias por defecto son de lunes a viernes`() {
        assertEquals(setOf(1, 2, 3, 4, 5), TrainingReminder.DEFAULT_DAYS)
    }
}
