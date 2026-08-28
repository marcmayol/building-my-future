package com.marc.gymplan100

import com.marc.gymplan100.data.Statistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Las calorías del entreno.
 *
 * La app ya las calculaba para mandarlas a Google Health, pero no las enseñaba en ningún sitio.
 * Al sacarlas a pantalla importa que el número sea el mismo que se guarda, y que no aparezca
 * ninguno cuando no hay con qué calcularlo.
 */
class CaloriesTest {

    @Test
    fun `la formula MET sale de peso y minutos`() {
        // 5 · 3,5 · 75 / 200 · 60 min = 393,75 -> 394
        assertEquals(394, Statistics.activeKcal(weightKg = 75, seconds = 3600))
        // La mitad de tiempo, la mitad de calorías.
        assertEquals(197, Statistics.activeKcal(weightKg = 75, seconds = 1800))
    }

    @Test
    fun `pesar mas quema mas en el mismo tiempo`() {
        val ligero = Statistics.activeKcal(60, 3600)!!
        val pesado = Statistics.activeKcal(90, 3600)!!
        assert(pesado > ligero)
    }

    @Test
    fun `sin peso no se inventa un numero`() {
        assertNull(Statistics.activeKcal(weightKg = 0, seconds = 3600))
        assertNull(Statistics.activeKcal(weightKg = -5, seconds = 3600))
    }

    @Test
    fun `sin tiempo tampoco`() {
        assertNull(Statistics.activeKcal(weightKg = 75, seconds = 0))
        assertNull(Statistics.activeKcal(weightKg = 75, seconds = -10))
    }

    @Test
    fun `un entreno de dos minutos no da cero`() {
        // Redondear a cero seria ensenar "0 kcal", que parece un fallo. O hay numero o no hay.
        val kcal = Statistics.activeKcal(weightKg = 75, seconds = 120)
        assertEquals(13, kcal)
    }
}
