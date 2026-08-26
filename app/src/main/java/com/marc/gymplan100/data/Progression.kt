package com.marc.gymplan100.data

/**
 * Qué toca hoy en un ejercicio, y por qué.
 *
 * Hasta ahora la app recordaba el último peso y lo repetía: eso es memoria, no progresión. El
 * número no subía solo nunca, y subirlo dependía de acordarse. Aquí vive la regla que lo mueve.
 *
 * La regla es la **doble progresión**, que es la que encaja con los esquemas del plan ("3 x 8-12"):
 * primero se sube de repeticiones dentro del rango y, cuando TODAS las series llegan al techo,
 * se sube el peso y se vuelve al suelo del rango. Si dos sesiones seguidas se quedan igual con
 * el mismo peso, se propone bajar un 10 % y volver a subir desde ahí.
 *
 * Todo son funciones puras, como [SessionEngine] y [Statistics]: la pantalla solo pinta el
 * número y la frase.
 */
object Progression {

    /** Qué hacer hoy con este ejercicio. */
    enum class Kind {
        /** Nunca se ha hecho: no hay de dónde deducir nada. */
        FIRST_TIME,

        /** Mismo peso, una repetición más que la última vez. */
        MORE_REPS,

        /** Se cerró el rango entero: sube el peso y vuelven las repeticiones al suelo. */
        MORE_WEIGHT,

        /** Dos sesiones atascado: se baja para volver a subir. */
        DELOAD
    }

    /**
     * Lo que se propone para la próxima serie. [reason] es la mitad importante: un número que
     * cambia solo y sin explicación se lee como un error de la app.
     */
    data class Suggestion(
        val kind: Kind,
        val weight: String,
        val reps: String,
        val reason: String
    )

    /**
     * Sesiones seguidas sin mejorar (con el mismo peso) que se consideran un atasco.
     *
     * Tres y no dos: una semana mala la tiene cualquiera —se durmió poco, se llegó con prisa—
     * y bajarle el peso a alguien por un mal día es la forma más rápida de que deje de fiarse
     * de la propuesta.
     */
    const val STALL_SESSIONS = 3

    /** Cuánto se baja al desatascar. */
    const val DELOAD_FACTOR = 0.9

    /**
     * Lo que sube el peso de una vez, según lo que ya se mueva.
     *
     * Sugerir 13,75 kg no sirve de nada: hay que poder ponerlo en la máquina. Por debajo de
     * 20 kg (mancuernas pequeñas, poleas ligeras) se sube de kilo en kilo; a partir de ahí, de
     * 2,5 en 2,5, que es el disco pequeño a cada lado de una barra.
     */
    fun weightIncrement(currentKg: Double): Double = if (currentKg < 20.0) 1.0 else 2.5

    /** Redondea a medio kilo, que es el escalón más fino que la app sabe escribir. */
    fun roundToStep(kg: Double): Double = Math.round(kg / WEIGHT_STEP) * WEIGHT_STEP

    /**
     * Las sesiones en las que se hizo [exerciseName], de la más reciente a la más antigua, con
     * sus series apuntadas. Solo entran las que tienen desglose: sin series no hay nada que leer.
     */
    fun historyOf(
        progress: ProgressState,
        exerciseName: String,
        excludeDay: Int? = null
    ): List<List<SetLog>> =
        progress.logs.entries.mapNotNull { (key, log) ->
            val dash = key.indexOf('-')
            if (dash <= 0) return@mapNotNull null
            val day = key.substring(0, dash).toIntOrNull() ?: return@mapNotNull null
            // El día que se está entrenando no es historia todavía.
            if (day == excludeDay) return@mapNotNull null
            val idx = key.substring(dash + 1).toIntOrNull() ?: return@mapNotNull null
            val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(idx)?.name
            if (name != exerciseName) return@mapNotNull null
            // Las de calentamiento no cuentan para decidir si toca subir peso.
            val series = log.workingSets
            if (series.isEmpty()) return@mapNotNull null
            day to series
        }.sortedByDescending { it.first }.map { it.second }

    /** El peso de una sesión: el más alto que se movió, que es lo que cuenta como "lo que levantas". */
    private fun sessionWeight(sets: List<SetLog>): Double? = parseKg(heaviestWeight(sets.map { it.weight }))

    /** Las repeticiones apuntadas de una sesión, en orden. */
    private fun sessionReps(sets: List<SetLog>): List<Int> = sets.mapNotNull { parseReps(it.reps) }

