package com.marc.gymplan100

import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.ExerciseGroup
import com.marc.gymplan100.data.ExerciseGroups
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * «Mis pesos» agrupado por músculo. Lo que no se ve aquí se ve en el gimnasio: un ejercicio
 * en «Otros» es un ejercicio que no encuentras donde lo buscas.
 */
class ExerciseGroupsTest {

    private val dir = File("src/main/assets/planes")

    private fun planesIncluidos(): Map<String, Set<String>> =
        PlanCodec.decodeIdList(File(dir, "index.json").readText()).associateWith { id ->
            val plan = (PlanCodec.parseJson(File(dir, "$id.json").readText(), now = 0L) as PlanImport.Ok).plan
            plan.days.flatMap { it.template.exercises }.map { it.name }.toSet()
        } + ("100-dias" to BuiltinPlan.plan.days.flatMap { it.template.exercises }.map { it.name }.toSet())

    @Test
    fun `ningun ejercicio de los planes incluidos se queda en Otros`() {
        val sinGrupo = planesIncluidos().mapValues { (_, nombres) ->
            nombres.filter { ExerciseGroups.of(it) == ExerciseGroup.OTROS }
        }.filterValues { it.isNotEmpty() }
        assertTrue("Sin grupo: $sinGrupo", sinGrupo.isEmpty())
    }

    @Test
    fun `los nombres que tocan dos grupos van al que manda`() {
        mapOf(
            "Curl de piernas (máquina)" to ExerciseGroup.PIERNA,
            "Curl de bíceps con mancuernas" to ExerciseGroup.BICEPS,
            "Jalón al pecho (polea)" to ExerciseGroup.ESPALDA,
            "Rodillas al pecho" to ExerciseGroup.CORE,
            "Apertura de pecho tumbado (libro abierto)" to ExerciseGroup.MOVILIDAD,
            "Press de pecho en máquina" to ExerciseGroup.PECHO,
            "Remo en polea a la cara con cuerda" to ExerciseGroup.HOMBROS,
            "Remo sentado en máquina" to ExerciseGroup.ESPALDA,
            "Elevación de piernas" to ExerciseGroup.CORE,
            "Elevación de gemelos" to ExerciseGroup.PIERNA,
            "Flexión en diamante" to ExerciseGroup.TRICEPS,
            "Flexión con pies elevados o pseudo planche" to ExerciseGroup.PECHO,
            "Plancha con toque de hombro" to ExerciseGroup.CORE,
            "Elevaciones laterales" to ExerciseGroup.HOMBROS,
            "Glúteo y piriforme (figura 4 tumbado)" to ExerciseGroup.MOVILIDAD,
            "Hip thrust o puente de glúteos" to ExerciseGroup.PIERNA,
            "8 x (1 min intenso + 2 min suave)" to ExerciseGroup.CARDIO
        ).forEach { (nombre, grupo) -> assertEquals(nombre, grupo, ExerciseGroups.of(nombre)) }
    }

    @Test
    fun `sin tildes tambien`() {
        assertEquals(ExerciseGroup.ESPALDA, ExerciseGroups.of("jalon al pecho"))
    }

    @Test
    fun `lo que no se reconoce va a Otros`() {
        assertEquals(ExerciseGroup.OTROS, ExerciseGroups.of("Mi ejercicio raro"))
    }
}
