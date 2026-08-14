package com.marc.gymplan100

import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.Phase
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.PlanAdvisor.Days
import com.marc.gymplan100.data.PlanAdvisor.Place
import com.marc.gymplan100.data.PlanAdvisor.Shape
import com.marc.gymplan100.data.PlanEquipment
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.data.PlanLevel
import com.marc.gymplan100.data.TrainingPlan
import com.marc.gymplan100.data.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** El asistente que recomienda plan al abrir la app por primera vez. */
class PlanAdvisorTest {

    private fun plan(
        id: String,
        goal: PlanGoal,
        level: PlanLevel,
        days: Int,
        equipment: PlanEquipment = PlanEquipment.GYM,
        addOn: Boolean = false
    ) = TrainingPlan(
        id = id,
        name = id,
        phases = listOf(
            Phase(
                number = 1, name = "F", range = "", weeks = "", weeksCount = 1,
                description = "", progression = "",
                templates = listOf(
                    WorkoutTemplate("Día 1", "T", "", listOf(Exercise("Sentadilla", "3 x 10")))
                )
            )
        ),
        goal = goal, level = level, daysPerWeek = days, equipment = equipment, addOn = addOn
    )

    /** El catálogo real, en versión mínima. */
    private val catalogo = listOf(
        plan("arranque", PlanGoal.START, PlanLevel.ZERO, 3),
        plan("plan100", PlanGoal.LOSE_FAT, PlanLevel.BEGINNER, 5),
        plan("hipertrofia", PlanGoal.MUSCLE, PlanLevel.INTERMEDIATE, 4),
        plan("fuerza", PlanGoal.STRENGTH, PlanLevel.INTERMEDIATE, 3),
        plan("mantenimiento", PlanGoal.MAINTAIN, PlanLevel.ANY, 3),
        plan("calistenia", PlanGoal.MUSCLE, PlanLevel.ANY, 3, PlanEquipment.BAR),
        plan("cardio", PlanGoal.LOSE_FAT, PlanLevel.ANY, 4),
        plan("movilidad", PlanGoal.MOBILITY, PlanLevel.ANY, 3, PlanEquipment.NONE, addOn = true),
        plan("bloque_core", PlanGoal.MUSCLE, PlanLevel.ANY, 3, PlanEquipment.NONE, addOn = true),
    )

    private fun recomienda(goal: PlanGoal, shape: Shape, days: Days, place: Place) =
        PlanAdvisor.recommend(catalogo, PlanAdvisor.Answers(goal, shape, days, place))

    @Test fun `quien lleva tiempo parado empieza por Arranque aunque pida musculo`() {
        val r = recomienda(PlanGoal.MUSCLE, Shape.STOPPED, Days.THREE, Place.GYM)
        assertEquals("arranque", r.best?.plan?.id)
    }

    @Test fun `a quien ya entrena no se le ofrece el plan de empezar`() {
        val r = recomienda(PlanGoal.MUSCLE, Shape.REGULAR, Days.FOUR, Place.GYM)
        assertTrue(r.alternatives.none { it.plan.id == "arranque" })
        assertEquals("hipertrofia", r.best?.plan?.id)
    }

    @Test fun `sin material no se propone nada que necesite gimnasio`() {
        val r = recomienda(PlanGoal.MUSCLE, Shape.REGULAR, Days.THREE, Place.HOME)
        assertEquals("calistenia", r.best?.plan?.id)
        val propuestos = listOfNotNull(r.best) + r.alternatives
        assertTrue(propuestos.none { it.plan.equipment == PlanEquipment.GYM })
    }

    @Test fun `con tres dias no se recomienda un plan de cinco`() {
        val r = recomienda(PlanGoal.LOSE_FAT, Shape.SOMETIMES, Days.THREE, Place.GYM)
        assertEquals("cardio", r.best?.plan?.id)   // 4 días entra; el de 5 no
        assertTrue(r.alternatives.none { it.plan.id == "plan100" })
    }

    @Test fun `con cinco dias libres si cabe el plan de cinco`() {
        val r = recomienda(PlanGoal.LOSE_FAT, Shape.SOMETIMES, Days.FIVE, Place.GYM)
        assertEquals("plan100", r.best?.plan?.id)
    }

    @Test fun `levantar mas peso lleva a fuerza`() {
        val r = recomienda(PlanGoal.STRENGTH, Shape.REGULAR, Days.THREE, Place.GYM)
        assertEquals("fuerza", r.best?.plan?.id)
    }

    @Test fun `los bloques extra nunca son el plan principal`() {
        for (goal in PlanGoal.entries) {
            for (shape in Shape.entries) {
                val r = recomienda(goal, shape, Days.FOUR, Place.GYM)
                assertTrue(
                    "«$goal/$shape» propuso un bloque extra como principal",
                    r.best?.plan?.addOn != true
                )
            }
        }
    }

    @Test fun `quien quiere moverse mejor recibe movilidad como anadido`() {
        val r = recomienda(PlanGoal.MOBILITY, Shape.SOMETIMES, Days.THREE, Place.GYM)
        assertTrue(r.addOns.any { it.id == "movilidad" })
    }

    @Test fun `con solo dos dias no se le carga con bloques extra`() {
        val r = recomienda(PlanGoal.MUSCLE, Shape.REGULAR, Days.TWO, Place.GYM)
        assertTrue(r.addOns.isEmpty())
    }

    @Test fun `siempre explica por que`() {
        val r = recomienda(PlanGoal.MUSCLE, Shape.REGULAR, Days.FOUR, Place.GYM)
        assertNotNull(r.best)
        assertTrue(r.best!!.reason.length > 20)
        assertTrue(r.best!!.reason.endsWith("."))
    }

    @Test fun `sin planes que encajen no se inventa una recomendacion`() {
        val soloGimnasio = catalogo.filter { it.equipment == PlanEquipment.GYM && !it.addOn }
        val r = PlanAdvisor.recommend(
            soloGimnasio,
            PlanAdvisor.Answers(PlanGoal.MUSCLE, Shape.REGULAR, Days.THREE, Place.HOME)
        )
        assertNull(r.best)
    }
}
