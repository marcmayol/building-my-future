package com.marc.gymplan100

import com.marc.gymplan100.data.FreeSessionEngine
import com.marc.gymplan100.data.MyExercises
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.RestDefaults
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Idea de Notion (5-nov-2026): en el entreno libre, lo que apuntas y la app no conoce se
 * guarda en el móvil y sale la próxima vez como un ejercicio más, y hay ejercicios «peso
 * corporal» que no piden kilos.
 */
class MyExercisesTest {

    private val conocidos = setOf("Press de banca", "Sentadilla")
    private fun conocido(nombre: String) = conocidos.any { it.equals(nombre, ignoreCase = true) }

    @After fun limpia() = MyExercises.set(emptyList(), emptySet())

    @Test fun `guarda solo lo que la app no conoce`() {
        val s = MyExercises.remember(ProgressState(), listOf("Press de banca", "Remo en TRX", " "), ::conocido)
        assertEquals(listOf("Remo en TRX"), s.customExercises)
    }

    @Test fun `el ultimo usado sube arriba y no se repite`() {
        var s = MyExercises.remember(ProgressState(), listOf("Remo en TRX", "Fondos en banco"), ::conocido)
        s = MyExercises.remember(s, listOf("fondos en banco"), ::conocido)
        assertEquals(listOf("fondos en banco", "Remo en TRX"), s.customExercises)
    }

    @Test fun `sin nada nuevo devuelve el mismo estado`() {
        val s = MyExercises.remember(ProgressState(), listOf("Remo en TRX"), ::conocido)
        assertSame(s, MyExercises.remember(s, listOf("Remo en TRX"), ::conocido))
        assertSame(s, MyExercises.remember(s, listOf("Sentadilla"), ::conocido))
    }

    @Test fun `no pasa del maximo`() {
        val muchos = (1..MyExercises.MAX + 20).map { "Ejercicio $it" }
        val s = MyExercises.remember(ProgressState(), muchos, ::conocido)
        assertEquals(MyExercises.MAX, s.customExercises.size)
    }

    @Test fun `el buscador encuentra los tuyos por el principio de las palabras`() {
        MyExercises.set(listOf("Remo en TRX", "Fondos en banco"), emptySet())
        assertEquals(listOf("Remo en TRX"), MyExercises.search("trx"))
        assertEquals(listOf("Fondos en banco"), MyExercises.search("fond ban"))
        assertEquals(2, MyExercises.search("").size)
        assertTrue(MyExercises.search("curl").isEmpty())
    }

    @Test fun `peso corporal se marca, se desmarca y se olvida`() {
        var s = MyExercises.setBodyweight(ProgressState(), "Dominadas en casa ", true)
        assertTrue("Dominadas en casa" in s.bodyweightExercises)
        s = MyExercises.setBodyweight(s, "Dominadas en casa", false)
        assertFalse("Dominadas en casa" in s.bodyweightExercises)

        s = MyExercises.remember(s, listOf("Dominadas en casa"), ::conocido)
        s = MyExercises.setBodyweight(s, "Dominadas en casa", true)
        s = MyExercises.forget(s, "Dominadas en casa")
        assertTrue(s.customExercises.isEmpty())
        assertTrue(s.bodyweightExercises.isEmpty())
    }

    @Test fun `un progreso guardado antes de esto se sigue leyendo`() {
        val json = Json { ignoreUnknownKeys = true }
        val viejo = json.decodeFromString<ProgressState>("""{"completedDays":[1,2]}""")
        assertTrue(viejo.customExercises.isEmpty())
        assertTrue(viejo.bodyweightExercises.isEmpty())
        val ida = json.encodeToString(ProgressState.serializer(),
            ProgressState(customExercises = listOf("Remo en TRX"), bodyweightExercises = setOf("Remo en TRX")))
        val vuelta = json.decodeFromString<ProgressState>(ida)
        assertEquals(listOf("Remo en TRX"), vuelta.customExercises)
        assertEquals(setOf("Remo en TRX"), vuelta.bodyweightExercises)
    }

    @Test fun `en el libre un ejercicio de peso corporal arranca con repeticiones y sin kilos`() {
        // Así lo llama el ViewModel: los kilos de «Mis pesos» sin los de peso corporal.
        val pesos = mapOf("Fondos" to "10") - setOf("Fondos")
        val reps = mapOf("Fondos" to "12")
        var s = FreeSessionEngine.endWarmup(FreeSessionEngine.start(dayNumber = 1, now = 0L, warmupSeconds = 60))
        s = FreeSessionEngine.setDone(s, 1_000L, RestDefaults.Base(), emptyMap(), pesos, reps)
        s = FreeSessionEngine.nameCurrent(s, "Fondos", pesos, reps)
        val serie = s.freeLog.single().sets.single()
        assertEquals("", serie.weight)
        assertEquals("12", serie.reps)
    }
}
