package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.RestDefaults
import com.marc.gymplan100.data.RestDefaults.Source
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Cuánto se descansa entre series.
 *
 * Sale de la primera crítica de un usuario (21-oct-2026): «en series de fuerza, a 8
 * repeticiones o menos, menos de 3 minutos no se recomienda». La app daba 90 s a todo, y el
 * plan de Fuerza Máxima escribía «3 min de descanso» en la nota mientras la cuenta atrás
 * sonaba al minuto y medio.
 */
class RestDefaultsTest {

    @After
    fun restaura() = PlanData.setActive(BuiltinPlan.plan)

    private fun ex(name: String, scheme: String, note: String = "", kind: ExerciseKind = ExerciseKind.STRENGTH) =
        Exercise(name, scheme, note, kind)

    @Test fun `la nota del plan manda`() {
        assertEquals(180, RestDefaults.fromNote("3 min de descanso"))
        // Un rango se queda en el suelo: lo que sobre, con +30 s.
        assertEquals(180, RestDefaults.fromNote("3-4 min de descanso"))
        assertEquals(90, RestDefaults.fromNote("descansa 90 s"))
        assertEquals(150, RestDefaults.fromNote("Descanso de 2,5 min"))
        // Una nota que no habla de descanso no es un descanso.
        assertNull(RestDefaults.fromNote("Agarre ancho, 3 s de bajada"))
        assertNull(RestDefaults.fromNote(""))
    }

    @Test fun `pocas repeticiones en un basico, descanso largo`() {
        assertEquals(RestDefaults.Rest(180, Source.HEAVY), RestDefaults.between(ex("Sentadilla con barra", "5 x 5")))
        assertEquals(180, RestDefaults.between(ex("Press de pecho en máquina", "4 x 6-8")).seconds)
        assertEquals(180, RestDefaults.between(ex("Jalón al pecho", "3 x 8")).seconds)
    }

    @Test fun `un aislado a pocas repeticiones descansa menos que un basico`() {
        assertEquals(120, RestDefaults.between(ex("Curl de bíceps con barra", "3 x 8")).seconds)
        // A cinco o menos, da igual: es fuerza.
        assertEquals(180, RestDefaults.between(ex("Curl de bíceps con barra", "3 x 5")).seconds)
    }

    @Test fun `en hipertrofia sigue siendo minuto y medio`() {
        assertEquals(RestDefaults.Rest(90, Source.DEFAULT), RestDefaults.between(ex("Press de pecho en máquina", "3 x 12")))
        assertEquals(90, RestDefaults.between(ex("Prensa de piernas", "3 x 8-12")).seconds)
    }

    @Test fun `lo que hiciste de verdad manda sobre el esquema`() {
        // El plan dice 12, pero hoy has cargado y han salido 6.
        assertEquals(180, RestDefaults.between(ex("Prensa de piernas", "3 x 12"), repsDone = "6").seconds)
    }

    @Test fun `tu descanso manda sobre todo`() {
        val r = RestDefaults.between(
            ex("Sentadilla con barra", "5 x 5", note = "3 min de descanso"),
            yours = mapOf("Sentadilla con barra" to 240)
        )
        assertEquals(RestDefaults.Rest(240, Source.YOURS), r)
    }

    @Test fun `planchas, circuitos y cardio, lo de siempre`() {
        assertEquals(90, RestDefaults.between(ex("Plancha", "3 x 30 s")).seconds)
        assertEquals(90, RestDefaults.between(ex("Circuito core", "3 vueltas")).seconds)
        assertEquals(90, RestDefaults.between(ex("Cinta", "20 min", kind = ExerciseKind.CARDIO)).seconds)
    }

    @Test fun `entre ejercicios nunca menos que entre series`() {
        assertEquals(120, RestDefaults.betweenExercises(RestDefaults.Rest(90, Source.DEFAULT)).seconds)
        assertEquals(180, RestDefaults.betweenExercises(RestDefaults.Rest(180, Source.HEAVY)).seconds)
    }

    @Test fun `el plan de fuerza descansa lo que dice su nota`() {
        val plan = (PlanCodec.parseJson(
            File("src/main/assets/planes/fuerza-maxima.json").readText(), now = 0L
        ) as PlanImport.Ok).plan
        PlanData.setActive(plan)
        val dia = plan.days.first()
        val sentadilla = dia.template.exercises.indexOfFirst { it.note.contains("descanso") }
        val s = ActiveSession(
            dayNumber = dia.number, startMillis = 0L, exerciseIndex = sentadilla,
            phase = SessionPhase.WORKING, order = dia.template.exercises.indices.toList()
        )
        val tras = SessionEngine.completeSet(s, "100", now = 1_000L, reps = "5")
        assertEquals(SessionPhase.RESTING, tras.phase)
        assertEquals(180, tras.restTargetSeconds)
        assertEquals(Source.PLAN.name, tras.restSource)
    }
}
