package com.marc.gymplan100

import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanEquipment
import com.marc.gymplan100.data.PlanImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El plan de casa se hace sin material y con los ejercicios que la app ya sabe dibujar: si uno
 * no tuviera ilustración ni ficha, se vería en el salón y no aquí.
 */
class PlanEnCasaTest {

    private val dir = File("src/main/assets/planes")

    private fun plan() =
        (PlanCodec.parseJson(File(dir, "en-casa.json").readText(), now = 0L) as PlanImport.Ok).plan

    @Test
    fun `esta en el catalogo`() {
        assertTrue("en-casa" in PlanCodec.decodeIdList(File(dir, "index.json").readText()))
    }

    @Test
    fun `ocho semanas de tres dias sin material`() {
        val p = plan()
        assertEquals(24, p.days.size)
        assertEquals(3, p.daysPerWeek)
        assertEquals(PlanEquipment.NONE, p.equipment)
    }

    @Test
    fun `todos sus ejercicios tienen ilustracion`() {
        val sinImagen = plan().days
            .flatMap { it.template.exercises }
            .map { it.name }
            .toSet()
            .filterNot { ExerciseImages.hasVisual(it) }
        assertTrue("Sin ilustración: $sinImagen", sinImagen.isEmpty())
    }
}
