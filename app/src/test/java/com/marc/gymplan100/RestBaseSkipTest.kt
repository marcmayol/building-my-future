package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.Exercise
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.RestDefaults
import com.marc.gymplan100.data.RestDefaults.Source
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La segunda crítica del mismo usuario (22-oct-2026), y lo que Marc ya le había prometido:
 *
 * - Tus descansos de siempre se ponen en Ajustes, en vez de ser 90 s y 2 min para todo el mundo.
 * - «¿Cómo ha ido?» alarga el descanso: justo, medio minuto; al fallo, un minuto. «Cuando voy
 *   al fallo no hago la siguiente hasta que no han pasado 3 minutos de reloj.»
 * - Un ejercicio que no vas a hacer se tacha. Antes, para pasar de la prensa que no tenía en
 *   casa, marcó «Serie hecha» tres veces sin hacerlas.
 */
class RestBaseSkipTest {

    private fun ex(name: String, scheme: String, note: String = "", kind: ExerciseKind = ExerciseKind.STRENGTH) =
        Exercise(name, scheme, note, kind)

    private val dosMinutos = RestDefaults.Base(sets = 120, exercises = 180)

    // --- Tus descansos de siempre ---------------------------------------------

    @Test fun `sin tocar nada siguen siendo 90 s y 2 min`() {
        val base = RestDefaults.Base()
        assertEquals(90, base.sets)
        assertEquals(120, base.exercises)
    }

    @Test fun `tu descanso entre series sustituye al minuto y medio`() {
        assertEquals(
            RestDefaults.Rest(120, Source.DEFAULT),
            RestDefaults.between(ex("Press de pecho en máquina", "3 x 12"), base = dosMinutos)
        )
    }

    @Test fun `tu descanso entre ejercicios sustituye a los dos minutos`() {
        val entre = RestDefaults.between(ex("Press de pecho en máquina", "3 x 12"), base = dosMinutos)
        assertEquals(180, RestDefaults.betweenExercises(entre, dosMinutos).seconds)
    }

    @Test fun `una serie pesada no te deja descansar menos que lo tuyo`() {
        // Con 4 minutos de siempre, el «descanso largo» de 3 se quedaría corto.
        val largo = RestDefaults.Base(sets = 240)
        assertEquals(240, RestDefaults.between(ex("Sentadilla con barra", "5 x 5"), base = largo).seconds)
        // Y con uno corto, la serie pesada sigue pidiendo sus 3 minutos.
        val corto = RestDefaults.Base(sets = 60)
        assertEquals(
            RestDefaults.Rest(180, Source.HEAVY),
            RestDefaults.between(ex("Sentadilla con barra", "5 x 5"), base = corto)
        )
    }

    @Test fun `lo que pide el plan y lo que pusiste en el ejercicio siguen mandando`() {
        assertEquals(180, RestDefaults.between(ex("Remo", "3 x 12", "3 min de descanso"), base = dosMinutos).seconds)
        assertEquals(
            75,
            RestDefaults.between(ex("Remo", "3 x 12"), yours = mapOf("Remo" to 75), base = dosMinutos).seconds
        )
    }

    @Test fun `la sesion usa tus descansos`() {
        val dia = PlanData.days.first()
        val s = ActiveSession(
            dayNumber = dia.number, startMillis = 0L, exerciseIndex = 0,
            phase = SessionPhase.WORKING, order = dia.template.exercises.indices.toList()
        )
        val tras = SessionEngine.completeSet(s, "20", now = 1_000L, reps = "12", restBase = RestDefaults.Base(sets = 105))
        assertEquals(SessionPhase.RESTING, tras.phase)
        // 105 o el descanso largo de una serie pesada; nunca los 90 de antes.
        assertTrue(tras.restTargetSeconds >= 105)
    }

    // --- Cómo ha ido la serie ---------------------------------------------------

    @Test fun `facil no suma, justo medio minuto, al fallo un minuto`() {
        assertEquals(0, RestDefaults.forEffort("3"))
        assertEquals(30, RestDefaults.forEffort("1"))
        assertEquals(60, RestDefaults.forEffort("0"))
        assertEquals(0, RestDefaults.forEffort(""))
    }

    // --- Saltar ejercicio ---------------------------------------------------------

    private val dia = PlanData.days.first { d ->
        d.template.exercises.size >= 3 && d.template.exercises.none { it.supersetGroup.isNotBlank() }
    }

    private fun sesion(exerciseIndex: Int, completed: List<CompletedSet> = emptyList()) = ActiveSession(
        dayNumber = dia.number,
        startMillis = 0L,
        exerciseIndex = exerciseIndex,
        phase = SessionPhase.WORKING,
        completedSets = completed,
        order = dia.template.exercises.indices.toList()
    )

    @Test fun `no lo hago hoy tacha el ejercicio y pasa al siguiente`() {
        val next = SessionEngine.dropExercise(sesion(0))
        assertEquals(SessionPhase.WORKING, next.phase)
        assertEquals(1, next.exerciseIndex)
        assertEquals(1, next.setNumber)
        assertFalse(0 in next.order)
        assertEquals(listOf(0), next.skipped)
        // Y no se ha apuntado ninguna serie que no se hizo.
        assertTrue(next.completedSets.isEmpty())
    }

    @Test fun `las series hechas se quedan apuntadas al dejarlo`() {
        val hecha = CompletedSet(0, 1, "40", reps = "10")
        val next = SessionEngine.dropExercise(sesion(0, listOf(hecha)))
        assertEquals(listOf(hecha), next.completedSets)
        assertEquals(1, next.exerciseIndex)
    }

    @Test fun `tachar el ultimo cierra el dia`() {
        val ultimo = dia.template.exercises.lastIndex
        val next = SessionEngine.dropExercise(sesion(ultimo))
        assertEquals(SessionPhase.FINISHED, next.phase)
        assertEquals(listOf(ultimo), next.skipped)
    }

    @Test fun `tras tachar uno el dia termina en el que queda`() {
        // Se tacha el penúltimo: después del último ya no queda nada y se cierra.
        val n = dia.template.exercises.size
        val s = sesion(n - 2)
        val tachado = SessionEngine.dropExercise(s)
        assertEquals(n - 1, tachado.exerciseIndex)
        val series = dia.template.exercises[n - 1].let { com.marc.gymplan100.data.setCountFromScheme(it.scheme) }
        var cur = tachado
        repeat(series) { i ->
            cur = SessionEngine.completeSet(cur, "10", now = 1_000L * (i + 1), reps = "10")
            if (cur.phase == SessionPhase.RESTING) cur = SessionEngine.endRest(cur, 1_000L * (i + 1) + 500)
        }
        assertEquals(SessionPhase.FINISHED, cur.phase)
    }

    @Test fun `durante el descanso no se tacha nada`() {
        val s = sesion(0).copy(phase = SessionPhase.RESTING)
        assertTrue(SessionEngine.dropExercise(s) === s)
    }
}
