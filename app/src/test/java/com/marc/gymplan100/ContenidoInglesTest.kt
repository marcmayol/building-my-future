package com.marc.gymplan100

import com.marc.gymplan100.data.Contenido
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Con la app en inglés, el contenido de serie sale en inglés: ningún nombre de ejercicio,
 * texto de plan, ficha o entrenamiento especial se queda sin traducir, y los esquemas se
 * leen en inglés sin tocar el que guarda el plan.
 */
class ContenidoInglesTest {

    private val dic: Map<String, String> =
        Json.decodeFromString(File(ContenidoFuentes.assets, "en/contenido.json").readText())

    @Before
    fun ingles() = Contenido.set(ingles = true, dic = dic)

    @After
    fun castellano() = Contenido.set(ingles = false, dic = emptyMap())

    @Test
    fun todos_los_nombres_de_ejercicio_tienen_traduccion() {
        val faltan = ContenidoFuentes.nombres().filterNot(Contenido::traducible)
        assertTrue("Sin traducir: $faltan", faltan.isEmpty())
    }

    @Test
    fun todos_los_textos_de_serie_tienen_traduccion() {
        val faltan = ContenidoFuentes.textos().filterNot(Contenido::traducible)
        assertTrue("Sin traducir: $faltan", faltan.isEmpty())
    }

    @Test
    fun las_fichas_del_catalogo_tienen_traduccion() {
        val fichas: Map<String, com.marc.gymplan100.data.ExerciseGuide> =
            Json.decodeFromString(File(ContenidoFuentes.assets, "fichas.json").readText())
        val faltan = fichas.values.flatMap { g ->
            listOf(g.muscles, g.howTo, g.videoQuery) + g.mistakes + g.alternatives
        }.filter { it.isNotBlank() }.filterNot(Contenido::traducible)
        assertTrue("Sin traducir: $faltan", faltan.isEmpty())
    }

    @Test
    fun ningun_esquema_se_queda_en_castellano() {
        val castellano = Regex("""(?i)\b(por|lado|pierna|vueltas?|series?|m[áa]ximo|trabajo|descanso|cada)\b""")
        val malos = ContenidoFuentes.esquemas().map(Contenido::esquema).filter { castellano.containsMatchIn(it) }
        assertTrue("Esquemas en castellano: $malos", malos.isEmpty())
        assertEquals("3 x 12 per side", Contenido.esquema("3 x 12 por lado"))
        assertEquals("4 rounds · 40 s work / 20 s rest", Contenido.esquema("4 vueltas · 40 s trabajo / 20 s descanso"))
    }

    @Test
    fun lo_generado_al_leer_un_plan_tambien() {
        assertEquals("Day 3", Contenido.t("Día 3"))
        assertEquals("Days 1 to 20", Contenido.t("Días 1 al 20"))
        assertEquals("Weeks 5-10", Contenido.t("Semanas 5-10"))
    }

    @Test
    fun la_clave_sigue_en_castellano() {
        val en = Contenido.t("Prensa de piernas")
        assertTrue(en != "Prensa de piernas")
        assertEquals("Prensa de piernas", Contenido.clave(en))
        // Lo que no es una traducción conocida (un ejercicio tuyo) se queda como lo escribiste.
        assertEquals("Mi press raro", Contenido.clave("Mi press raro"))
        assertEquals("Mi press raro", Contenido.t("Mi press raro"))
    }

    @Test
    fun en_castellano_no_cambia_nada() {
        Contenido.set(ingles = false, dic = dic)
        assertEquals("Prensa de piernas", Contenido.t("Prensa de piernas"))
        assertEquals("3 x 12 por lado", Contenido.esquema("3 x 12 por lado"))
        assertEquals("Leg press", Contenido.clave("Leg press"))
    }
}
