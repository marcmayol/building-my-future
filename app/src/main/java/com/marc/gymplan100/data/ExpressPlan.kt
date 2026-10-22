package com.marc.gymplan100.data

import kotlinx.serialization.Serializable

/**
 * Un día del plan recortado para que quepa en el tiempo que hay.
 *
 * Nace de una queja de verdad: quien entrena de 6:30 a 7:10 antes de trabajar no llega a los
 * seis ejercicios del día, hace tres y el resto se queda sin hacer y sin apuntar. Recortar a
 * mano cada mañana es otra decisión más con prisa; aquí se dice cuánto tiempo hay y la app
 * decide qué entra.
 *
 * [order] son los índices del día que se hacen, en orden; [maxSets] el tope de series por
 * ejercicio (0 = las del plan); [dropped] los nombres que se quedan fuera, para decirlo.
 */
@Serializable
data class ExpressPlan(
    val order: List<Int>,
    val maxSets: Int,
    val warmupSeconds: Int,
    val estimatedSeconds: Int,
    val dropped: List<String> = emptyList()
)

object Express {

    /** Lo que se tarda en hacer una serie de repeticiones, colocarse incluido. */
    const val WORK_SECONDS = 45

    /** Lo que se tarda en cambiar de máquina y prepararla, además del descanso. */
    const val CHANGE_SECONDS = 60

    /** El calentamiento del exprés: corto, pero no se quita. Ir en frío con prisa es como uno se lesiona. */
    const val EXPRESS_WARMUP = 5 * 60

    /** Series por ejercicio cuando hay que recortar: dos series duras dan casi todo el estímulo. */
    const val EXPRESS_SETS = 2

    /**
     * Cuánto dura [day] haciendo los ejercicios de [order] con [maxSets] series como mucho.
     *
     * [scale] corrige el modelo con lo que de verdad tarda esta persona (ver [scaleFrom]): el
     * modelo cuenta descansos y series, pero no las colas en la máquina ni el móvil.
     */
    fun estimateSeconds(
        day: TrainingDay,
        order: List<Int> = day.template.exercises.indices.toList(),
        maxSets: Int = 0,
        warmupSeconds: Int = warmupSecondsFromText(day.template.warmup),
        yourRests: Map<String, Int> = emptyMap(),
        scale: Double = 1.0,
        restBase: RestDefaults.Base = RestDefaults.Base()
    ): Int {
        var total = 0
        order.forEachIndexed { pos, idx ->
            val ex = day.template.exercises.getOrNull(idx) ?: return@forEachIndexed
            // Un bloque de cardio se mide en minutos, no en series: «20 min» son veinte minutos.
            cardioMinutes(ex)?.let { min ->
                total += min * 60
                if (pos < order.lastIndex) total += CHANGE_SECONDS
                return@forEachIndexed
            }
            val delPlan = setCountFromScheme(ex.scheme)
            val sets = if (maxSets > 0) minOf(delPlan, maxSets) else delPlan
            // «por lado» es la serie dos veces.
            val lados = if (ex.scheme.contains("por lado", ignoreCase = true)) 2 else 1
            val work = (secondsPerSetFromScheme(ex.scheme.replace(Regex("""(?i)\s*por lado"""), "")) ?: WORK_SECONDS) * lados
            val entreSeries = RestDefaults.between(ex, yours = yourRests, base = restBase)
            total += sets * work + (sets - 1).coerceAtLeast(0) * entreSeries.seconds
            if (pos < order.lastIndex) total += RestDefaults.betweenExercises(entreSeries, restBase).seconds + CHANGE_SECONDS
        }
        return warmupSeconds + (total * scale).toInt()
    }

    /** Los minutos de un bloque de cardio («20 min», «15-20 min»: el mayor), o null si no lo es. */
    private fun cardioMinutes(ex: Exercise): Int? {
        if (ex.kind != ExerciseKind.CARDIO) return null
        val m = Regex("""(\d+)\s*(?:-\s*(\d+)\s*)?min""").find(ex.scheme.lowercase()) ?: return null
        return (m.groupValues[2].toIntOrNull() ?: m.groupValues[1].toInt()).coerceIn(1, 180)
    }

    /**
     * Lo que tarda esta persona respecto al modelo, sacado de sus entrenos guiados: si el
     * modelo dice 40 minutos y a ella le llevan 55, el factor es 1,4. Con menos de dos
     * entrenos no hay de dónde sacarlo y vale 1. Se acota para que un día de charla en el
     * gimnasio no convierta todos los exprés en uno de 15 minutos.
     */
    fun scaleFrom(history: List<SessionRecord>): Double {
        val ratios = history
            .filter { !it.special && !it.extra && it.routineId == null && it.totalSets >= 4 }
            .takeLast(10)
            .mapNotNull { r ->
                val day = PlanData.dayByNumber(r.dayNumber) ?: return@mapNotNull null
                val warmup = warmupSecondsFromText(day.template.warmup)
                // El modelo para las series que se hicieron, con el descanso de siempre.
                val model = warmup + r.totalSets * (WORK_SECONDS + RestDefaults.BETWEEN_SETS)
                if (model <= 0) null else r.durationSeconds.toDouble() / model
            }
            .sorted()
        if (ratios.size < 2) return 1.0
        return ratios[ratios.size / 2].coerceIn(0.8, 2.0)
    }

    /**
     * El día recortado para que quepa en [budgetSeconds], o null si entero ya cabe.
     *
     * Primero se baja todo a [EXPRESS_SETS] series —así no se queda ningún músculo del día sin
     * tocar— y solo si aun así no cabe se quitan ejercicios por el final, que es donde los
     * planes ponen los accesorios. Una superserie entra o sale entera. Siempre queda al menos
     * un ejercicio: un exprés de nada no es un entreno.
     */
    fun plan(
        day: TrainingDay,
        budgetSeconds: Int,
        yourRests: Map<String, Int> = emptyMap(),
        scale: Double = 1.0,
        restBase: RestDefaults.Base = RestDefaults.Base()
    ): ExpressPlan? {
        val todos = day.template.exercises.indices.toList()
        if (estimateSeconds(day, todos, yourRests = yourRests, scale = scale, restBase = restBase) <= budgetSeconds) return null

        val warmup = minOf(warmupSecondsFromText(day.template.warmup), EXPRESS_WARMUP)
        fun estimar(order: List<Int>) =
            estimateSeconds(day, order, EXPRESS_SETS, warmup, yourRests, scale, restBase)

        // Bloques que no se parten: un ejercicio suelto o una superserie entera.
        val bloques = mutableListOf<List<Int>>()
        todos.forEach { idx ->
            val etiqueta = day.template.exercises[idx].supersetGroup
            val anterior = bloques.lastOrNull()
            if (etiqueta.isNotBlank() && anterior != null &&
                day.template.exercises[anterior.last()].supersetGroup == etiqueta
            ) {
                bloques[bloques.lastIndex] = anterior + idx
            } else bloques += listOf(idx)
        }

        var order = todos
        while (estimar(order) > budgetSeconds && bloques.size > 1) {
            bloques.removeAt(bloques.lastIndex)
            order = bloques.flatten()
        }
        val fuera = todos.filter { it !in order }.map { day.template.exercises[it].name }
        return ExpressPlan(
            order = order,
            maxSets = EXPRESS_SETS,
            warmupSeconds = warmup,
            estimatedSeconds = estimar(order),
            dropped = fuera
        )
    }
}
