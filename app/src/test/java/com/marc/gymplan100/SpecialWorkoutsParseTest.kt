package com.marc.gymplan100

import com.marc.gymplan100.data.SpecialWorkoutsLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parsea el asset real `entrenamientos_especiales.json` y comprueba que los modelos tipados
 * recogen su estructura completa (pasos militares, catálogo, protocolos y campos flexibles
 * como reps que llegan a veces como número y a veces como texto).
 */
class SpecialWorkoutsParseTest {

    private val json: String =
        File("src/main/assets/entrenamientos_especiales.json").readText()

    private val data = SpecialWorkoutsLoader.parse(json)

    @Test fun `se cargan las dos rutinas`() {
        assertEquals(2, data.rutinas.size)
        assertNotNull(data.militar)
        assertNotNull(data.quemaGrasa)
    }

    @Test fun `la militar tiene 13 pasos ordenados`() {
        val militar = data.militar!!
        assertEquals(13, militar.pasos.size)
        val ordenes = militar.pasosOrdenados.map { it.orden }
        assertEquals((1..13).toList(), ordenes)
        assertEquals(3, militar.frecuencia_semanal.max)
    }

    @Test fun `reps flexible - numero y AMRAP se leen como texto`() {
        val pasos = data.militar!!.pasosOrdenados
        assertEquals("AMRAP", pasos[1].reps)          // paso 2: "AMRAP"
        assertEquals("15", pasos[2].reps)             // paso 3: 15 (número)
        assertEquals("10 pasos por pierna", pasos[6].reps) // paso 7: texto
    }

    @Test fun `el paso de burpees tiene alternativa por tiempo`() {
        val burpees = data.militar!!.pasosOrdenados.first { it.nombre.startsWith("Burpees") }
        assertNotNull(burpees.alternativa)
        assertEquals("Jumping jacks", burpees.alternativa!!.nombre)
        assertEquals(45, burpees.alternativa!!.duracion_seg)
    }

    @Test fun `el paso isometrico usa el minimo del rango como objetivo`() {
        val iso = data.militar!!.pasosOrdenados.first { it.nombre.contains("isometrica") }
        assertEquals(30, iso.objetivoSeg)     // duracion_seg_min
        assertEquals("30-45 s", iso.etiquetaTiempo)
    }

    @Test fun `quema grasa tiene 9 ejercicios`() {
        assertEquals(9, data.quemaGrasa!!.ejercicios.size)
    }

    @Test fun `protocolo Tabata se reconoce como intervalos`() {
        val burpees = data.ejercicio("burpees_hiit")!!
        val tabata = burpees.protocolos.first { it.nombre == "Tabata" }
        assertTrue(tabata.esIntervalos)
        assertEquals(20, tabata.trabajoSeg)
        assertEquals(10, tabata.descansoSeg)
        assertEquals(8, tabata.numRondas)
    }

    @Test fun `protocolo de peso muerto se reconoce como series`() {
        val pesoMuerto = data.ejercicio("peso_muerto")!!
        val principiante = pesoMuerto.protocolos.first { it.nombre == "Principiante" }
        assertTrue(principiante.esSeries)
        assertEquals(3, principiante.numSeries)   // series_min
        assertEquals("6-8", principiante.repsLabel)
    }

    @Test fun `plancha se reconoce como tiempo unico`() {
        val plancha = data.ejercicio("plancha")!!
        val prog = plancha.protocolos.first()
        assertTrue(prog.esTiempoUnico)
        assertEquals(30, prog.duracion_inicial_seg)
    }

    @Test fun `cada ejercicio tiene su aviso de frecuencia`() {
        data.quemaGrasa!!.ejercicios.forEach {
            assertTrue("${it.id} sin aviso", it.aviso_frecuencia.isNotBlank())
            assertTrue("${it.id} sin frecuencia max", it.frecuencia_semanal.max > 0)
        }
    }
}
