package com.marc.gymplan100.data

/**
 * Transiciones de una sesión de RUTINA ESPECIAL (militar / quema grasa) como funciones puras
 * sobre [ActiveSession], al estilo de [SessionEngine]. No tienen efectos laterales: no guardan
 * ni programan alarmas (eso lo hace el ViewModel, que reutiliza `RestReminder`).
 *
 * Reutiliza las fases y campos ya existentes:
 *  - Paso/serie POR TIEMPO o intervalo de TRABAJO -> [SessionPhase.TIMED_SET] + campos `timed*`.
 *  - Paso/serie POR REPETICIONES -> [SessionPhase.WORKING] (registro de reps).
 *  - Descanso entre rondas/series -> [SessionPhase.RESTING] + campos `rest*`.
 *  - Fin -> [SessionPhase.FINISHED].
 *
 * La militar es continua (sin descanso entre pasos); la quema grasa sí descansa entre rondas/series.
 */
object SpecialSessionEngine {

    /** Descanso por defecto entre series de quema grasa cuando el protocolo no lo especifica. */
    const val DEFAULT_SERIES_REST = 90

    // ---------------------------------------------------------------- Militar

    /** Construye la sesión militar en su primer paso, en marcha. */
    fun startMilitary(rutina: Rutina, dayNumber: Int, now: Long): ActiveSession {
        val steps = rutina.pasosOrdenados
        val base = ActiveSession(
            dayNumber = dayNumber,
            startMillis = now,
            routineId = rutina.id,
            totalUnits = steps.size,
            stepIndex = 0
        )
        return militaryStepState(rutina, base, 0, useAlternative = false, now = now)
    }

    /** Estado de la sesión para el paso [stepIndex] de la militar (fija fase y temporizador). */
    private fun militaryStepState(
        rutina: Rutina,
        base: ActiveSession,
        stepIndex: Int,
        useAlternative: Boolean,
        now: Long
    ): ActiveSession {
        val steps = rutina.pasosOrdenados
        val paso = steps.getOrNull(stepIndex) ?: return base.copy(phase = SessionPhase.FINISHED)
        val altTime = useAlternative && paso.alternativa != null
        val esTiempo = altTime || paso.esTiempo
        val target = if (altTime) paso.alternativa!!.duracion_seg else paso.objetivoSeg
        return if (esTiempo) {
            base.copy(
                stepIndex = stepIndex,
                useAlternative = useAlternative,
                phase = SessionPhase.TIMED_SET,
                timedTargetSeconds = target,
                timedStartMillis = now,
                timedElapsedBeforePause = 0,
                timedPaused = false
            )
        } else {
            base.copy(
                stepIndex = stepIndex,
                useAlternative = useAlternative,
                phase = SessionPhase.WORKING
            )
        }
    }

    /**
     * Marca el paso actual como hecho ([repsLogged] para los de repeticiones) y avanza al
     * siguiente. Si era el último, la sesión pasa a FINISHED (cuenta como rutina completa).
     */
    fun advanceMilitary(rutina: Rutina, s: ActiveSession, repsLogged: String, now: Long): ActiveSession {
        if (s.routineId != rutina.id) return s
        val done = CompletedSet(
            exerciseIndex = s.stepIndex,
            setNumber = s.stepIndex + 1,
            reps = repsLogged.trim()
        )
        val withLog = s.copy(completedSets = s.completedSets + done, useAlternative = false)
        val nextIndex = s.stepIndex + 1
        if (nextIndex >= rutina.pasosOrdenados.size) {
            return withLog.copy(phase = SessionPhase.FINISHED, stepIndex = s.stepIndex)
        }
        return militaryStepState(rutina, withLog, nextIndex, useAlternative = false, now = now)
    }

    /** En el paso con alternativa (burpees -> jumping jacks), la cambia por la variante por tiempo. */
    fun chooseMilitaryAlternative(rutina: Rutina, s: ActiveSession, now: Long): ActiveSession {
        val paso = rutina.pasosOrdenados.getOrNull(s.stepIndex) ?: return s
        if (paso.alternativa == null || s.useAlternative) return s
        return militaryStepState(rutina, s, s.stepIndex, useAlternative = true, now = now)
    }

    // ------------------------------------------------------------ Quema grasa

