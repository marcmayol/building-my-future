package com.marc.gymplan100

import com.marc.gymplan100.data.Achievements
import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.Motivation
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.ProgressState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los logros salen del plan activo, no de un 100 fijo. Lo importante aquí es que el plan de la
 * app conserve EXACTAMENTE los hitos de siempre (quien lleva 27 días no puede ver cómo se le
 * bloquean logros ya conseguidos) y que un plan corto tenga hitos que se puedan alcanzar.
 */
class AchievementsPlanTest {

    @After fun restaurarPlanDeLaApp() {
        PlanData.setActive(BuiltinPlan.plan)
    }

    private fun activarPlanDe(dias: Int) {
        val ejercicios = """[ { "nombre": "Prensa", "esquema": "3 x 12" } ]"""
        val json = """
            { "nombre": "Plan de $dias", "fases": [ { "nombre": "Única", "semanas": 1, "dias": [
                ${(1..dias).joinToString(",") { """{ "dia": "Día $it", "ejercicios": $ejercicios }""" }}
            ] } ] }
        """.trimIndent()
        val result = PlanCodec.parseJson(json) as PlanImport.Ok
        PlanData.setActive(result.plan)
        assertEquals(dias, PlanData.TOTAL_DAYS)
    }

    private fun progresoDe(dias: Int) = ProgressState(completedDays = (1..dias).toSet())

    @Test fun `el plan de la app conserva sus hitos de siempre`() {
        PlanData.setActive(BuiltinPlan.plan)
        val ids = Achievements.all.map { it.id }
        // Mismos logros que siempre y en orden de dificultad real: las fases duran 20, 30, 30 y
        // 20 días, así que la tercera se termina el día 80, ya pasado el hito de las tres cuartas
        // partes. La lista escrita a mano de antes la ponía justo antes, que era engañoso.
        assertEquals(
            listOf(
                "first", "week", "ten", "phase1", "quarter", "phase2",
                "half", "threequarter", "phase3", "phase4", "complete"
            ),
            ids
        )
        val a25 = Achievements.unlockedIds(progresoDe(25))
        assertTrue("quarter" in a25)
        assertFalse("half" in a25)
        assertTrue("complete" in Achievements.unlockedIds(progresoDe(100)))
        assertFalse("complete" in Achievements.unlockedIds(progresoDe(99)))
    }

    @Test fun `las fases mantienen su nombre en los logros`() {
        PlanData.setActive(BuiltinPlan.plan)
        val titulos = Achievements.all.filter { it.id.startsWith("phase") }.map { it.title }
        assertEquals(
            listOf(
                "Adaptación superada",
                "Construcción superada",
                "Progresión superada",
                "Consolidación superada"
            ),
            titulos
        )
    }

    @Test fun `en un plan de 20 dias los cuartos son de 20`() {
        activarPlanDe(20)
        val ids = Achievements.all.map { it.id }
        assertTrue(ids.containsAll(listOf("first", "week", "ten", "quarter", "half", "complete")))
        assertTrue("quarter" in Achievements.unlockedIds(progresoDe(5)))
        assertTrue("half" in Achievements.unlockedIds(progresoDe(10)))
        assertTrue("complete" in Achievements.unlockedIds(progresoDe(20)))
    }

    @Test fun `un plan corto no ofrece logros imposibles`() {
        activarPlanDe(4)
        val ids = Achievements.all.map { it.id }
        assertFalse("week" in ids)   // pediría 5 días de un plan de 4
        assertFalse("ten" in ids)
        assertTrue("first" in ids)
        assertTrue("complete" in ids)
        // Terminarlo entero desbloquea todo lo que ofrece.
        assertEquals(ids.toSet(), Achievements.unlockedIds(progresoDe(4)))
    }

    @Test fun `no se repiten logros con el mismo umbral`() {
        activarPlanDe(4)
        val ids = Achievements.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test fun `el mensaje del final habla de los dias que dura el plan`() {
        activarPlanDe(20)
        assertTrue(Motivation.message(20).contains("20 días"))
        assertTrue(Motivation.message(5).contains("cuarto"))
    }

    @Test fun `los logros nuevos son solo los que acaban de caer`() {
        PlanData.setActive(BuiltinPlan.plan)
        val nuevos = Achievements.newlyUnlocked(progresoDe(24), progresoDe(25)).map { it.id }
        assertEquals(listOf("quarter"), nuevos)
    }
}
