package com.marc.gymplan100

import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.PlanAdvisor.Days
import com.marc.gymplan100.data.PlanAdvisor.Place
import com.marc.gymplan100.data.PlanAdvisor.SessionTime
import com.marc.gymplan100.data.PlanAdvisor.Shape
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.TrainingPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La pregunta de cuánto tiempo hay cada día (idea de Notion, 21-oct-2026): un plan cuyas
 * sesiones no caben en ese rato no se propone, y si se pasa poco se propone diciendo que el
 * exprés lo recorta.
 */
class PlanAdvisorTimeTest {

    /** Los planes reales que trae la app, con los mismos datos que ve el asistente. */
    private val planes: List<TrainingPlan> by lazy {
        val dir = File("src/main/assets/planes")
        PlanCodec.decodeIdList(File(dir, "index.json").readText()).map { id ->
            (PlanCodec.parseJson(File(dir, "$id.json").readText(), now = 0L) as PlanImport.Ok).plan
        } + BuiltinPlan.plan
    }

    @Test fun `cada plan tiene una duracion de sesion razonable`() {
        planes.forEach { p ->
            val min = PlanAdvisor.typicalMinutes(p)
            println("${p.id}: $min min")
            assertTrue("${p.id} dura $min min", min in 5..120)
        }
    }

    private fun answers(time: SessionTime?, goal: PlanGoal = PlanGoal.MUSCLE) =
        PlanAdvisor.Answers(goal, Shape.REGULAR, Days.FOUR, Place.GYM, time)

    @Test fun `lo que se propone cabe en el tiempo, o casi`() {
        for (t in SessionTime.entries) for (goal in PlanGoal.entries) {
            val r = PlanAdvisor.recommend(planes, answers(t, goal))
            (listOfNotNull(r.best) + r.alternatives).forEach { s ->
                val dura = PlanAdvisor.typicalMinutes(s.plan)
                assertTrue("${s.plan.id} ($dura min) propuesto con ${t.minutes} min", dura <= t.minutes + 10)
            }
        }
    }

    @Test fun `sin decir el tiempo, se recomienda como antes`() {
        val sin = PlanAdvisor.recommend(planes, answers(null))
        val conMucho = PlanAdvisor.recommend(planes, answers(SessionTime.MIN90))
        assertEquals(sin.best?.plan?.id, conMucho.best?.plan?.id)
    }

    @Test fun `con poco tiempo se dice por que no encaja`() {
        val r = PlanAdvisor.recommend(planes, answers(SessionTime.MIN30, PlanGoal.STRENGTH))
        if (r.best == null) {
            assertNotNull(r.closest)
        } else {
            assertTrue(r.best!!.reason.contains("min"))
        }
    }
}
