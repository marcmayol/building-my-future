package com.marc.gymplan100.data

/**
 * Transiciones de fase de una sesión, como funciones puras sobre [ActiveSession].
 *
 * Las usan tanto el PlanViewModel (en primer plano) como SkipActionReceiver (en segundo
 * plano, al pulsar "Saltar" en la notificación) para que la lógica nunca diverja.
 *
 * NO tienen efectos laterales: no guardan en disco, no programan alarmas ni notificaciones
 * y no tocan los pesos de referencia (eso se queda en el ViewModel). Si la sesión no está en
 * la fase esperada devuelven la MISMA instancia recibida, para que el llamante detecte el no-op.
 */
object SessionEngine {

    private fun ActiveSession.orderOrDefault(): List<Int> =
        order.ifEmpty { PlanData.dayByNumber(dayNumber)?.template?.exercises?.indices?.toList() ?: listOf(0) }

    /** Primer día sin completar; si están todos hechos, devuelve el último. */
    fun nextDay(completedDays: Set<Int>): Int {
        for (n in 1..PlanData.TOTAL_DAYS) if (n !in completedDays) return n
        return PlanData.TOTAL_DAYS
    }

    /**
     * Construye una sesión nueva (fase de calentamiento en marcha) para [dayNumber]. Función pura:
     * no guarda ni programa alarmas; el llamante (móvil o servicio del reloj) se encarga de eso.
     * [watchControlled] marca las que arrancan desde el reloj para el cálculo de calorías.
     */
    fun startSession(dayNumber: Int, now: Long, watchControlled: Boolean = false): ActiveSession {
        val day = PlanData.dayByNumber(dayNumber)
        val order = day?.template?.exercises?.indices?.toList() ?: listOf(0)
        val warmupSecs = warmupSecondsFromText(day?.template?.warmup ?: "")
        return ActiveSession(
            dayNumber = dayNumber,
            startMillis = now,
            exerciseIndex = order.first(),
            order = order,
            phase = SessionPhase.WARMUP,
            warmupTargetSeconds = warmupSecs,
            warmupStartMillis = now,
            watchControlled = watchControlled
        )
    }

    /** Termina el calentamiento y pasa a la primera serie. */
    fun endWarmup(s: ActiveSession): ActiveSession {
        if (s.phase != SessionPhase.WARMUP) return s
        return s.copy(phase = SessionPhase.WORKING, warmupPaused = false)
    }

    /**
     * Peso que le toca a la serie en curso, con la misma cuenta que hace la pantalla del móvil:
     * lo que se dejó preparado, si no el de la serie anterior de esa misma máquina, y si no el
     * último peso conocido del ejercicio.
     *
     * Vive aquí, y no en la pantalla, porque la serie también se marca desde el reloj: allí no
     * hay campo de peso que leer, y darla por hecha con el peso vacío tiraba los kilos que se
     * acababan de escribir en el móvil (ni la serie, ni el registro del día, ni "Mis pesos").
     */
    fun weightForSet(s: ActiveSession, knownWeights: Map<String, String>): String {
        val exercise = PlanData.dayByNumber(s.dayNumber)
            ?.template?.exercises?.getOrNull(s.exerciseIndex) ?: return ""
        // Ni una plancha ni el cardio llevan kilos, y una serie por tiempo tampoco los pregunta.
        if (exercise.withoutWeight || secondsPerSetFromScheme(exercise.scheme) != null) return ""
        if (s.plannedWeight.isNotBlank()) return s.plannedWeight.trim()
        s.completedSets
            .lastOrNull { it.exerciseIndex == s.exerciseIndex && it.weight.isNotBlank() }
            ?.let { return it.weight }
        return knownWeights[exercise.name].orEmpty()
    }

