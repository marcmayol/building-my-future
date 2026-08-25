package com.marc.gymplan100

import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.LoggedExercise
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.heaviestWeight
import com.marc.gymplan100.data.setsSummary
import com.marc.gymplan100.data.weightsSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El peso de CADA serie, que es como se entrena de verdad: la primera de bíceps con 10, la
 * segunda con 12 y la tercera con 11.
 *
 * Hasta la v2.3 el desglose vivía solo mientras duraba la sesión: al cerrar el día se guardaba
 * el peso de la última serie y las otras dos se perdían, así que el día salía en Resultados
 * como si hubiera ido entero con 11 kg.
 */
class SetBreakdownTest {

    private val dia = PlanData.days.first { d ->
        d.template.exercises.any { it.name == "Prensa de piernas" }
    }
    private val prensa = dia.template.exercises.indexOfFirst { it.name == "Prensa de piernas" }

    // --- Lo que se guarda al cerrar el día --------------------------------

    @Test fun `el dia guarda el peso de cada serie, no solo el ultimo`() {
        val estado = ProgressState().withFinishedDay(
            dia.number,
            listOf(
                CompletedSet(prensa, 1, "10"),
                CompletedSet(prensa, 2, "12"),
                CompletedSet(prensa, 3, "11")
            )
        )
        val log = estado.logs["${dia.number}-$prensa"]!!
        assertEquals(listOf("10", "12", "11"), log.sets.map { it.weight })
    }

    @Test fun `el peso del ejercicio es el mas alto de sus series`() {
        val estado = ProgressState().withFinishedDay(
            dia.number,
            listOf(
                CompletedSet(prensa, 1, "10"),
                CompletedSet(prensa, 2, "12"),
                CompletedSet(prensa, 3, "11")
            )
        )
        assertEquals("12", estado.logs["${dia.number}-$prensa"]!!.weight)
        assertEquals("12", estado.exerciseWeights["Prensa de piernas"])
    }

    @Test fun `las series se guardan en el orden en que se hicieron`() {
        // Marcar una serie desde el reloj puede meterlas desordenadas en la lista.
        val estado = ProgressState().withFinishedDay(
            dia.number,
            listOf(
                CompletedSet(prensa, 3, "11"),
                CompletedSet(prensa, 1, "10"),
                CompletedSet(prensa, 2, "12")
            )
        )
        assertEquals(
            listOf("10", "12", "11"),
            estado.logs["${dia.number}-$prensa"]!!.sets.map { it.weight }
        )
    }

    @Test fun `un ejercicio sin kilos (una plancha) no inventa desglose`() {
        val estado = ProgressState().withFinishedDay(
            dia.number,
            listOf(CompletedSet(prensa, 1, ""), CompletedSet(prensa, 2, ""))
        )
        val log = estado.logs["${dia.number}-$prensa"]!!
        assertTrue(log.sets.isEmpty())
        assertTrue(log.weight.isBlank())
        assertTrue(estado.exerciseWeights.isEmpty())
    }

    @Test fun `un dia ya apuntado a mano conserva su peso si el guiado no trae ninguno`() {
        val previo = ProgressState(
            logs = mapOf("${dia.number}-$prensa" to ExerciseLog(weight = "80"))
        )
        val estado = previo.withFinishedDay(dia.number, listOf(CompletedSet(prensa, 1, "")))
        assertEquals("80", estado.logs["${dia.number}-$prensa"]!!.weight)
    }

    // --- El peso más alto -------------------------------------------------

    @Test fun `el mas alto se compara por numero, no por texto`() {
        assertEquals("10", heaviestWeight(listOf("9", "10", "8")))
    }

    @Test fun `el mas alto entiende la coma decimal`() {
        assertEquals("12,5", heaviestWeight(listOf("12", "12,5", "11")))
    }

    @Test fun `sin ningun numero no hay peso mas alto`() {
        assertEquals("", heaviestWeight(listOf("", "  ", "-")))
    }

    // --- Cómo se lee el desglose -----------------------------------------

    @Test fun `tres pesos distintos se leen uno a uno`() {
        val resumen = weightsSummary(listOf(SetLog("10"), SetLog("12"), SetLog("11")))
        assertEquals("10 · 12 · 11 kg", resumen)
    }

    @Test fun `tres series iguales no repiten el numero tres veces`() {
        val resumen = weightsSummary(listOf(SetLog("12"), SetLog("12"), SetLog("12")))
        assertEquals("12 kg ×3", resumen)
    }

    @Test fun `una serie sin peso en medio deja su hueco`() {
        val resumen = weightsSummary(listOf(SetLog("10"), SetLog(""), SetLog("11")))
        assertEquals("10 · — · 11 kg", resumen)
    }

    @Test fun `las series sin apuntar del final no salen`() {
        val resumen = weightsSummary(listOf(SetLog("10"), SetLog("12"), SetLog("")))
        assertEquals("10 · 12 kg", resumen)
    }

    @Test fun `sin ningun peso no hay nada que leer`() {
        assertEquals("", weightsSummary(listOf(SetLog(), SetLog())))
    }

    @Test fun `en el entreno libre se leen kilos y repeticiones`() {
        val resumen = setsSummary(
            listOf(SetLog("10", "12"), SetLog("12", "12"), SetLog("11", "10"))
        )
        assertEquals("10 kg × 12 · 12 kg × 12 · 11 kg × 10", resumen)
    }

    @Test fun `sin repeticiones apuntadas, el libre se lee como el desglose de pesos`() {
        assertEquals("10 · 12 kg", setsSummary(listOf(SetLog("10"), SetLog("12"))))
    }

    // --- Entrenos apuntados antes de que existieran las series ------------

    @Test fun `un entreno libre viejo hace de serie unica`() {
        val viejo = LoggedExercise(name = "Prensa de piernas", weight = "80", reps = "12")
        assertEquals(listOf(SetLog("80", "12")), viejo.setsOrSingle)
    }

    @Test fun `un entreno libre sin nada apuntado no tiene series`() {
        assertTrue(LoggedExercise(name = "Prensa de piernas").setsOrSingle.isEmpty())
    }
}
