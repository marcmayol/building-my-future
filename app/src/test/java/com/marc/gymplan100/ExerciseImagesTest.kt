package com.marc.gymplan100

import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.PlanData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ningún ejercicio del plan puede quedarse sin ilustración.
 *
 * Nació de un hueco real: la superserie salía sin miniatura en el plan del día, y solo se
 * vio entrenando. Este test recorre los 100 días y canta el nombre exacto que falta, así
 * que añadir un ejercicio nuevo sin imagen se detecta aquí y no en el gimnasio.
 */
class ExerciseImagesTest {

    private fun nombresDelPlan(): Set<String> =
        (1..PlanData.TOTAL_DAYS)
            .mapNotNull { PlanData.dayByNumber(it) }
            .flatMap { it.template.exercises }
            .map { it.name }
            .toSet()

    @Test
    fun `todos los ejercicios del plan tienen ilustracion`() {
        val sinImagen = nombresDelPlan().filterNot { ExerciseImages.hasVisual(it) }

        assertTrue(
            "Estos ejercicios del plan no tienen ilustración: $sinImagen",
            sinImagen.isEmpty(),
        )
    }

    @Test
    fun `una superserie se ilustra con los movimientos que la componen`() {
        // No tiene imagen propia: la miniatura sale del primero de sus movimientos.
        assertTrue(ExerciseImages.hasVisual("Superserie curl + extensión"))
        assertTrue(ExerciseImages.hasVisual("Superserie curl + extensión de tríceps"))
    }
}
