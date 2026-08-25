package com.marc.gymplan100

import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.MuscleLoad
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lo que se ha llevado cada músculo.
 *
 * La ficha de un ejercicio dice qué trabaja; solo el acumulado dice si estás compensado o si
 * llevas tres semanas sin tocar pierna. Se cuenta en series porque es como se habla de volumen
 * en el gimnasio y porque no depende de que se hayan apuntado los kilos.
 */
class MuscleLoadTest {

    private val zona: ZoneId = ZoneId.of("UTC")
    private val hoy: LocalDate = LocalDate.of(2026, 8, 25)

    /** Un día del plan que empieza por prensa de piernas. */
    private val dia = PlanData.days.first { d ->
        d.template.exercises.any { it.name == "Prensa de piernas" }
    }
    private val prensa = dia.template.exercises.indexOfFirst { it.name == "Prensa de piernas" }

    private fun conSeries(vararg series: SetLog) = ProgressState(
        logs = mapOf("${dia.number}-$prensa" to ExerciseLog(sets = series.toList(), done = true))
    )

    @Test
    fun `las series apuntadas cuentan enteras para el musculo que trabaja`() {
        val estado = conSeries(SetLog("80", "12"), SetLog("80", "12"), SetLog("80", "10"))
        val carga = MuscleLoad.byGroup(estado, setOf(dia.number)).associate { it.group to it.sets }
        val objetivo = MuscleTargets.forName("Prensa de piernas")!!
        objetivo.primary.forEach { assertEquals(3f, carga[it]!!, 0.001f) }
    }

    @Test
    fun `el musculo que solo ayuda se lleva media serie`() {
        val estado = conSeries(SetLog("80", "12"), SetLog("80", "12"), SetLog("80", "10"))
        val carga = MuscleLoad.byGroup(estado, setOf(dia.number)).associate { it.group to it.sets }
        val objetivo = MuscleTargets.forName("Prensa de piernas")!!
        objetivo.secondary.forEach { assertEquals(1.5f, carga[it]!!, 0.001f) }
    }

    @Test
    fun `un dia marcado a mano cuenta las series que pedia el plan`() {
        // Sin desglose pero dado por hecho: se hizo, aunque no se anotara serie a serie.
        val estado = ProgressState(
            logs = mapOf("${dia.number}-$prensa" to ExerciseLog(weight = "80", done = true))
        )
        val esperadas = com.marc.gymplan100.data.setCountFromScheme(
            dia.template.exercises[prensa].scheme
        ).toFloat()
        val carga = MuscleLoad.byGroup(estado, setOf(dia.number)).associate { it.group to it.sets }
        assertEquals(esperadas, carga["quadriceps"]!!, 0.001f)
    }

    @Test
    fun `un ejercicio ni empezado no suma nada`() {
        val estado = ProgressState(
            logs = mapOf("${dia.number}-$prensa" to ExerciseLog(weight = "80", done = false))
        )
        assertTrue(MuscleLoad.byGroup(estado, setOf(dia.number)).isEmpty())
    }

    @Test
    fun `los dias fuera del periodo no entran`() {
        val estado = conSeries(SetLog("80", "12"))
        assertTrue(MuscleLoad.byGroup(estado, setOf(dia.number + 1)).isEmpty())
    }

    @Test
    fun `sale ordenado de mas a menos trabajado`() {
        val estado = conSeries(SetLog("80", "12"), SetLog("80", "12"))
        val carga = MuscleLoad.byGroup(estado, setOf(dia.number))
        assertEquals(carga.map { it.sets }.sortedDescending(), carga.map { it.sets })
    }

    // --- Qué días cuentan como "esta semana" ---------------------------------

    private fun sesion(dayNumber: Int, fecha: LocalDate, extra: Boolean = false) = SessionRecord(
        dayNumber = dayNumber,
        startMillis = fecha.atStartOfDay(zona).toInstant().toEpochMilli(),
        endMillis = fecha.atStartOfDay(zona).toInstant().toEpochMilli(),
        totalSets = 6,
        totalRestSeconds = 0,
        extra = extra
    )

    @Test
    fun `la semana son los siete ultimos dias de calendario`() {
        val historial = listOf(
            sesion(10, hoy),
            sesion(9, hoy.minusDays(6)),
            sesion(8, hoy.minusDays(7))   // justo fuera
        )
        val dias = MuscleLoad.recentDays(historial, days = 7, zone = zona, today = hoy)
        assertTrue(10 in dias)
        assertTrue(9 in dias)
        assertFalse(8 in dias)
    }

    @Test
    fun `los entrenos extra no son carga del plan`() {
        val historial = listOf(sesion(11, hoy, extra = true))
        assertTrue(MuscleLoad.recentDays(historial, days = 7, zone = zona, today = hoy).isEmpty())
    }

    // --- Los tres niveles del mapa -------------------------------------------

    @Test
    fun `los niveles se reparten respecto al musculo mas trabajado`() {
        val cargas = listOf(
            MuscleLoad.GroupLoad("chest", 9f),      // el tope
            MuscleLoad.GroupLoad("deltoids", 5f),   // 0,55
            MuscleLoad.GroupLoad("calves", 1f)      // 0,11
        )
        val niveles = MuscleLoad.levels(cargas)
        assertEquals(3, niveles["chest"])
        assertEquals(2, niveles["deltoids"])
        assertEquals(1, niveles["calves"])
    }

    @Test
    fun `sin carga no hay niveles que pintar`() {
        assertTrue(MuscleLoad.levels(emptyList()).isEmpty())
    }

    @Test
    fun `las medias series se escriben con coma`() {
        assertEquals("3", MuscleLoad.formatSets(3f))
        assertEquals("4,5", MuscleLoad.formatSets(4.5f))
    }
}
