package com.marc.gymplan100.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El borrador de lo apuntado durante un entreno libre, que vive en la sesión en curso. */
class FreeLogDraftTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `una sesion guardada antes de la 2_14 se lee con el borrador vacio`() {
        val vieja = """{"dayNumber":7,"startMillis":1000,"phase":"FREE","special":true}"""
        val s = json.decodeFromString<ActiveSession>(vieja)
        assertTrue(s.freeLog.isEmpty())
        assertFalse(s.hasFreeLog)
    }

    @Test
    fun `el borrador sobrevive a guardar y leer la sesion`() {
        val s = ActiveSession(
            dayNumber = 3, startMillis = 0, phase = SessionPhase.FREE,
            freeLog = listOf(
                LoggedExercise("Press banca", sets = listOf(SetLog("40", "10"), SetLog("42,5", "8")))
            )
        )
        val leida = json.decodeFromString<ActiveSession>(json.encodeToString(s))
        assertEquals(s.freeLog, leida.freeLog)
        assertTrue(leida.hasFreeLog)
    }

    @Test
    fun `filas con nombre pero sin series no cuentan como apuntado`() {
        val s = ActiveSession(
            dayNumber = 1, startMillis = 0, phase = SessionPhase.FREE,
            freeLog = listOf(LoggedExercise("Remo", sets = listOf(SetLog())))
        )
        assertFalse(s.hasFreeLog)
    }

    @Test
    fun `series sin nombre de ejercicio no cuentan como apuntado`() {
        val s = ActiveSession(
            dayNumber = 1, startMillis = 0, phase = SessionPhase.FREE,
            freeLog = listOf(LoggedExercise("", sets = listOf(SetLog("20", "12"))))
        )
        assertFalse(s.hasFreeLog)
    }
}