    /**
     * Los ejercicios encadenados en superserie con [index], en el orden del plan.
     *
     * Una superserie son dos o mas ejercicios CONSECUTIVOS del dia con la misma etiqueta: se
     * hacen uno detras de otro sin descanso y se descansa al acabar el ultimo. Un ejercicio
     * suelto es un grupo de uno, para que el resto del motor no tenga que distinguir casos.
     */
    fun supersetMembers(dayNumber: Int, index: Int): List<Int> {
        val exercises = PlanData.dayByNumber(dayNumber)?.template?.exercises ?: return listOf(index)
        val etiqueta = exercises.getOrNull(index)?.supersetGroup.orEmpty()
        if (etiqueta.isBlank()) return listOf(index)
        var desde = index
        while (desde > 0 && exercises[desde - 1].supersetGroup == etiqueta) desde--
        var hasta = index
        while (hasta < exercises.lastIndex && exercises[hasta + 1].supersetGroup == etiqueta) hasta++
        return (desde..hasta).toList()
    }

    /**
     * Repeticiones que le tocan a la serie en curso, con la misma cuenta en cascada que el peso:
     * lo que se haya tocado en esta serie, si no lo que se hizo en la anterior de esta misma
     * máquina, si no lo de la última vez que se hizo el ejercicio, y si no lo que pide el plan.
     *
     * Vive aquí por el mismo motivo que [weightForSet]: la serie también se cierra desde el
     * reloj y desde la notificación, donde no hay pantalla que leer, y dar la serie por hecha
     * sin repeticiones dejaría el registro a medias.
     */
    fun repsForSet(s: ActiveSession, knownReps: Map<String, String>): String {
        val exercise = PlanData.dayByNumber(s.dayNumber)
            ?.template?.exercises?.getOrNull(s.exerciseIndex) ?: return ""
        // Una plancha se mide en segundos y un circuito en vueltas: ahí no hay repeticiones.
        if (repsRangeFromScheme(exercise.scheme) == null) return ""
        if (s.currentReps.isNotBlank()) return s.currentReps.trim()
        s.completedSets
            .lastOrNull { it.exerciseIndex == s.exerciseIndex && it.reps.isNotBlank() }
            ?.let { return it.reps }
        knownReps[exercise.name]?.takeIf { it.isNotBlank() }?.let { return it }
        return defaultRepsFromScheme(exercise.scheme)
    }

    /**
     * Marca la serie actual como hecha y devuelve la sesión resultante: descanso (entre series
     * o entre ejercicios) con [now] como inicio, o FINISHED si era la última serie de todo.
     *
     * [reps] son las repeticiones que se hicieron de verdad; va al final y con valor por defecto
     * para que quien no las tenga (una serie por tiempo) siga llamando igual que siempre.
     */
    fun completeSet(
        s: ActiveSession,
        weight: String,
        now: Long,
        reps: String = "",
        warmup: Boolean = false,
        rir: String = ""
    ): ActiveSession {
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return s
        val day = PlanData.dayByNumber(s.dayNumber) ?: return s
        val exercise = day.template.exercises.getOrNull(s.exerciseIndex) ?: return s
        val totalSets = setCountFromScheme(exercise.scheme)

        val cleanWeight = weight.trim()
        val newSet = CompletedSet(
            s.exerciseIndex, s.setNumber, cleanWeight,
            reps = reps.trim(), warmup = warmup, rir = rir.trim()
        )
        val order = s.orderOrDefault()

        // Superserie: si quedan ejercicios encadenados, se pasa al siguiente SIN descanso.
        // El descanso de una superserie va al final del par, no en medio; ponerlo en medio es
        // justo lo que la convierte en dos ejercicios normales.
        val grupo = supersetMembers(s.dayNumber, s.exerciseIndex)
        val posEnGrupo = grupo.indexOf(s.exerciseIndex)
        if (posEnGrupo >= 0 && posEnGrupo < grupo.lastIndex) {
            return s.copy(
                exerciseIndex = grupo[posEnGrupo + 1],
                completedSets = s.completedSets + newSet,
                occupiedSkips = 0,
                plannedWeight = "",
                currentReps = ""
            )
        }

        // Una serie de calentamiento no gasta serie del plan: se apunta y se repite la misma.
        val isLastSet = !warmup && s.setNumber >= totalSets
        // El grupo entero cuenta como una posicion: lo que importa es si el ULTIMO del grupo
        // es el ultimo del dia.
        val isLastExercise = order.indexOf(grupo.last()) >= order.lastIndex

        return if (isLastSet && isLastExercise) {
            s.copy(
                phase = SessionPhase.FINISHED,
                completedSets = s.completedSets + newSet,
                occupiedSkips = 0,
                currentReps = ""
            )
        } else {
            val betweenExercises = isLastSet
            val target = if (betweenExercises) RestDefaults.BETWEEN_EXERCISES else RestDefaults.BETWEEN_SETS
            s.copy(
                phase = SessionPhase.RESTING,
                restStartMillis = now,
                restTargetSeconds = target,
                restBetweenExercises = betweenExercises,
                completedSets = s.completedSets + newSet,
                occupiedSkips = 0,
                // Nuevo descanso: se recalcula la sugerencia de peso desde cero.
                plannedWeight = "",
                currentReps = ""
            )
        }
    }

