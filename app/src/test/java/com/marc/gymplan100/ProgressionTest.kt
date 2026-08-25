package com.marc.gymplan100

import com.marc.gymplan100.data.Progression
import com.marc.gymplan100.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La regla que hace que el peso suba solo.
 *
 * Hasta la 2.4 la app repetía el último peso para siempre: progresar dependía de acordarse.
 * Estas pruebas fijan cuándo toca subir, cuándo toca insistir y cuándo toca bajar para
 * desatascarse, que es la parte que nadie hace por su cuenta.
 */
class ProgressionTest {

    private fun serie(kg: String, reps: String) = SetLog(weight = kg, reps = reps)

    /** Una sesión de tres series iguales. */
    private fun sesion(kg: String, reps: String) = List(3) { serie(kg, reps) }

    @Test
    fun `sin historial no inventa un peso`() {
        val s = Progression.suggest("3 x 8-12", emptyList())!!
        assertEquals(Progression.Kind.FIRST_TIME, s.kind)
        assertEquals("", s.weight)
        assertEquals("8", s.reps)
    }

    @Test
    fun `cerrar el rango en todas las series sube el peso y vuelve al suelo`() {
        val s = Progression.suggest("3 x 8-12", listOf(sesion("20", "12")))!!
        assertEquals(Progression.Kind.MORE_WEIGHT, s.kind)
        assertEquals("22,5", s.weight)
        assertEquals("8", s.reps)
    }

    @Test
    fun `por debajo de veinte kilos se sube de kilo en kilo`() {
        val s = Progression.suggest("3 x 8-12", listOf(sesion("10", "12")))!!
        assertEquals(Progression.Kind.MORE_WEIGHT, s.kind)
        assertEquals("11", s.weight)
    }

    @Test
    fun `sin cerrar el rango se mantiene el peso y se pide una repeticion mas`() {
        val s = Progression.suggest("3 x 8-12", listOf(sesion("20", "8")))!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
        assertEquals("20", s.weight)
        assertEquals("9", s.reps)
    }

    @Test
    fun `el rango solo se cierra si estan TODAS las series del plan`() {
        // Dos series a doce de las tres que pide el plan: aún no toca subir.
        val s = Progression.suggest("3 x 8-12", listOf(listOf(serie("20", "12"), serie("20", "12"))))!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
        assertEquals("20", s.weight)
    }

    @Test
    fun `tres sesiones sin mejorar proponen bajar un diez por ciento`() {
        val atascado = List(Progression.STALL_SESSIONS) { sesion("20", "8") }
        val s = Progression.suggest("3 x 8-12", atascado)!!
        assertEquals(Progression.Kind.DELOAD, s.kind)
        assertEquals("18", s.weight)
        assertTrue(s.reason.contains("Baja"))
    }

    @Test
    fun `dos sesiones iguales todavia no son un atasco`() {
        val s = Progression.suggest("3 x 8-12", listOf(sesion("20", "8"), sesion("20", "8")))!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
    }

    @Test
    fun `mejorar repeticiones con el mismo peso no es estar atascado`() {
        val historial = listOf(sesion("20", "10"), sesion("20", "9"), sesion("20", "8"))
        val s = Progression.suggest("3 x 8-12", historial)!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
        assertEquals("11", s.reps)
    }

    @Test
    fun `cambiar de peso rompe el atasco aunque las repeticiones no suban`() {
        val historial = listOf(sesion("22,5", "8"), sesion("20", "8"), sesion("20", "8"))
        val s = Progression.suggest("3 x 8-12", historial)!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
    }

    @Test
    fun `un esquema por tiempo o por vueltas no tiene progresion que calcular`() {
        assertNull(Progression.suggest("3 x 30 s", listOf(sesion("0", "30"))))
        assertNull(Progression.suggest("3 vueltas", listOf(sesion("0", "10"))))
    }

    @Test
    fun `sin repeticiones apuntadas no se propone nada`() {
        val soloPesos = listOf(List(3) { SetLog(weight = "20") })
        assertNull(Progression.suggest("3 x 8-12", soloPesos))
    }

    // --- Peso corporal: no hay disco que añadir -------------------------------

    @Test
    fun `sin kilos se progresa en repeticiones`() {
        val dominadas = listOf(List(3) { serie("", "8") })
        val s = Progression.suggest("3 x 8-12", dominadas)!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
        assertEquals("", s.weight)
        assertEquals("9", s.reps)
    }

    @Test
    fun `sin kilos, cerrar el rango pide una variante mas dificil`() {
        val dominadas = listOf(List(3) { serie("", "12") })
        val s = Progression.suggest("3 x 8-12", dominadas)!!
        assertEquals(Progression.Kind.MORE_REPS, s.kind)
        assertEquals("", s.weight)
        assertTrue(s.reason.contains("variante"))
    }

    @Test
    fun `los pesos propuestos siempre caen en medio kilo`() {
        // 17 kg cerrando rango: 17 + 1 = 18. 17,3 no existiría en ninguna máquina.
        val s = Progression.suggest("3 x 10", listOf(sesion("17", "10")))!!
        assertEquals("18", s.weight)
        assertEquals(0.0, Progression.roundToStep(21.3) % 0.5, 1e-9)
    }

    @Test
    fun `un numero fijo de repeticiones tambien cierra rango`() {
        // "3 x 10" es un rango de 10 a 10: hacer las tres series a diez es cerrarlo.
        val s = Progression.suggest("3 x 10", listOf(sesion("30", "10")))!!
        assertEquals(Progression.Kind.MORE_WEIGHT, s.kind)
        assertEquals("32,5", s.weight)
    }
}