    /**
     * Qué toca hoy en el ejercicio con este [scheme], visto lo que se hizo en [history] (de la
     * sesión más reciente a la más antigua).
     *
     * Devuelve null cuando no hay progresión que calcular: un ejercicio por tiempo o por vueltas
     * no se mide en repeticiones, y sin peso conocido no hay nada que subir.
     */
    fun suggest(
        scheme: String,
        history: List<List<SetLog>>,
        hasWeight: Boolean = true
    ): Suggestion? {
        val rango = repsRangeFromScheme(scheme) ?: return null
        val seriesDelPlan = setCountFromScheme(scheme)
        val ultima = history.firstOrNull()
            ?: return Suggestion(
                kind = Kind.FIRST_TIME,
                weight = "",
                reps = rango.first.toString(),
                // Pedirle los kilos a unas dominadas es la clase de detalle que hace dudar de
                // todo lo demás que dice la app.
                reason = if (hasWeight) "Primera vez con este ejercicio: apunta el peso que uses hoy."
                else "Primera vez con este ejercicio: apunta las repeticiones que te salgan."
            )

        val reps = sessionReps(ultima)
        if (reps.isEmpty()) return null
        val cerroElRango = reps.size >= seriesDelPlan && reps.all { it >= rango.last }

        // Sin kilos (dominadas, fondos, cualquier cosa de peso corporal) solo se progresa en
        // repeticiones: no hay disco que añadir. Cerrar el rango aquí no es subir peso, es
        // haberse quedado corto de ejercicio, y eso lo decide quien entrena.
        val peso = sessionWeight(ultima)
        if (peso == null) {
            val arranqueLibre = reps.first()
            val objetivoLibre = (arranqueLibre + 1).coerceAtMost(rango.last)
            return Suggestion(
                kind = Kind.MORE_REPS,
                weight = "",
                reps = objetivoLibre.toString(),
                reason = if (cerroElRango)
                    "Ya cierras las ${reps.size} series a ${rango.last}. Sin peso que añadir, " +
                        "el siguiente paso es una variante más difícil."
                else "La última vez empezaste con $arranqueLibre. Prueba $objetivoLibre."
            )
        }

        // Rango cerrado: todas las series del plan hechas y todas en el techo.
        if (cerroElRango) {
            val nuevo = roundToStep(peso + weightIncrement(peso))
            return Suggestion(
                kind = Kind.MORE_WEIGHT,
                weight = formatKg(nuevo),
                reps = rango.first.toString(),
                reason = "La última vez cerraste las ${reps.size} series a ${rango.last}. " +
                    "Sube a ${formatKg(nuevo)} kg y vuelve a ${rango.first}."
            )
        }

        // Atasco: varias sesiones seguidas con el mismo peso sin mejorar las repeticiones.
        if (estaAtascado(history)) {
            val bajada = roundToStep(peso * DELOAD_FACTOR)
            if (bajada > 0 && bajada < peso) {
                return Suggestion(
                    kind = Kind.DELOAD,
                    weight = formatKg(bajada),
                    reps = rango.first.toString(),
                    reason = "Llevas $STALL_SESSIONS sesiones sin pasar de aquí. " +
                        "Baja a ${formatKg(bajada)} kg y vuelve a subir: es la forma de romperlo."
                )
            }
        }

        // El caso de todos los días: mismo peso, una repetición más que la última vez.
        val arranque = reps.first()
        val objetivo = (arranque + 1).coerceAtMost(rango.last)
        return Suggestion(
            kind = Kind.MORE_REPS,
            weight = formatKg(peso),
            reps = objetivo.toString(),
            // En un esquema de repeticiones fijas ("3 x 12") no hay rango que subir: lo que
            // falta es cerrarlo entero. Se dice como lo que hay que hacer hoy, no como un
            // reproche por lo de la última vez (que pudo pedir menos series que hoy).
            reason = if (objetivo > arranque)
                "La última vez empezaste con $arranque. Prueba $objetivo con el mismo peso."
            else "Mismo peso: para subir, cierra hoy las $seriesDelPlan series a ${rango.last}."
        )
    }

    /**
     * Atascado: las [STALL_SESSIONS] últimas sesiones con el MISMO peso y sin mejorar las
     * repeticiones totales. Un mal día no es un atasco; dos seguidos ya dicen algo.
     */
    fun estaAtascado(history: List<List<SetLog>>): Boolean {
        if (history.size < STALL_SESSIONS) return false
        val ultimas = history.take(STALL_SESSIONS)
        val pesos = ultimas.mapNotNull { sessionWeight(it) }
        if (pesos.size < STALL_SESSIONS || pesos.distinct().size != 1) return false
        val totales = ultimas.map { sessionReps(it).sum() }
        if (totales.any { it == 0 }) return false
        // De la más antigua a la más reciente: si nunca sube, no se avanza.
        return totales.zipWithNext().all { (reciente, anterior) -> reciente <= anterior }
    }
}
