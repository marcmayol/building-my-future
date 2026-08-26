package com.marc.gymplan100

import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.Backup
import com.marc.gymplan100.data.BackupFile
import com.marc.gymplan100.data.CompletedSet
import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.MuscleLoad
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.Progression
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.Statistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Superseries de verdad y series de calentamiento.
 *
 * Las dos cosas cambian lo que la app considera "una serie": una superserie encadena dos
 * ejercicios antes de descansar, y una serie de aproximación se apunta pero no cuenta. Si
 * alguna de las dos se cuela en los cálculos, los kilos movidos y la progresión mienten.
 */
class SupersetWarmupTest {

    /** Un día del plan que acaba en la superserie de brazos. */
    private val dia = PlanData.days.first { d ->
        d.template.exercises.any { it.supersetGroup.isNotBlank() }
    }
    private val grupo = dia.template.exercises.indices
        .filter { dia.template.exercises[it].supersetGroup.isNotBlank() }
    private val curl = grupo.first()
    private val extension = grupo.last()
    private val primero = 0

    private fun sesion(
        exerciseIndex: Int,
        setNumber: Int = 1,
        completedSets: List<CompletedSet> = emptyList()
    ) = ActiveSession(
        dayNumber = dia.number,
        startMillis = 0L,
        exerciseIndex = exerciseIndex,
        setNumber = setNumber,
        phase = SessionPhase.WORKING,
        completedSets = completedSets,
        order = dia.template.exercises.indices.toList()
    )

    // --- Superserie -----------------------------------------------------------

    @Test
    fun `el plan trae la superserie como dos ejercicios encadenados`() {
        assertEquals(2, grupo.size)
        assertNotEquals(curl, extension)
        assertEquals(listOf(curl, extension), SessionEngine.supersetMembers(dia.number, curl))
    }

    @Test
    fun `un ejercicio suelto es un grupo de uno`() {
        assertEquals(listOf(primero), SessionEngine.supersetMembers(dia.number, primero))
    }

    @Test
    fun `del primero del par se pasa al segundo SIN descansar`() {
        val s = sesion(curl)
        val next = SessionEngine.completeSet(s, "10", now = 1_000L, reps = "12")
        assertEquals(SessionPhase.WORKING, next.phase)
        assertEquals(extension, next.exerciseIndex)
        // La misma serie del par: no se ha gastado una serie de mas.
        assertEquals(1, next.setNumber)
    }

    @Test
    fun `al acabar el segundo del par si se descansa`() {
        val s = sesion(extension, completedSets = listOf(CompletedSet(curl, 1, "10", reps = "12")))
        val next = SessionEngine.completeSet(s, "25", now = 1_000L, reps = "12")
        assertEquals(SessionPhase.RESTING, next.phase)
    }

    @Test
    fun `tras el descanso se vuelve al primero del par con la serie siguiente`() {
        val descansando = SessionEngine.completeSet(
            sesion(extension, completedSets = listOf(CompletedSet(curl, 1, "10", reps = "12"))),
            "25", now = 1_000L, reps = "12"
        )
        val next = SessionEngine.endRest(descansando, now = 90_000L)
        assertEquals(curl, next.exerciseIndex)
        assertEquals(2, next.setNumber)
    }

    @Test
    fun `cada mitad del par guarda su propio peso`() {
        val tras1 = SessionEngine.completeSet(sesion(curl), "10", now = 1_000L, reps = "12")
        val tras2 = SessionEngine.completeSet(tras1, "25", now = 2_000L, reps = "12")
        val pesos = tras2.completedSets.associate { it.exerciseIndex to it.weight }
        assertEquals("10", pesos[curl])
        assertEquals("25", pesos[extension])
    }

    @Test
    fun `maquina ocupada mueve el par entero`() {
        val s = sesion(curl)
        val next = SessionEngine.skipExercise(s)
        // El par sigue junto y en el mismo orden, al final de la cola.
        assertEquals(listOf(curl, extension), next.order.takeLast(2))
    }

    // --- Calentamiento --------------------------------------------------------

    @Test
    fun `una serie de calentamiento no gasta serie del plan`() {
        val s = sesion(primero)
        val next = SessionEngine.completeSet(s, "20", now = 1_000L, reps = "12", warmup = true)
        assertTrue(next.completedSets.last().warmup)
        // Sigue siendo la serie 1: aproximar no avanza el plan.
        assertEquals(SessionPhase.RESTING, next.phase)
        val trasDescanso = SessionEngine.endRest(next, now = 90_000L)
        assertEquals(1, trasDescanso.setNumber)
    }

