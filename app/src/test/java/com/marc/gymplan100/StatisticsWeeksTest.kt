package com.marc.gymplan100

import com.marc.gymplan100.data.Statistics
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * La gráfica de entrenos por semana tiene que leerse sin explicación.
 *
 * Las semanas vacías anteriores al primer entreno no informan de nada y hacían dudar del
 * orden de las columnas, así que se recortan; pero si aún no hay ningún entreno la
 * gráfica no puede quedarse en blanco.
 */
class StatisticsWeeksTest {

    private val lunes = LocalDate.of(2026, 5, 18)

    private fun semanas(vararg cuentas: Int): List<Statistics.WeekCount> =
        cuentas.mapIndexed { i, n ->
            Statistics.WeekCount(lunes.plusWeeks(i.toLong()), n)
        }

    @Test
    fun `las semanas vacias del principio no se pintan`() {
        val recortadas = Statistics.trimLeadingEmpty(semanas(0, 0, 0, 0, 4, 5, 3))

        assertEquals(3, recortadas.size)
        assertEquals(4, recortadas.first().count)
    }

    @Test
    fun `una semana vacia por el medio si se pinta`() {
        // Un parón cuenta la verdad de tu constancia: solo se recorta el principio.
        val recortadas = Statistics.trimLeadingEmpty(semanas(0, 4, 0, 5))

        assertEquals(listOf(4, 0, 5), recortadas.map { it.count })
    }

    @Test
    fun `sin ningun entreno la grafica no se queda vacia`() {
        val recortadas = Statistics.trimLeadingEmpty(semanas(0, 0, 0, 0, 0, 0), minimo = 4)

        assertEquals(4, recortadas.size)
    }

    @Test
    fun `la ultima semana devuelta es siempre la actual`() {
        val todas = semanas(0, 0, 4, 5)

        assertEquals(todas.last(), Statistics.trimLeadingEmpty(todas).last())
    }
}
