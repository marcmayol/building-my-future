package com.marc.gymplan100

import com.marc.gymplan100.data.Express
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SessionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El entreno exprés.
 *
 * Primera crítica de un usuario (21-oct-2026): entrena de 6:30 a 7:10, y los seis ejercicios
 * del día le llevarían más de una hora; hizo tres y el resto se quedó sin hacer y sin apuntar.
 */
class ExpressPlanTest {

    private val dia = PlanData.days.first { it.template.exercises.size >= 6 }

    @Test fun `si entero cabe, no se recorta`() {
        assertNull(Express.plan(dia, budgetSeconds = 3 * 3600))
    }

    @Test fun `primero baja las series y luego quita del final`() {
        val entero = Express.estimateSeconds(dia)
        val p = Express.plan(dia, budgetSeconds = 30 * 60)
        assertNotNull(p)
        p!!
        assertEquals(Express.EXPRESS_SETS, p.maxSets)
        assertTrue("Cabe en 30 min: ${p.estimatedSeconds}", p.estimatedSeconds <= 30 * 60)
        assertTrue(p.estimatedSeconds < entero)
        // Se queda con los primeros (los básicos) y quita los del final.
        assertEquals(dia.template.exercises.indices.take(p.order.size), p.order)
        assertEquals(dia.template.exercises.size - p.order.size, p.dropped.size)
        assertTrue(p.warmupSeconds <= Express.EXPRESS_WARMUP)
    }

    @Test fun `con muy poco tiempo, al menos un ejercicio`() {
        val p = Express.plan(dia, budgetSeconds = 60)!!
        assertTrue(p.order.isNotEmpty())
    }

    @Test fun `una superserie entra o sale entera`() {
        val conSuper = PlanData.days.first { d -> d.template.exercises.any { it.supersetGroup.isNotBlank() } }
        val miembros = conSuper.template.exercises.indices.filter { conSuper.template.exercises[it].supersetGroup.isNotBlank() }
        for (minutos in 10..60 step 5) {
            val p = Express.plan(conSuper, minutos * 60) ?: continue
            val dentro = miembros.count { it in p.order }
            assertTrue("Superserie partida a $minutos min: ${p.order}", dentro == 0 || dentro == miembros.size)
        }
    }

    @Test fun `la sesion exprés hace dos series y salta lo que se quedo fuera`() {
        val p = Express.plan(dia, budgetSeconds = 30 * 60)!!
        var s = SessionEngine.startSession(dia.number, 0L, express = p)
        assertTrue(s.isExpress)
        s = SessionEngine.endWarmup(s)
        var t = 1_000L
        var series = 0
        while (s.phase != SessionPhase.FINISHED && series < 100) {
            s = when (s.phase) {
                SessionPhase.WORKING, SessionPhase.TIMED_SET -> { series++; SessionEngine.completeSet(s, "10", t, reps = "10") }
                SessionPhase.RESTING -> SessionEngine.endRest(s, t)
                else -> error("fase inesperada ${s.phase}")
            }
            t += 1_000L
        }
        assertEquals(SessionPhase.FINISHED, s.phase)
        val porEjercicio = s.completedSets.groupBy { it.exerciseIndex }
        assertEquals(p.order.toSet(), porEjercicio.keys)
        porEjercicio.values.forEach { assertTrue(it.size <= Express.EXPRESS_SETS) }
    }

    @Test fun `el modelo aprende lo que tardas`() {
        assertEquals(1.0, Express.scaleFrom(emptyList()), 0.0)
        val lento = List(4) {
            SessionRecord(dayNumber = dia.number, startMillis = 0L, endMillis = 90L * 60_000, totalSets = 15, totalRestSeconds = 0)
        }
        assertTrue(Express.scaleFrom(lento) > 1.0)
        // Un entreno libre no cuenta: no tiene series con las que comparar.
        val libres = lento.map { it.copy(special = true) }
        assertEquals(1.0, Express.scaleFrom(libres), 0.0)
    }
}
