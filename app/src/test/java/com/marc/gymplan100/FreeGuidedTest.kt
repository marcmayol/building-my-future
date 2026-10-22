package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.FreeSessionEngine
import com.marc.gymplan100.data.FreeStep
import com.marc.gymplan100.data.LoggedExercise
import com.marc.gymplan100.data.RestDefaults
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El entreno libre guiado (22-oct-2026, idea de Marc): calentar, «Serie terminada» con el
 * descanso arrancando en el acto, el ejercicio se pregunta descansando, «Terminar ejercicio»
 * da el descanso largo, y al final todo está apuntado.
 */
class FreeGuidedTest {

    private val base = RestDefaults.Base(sets = 90, exercises = 120)

    private fun trabajando() = FreeSessionEngine.endWarmup(
        FreeSessionEngine.start(dayNumber = 1, now = 0L, warmupSeconds = 360)
    )

    @Test fun `empieza calentando y ya he calentado pasa a la serie`() {
        val s = FreeSessionEngine.start(dayNumber = 1, now = 0L, warmupSeconds = 360)
        assertEquals(SessionPhase.FREE, s.phase)
        assertEquals(FreeStep.WARMUP, s.freeStep)
        assertEquals(360, s.warmupTargetSeconds)
        assertEquals(FreeStep.WORK, FreeSessionEngine.endWarmup(s).freeStep)
    }

    @Test fun `sin calentamiento va directo a la serie`() {
        assertEquals(FreeStep.WORK, FreeSessionEngine.start(1, 0L).freeStep)
    }

    @Test fun `un libre de antes sigue en su cronometro`() {
        // Las sesiones guardadas antes de la 2.21 no traen freeStep: valen WORK.
        assertEquals(FreeStep.WORK, ActiveSession(dayNumber = 1, startMillis = 0L, phase = SessionPhase.FREE).freeStep)
    }

    @Test fun `serie terminada arranca el descanso al momento con un ejercicio sin nombre`() {
        val s = FreeSessionEngine.setDone(trabajando(), now = 5_000L, restBase = base)
        assertEquals(FreeStep.REST, s.freeStep)
        assertEquals(5_000L, s.restStartMillis)
        assertEquals(90, s.restTargetSeconds)
        assertEquals(0, s.freeCurrent)
        assertEquals("", s.freeLog[0].name)
        assertEquals(1, s.freeLog[0].sets.size)
    }

    @Test fun `al ponerle nombre se rellena con lo de la ultima vez`() {
        val s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        val nombrado = FreeSessionEngine.nameCurrent(
            s, "Press de banca", weights = mapOf("Press de banca" to "60"), reps = mapOf("Press de banca" to "8")
        )
        assertEquals("Press de banca", nombrado.freeLog[0].name)
        assertEquals(SetLog("60", "8"), nombrado.freeLog[0].sets[0])
    }

    @Test fun `la segunda serie sale con lo de la primera y el descanso que pusiste`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.nameCurrent(s, "Remo")
        s = FreeSessionEngine.editLastSet(s, "40", "10")
        s = FreeSessionEngine.endRest(s)
        s = FreeSessionEngine.setDone(s, 100_000L, base, yourRests = mapOf("Remo" to 75))
        assertEquals(listOf(SetLog("40", "10"), SetLog("40", "10")), s.freeLog[0].sets)
        assertEquals(75, s.restTargetSeconds)
    }

    @Test fun `al fallo suma un minuto y cambiar a justo lo corrige`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.setLastRir(s, "0")
        assertEquals(150, s.restTargetSeconds)
        s = FreeSessionEngine.setLastRir(s, "1")
        assertEquals(120, s.restTargetSeconds)
    }

    @Test fun `terminar ejercicio descansando alarga ese mismo descanso`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.nameCurrent(s, "Remo")
        val fin = FreeSessionEngine.finishExercise(s, now = 20_000L, restBase = base)
        assertTrue(fin.restBetweenExercises)
        assertEquals(-1, fin.freeCurrent)
        // Mismo inicio: el descanso ya iba corriendo.
        assertEquals(5_000L, fin.restStartMillis)
        assertEquals(120, fin.restTargetSeconds)
    }

    @Test fun `terminar ejercicio respeta lo que sumo el fallo`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.setLastRir(s, "0")
        assertEquals(180, FreeSessionEngine.finishExercise(s, 20_000L, base).restTargetSeconds)
    }

    @Test fun `terminar ejercicio con la serie delante empieza el descanso ahora`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.endRest(FreeSessionEngine.nameCurrent(s, "Remo"))
        val fin = FreeSessionEngine.finishExercise(s, now = 50_000L, restBase = base)
        assertEquals(FreeStep.REST, fin.freeStep)
        assertEquals(50_000L, fin.restStartMillis)
        assertEquals(120, fin.restTargetSeconds)
    }

    @Test fun `tras cambiar de ejercicio la siguiente serie vuelve a preguntar`() {
        var s = FreeSessionEngine.setDone(trabajando(), 5_000L, base)
        s = FreeSessionEngine.nameCurrent(s, "Remo")
        s = FreeSessionEngine.endRest(FreeSessionEngine.finishExercise(s, 20_000L, base))
        s = FreeSessionEngine.setDone(s, 200_000L, base)
        assertEquals(2, s.freeLog.size)
        assertEquals(1, s.freeCurrent)
        assertEquals("", s.freeLog[1].name)
    }

    @Test fun `volver a un ejercicio de hoy junta las series`() {
        val s = trabajando().copy(
            freeLog = listOf(
                LoggedExercise("Press de banca", sets = listOf(SetLog("60", "8"))),
                LoggedExercise("", sets = listOf(SetLog()))
            ),
            freeCurrent = 1,
            freeStep = FreeStep.REST
        )
        val junto = FreeSessionEngine.nameCurrent(s, "press de banca")
        assertEquals(1, junto.freeLog.size)
        assertEquals(0, junto.freeCurrent)
        assertEquals(2, junto.freeLog[0].sets.size)
    }

    @Test fun `nada se mueve fuera de su momento`() {
        val calentando = FreeSessionEngine.start(1, 0L, warmupSeconds = 300)
        assertTrue(FreeSessionEngine.setDone(calentando, 1L) === calentando)
        val trabajando = trabajando()
        assertTrue(FreeSessionEngine.endRest(trabajando) === trabajando)
        assertTrue(FreeSessionEngine.finishExercise(trabajando, 1L) === trabajando)
    }
}