    @Test
    fun `el calentamiento no cuenta para los kilos movidos ni para la fuerza`() {
        val log = ExerciseLog(
            sets = listOf(
                SetLog("20", "12", warmup = true),
                SetLog("60", "10"),
                SetLog("60", "8")
            )
        )
        assertEquals(2, log.workingSets.size)
        assertEquals(3, log.filledSets.size)
        // 60x10 + 60x8 = 1080; el calentamiento (240) queda fuera.
        assertEquals(1080f, Statistics.volume(log.workingSets), 0.01f)
        assertEquals(80f, Statistics.bestOneRm(log.workingSets)!!, 0.01f)
    }

    @Test
    fun `el peso de referencia del dia ignora el calentamiento`() {
        val series = listOf(
            CompletedSet(primero, 1, "20", warmup = true),
            CompletedSet(primero, 1, "60"),
            CompletedSet(primero, 2, "55")
        )
        val estado = ProgressState().withFinishedDay(dia.number, series)
        assertEquals("60", estado.logs["${dia.number}-$primero"]!!.weight)
    }

    @Test
    fun `la progresion no mira las series de aproximacion`() {
        // Tres series de trabajo cerrando el rango, con un calentamiento delante.
        val historial = listOf(
            listOf(
                SetLog("20", "12", warmup = true),
                SetLog("30", "12"),
                SetLog("30", "12"),
                SetLog("30", "12")
            ).filter { !it.warmup }
        )
        val s = Progression.suggest("3 x 12", historial)!!
        assertEquals(Progression.Kind.MORE_WEIGHT, s.kind)
        assertEquals("32,5", s.weight)
    }

    @Test
    fun `el mapa muscular tampoco cuenta el calentamiento`() {
        val log = ExerciseLog(
            sets = listOf(SetLog("20", "12", warmup = true), SetLog("60", "10")),
            done = true
        )
        val estado = ProgressState(logs = mapOf("${dia.number}-$primero" to log))
        val carga = MuscleLoad.byGroup(estado, setOf(dia.number))
        // Una sola serie de trabajo: el musculo principal se lleva 1, no 2.
        assertTrue(carga.isNotEmpty())
        assertEquals(1f, carga.first().sets, 0.001f)
    }

    // --- Copia de seguridad ---------------------------------------------------

    @Test
    fun `una copia buena se lee y dice cuantos dias trae`() {
        val progreso = ProgressState(completedDays = setOf(1, 2, 3))
        val raw = kotlinx.serialization.json.Json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                creado = 1L,
                version = "2.6",
                progreso = mapOf(
                    "progress_json__plan" to kotlinx.serialization.json.Json.encodeToString(
                        ProgressState.serializer(), progreso
                    )
                ),
                planes = mapOf("plan_activo" to "plan")
            )
        )
        val leida = Backup.parse(raw)
        assertTrue(leida is Backup.Result.Ok)
        assertTrue((leida as Backup.Result.Ok).file.resumen().contains("3"))
    }

    @Test
    fun `un archivo cualquiera no se traga por copia`() {
        assertTrue(Backup.parse("{\"hola\":1}") is Backup.Result.Error)
        assertTrue(Backup.parse("esto no es json") is Backup.Result.Error)
    }

    @Test
    fun `una copia de otra app se rechaza`() {
        val raw = "{\"app\":\"otra-cosa\",\"progreso\":{\"a\":\"b\"}}"
        val leida = Backup.parse(raw)
        assertTrue(leida is Backup.Result.Error)
        assertTrue((leida as Backup.Result.Error).message.contains("otra app"))
    }

    @Test
    fun `una copia de una version futura se rechaza en vez de romperse`() {
        val raw = "{\"formato\":99,\"app\":\"building-my-future\",\"progreso\":{\"a\":\"b\"}}"
        assertTrue(Backup.parse(raw) is Backup.Result.Error)
    }

    @Test
    fun `el nombre del archivo lleva la fecha delante`() {
        val nombre = Backup.suggestedName(System.currentTimeMillis())
        assertTrue(nombre.startsWith("building-my-future-"))
        assertTrue(nombre.endsWith(".json"))
    }

    @Test
    fun `una copia vacia no vale`() {
        val raw = "{\"app\":\"building-my-future\"}"
        assertTrue(Backup.parse(raw) is Backup.Result.Error)
        assertFalse(Backup.parse(raw) is Backup.Result.Ok)
    }
}