    /**
     * Máquina ocupada: pospone el ejercicio actual AL FINAL de la cola y pasa al siguiente
     * pendiente. Al mandarlo al final (y no intercambiarlo con el siguiente), pulsar varias
     * veces seguidas recorre todos los ejercicios pendientes en vez de rebotar entre las dos
     * mismas máquinas cuando hay varias ocupadas a la vez.
     */
    fun skipExercise(s: ActiveSession): ActiveSession {
        if (s.phase != SessionPhase.WORKING) return s
        val order = s.orderOrDefault()
        // Una superserie se mueve entera: dejar medio par colgando no tiene sentido.
        val grupo = supersetMembers(s.dayNumber, s.exerciseIndex)
        val pos = order.indexOf(grupo.first())
        if (pos < 0 || order.indexOf(grupo.last()) >= order.lastIndex) return s
        val newOrder = order.toMutableList()
        newOrder.removeAll(grupo)
        newOrder.addAll(grupo)
        val nextEx = newOrder[pos]
        return s.copy(
            order = newOrder,
            exerciseIndex = nextEx,
            // Las de calentamiento no gastan serie del plan.
            setNumber = s.completedSets.count { it.exerciseIndex == nextEx && !it.warmup } + 1,
            phase = SessionPhase.WORKING,
            occupiedSkips = s.occupiedSkips + 1,
            // El peso preparado era para la máquina que dejamos: ya no aplica.
            plannedWeight = "",
            currentReps = ""
        )
    }

    /** Termina el descanso (guardando el tiempo real en [now]) y avanza a la siguiente serie/ejercicio. */
    fun endRest(s: ActiveSession, now: Long): ActiveSession {
        if (s.phase != SessionPhase.RESTING) return s
        val day = PlanData.dayByNumber(s.dayNumber) ?: return s
        val real = ((now - s.restStartMillis) / 1000).toInt().coerceAtLeast(0)

        val sets = s.completedSets.toMutableList()
        if (sets.isNotEmpty()) {
            sets[sets.lastIndex] = sets.last().copy(restSeconds = real)
        }

        val exercise = day.template.exercises[s.exerciseIndex]
        val totalSets = setCountFromScheme(exercise.scheme)
        val order = s.orderOrDefault()
        // En superserie se descansa al final del par y se vuelve al PRIMERO para la ronda
        // siguiente; al acabar las series del grupo se sale del grupo entero.
        val grupo = supersetMembers(s.dayNumber, s.exerciseIndex)
        // Cual toca ahora se deduce del trabajo hecho, no de sumar uno al contador: una serie
        // de aproximacion se apunta y descansa como las demas, pero no avanza el plan.
        val trabajoHecho = sets.count { it.exerciseIndex == grupo.first() && !it.warmup }
        val (nextEx, nextSet) = if (trabajoHecho < totalSets) {
            grupo.first() to (trabajoHecho + 1)
        } else {
            val pos = order.indexOf(grupo.last())
            val ne = order.getOrElse(pos + 1) { s.exerciseIndex }
            ne to (s.completedSets.count { it.exerciseIndex == ne && !it.warmup } + 1)
        }

        return s.copy(
            phase = SessionPhase.WORKING,
            exerciseIndex = nextEx,
            setNumber = nextSet,
            restTargetSeconds = 0,
            completedSets = sets
        )
    }
}
