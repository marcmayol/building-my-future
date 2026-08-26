package com.marc.gymplan100.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cálculos de estadísticas a partir del progreso y del histórico de sesiones.
 *
 * Todo son funciones puras (sin efectos ni dependencias de Android) para poder probarlas
 * y para que la UI solo tenga que pintar. Las fechas usan java.time (disponible desde minSdk 26).
 */
object Statistics {

    // ------------------------------------------------------------------ Resumen global

    data class Summary(
        val completedDays: Int,
        val totalDays: Int,
        val percent: Int,
        /** Días consecutivos del plan terminando en el último completado. */
        val currentStreak: Int,
        /** Racha más larga de días consecutivos del plan. */
        val bestStreak: Int,
        /** Todas las sesiones registradas (normales + especiales + extra). */
        val totalWorkouts: Int,
        val extraWorkouts: Int,
        val totalTrainingSeconds: Long,
        val totalSets: Int
    )

    fun summary(progress: ProgressState, history: List<SessionRecord>): Summary {
        val days = progress.completedDays.filter { it in 1..PlanData.TOTAL_DAYS }.toSortedSet()
        return Summary(
            completedDays = days.size,
            totalDays = PlanData.TOTAL_DAYS,
            percent = if (PlanData.TOTAL_DAYS == 0) 0 else days.size * 100 / PlanData.TOTAL_DAYS,
            currentStreak = currentStreak(days),
            bestStreak = bestStreak(days),
            totalWorkouts = history.size,
            extraWorkouts = history.count { it.extra },
            totalTrainingSeconds = history.sumOf { it.durationSeconds.toLong() },
            totalSets = history.sumOf { it.totalSets }
        )
    }

