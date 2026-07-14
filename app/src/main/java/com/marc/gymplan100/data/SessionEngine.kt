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
     * Marca la serie actual como hecha y devuelve la sesión resultante: descanso (entre series
     * o entre ejercicios) con [now] como inicio, o FINISHED si era la última serie de todo.
     */
    fun completeSet(s: ActiveSession, weight: String, now: Long): ActiveSession {
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return s
        val day = PlanData.dayByNumber(s.dayNumber) ?: return s
        val exercise = day.template.exercises.getOrNull(s.exerciseIndex) ?: return s
        val totalSets = setCountFromScheme(exercise.scheme)

        val cleanWeight = weight.trim()
        val newSet = CompletedSet(s.exerciseIndex, s.setNumber, cleanWeight)
        val order = s.orderOrDefault()
        val isLastSet = s.setNumber >= totalSets
        val isLastExercise = order.indexOf(s.exerciseIndex) >= order.lastIndex

        return if (isLastSet && isLastExercise) {
            s.copy(phase = SessionPhase.FINISHED, completedSets = s.completedSets + newSet, occupiedSkips = 0)
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
                plannedWeight = ""
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
        val pos = order.indexOf(s.exerciseIndex)
        if (pos < 0 || pos >= order.lastIndex) return s // no hay otro ejercicio al que pasar
        val newOrder = order.toMutableList()
        newOrder.add(newOrder.removeAt(pos))
        val nextEx = newOrder[pos]
        return s.copy(
            order = newOrder,
            exerciseIndex = nextEx,
            setNumber = s.completedSets.count { it.exerciseIndex == nextEx } + 1,
            phase = SessionPhase.WORKING,
            occupiedSkips = s.occupiedSkips + 1,
            // El peso preparado era para la máquina que dejamos: ya no aplica.
            plannedWeight = ""
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
        val (nextEx, nextSet) = if (s.setNumber < totalSets) {
            s.exerciseIndex to (s.setNumber + 1)
        } else {
            val pos = order.indexOf(s.exerciseIndex)
            val ne = order.getOrElse(pos + 1) { s.exerciseIndex }
            ne to (s.completedSets.count { it.exerciseIndex == ne } + 1)
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
