package com.marc.gymplan100

import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.PlanMarkdownParser
import com.marc.gymplan100.data.PlanSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El Markdown es la vía cómoda para escribir un plan (a mano o pidiéndoselo a una IA), así que
 * el parser tiene que aguantar que cada uno lo escriba a su manera: distintos separadores, con
 * o sin fases, con líneas sueltas por medio.
 */
class PlanMarkdownTest {

    private fun ok(md: String): PlanImport.Ok {
        val result = PlanMarkdownParser.parse(md, now = 2_000L)
        assertTrue("Se esperaba un plan válido, salió: $result", result is PlanImport.Ok)
        return result as PlanImport.Ok
    }

    private fun errorOf(md: String): String {
        val result = PlanMarkdownParser.parse(md, now = 2_000L)
        assertTrue("Se esperaba un error, salió: $result", result is PlanImport.Error)
        return (result as PlanImport.Error).message
    }

    private val completo = """
        # Mi plan de fuerza
        Tres días por semana durante ocho semanas.

        ## Base (4 semanas)
        Aprender el movimiento con peso cómodo.
        Progresión: sube peso cuando las 10 salgan fáciles.

        ### Lunes — Empuje
        Calentamiento: 6 min bici + movilidad de hombros
        - Press de banca — 4 x 8
        - Press de hombros — 3 x 10 (sin bloquear el codo)
        - Plancha — 3 x 30 s
        Estiramientos: 5 min de espalda

        ### Jueves — Tirón
        - Jalón al pecho — 4 x 10

        ## Carga (2 semanas)

        ### Lunes — Fuerza
        - Sentadilla — 5 x 5
    """.trimIndent()

    @Test fun `se leen plan, fases y dias`() {
        val plan = ok(completo).plan
        assertEquals("Mi plan de fuerza", plan.name)
        assertEquals("Tres días por semana durante ocho semanas.", plan.description)
        assertEquals(2, plan.phases.size)
        assertEquals("Base", plan.phases[0].name)
        assertEquals(4, plan.phases[0].weeksCount)
        assertEquals("Carga", plan.phases[1].name)
        // 2 días x 4 semanas + 1 día x 2 semanas
        assertEquals(10, plan.totalDays)
        assertEquals(PlanSource.MARKDOWN, plan.source)
    }

    @Test fun `el dia guarda su nombre, titulo, calentamiento y estiramientos`() {
        val template = ok(completo).plan.dayByNumber(1)!!.template
        assertEquals("Lunes", template.weekday)
        assertEquals("Empuje", template.title)
        assertEquals("6 min bici + movilidad de hombros", template.warmup)
        assertEquals("5 min de espalda", template.cooldown)
    }

    @Test fun `los ejercicios separan nombre, series y nota`() {
        val exercises = ok(completo).plan.dayByNumber(1)!!.template.exercises
        assertEquals(3, exercises.size)
        assertEquals("Press de banca", exercises[0].name)
        assertEquals("4 x 8", exercises[0].scheme)
        assertEquals("Press de hombros", exercises[1].name)
        assertEquals("sin bloquear el codo", exercises[1].note)
        assertEquals("3 x 30 s", exercises[2].scheme)
    }

    @Test fun `la progresion de la fase se recoge`() {
        assertEquals(
            "sube peso cuando las 10 salgan fáciles.",
            ok(completo).plan.phases[0].progression
        )
    }

    @Test fun `sirve cualquier separador habitual`() {
        val plan = ok(
            """
            # Variantes
            ### Día A
            - Press de banca: 4 x 8
            - Remo | 4 x 10
            * Curl de bíceps - 3 x 12
            1. Prensa de piernas – 4 x 12
            - Sentadilla 5 x 5
            """.trimIndent()
        ).plan
        val exercises = plan.dayByNumber(1)!!.template.exercises
        assertEquals(
            listOf("Press de banca", "Remo", "Curl de bíceps", "Prensa de piernas", "Sentadilla"),
            exercises.map { it.name }
        )
        assertEquals(listOf("4 x 8", "4 x 10", "3 x 12", "4 x 12", "5 x 5"), exercises.map { it.scheme })
    }

    @Test fun `un guion dentro del nombre no parte el ejercicio`() {
        val exercise = ok(
            """
            # Nombres con guion
            ### Día A
            - Curl 21 - 3 x 12
            """.trimIndent()
        ).plan.dayByNumber(1)!!.template.exercises.first()
        assertEquals("Curl 21", exercise.name)
        assertEquals("3 x 12", exercise.scheme)
    }

    @Test fun `sin fases todo va a una fase unica`() {
        val plan = ok(
            """
            # Rutina simple
            ### Día A
            - Dominadas — 3 x 6
            ### Día B
            - Fondos — 3 x 8
            """.trimIndent()
        ).plan
        assertEquals(1, plan.phases.size)
        assertEquals(2, plan.totalDays)
    }

    @Test fun `se admite numerar las fases y las negritas`() {
        val plan = ok(
            """
            # Plan numerado
            ## Fase 1: Adaptación - 3 semanas
            ### **Lunes — Empuje**
            - **Press de banca** — 4 x 8
            """.trimIndent()
        ).plan
        assertEquals("Adaptación", plan.phases[0].name)
        assertEquals(3, plan.phases[0].weeksCount)
        assertEquals("Lunes", plan.dayByNumber(1)!!.template.weekday)
        assertEquals("Press de banca", plan.dayByNumber(1)!!.template.exercises.first().name)
    }

    @Test fun `sin titulo no hay plan y se dice como ponerlo`() {
        val message = errorOf(
            """
            ### Lunes
            - Press — 4 x 8
            """.trimIndent()
        )
        assertTrue(message, message.contains("# Nombre del plan"))
    }

    @Test fun `sin dias se explica como escribirlos`() {
        val message = errorOf("# Solo el título\nUn párrafo suelto.")
        assertTrue(message, message.contains("###"))
    }

    @Test fun `un ejercicio sin series se caza en la validacion comun`() {
        val message = errorOf(
            """
            # Plan incompleto
            ### Lunes
            - Press de banca
            """.trimIndent()
        )
        assertTrue(message, message.contains("Press de banca"))
    }
}
