package com.marc.gymplan100

import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.ExerciseKind
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanEquipment
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.PlanLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Los dos planes para quien le cuesta moverse: uno en casa y otro en el gimnasio. Lo que
 * importa es que sean de verdad suaves y que todo lo que piden se vea dibujado: a quien empieza
 * así no se le puede dejar adivinando un ejercicio.
 */
class PlanPrimerosPasosTest {

    private val dir = File("src/main/assets/planes")
    private val ids = listOf("primeros-pasos-casa", "primeros-pasos-gimnasio")

    private fun plan(id: String) =
        (PlanCodec.parseJson(File(dir, "$id.json").readText(), now = 0L) as PlanImport.Ok).plan

    @Test
    fun `estan en el catalogo, los primeros`() {
        val indice = PlanCodec.decodeIdList(File(dir, "index.json").readText())
        assertEquals(ids, indice.take(2))
    }

    @Test
    fun `ocho semanas de tres dias, suaves y para empezar`() {
        for (id in ids) {
            val p = plan(id)
            assertEquals(id, 24, p.days.size)
            assertEquals(id, 3, p.daysPerWeek)
            assertEquals(id, PlanLevel.GENTLE, p.level)
            assertEquals(id, PlanGoal.START, p.goal)
        }
        assertEquals(PlanEquipment.NONE, plan("primeros-pasos-casa").equipment)
        assertEquals(PlanEquipment.GYM, plan("primeros-pasos-gimnasio").equipment)
    }

    @Test
    fun `todo lo que no es cardio tiene ilustracion`() {
        for (id in ids) {
            val sinImagen = plan(id).days
                .flatMap { it.template.exercises }
                .filter { it.kind != ExerciseKind.CARDIO }
                .map { it.name }
                .toSet()
                .filterNot { ExerciseImages.hasVisual(it) }
            assertTrue("$id sin ilustración: $sinImagen", sinImagen.isEmpty())
        }
    }

    @Test
    fun `nunca mas de tres series ni de seis ejercicios`() {
        for (id in ids) {
            for (dia in plan(id).days) {
                val ejercicios = dia.template.exercises
                assertTrue("$id día ${dia.number}", ejercicios.size <= 6)
                for (e in ejercicios) {
                    val series = Regex("""^(\d+)\s*x""").find(e.scheme)?.groupValues?.get(1)?.toInt() ?: 1
                    assertTrue("$id ${e.name} ${e.scheme}", series <= 3)
                }
            }
        }
    }

    @Test
    fun `en casa no se baja al suelo`() {
        // Para quien le cuesta moverse, levantarse del suelo ya es el ejercicio más difícil.
        val alSuelo = listOf("puente", "bird dog", "dead bug", "tumbado", "cuadrupedia", "plancha (")
        val nombres = plan("primeros-pasos-casa").days
            .flatMap { it.template.exercises }
            .map { it.name.lowercase() }
            .toSet()
        val malos = nombres.filter { n -> alSuelo.any { it in n } }
        assertTrue("Ejercicios en el suelo: $malos", malos.isEmpty())
    }
}
