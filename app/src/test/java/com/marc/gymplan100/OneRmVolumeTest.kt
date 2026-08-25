package com.marc.gymplan100

import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.Statistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fuerza estimada y kilos movidos.
 *
 * La gráfica de kilos engaña cuando cambian las repeticiones: 40 kg x 12 y 50 kg x 5 parecen
 * un salto enorme y son casi el mismo esfuerzo. Estas dos cuentas son las que lo enderezan.
 */
class OneRmVolumeTest {

    @Test
    fun `la formula de Epley sale de peso por repeticiones`() {
        // 40 x (1 + 10/30) = 53,33
        assertEquals(53.33f, Statistics.estimatedOneRm(40f, 10)!!, 0.01f)
        // Una sola repetición ya es el máximo: no hay nada que estimar.
        assertEquals(20f, Statistics.estimatedOneRm(20f, 1)!!, 0.01f)
    }

    @Test
    fun `por encima de doce repeticiones la formula deja de valer`() {
        assertNull(Statistics.estimatedOneRm(40f, 13))
        assertNull(Statistics.estimatedOneRm(40f, 0))
        assertNull(Statistics.estimatedOneRm(0f, 10))
    }

    @Test
    fun `de las series se queda con la que mas fuerza demuestra`() {
        val series = listOf(
            SetLog("40", "12"),   // 56
            SetLog("50", "5"),    // 58,33
            SetLog("45", "8")     // 57
        )
        assertEquals(58.33f, Statistics.bestOneRm(series)!!, 0.01f)
    }

    @Test
    fun `una serie sin repeticiones no estima nada`() {
        assertNull(Statistics.bestOneRm(listOf(SetLog("40"))))
        assertNull(Statistics.bestOneRm(emptyList()))
    }

    @Test
    fun `los kilos movidos son la suma de peso por repeticiones`() {
        val series = listOf(SetLog("10", "12"), SetLog("12", "10"), SetLog("12", "8"))
        // 120 + 120 + 96
        assertEquals(336f, Statistics.volume(series), 0.01f)
    }

    @Test
    fun `lo que no tiene repeticiones no suma kilos movidos`() {
        val series = listOf(SetLog("10", "12"), SetLog("50"), SetLog("", "10"))
        assertEquals(120f, Statistics.volume(series), 0.01f)
    }
}
