package com.marc.gymplan100

import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.PlanSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Importación de planes propios: lo que el usuario escriba (o le pida a una IA) tiene que
 * acabar en un plan usable o en un error que diga exactamente qué corregir. Un plan a medias
 * que revienta a mitad de sesión en el gimnasio no tiene arreglo.
 */
class PlanImportTest {

    private fun ok(json: String): PlanImport.Ok {
        val result = PlanCodec.parseJson(json, now = 1_000L)
        assertTrue("Se esperaba un plan válido, salió: $result", result is PlanImport.Ok)
        return result as PlanImport.Ok
    }

    private fun errorOf(json: String): String {
        val result = PlanCodec.parseJson(json, now = 1_000L)
        assertTrue("Se esperaba un error, salió: $result", result is PlanImport.Error)
        return (result as PlanImport.Error).message
    }

    private val conFases = """
        {
          "nombre": "Fuerza básica",
          "descripcion": "Dos días por semana.",
          "fases": [
            {
              "nombre": "Base",
              "semanas": 3,
              "dias": [
                {
                  "dia": "Lunes",
                  "titulo": "Empuje",
                  "calentamiento": "5-8 min bici",
                  "ejercicios": [
                    { "nombre": "Press de banca", "esquema": "4 x 8" },
                    { "nombre": "Plancha", "esquema": "3 x 30 s", "nota": "Sin arquear" }
                  ]
                },
                {
                  "dia": "Jueves",
                  "titulo": "Tirón",
                  "ejercicios": [ { "nombre": "Remo", "esquema": "4 x 10" } ]
                }
              ]
            },
            {
              "nombre": "Carga",
              "semanas": 2,
              "dias": [
                { "dia": "Lunes", "ejercicios": [ { "nombre": "Sentadilla", "esquema": "5 x 5" } ] }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test fun `las semanas multiplican los dias de cada fase`() {
        val plan = ok(conFases).plan
        // Fase 1: 2 días x 3 semanas = 6. Fase 2: 1 día x 2 semanas = 2.
        assertEquals(8, plan.totalDays)
        assertEquals(6, plan.daysOfPhase(1).size)
        assertEquals(2, plan.daysOfPhase(2).size)
    }

    @Test fun `los rangos de dias y semanas se calculan solos`() {
        val plan = ok(conFases).plan
        assertEquals("Días 1 al 6", plan.phases[0].range)
        assertEquals("Semanas 1-3", plan.phases[0].weeks)
        assertEquals("Días 7 al 8", plan.phases[1].range)
        assertEquals("Semanas 4-5", plan.phases[1].weeks)
    }

    @Test fun `el dia 7 ya es de la segunda fase y repite la plantilla`() {
        val plan = ok(conFases).plan
        val day7 = plan.dayByNumber(7)!!
        assertEquals(2, day7.phase.number)
        assertEquals("Sentadilla", day7.template.exercises.first().name)
        // La semana dentro de la fase avanza al repetirse la plantilla.
        assertEquals(1, plan.weekWithinPhase(day7))
        assertEquals(2, plan.weekWithinPhase(plan.dayByNumber(8)!!))
    }

    @Test fun `un plan sin fases se envuelve en una sola`() {
        val plan = ok(
            """
            {
              "nombre": "Rutina corta",
              "dias": [
                { "dia": "Día A", "ejercicios": [ { "nombre": "Dominadas", "esquema": "3 x 6" } ] },
                { "dia": "Día B", "ejercicios": [ { "nombre": "Fondos", "esquema": "3 x 8" } ] }
              ]
            }
            """.trimIndent()
        ).plan
        assertEquals(1, plan.phases.size)
        assertEquals(2, plan.totalDays)
        assertEquals("Rutina corta", plan.phases.first().name)
    }

    @Test fun `se aceptan las claves en ingles`() {
        val plan = ok(
            """
            {
              "name": "English plan",
              "days": [
                {
                  "weekday": "Monday",
                  "title": "Push",
                  "warmup": "6 min bike",
                  "exercises": [ { "name": "Bench press", "scheme": "4 x 8" } ]
                }
              ]
            }
            """.trimIndent()
        ).plan
        assertEquals("English plan", plan.name)
        assertEquals("Bench press", plan.dayByNumber(1)!!.template.exercises.first().name)
    }

    @Test fun `los huecos cosmeticos se rellenan solos`() {
        val plan = ok(
            """
            { "nombre": "Mínimo", "dias": [ { "ejercicios": [ { "nombre": "Prensa", "esquema": "3 x 12" } ] } ] }
            """.trimIndent()
        ).plan
        val template = plan.dayByNumber(1)!!.template
        assertEquals("Día 1", template.weekday)
        assertEquals("Día 1", template.title)
        assertTrue(template.warmup.isNotBlank())
        assertTrue(template.cooldown.isNotBlank())
    }

    @Test fun `el esquema se conserva tal cual para el motor de sesion`() {
        val plan = ok(conFases).plan
        val plancha = plan.dayByNumber(1)!!.template.exercises[1]
        assertEquals("3 x 30 s", plancha.scheme)
        assertEquals("Sin arquear", plancha.note)
    }

    // --- Lo que tiene que fallar ------------------------------------------

    @Test fun `sin nombre no hay plan`() {
        assertTrue(errorOf("""{ "dias": [] }""").contains("nombre"))
    }

    @Test fun `sin dias no hay plan`() {
        assertTrue(errorOf("""{ "nombre": "Vacío" }""").contains("ningún día"))
    }

    @Test fun `un dia sin ejercicios se rechaza diciendo cual`() {
        val message = errorOf(
            """{ "nombre": "X", "dias": [ { "dia": "Lunes", "ejercicios": [] } ] }"""
        )
        assertTrue(message, message.contains("Lunes"))
    }

    @Test fun `un ejercicio sin series se rechaza diciendo cual`() {
        val message = errorOf(
            """{ "nombre": "X", "dias": [ { "dia": "Lunes", "ejercicios": [ { "nombre": "Remo" } ] } ] }"""
        )
        assertTrue(message, message.contains("Remo"))
    }

    @Test fun `un json roto no revienta la app`() {
        assertTrue(errorOf("""{ "nombre": "X" ,,, }""").contains("JSON"))
    }

    @Test fun `el archivo vacio da un error claro`() {
        assertTrue(errorOf("   ").contains("vacío"))
    }

    @Test fun `un archivo que no es texto se detecta antes de parsearlo`() {
        // El selector enseña todos los archivos, así que se puede elegir una foto o un PDF por
        // error; decodificados como texto son ruido y hay que decirlo con esas palabras.
        val binario = String(ByteArray(300) { (it % 7).toByte() })
        assertFalse(PlanCodec.isProbablyText(binario))
        assertFalse(PlanCodec.isProbablyText("   "))
        assertTrue(PlanCodec.isProbablyText("# Un plan\n- Press — 4 x 8\n"))
        assertTrue(PlanCodec.isProbablyText(conFases))
    }

    @Test fun `no se puede suplantar el id del plan de la app`() {
        val message = errorOf(
            """
            { "id": "${BuiltinPlan.ID}", "nombre": "Falso",
              "dias": [ { "ejercicios": [ { "nombre": "X", "esquema": "3 x 3" } ] } ] }
            """.trimIndent()
        )
        assertTrue(message, message.contains("reservado"))
    }

    @Test fun `un plan desmesurado se corta antes de generarse`() {
        val message = errorOf(
            """
            { "nombre": "Eterno", "fases": [ { "nombre": "F", "semanas": 52,
              "dias": [
                { "ejercicios": [ { "nombre": "A", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "B", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "C", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "D", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "E", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "F", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "G", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "H", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "I", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "J", "esquema": "3 x 3" } ] },
                { "ejercicios": [ { "nombre": "K", "esquema": "3 x 3" } ] }
              ] } ] }
            """.trimIndent()
        )
        assertTrue(message, message.contains("máximo"))
    }

    // --- Identidad y guardado ---------------------------------------------

    @Test fun `sin id se genera uno y se marca la fecha`() {
        val result = ok(conFases)
        assertEquals("plan-1000", result.dto.id)
        assertEquals(1_000L, result.plan.importedAt)
        assertEquals(PlanSource.JSON, result.plan.source)
    }

    @Test fun `con id propio se respeta para poder corregir el plan sin perder progreso`() {
        val result = ok(
            """
            { "id": "mi-plan", "nombre": "Mío",
              "dias": [ { "ejercicios": [ { "nombre": "X", "esquema": "3 x 3" } ] } ] }
            """.trimIndent()
        )
        assertEquals("mi-plan", result.plan.id)
    }

    @Test fun `lo guardado se vuelve a leer igual`() {
        val original = ok(conFases)
        val reread = ok(PlanCodec.toJson(original.dto))
        assertEquals(original.plan.id, reread.plan.id)
        assertEquals(original.plan.name, reread.plan.name)
        assertEquals(original.plan.totalDays, reread.plan.totalDays)
        assertEquals(original.plan.importedAt, reread.plan.importedAt)
        assertEquals(
            original.plan.dayByNumber(1)!!.template.exercises,
            reread.plan.dayByNumber(1)!!.template.exercises
        )
    }
}
