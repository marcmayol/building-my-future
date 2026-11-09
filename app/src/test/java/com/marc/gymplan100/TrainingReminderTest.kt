package com.marc.gymplan100

import com.marc.gymplan100.notify.TrainingReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

    // --- Pausa («Pausar hasta el…»): la fecha es el último día en silencio. ---

    @Test
    fun `pausa que acaba entre semana, vuelve el dia siguiente si toca`() {
        // Lunes 24 ago, L-V a las 18:00, pausa hasta el miércoles 26 incluido → jueves 27.
        val next = TrainingReminder.nextTrigger(
            lunes10, setOf(1, 2, 3, 4, 5), 18 * 60, LocalDate.of(2026, 8, 26)
        )
        assertEquals(LocalDateTime.of(2026, 8, 27, 18, 0), next)
    }

    @Test
    fun `pausa que acaba en un dia no elegido, vuelve el primero elegido despues`() {
        // Solo lunes y jueves; pausa hasta el viernes 28 → el lunes 31, no el sábado.
        val next = TrainingReminder.nextTrigger(
            lunes10, setOf(1, 4), 18 * 60, LocalDate.of(2026, 8, 28)
        )
        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), next)
    }

    @Test
    fun `el dia de vuelta avisa aunque la hora sea temprana`() {
        // Pausa hasta el domingo 30 y aviso a las 7:00: el lunes 31 a las 7:00 sí cuenta.
        val next = TrainingReminder.nextTrigger(
            lunes10, setOf(1), 7 * 60, LocalDate.of(2026, 8, 30)
        )
        assertEquals(LocalDateTime.of(2026, 8, 31, 7, 0), next)
    }

    @Test
    fun `el ultimo dia de la pausa sigue en silencio`() {
        // Pausa hasta hoy mismo (lunes): aunque hoy toque y no haya pasado la hora, nada hoy.
        val next = TrainingReminder.nextTrigger(lunes10, setOf(1, 2), 18 * 60, LocalDate.of(2026, 8, 24))
        assertEquals(LocalDateTime.of(2026, 8, 25, 18, 0), next)
    }

    @Test
    fun `una pausa ya vencida no afecta`() {
        val sinPausa = TrainingReminder.nextTrigger(lunes10, setOf(1, 3), 18 * 60)
        val conVencida = TrainingReminder.nextTrigger(
            lunes10, setOf(1, 3), 18 * 60, LocalDate.of(2026, 8, 23)
        )
        assertEquals(sinPausa, conVencida)
        assertEquals(LocalDateTime.of(2026, 8, 24, 18, 0), conVencida)
    }

    @Test
    fun `pausa que cruza el cambio de hora de octubre`() {
        // El 25 de octubre de 2026 se atrasa el reloj en España. Pausa del jueves 22 al
        // domingo 25: el aviso vuelve el lunes 26 a las 18:00 de la hora nueva, no a las 17.
        val madrid = ZoneId.of("Europe/Madrid")
        val jueves = LocalDateTime.of(2026, 10, 22, 9, 0)
        val next = TrainingReminder.nextTrigger(
            jueves, setOf(1, 2, 3, 4, 5), 18 * 60, LocalDate.of(2026, 10, 25)
        )
        assertEquals(LocalDateTime.of(2026, 10, 26, 18, 0), next)
        val millis = TrainingReminder.triggerMillis(next!!, madrid)
        val enMadrid = Instant.ofEpochMilli(millis).atZone(madrid)
        assertEquals(18, enMadrid.hour)
        assertEquals("+01:00", enMadrid.offset.id)
    }

    @Test
    fun `dentro y fuera de la pausa`() {
        val hasta = LocalDate.of(2026, 11, 16)
        assertTrue(TrainingReminder.isPausedOn(hasta, LocalDate.of(2026, 11, 16)))
        assertTrue(TrainingReminder.isPausedOn(hasta, LocalDate.of(2026, 11, 10)))
        assertFalse(TrainingReminder.isPausedOn(hasta, LocalDate.of(2026, 11, 17)))
        assertFalse(TrainingReminder.isPausedOn(null, LocalDate.of(2026, 11, 10)))
    }

    @Test
    fun `los dias por defecto son de lunes a viernes`() {
        assertEquals(setOf(1, 2, 3, 4, 5), TrainingReminder.DEFAULT_DAYS)
    }
}
