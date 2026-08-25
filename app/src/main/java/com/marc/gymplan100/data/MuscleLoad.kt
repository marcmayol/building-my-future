package com.marc.gymplan100.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cuánto trabajo se ha llevado cada músculo, contado en SERIES.
 *
 * En series y no en kilos a propósito: es la medida con la que se habla de volumen en el
 * gimnasio ("diez series de pecho a la semana"), sirve igual para una prensa de 80 kg que para
 * unas dominadas, y no se va al garete el día que alguien no apunta la carga.
 *
 * Una serie cuenta entera para los músculos que hacen el trabajo y media para los que solo
 * ayudan: un press de banca no es un día de tríceps, pero tampoco los deja descansando.
 *
 * Esto es lo que ningún ejercicio suelto puede decir: si estás compensado, o si llevas tres
 * semanas sin tocar pierna. Todo son funciones puras, como [Statistics].
 */
object MuscleLoad {

    /** Lo que suma una serie para un músculo que solo ayuda. */
    const val SECONDARY_SHARE = 0.5f

    /** Un grupo muscular con las series que se ha llevado. */
    data class GroupLoad(val group: String, val sets: Float)

    /**
     * Series por grupo muscular en los días indicados del plan, de más a menos trabajado.
     *
     * Cuenta las series realmente apuntadas; si un día se marcó a mano sin desglose, se usan
     * las que pedía el plan, que es lo que se hizo aunque no se anotara una por una.
     */
    fun byGroup(progress: ProgressState, days: Set<Int>): List<GroupLoad> {
        val total = mutableMapOf<String, Float>()
        for ((key, log) in progress.logs) {
            val dash = key.indexOf('-')
            if (dash <= 0) continue
            val day = key.substring(0, dash).toIntOrNull() ?: continue
            if (day !in days) continue
            val idx = key.substring(dash + 1).toIntOrNull() ?: continue
            val exercise = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(idx) ?: continue
            val target = MuscleTargets.forName(exercise.name) ?: continue
            val series = seriesOf(log, exercise)
            if (series <= 0f) continue
            target.primary.forEach { total[it] = (total[it] ?: 0f) + series }
            target.secondary.forEach { total[it] = (total[it] ?: 0f) + series * SECONDARY_SHARE }
        }
        return total.map { (grupo, series) -> GroupLoad(grupo, series) }
            .sortedWith(compareByDescending<GroupLoad> { it.sets }.thenBy { it.group })
    }

    /**
     * Lo que un día va a trabajar, según lo que pide el plan. No hace falta haberlo entrenado:
     * sirve para saber de un vistazo si hoy toca pierna antes de salir de casa.
     */
    fun planned(exercises: List<Exercise>): List<GroupLoad> {
        val total = mutableMapOf<String, Float>()
        for (exercise in exercises) {
            val target = MuscleTargets.forName(exercise.name) ?: continue
            val series = setCountFromScheme(exercise.scheme).toFloat()
            target.primary.forEach { total[it] = (total[it] ?: 0f) + series }
            target.secondary.forEach { total[it] = (total[it] ?: 0f) + series * SECONDARY_SHARE }
        }
        return total.map { (grupo, series) -> GroupLoad(grupo, series) }
            .sortedWith(compareByDescending<GroupLoad> { it.sets }.thenBy { it.group })
    }

    /** Series que cuenta un ejercicio de un día: las apuntadas o, si no las hay, las del plan. */
    private fun seriesOf(log: ExerciseLog, exercise: Exercise): Float {
        val apuntadas = log.filledSets.size
        if (apuntadas > 0) return apuntadas.toFloat()
        return if (log.done) setCountFromScheme(exercise.scheme).toFloat() else 0f
    }

    /**
     * Días del plan entrenados en los últimos [days] días de calendario.
     *
     * Sale del historial y no de los días marcados, porque lo que interesa aquí es cuándo se
     * entrenó de verdad: un plan se puede retomar tres semanas después y esos días viejos no
     * cuentan como carga de esta semana. Los entrenos extra no entran: no son días del plan.
     */
    fun recentDays(
        history: List<SessionRecord>,
        days: Int = 7,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): Set<Int> {
        val desde = today.minusDays((days - 1).toLong())
        return history.asSequence()
            .filter { !it.extra && it.routineId == null }
            .filter {
                val fecha = Instant.ofEpochMilli(it.endMillis).atZone(zone).toLocalDate()
                !fecha.isBefore(desde) && !fecha.isAfter(today)
            }
            .map { it.dayNumber }
            .toSet()
    }

    /**
     * Reparte los grupos en tres niveles de intensidad para pintar el mapa, relativos al que
     * más ha trabajado. Es una comparación entre tus propios músculos, no contra una tabla:
     * lo que interesa ver de un vistazo es cuál se ha quedado corto respecto a los demás.
     */
    fun levels(loads: List<GroupLoad>): Map<String, Int> {
        val tope = loads.maxOfOrNull { it.sets } ?: return emptyMap()
        if (tope <= 0f) return emptyMap()
        return loads.associate { carga ->
            val parte = carga.sets / tope
            carga.group to when {
                parte >= 0.66f -> 3
                parte >= 0.33f -> 2
                else -> 1
            }
        }
    }

    private val NOMBRES = mapOf(
        "chest" to "Pecho",
        "upper-back" to "Espalda",
        "lower-back" to "Lumbares",
        "trapezius" to "Trapecio",
        "deltoids" to "Hombros",
        "biceps" to "Bíceps",
        "triceps" to "Tríceps",
        "forearm" to "Antebrazo",
        "abs" to "Abdomen",
        "obliques" to "Oblicuos",
        "quadriceps" to "Cuádriceps",
        "hamstring" to "Femoral",
        "gluteal" to "Glúteos",
        "calves" to "Gemelos",
        "adductors" to "Aductores",
        "tibialis" to "Tibial",
        "neck" to "Cuello"
    )

    /** El grupo, escrito como se llama en el gimnasio. */
    fun label(group: String): String = NOMBRES[group] ?: group.replace('-', ' ')

    /** "3" o "4,5": las medias series salen de los músculos que solo ayudan. */
    fun formatSets(sets: Float): String =
        if (sets % 1f == 0f) sets.toInt().toString() else "%.1f".format(sets).replace('.', ',')
}
