package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Con qué peso se apunta una serie.
 *
 * Existe por un fallo real: marcar "serie hecha" desde el reloj daba la serie por buena con el
 * peso vacío, así que los kilos escritos en el móvil se perdían, el día salía con un guion en
 * Resultados y "Mis pesos" seguía enseñando el peso de la semana anterior.
 */
class SessionWeightTest {

    /** Día del plan integrado que empieza por prensa de piernas y acaba en plancha (por tiempo). */
    private val dia = PlanData.days.first { d ->
        d.template.exercises.any { it.name == "Prensa de piernas" }
    }
    private val prensa = dia.template.exercises.indexOfFirst { it.name == "Prensa de piernas" }
    private val plancha = dia.template.exercises.indexOfFirst { it.name == "Plancha" }

    private fun sesion(
        exerciseIndex: Int = prensa,
        setNumber: Int = 1,
        plannedWeight: String = "",
        completedSets: List<CompletedSet> = emptyList(),
        phase: SessionPhase = SessionPhase.WORKING
    ) = ActiveSession(
        dayNumber = dia.number,
        startMillis = 0L,
        exerciseIndex = exerciseIndex,
        setNumber = setNumber,
        phase = phase,
        plannedWeight = plannedWeight,
        completedSets = completedSets,
        order = dia.template.exercises.indices.toList()
    )

    @Test fun `manda el peso que se escribio en el movil`() {
        val s = sesion(plannedWeight = "200")
        assertEquals("200", SessionEngine.weightForSet(s, mapOf("Prensa de piernas" to "180")))
    }

    @Test fun `sin peso escrito, el de la serie anterior de la misma maquina`() {
        val s = sesion(
            setNumber = 2,
            completedSets = listOf(CompletedSet(prensa, 1, "200"))
        )
        assertEquals("200", SessionEngine.weightForSet(s, mapOf("Prensa de piernas" to "180")))
    }

    @Test fun `primera serie sin tocar nada, el ultimo peso conocido`() {
        assertEquals("180", SessionEngine.weightForSet(sesion(), mapOf("Prensa de piernas" to "180")))
    }

    @Test fun `un ejercicio sin kilos no inventa peso`() {
        val s = sesion(exerciseIndex = plancha, plannedWeight = "200")
        assertEquals("", SessionEngine.weightForSet(s, mapOf("Plancha" to "20")))
    }

    @Test fun `el reloj apunta la serie con el peso preparado, no vacia`() {
        // Lo que hace WearCommandListenerService al recibir "serie hecha".
        val s = sesion(plannedWeight = "200")
        val peso = SessionEngine.weightForSet(s, mapOf("Prensa de piernas" to "180"))
        val next = SessionEngine.completeSet(s, peso, now = 1_000L)
        assertEquals("200", next.completedSets.single().weight)
    }

    @Test fun `el peso preparado no se arrastra a la maquina siguiente`() {
        // Al empezar el descanso se limpia; la siguiente serie vuelve a calcular su sugerencia.
        val s = sesion(plannedWeight = "200")
        val descansando = SessionEngine.completeSet(s, "200", now = 1_000L)
        assertEquals("", descansando.plannedWeight)
    }

    @Test fun `al cerrar el dia, Mis pesos se queda con lo que se levanto`() {
        val antes = com.marc.gymplan100.data.ProgressState(
            exerciseWeights = mapOf("Prensa de piernas" to "180")
        )
        val despues = antes.withFinishedDay(
            dia.number,
            listOf(
                CompletedSet(prensa, 1, "200"),
                CompletedSet(prensa, 2, "200"),
                CompletedSet(prensa, 3, "200"),
            )
        )
        assertEquals("200", despues.exerciseWeights["Prensa de piernas"])
        assertEquals("200", despues.logs["${dia.number}-$prensa"]?.weight)
        assertTrue(dia.number in despues.completedDays)
    }

    @Test fun `un ejercicio sin peso apuntado no borra el que ya habia`() {
        val antes = com.marc.gymplan100.data.ProgressState(
            exerciseWeights = mapOf("Prensa de piernas" to "180")
        )
        val despues = antes.withFinishedDay(dia.number, listOf(CompletedSet(prensa, 1, "")))
        assertEquals("180", despues.exerciseWeights["Prensa de piernas"])
        assertTrue(despues.logs["${dia.number}-$prensa"]?.done == true)
    }
}
