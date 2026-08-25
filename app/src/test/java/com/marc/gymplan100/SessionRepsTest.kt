package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.defaultRepsFromScheme
import com.marc.gymplan100.data.repsRangeFromScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las repeticiones del entreno guiado.
 *
 * Hasta la 2.4 solo se guardaban los kilos: tres semanas subiendo de 8 a 12 repeticiones con el
 * mismo peso eran tres semanas de línea plana, y sin repeticiones no hay ni fuerza estimada, ni
 * kilos movidos, ni progresión que calcular.
 */
class SessionRepsTest {

    private val dia = PlanData.days.first { d ->
        d.template.exercises.any { it.name == "Prensa de piernas" }
    }
    private val prensa = dia.template.exercises.indexOfFirst { it.name == "Prensa de piernas" }
    private val plancha = dia.template.exercises.indexOfFirst { it.name == "Plancha" }

    private fun sesion(
        exerciseIndex: Int = prensa,
        setNumber: Int = 1,
        currentReps: String = "",
        completedSets: List<CompletedSet> = emptyList()
    ) = ActiveSession(
        dayNumber = dia.number,
        startMillis = 0L,
        exerciseIndex = exerciseIndex,
        setNumber = setNumber,
        phase = SessionPhase.WORKING,
        currentReps = currentReps,
        completedSets = completedSets,
        order = dia.template.exercises.indices.toList()
    )

    // --- El esquema dice si hay repeticiones que contar -----------------------

    @Test
    fun `un rango se lee entero y un numero fijo es un rango de uno`() {
        assertEquals(8..12, repsRangeFromScheme("3 x 8-12"))
        assertEquals(12..12, repsRangeFromScheme("3 x 12"))
        assertEquals("8", defaultRepsFromScheme("4 x 8-10"))
    }

    @Test
    fun `lo que va por tiempo o por vueltas no lleva repeticiones`() {
        assertNull(repsRangeFromScheme("3 x 30 s"))
        assertNull(repsRangeFromScheme("3 vueltas"))
    }

    // --- La cascada, la misma que la del peso --------------------------------

    @Test
    fun `sin nada apuntado se parte de lo que pide el plan`() {
        val s = sesion()
        val esquema = dia.template.exercises[prensa].scheme
        assertEquals(defaultRepsFromScheme(esquema), SessionEngine.repsForSet(s, emptyMap()))
    }

    @Test
    fun `lo tocado en esta serie manda sobre todo lo demas`() {
        val s = sesion(currentReps = "7")
        assertEquals("7", SessionEngine.repsForSet(s, mapOf("Prensa de piernas" to "11")))
    }

    @Test
    fun `si no se ha tocado nada se repite la serie anterior de esta maquina`() {
        val s = sesion(
            setNumber = 2,
            completedSets = listOf(CompletedSet(prensa, 1, "60", reps = "9"))
        )
        assertEquals("9", SessionEngine.repsForSet(s, mapOf("Prensa de piernas" to "11")))
    }

    @Test
    fun `sin nada en esta sesion se mira la ultima vez que se hizo el ejercicio`() {
        val s = sesion()
        assertEquals("11", SessionEngine.repsForSet(s, mapOf("Prensa de piernas" to "11")))
    }

    @Test
    fun `una serie por tiempo no pregunta repeticiones`() {
        val s = sesion(exerciseIndex = plancha, currentReps = "12")
        assertEquals("", SessionEngine.repsForSet(s, emptyMap()))
    }

    // --- Cerrar la serie ------------------------------------------------------

    @Test
    fun `la serie se apunta con sus repeticiones y deja el contador limpio`() {
        val s = sesion(currentReps = "10")
        val next = SessionEngine.completeSet(s, "60", now = 1_000L, reps = "10")
        assertEquals("10", next.completedSets.last().reps)
        assertEquals("60", next.completedSets.last().weight)
        // La siguiente serie vuelve a partir del plan, no de lo que se escribió en esta.
        assertEquals("", next.currentReps)
    }

    @Test
    fun `cambiar de maquina olvida lo apuntado para la anterior`() {
        val s = sesion(currentReps = "10")
        val next = SessionEngine.skipExercise(s)
        assertEquals("", next.currentReps)
    }

    // --- Lo que queda guardado al cerrar el día -------------------------------

    @Test
    fun `al cerrar el dia cada serie conserva sus repeticiones`() {
        val series = listOf(
            CompletedSet(prensa, 1, "60", reps = "12"),
            CompletedSet(prensa, 2, "60", reps = "11"),
            CompletedSet(prensa, 3, "55", reps = "10")
        )
        val estado = ProgressState().withFinishedDay(dia.number, series)
        val log = estado.logs["${dia.number}-$prensa"]!!
        assertEquals(listOf("12", "11", "10"), log.sets.map { it.reps })
        // El peso del ejercicio sigue siendo el más alto del día.
        assertEquals("60", log.weight)
        // Y se recuerdan las repeticiones de ARRANQUE, no las del final cansado.
        assertEquals("12", estado.exerciseReps["Prensa de piernas"])
    }

    @Test
    fun `un dia sin repeticiones no ensucia lo que ya se sabia`() {
        val previo = ProgressState(exerciseReps = mapOf("Prensa de piernas" to "10"))
        val series = listOf(CompletedSet(prensa, 1, "60"))
        val estado = previo.withFinishedDay(dia.number, series)
        assertEquals("10", estado.exerciseReps["Prensa de piernas"])
        assertTrue(estado.logs["${dia.number}-$prensa"]!!.done)
    }
}
