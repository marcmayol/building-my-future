package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.CatalogEntry
import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.Progression
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.Statistics
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Hacer otro ejercicio en lugar del del plan, y que quede apuntado como lo que fue.
 *
 * Primera crítica de un usuario (21-oct-2026): «no tengo pec deck en casa, tengo un banco; he
 * hecho aperturas planas, pero no lo he podido dejar reflejado. Me ha pasado lo mismo con las
 * dominadas con goma». La ficha le decía qué hacer, y luego la serie se apuntaba como pec deck.
 */
class ExerciseSwapTest {

    companion object {
        @BeforeClass @JvmStatic
        fun catalogo() {
            ExerciseCatalog.set(
                Json { ignoreUnknownKeys = true }.decodeFromString<List<CatalogEntry>>(
                    File("src/main/assets/catalogo.json").readText()
                )
            )
        }
    }

    // --- Las alternativas de las fichas, enlazadas al catálogo

    private fun match(texto: String) = ExerciseCatalog.matchAlternative(texto)?.nombre

    @Test fun `las del usuario, enlazadas a su ejercicio`() {
        assertEquals("Aperturas con mancuernas", match("Aperturas con mancuernas en banco."))
        assertEquals("Dominadas", match("Dominada asistida en máquina o con goma."))
        assertEquals("Aperturas en máquina (pec deck)", match("Pec deck o aperturas en máquina."))
    }

    @Test fun `la mas concreta gana a la generica`() {
        assertEquals("Press de banca con mancuernas", match("Press con mancuernas en banco plano."))
        assertEquals("Cruce de poleas", match("Cruce de poleas (cable crossover)."))
    }

    @Test fun `un consejo no es un ejercicio`() {
        assertNull(match("Hazlas sentado para más estabilidad."))
        assertNull(match("Más cerca de la pared si hoy cuesta."))
        assertNull(match(""))
    }

    @Test fun `todo el catalogo tiene dibujo y musculos`() {
        val sinMusculos = ExerciseCatalog.all.filter { MuscleTargets.forName(it.nombre) == null }.map { it.nombre }
        assertTrue("Sin músculos: $sinMusculos", sinMusculos.isEmpty())
        val sinDibujo = ExerciseCatalog.all.filterNot { e ->
            listOf("m", "f").all { g ->
                listOf("jpg", "webp").any { ext ->
                    File("src/main/res/drawable-nodpi/ex_${e.slug.replace('-', '_')}_$g.$ext").exists()
                }
            }
        }.map { it.slug }
        assertTrue("Sin dibujo: $sinDibujo", sinDibujo.isEmpty())
        assertTrue(ExerciseImages.hasVisual("Aperturas con mancuernas"))
    }

    // --- La sesión y el registro

    /** Día del plan integrado con pec deck (el primero que lo tenga). */
    private val dia = PlanData.days.first { d -> d.template.exercises.any { it.name.startsWith("Pec deck") } }
    private val pecDeck = dia.template.exercises.indexOfFirst { it.name.startsWith("Pec deck") }
    private val nombrePlan = dia.template.exercises[pecDeck].name

    private fun sesion(swaps: Map<Int, String>) = ActiveSession(
        dayNumber = dia.number, startMillis = 0L, exerciseIndex = pecDeck,
        phase = SessionPhase.WORKING, order = dia.template.exercises.indices.toList(), swaps = swaps
    )

    @Test fun `la sesion trabaja con el ejercicio cambiado`() {
        val s = sesion(mapOf(pecDeck to "Aperturas con mancuernas"))
        assertEquals("Aperturas con mancuernas", s.exerciseAt(pecDeck)?.name)
        assertEquals(nombrePlan, s.planNameAt(pecDeck))
        // Sus kilos, no los de la máquina.
        val pesos = mapOf(nombrePlan to "45", "Aperturas con mancuernas" to "10")
        assertEquals("10", SessionEngine.weightForSet(s, pesos))
    }

    @Test fun `al cerrar el dia se apunta lo que fue y sus kilos van a su nombre`() {
        val sets = listOf(
            CompletedSet(pecDeck, 1, "10", reps = "12"),
            CompletedSet(pecDeck, 2, "12", reps = "10")
        )
        val antes = ProgressState(exerciseWeights = mapOf(nombrePlan to "45"))
        val despues = antes.withFinishedDay(dia.number, sets, mapOf(pecDeck to "Aperturas con mancuernas"))
        val log = despues.logs["${dia.number}-$pecDeck"]
        assertNotNull(log)
        assertEquals("Aperturas con mancuernas", log!!.doneAs)
        assertEquals("12", despues.exerciseWeights["Aperturas con mancuernas"])
        // El pec deck no se toca: no se ha subido a la máquina.
        assertEquals("45", despues.exerciseWeights[nombrePlan])
        // Y la historia y las gráficas lo leen como lo que fue.
        assertEquals(1, Progression.historyOf(despues, "Aperturas con mancuernas").size)
        assertTrue(Progression.historyOf(despues, nombrePlan).isEmpty())
        assertTrue("Aperturas con mancuernas" in Statistics.weightProgression(despues).keys)
    }

    @Test fun `sin cambio todo sigue como siempre`() {
        val sets = listOf(CompletedSet(pecDeck, 1, "40", reps = "12"))
        val despues = ProgressState().withFinishedDay(dia.number, sets)
        assertEquals("", despues.logs["${dia.number}-$pecDeck"]!!.doneAs)
        assertEquals("40", despues.exerciseWeights[nombrePlan])
    }

    @Test fun `un cambio recordado arranca puesto`() {
        val s = SessionEngine.startSession(dia.number, 0L, swaps = mapOf(pecDeck to "Aperturas con mancuernas"))
        assertEquals("Aperturas con mancuernas", s.exerciseAt(pecDeck)?.name)
    }
}