    /** Construye la sesión de un ejercicio de quema grasa con el protocolo elegido, en marcha. */
    fun startFatburn(
        exercise: EjercicioCatalogo,
        protocol: Protocolo,
        dayNumber: Int,
        now: Long
    ): ActiveSession {
        val total = when {
            protocol.esIntervalos -> protocol.numRondas
            protocol.esSeries -> protocol.numSeries
            else -> 1
        }
        val base = ActiveSession(
            dayNumber = dayNumber,
            startMillis = now,
            routineId = "quema_grasa",
            exerciseId = exercise.id,
            protocolName = protocol.nombre,
            totalUnits = total,
            stepIndex = 0
        )
        return fatburnUnitState(protocol, base, 0, now)
    }

    /** Estado para la unidad [index] (ronda/serie) del protocolo de quema grasa. */
    private fun fatburnUnitState(
        protocol: Protocolo,
        base: ActiveSession,
        index: Int,
        now: Long
    ): ActiveSession = when {
        protocol.esIntervalos -> base.copy(
            stepIndex = index,
            phase = SessionPhase.TIMED_SET,
            timedTargetSeconds = protocol.trabajoSeg,
            timedStartMillis = now,
            timedElapsedBeforePause = 0,
            timedPaused = false
        )
        protocol.esTiempoUnico -> base.copy(
            stepIndex = index,
            phase = SessionPhase.TIMED_SET,
            timedTargetSeconds = protocol.duracion_inicial_seg,
            timedStartMillis = now,
            timedElapsedBeforePause = 0,
            timedPaused = false
        )
        else -> base.copy(stepIndex = index, phase = SessionPhase.WORKING) // series por reps
    }

    /**
     * Completa la unidad actual (trabajo por tiempo o serie por reps). Si era la última, FINISHED;
     * si no, arranca el descanso hacia la siguiente. [repsLogged] solo aplica a series por reps.
     */
    fun completeFatburnUnit(
        protocol: Protocolo,
        s: ActiveSession,
        repsLogged: String,
        now: Long
    ): ActiveSession {
        val done = CompletedSet(
            exerciseIndex = s.stepIndex,
            setNumber = s.stepIndex + 1,
            reps = repsLogged.trim()
        )
        val withLog = s.copy(completedSets = s.completedSets + done)
        val isLast = s.stepIndex >= s.totalUnits - 1
        if (isLast) {
            return withLog.copy(phase = SessionPhase.FINISHED)
        }
        val rest = if (protocol.esIntervalos) protocol.descansoSeg.coerceAtLeast(1)
        else DEFAULT_SERIES_REST
        return withLog.copy(
            phase = SessionPhase.RESTING,
            restStartMillis = now,
            restTargetSeconds = rest,
            restBetweenExercises = false
        )
    }

    /** Termina el descanso entre rondas/series y arranca la siguiente unidad. */
    fun endFatburnRest(protocol: Protocolo, s: ActiveSession, now: Long): ActiveSession {
        if (s.phase != SessionPhase.RESTING) return s
        return fatburnUnitState(protocol, s.copy(restTargetSeconds = 0), s.stepIndex + 1, now)
    }

    // ------------------------------------------------- Desde la notificación

    /**
     * Aplica la transición que corresponde al botón "Saltar/Hecho" de la notificación persistente,
     * en segundo plano. Solo actúa sobre fases con cuenta atrás (TIMED_SET / RESTING); en las de
     * repeticiones (WORKING) devuelve la misma sesión (se completan desde la app, con el registro).
     */
    fun skipFromNotification(data: SpecialWorkoutsData, s: ActiveSession, now: Long): ActiveSession {
        val routineId = s.routineId ?: return s
        if (routineId == SpecialWorkoutsLoader.MILITAR_ID) {
            val rutina = data.militar ?: return s
            return if (s.phase == SessionPhase.TIMED_SET) advanceMilitary(rutina, s, "", now) else s
        }
        val exercise = data.ejercicio(s.exerciseId ?: return s) ?: return s
        val protocol = exercise.protocolos.firstOrNull { it.nombre == s.protocolName } ?: return s
        return when (s.phase) {
            SessionPhase.TIMED_SET -> completeFatburnUnit(protocol, s, "", now)
            SessionPhase.RESTING -> endFatburnRest(protocol, s, now)
            else -> s
        }
    }
}
