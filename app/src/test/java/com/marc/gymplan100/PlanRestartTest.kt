package com.marc.gymplan100

import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.ProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repetir un plan terminado. Es lo único de la app que borra progreso a propósito, así que
 * conviene tener escrito qué se lleva por delante y qué no.
 */
class PlanRestartTest {

    private val terminado = ProgressState(
        completedDays = (1..12).toSet(),
        logs = mapOf(
            "1-0" to ExerciseLog(weight = "40", reps = "10", done = true),
            "12-0" to ExerciseLog(weight = "60", reps = "8", done = true),
        ),
        exerciseWeights = mapOf("Prensa de piernas" to "80"),
        startedAt = 1_000L,
        finishedAt = 9_000L,
        rounds = 0
    )

    @Test fun `empezar de cero vacia los dias y cuenta la vuelta`() {
        val nuevo = terminado.restartedFromScratch(now = 50_000L)
        assertTrue(nuevo.completedDays.isEmpty())
        assertTrue(nuevo.logs.isEmpty())
        assertEquals(1, nuevo.rounds)
        assertEquals(50_000L, nuevo.startedAt)
        assertFalse(nuevo.isFinished)
    }

    @Test fun `empezar de cero conserva los pesos aprendidos`() {
        val nuevo = terminado.restartedFromScratch(now = 50_000L)
        assertEquals(mapOf("Prensa de piernas" to "80"), nuevo.exerciseWeights)
    }

    @Test fun `repetir la ultima fase solo borra esos dias`() {
        val ultimaFase = setOf(10, 11, 12)
        val nuevo = terminado.restartedDays(ultimaFase, now = 50_000L)
        assertEquals((1..9).toSet(), nuevo.completedDays)
        assertTrue("el registro del día 12 tenía que irse", "12-0" !in nuevo.logs)
        assertTrue("el del día 1 tenía que quedarse", "1-0" in nuevo.logs)
        assertEquals(1, nuevo.rounds)
        assertFalse(nuevo.isFinished)
    }

    @Test fun `repetir una fase deja el plan sin terminar`() {
        val nuevo = terminado.restartedDays(setOf(12), now = 50_000L)
        assertEquals(11, nuevo.completedDays.size)
        assertFalse(nuevo.isFinished)
    }

    @Test fun `las vueltas se acumulan`() {
        val tras3 = terminado
            .restartedFromScratch(1L)
            .copy(completedDays = (1..12).toSet(), finishedAt = 2L)
            .restartedFromScratch(3L)
            .copy(completedDays = (1..12).toSet(), finishedAt = 4L)
            .restartedFromScratch(5L)
        assertEquals(3, tras3.rounds)
    }

    @Test fun `un plan a medias no se considera terminado`() {
        val aMedias = ProgressState(completedDays = setOf(1, 2, 3))
        assertFalse(aMedias.isFinished)
    }

    /**
     * La portada y la lista dicen "Terminado el 14 de agosto": esa frase no puede depender de
     * si se cerró un aviso ni desaparecer por volver a empezar el plan.
     */
    @Test fun `la fecha de fin sobrevive a la vuelta siguiente`() {
        val nuevo = terminado.restartedFromScratch(now = 50_000L)
        assertEquals(9_000L, nuevo.finishedAt)
        assertTrue(nuevo.everFinished)
        assertFalse("el cierre ya no debe volver a salir solo", nuevo.isFinished)
    }

    @Test fun `ver el cierre y cerrarlo no toca la fecha`() {
        val visto = terminado.copy(finishedSeen = true)
        assertEquals(9_000L, visto.finishedAt)
        assertFalse(visto.isFinished)
        assertTrue(visto.everFinished)
    }
}