    /** Racha más larga de días CONSECUTIVOS del plan completados. */
    fun bestStreak(days: Set<Int>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.toSortedSet()
        var best = 1
        var run = 1
        var prev = sorted.first()
        for (d in sorted.drop(1)) {
            run = if (d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    /** Racha vigente: días consecutivos del plan terminando en el último completado. */
    fun currentStreak(days: Set<Int>): Int {
        if (days.isEmpty()) return 0
        var n = 0
        var d = days.max()
        while (d in days) { n++; d-- }
        return n
    }

    // ------------------------------------------------------------- Progresión de peso

    data class WeightPoint(val day: Int, val weight: Float)

    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")

    /** Extrae el primer número de un texto de peso ("52,5 kg" -> 52.5). Null si no hay número. */
    fun parseWeight(raw: String): Float? =
        NUMBER.find(raw)?.value?.replace(',', '.')?.toFloatOrNull()

    /**
     * Para cada ejercicio con algún peso registrado, sus puntos (día del plan, peso) ordenados
     * por día. La clave del mapa es el nombre del ejercicio; se ordena alfabéticamente.
     *
     * El peso de cada día es el más alto que se movió ese día (lo que guarda [ExerciseLog.weight]),
     * no el de la última serie: bajar en la tercera para llegar a las repeticiones no es una
     * bajada de fuerza, y la gráfica dibujaba un diente de sierra que no había pasado.
     */
    fun weightProgression(progress: ProgressState): Map<String, List<WeightPoint>> {
        val out = sortedMapOf<String, MutableList<WeightPoint>>()
        for ((key, log) in progress.logs) {
            val w = parseWeight(log.weight) ?: continue
            val dash = key.indexOf('-')
            if (dash <= 0) continue
            val day = key.substring(0, dash).toIntOrNull() ?: continue
            val idx = key.substring(dash + 1).toIntOrNull() ?: continue
            val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(idx)?.name ?: continue
            out.getOrPut(name) { mutableListOf() }.add(WeightPoint(day, w))
        }
        return out.mapValues { (_, v) -> v.sortedBy { it.day } }
    }

    // --------------------------------------------------- Fuerza estimada (1RM) y volumen

    /**
     * Tope de repeticiones con el que la estimación de 1RM todavía vale algo.
     *
     * Por encima de doce, una serie deja de medir fuerza y empieza a medir aguante: la fórmula
     * sigue dando un número, pero ese número ya no dice lo que parece.
     */
    const val MAX_REPS_FOR_ONE_RM = 12

    /**
     * Fuerza estimada a una repetición (fórmula de Epley: peso x (1 + reps/30)).
     *
     * Es lo único que permite comparar días con esquemas distintos: 40 kg x 12 y 50 kg x 5
     * dibujan un salto enorme en la gráfica de kilos y son casi el mismo esfuerzo (56 y 58 kg
     * estimados). Nadie prueba su máximo cada semana, así que se deduce de una serie normal.
     *
     * Null si las repeticiones se salen del rango en el que la fórmula es honesta.
     */
    fun estimatedOneRm(weightKg: Float, reps: Int): Float? {
        if (weightKg <= 0f || reps < 1 || reps > MAX_REPS_FOR_ONE_RM) return null
        // Con una sola repetición no hay nada que estimar: ese peso ES el máximo. La fórmula
        // por sí sola devolvería un 3 % de más y apuntaría un récord que no ha pasado.
        if (reps == 1) return weightKg
        return weightKg * (1f + reps / 30f)
    }

    /** La mejor fuerza estimada de una lista de series (la serie que más dice, no la última). */
    fun bestOneRm(sets: List<SetLog>): Float? =
        sets.mapNotNull { serie ->
            val kg = parseWeight(serie.weight) ?: return@mapNotNull null
            val reps = serie.reps.trim().toIntOrNull() ?: return@mapNotNull null
            estimatedOneRm(kg, reps)
        }.maxOrNull()

    /**
     * Fuerza estimada por ejercicio y día, con la misma forma que [weightProgression] para que
     * la gráfica pueda pintar una u otra sin enterarse.
     *
     * Solo aparecen los días con series apuntadas CON repeticiones: sin ellas no hay fórmula
     * que valga, y rellenar el hueco con una suposición sería dibujar un progreso inventado.
     */
    fun oneRmProgression(progress: ProgressState): Map<String, List<WeightPoint>> {
        val out = sortedMapOf<String, MutableList<WeightPoint>>()
        for ((key, log) in progress.logs) {
            // Solo las series de trabajo: calentar mucho no te hace mas fuerte.
            val best = bestOneRm(log.workingSets) ?: continue
            val dash = key.indexOf('-')
            if (dash <= 0) continue
            val day = key.substring(0, dash).toIntOrNull() ?: continue
            val idx = key.substring(dash + 1).toIntOrNull() ?: continue
            val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(idx)?.name ?: continue
            out.getOrPut(name) { mutableListOf() }.add(WeightPoint(day, best))
        }
        return out.mapValues { (_, v) -> v.sortedBy { it.day } }
    }

    /**
     * Kilos movidos en una lista de series: la suma de peso x repeticiones.
     *
     * Es la medida del trabajo hecho, y la que explica por qué un día de 40 kg puede cansar
     * mas que uno de 60: tres series de doce mueven mas que tres de cinco.
     */
    fun volume(sets: List<SetLog>): Float =
        sets.sumOf { serie ->
            val kg = parseWeight(serie.weight)?.toDouble() ?: return@sumOf 0.0
            val reps = serie.reps.trim().toIntOrNull() ?: return@sumOf 0.0
            kg * reps
        }.toFloat()

    /**
     * Kilos movidos por día del plan. Solo cuenta lo apuntado serie a serie: los días de antes
     * de que la app guardara las repeticiones no tienen con qué calcularse y se quedan fuera,
     * que es preferible a inventarles un número.
     */
    fun volumeByDay(progress: ProgressState): Map<Int, Float> {
        val out = sortedMapOf<Int, Float>()
        for ((key, log) in progress.logs) {
            val v = volume(log.workingSets)
            if (v <= 0f) continue
            val day = key.substringBefore('-').toIntOrNull() ?: continue
            out[day] = (out[day] ?: 0f) + v
        }
        return out
    }

    /** Kilos movidos en total, sumando todos los días con desglose. */
    fun totalVolume(progress: ProgressState): Float = volumeByDay(progress).values.sum()

    // ------------------------------------------------------------------- Constancia

    private fun millisToDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Lunes de la semana a la que pertenece [date]. */
    fun weekStart(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    data class WeekCount(val weekStart: LocalDate, val count: Int)

    /** Entrenos por semana (lunes-domingo): últimas [weeks] semanas incluyendo la actual, con ceros. */
    fun workoutsPerWeek(
        history: List<SessionRecord>,
        weeks: Int = 10,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): List<WeekCount> {
        val counts = history.groupingBy { weekStart(millisToDate(it.endMillis, zone)) }.eachCount()
        val currentWeek = weekStart(today)
        return (weeks - 1 downTo 0).map { back ->
            val ws = currentWeek.minusWeeks(back.toLong())
            WeekCount(ws, counts[ws] ?: 0)
        }
    }

    /**
     * Quita las semanas vacías del principio: la gráfica empieza cuando empezaste tú.
     *
     * Sin esto, las primeras columnas son ceros de antes del plan y confunden más de lo
     * que informan. Si aún no hay ningún entreno se dejan las últimas [minimo] semanas,
     * para que la gráfica no aparezca vacía del todo.
     */
    fun trimLeadingEmpty(weeks: List<WeekCount>, minimo: Int = 4): List<WeekCount> {
        val conDatos = weeks.dropWhile { it.count == 0 }
        return if (conDatos.isEmpty()) weeks.takeLast(minimo) else conDatos
    }

    /** Días de calendario con al menos un entreno. */
    fun trainedDays(
        history: List<SessionRecord>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Set<LocalDate> = history.map { millisToDate(it.endMillis, zone) }.toSet()

    /** Cuenta entrenos cuya fecha cae en el rango [from, to] inclusive. */
    fun workoutsBetween(
        history: List<SessionRecord>,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = history.count {
        val d = millisToDate(it.endMillis, zone)
        !d.isBefore(from) && !d.isAfter(to)
    }

    // ---------------------------------------------------------------------- Records

    data class ExerciseRecord(val exercise: String, val weight: Float, val day: Int)

    /** Peso máximo alcanzado en cada ejercicio (y el día), ordenado de mayor a menor. */
    fun personalRecords(progress: ProgressState): List<ExerciseRecord> =
        weightProgression(progress).mapNotNull { (name, pts) ->
            val best = pts.maxByOrNull { it.weight } ?: return@mapNotNull null
            ExerciseRecord(name, best.weight, best.day)
        }.sortedByDescending { it.weight }

    fun longestSession(history: List<SessionRecord>): SessionRecord? =
        history.maxByOrNull { it.durationSeconds }

    /** Sesión con más series (excluye las libres/especiales, que no registran series). */
    fun mostProductiveSession(history: List<SessionRecord>): SessionRecord? =
        history.filter { it.totalSets > 0 }.maxByOrNull { it.totalSets }
}
