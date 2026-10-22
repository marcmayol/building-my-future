package com.marc.gymplan100.data

/**
 * El entreno libre, guiado: lo mismo que el del plan, pero sin plan.
 *
 * Antes el libre era un cronómetro y una libreta: se entrenaba y luego se apuntaba todo de
 * memoria. Ahora va como el guiado: calientas, haces la serie, pulsas «Serie terminada» y el
 * descanso arranca en el acto; mientras corre, la app pregunta qué ejercicio era (solo la
 * primera vez) y deja escritos los kilos y las repeticiones de la última vez para corregirlos
 * si hace falta. «Terminar ejercicio» da el descanso entre ejercicios, y vuelta a empezar. Al
 * finalizar ya está todo apuntado: solo queda repasarlo.
 *
 * Todo pasa dentro de [SessionPhase.FREE], con [ActiveSession.freeStep] diciendo en qué
 * momento se está, para que el reloj, la notificación y lo que ya sabía tratar un libre sigan
 * funcionando igual. Lo apuntado vive en [ActiveSession.freeLog], el mismo borrador de antes.
 *
 * Funciones puras, como [SessionEngine]: no guardan ni programan alarmas.
 */
object FreeSessionEngine {

    /** Un libre nuevo, calentando [warmupSeconds] (0 = sin calentamiento, directo a la serie). */
    fun start(
        dayNumber: Int,
        now: Long,
        extra: Boolean = false,
        warmupSeconds: Int = 0
    ): ActiveSession = ActiveSession(
        dayNumber = dayNumber,
        startMillis = now,
        phase = SessionPhase.FREE,
        special = true,
        extra = extra,
        freeStep = if (warmupSeconds > 0) FreeStep.WARMUP else FreeStep.WORK,
        warmupTargetSeconds = warmupSeconds,
        warmupStartMillis = now
    )

    private fun ActiveSession.isFree(step: FreeStep) = phase == SessionPhase.FREE && freeStep == step

    /** «Ya he calentado». */
    fun endWarmup(s: ActiveSession): ActiveSession {
        if (!s.isFree(FreeStep.WARMUP)) return s
        return s.copy(freeStep = FreeStep.WORK, warmupPaused = false)
    }

    /**
     * «Serie terminada»: apunta la serie y arranca el descanso entre series en [now].
     *
     * Sin ejercicio en curso se abre uno sin nombre, que se le pone durante el descanso (ver
     * [nameCurrent]): pedirlo ANTES de la serie obligaría a pararse con la barra cargada. La
     * serie nueva sale con los kilos y las repeticiones de la anterior del mismo ejercicio, que
     * es lo que casi siempre se repite; si es la primera, con los de la última vez ([weights] y
     * [reps], por nombre).
     */
    fun setDone(
        s: ActiveSession,
        now: Long,
        restBase: RestDefaults.Base = RestDefaults.Base(),
        yourRests: Map<String, Int> = emptyMap(),
        weights: Map<String, String> = emptyMap(),
        reps: Map<String, String> = emptyMap()
    ): ActiveSession {
        if (!s.isFree(FreeStep.WORK)) return s
        val log = s.freeLog.toMutableList()
        val actual = log.getOrNull(s.freeCurrent)
        val indice: Int
        if (actual == null) {
            log += LoggedExercise(name = "", sets = listOf(SetLog()))
            indice = log.lastIndex
        } else {
            val anterior = actual.sets.lastOrNull()
            val nueva = anterior?.copy(rir = "", warmup = false)
                ?: SetLog(weights[actual.name].orEmpty(), reps[actual.name].orEmpty())
            log[s.freeCurrent] = actual.copy(sets = actual.sets + nueva)
            indice = s.freeCurrent
        }
        val nombre = log[indice].name
        val tuyo = yourRests[nombre]?.takeIf { it > 0 && nombre.isNotBlank() }
        return s.copy(
            freeLog = log,
            freeCurrent = indice,
            freeStep = FreeStep.REST,
            restStartMillis = now,
            restTargetSeconds = tuyo ?: restBase.sets,
            restBetweenExercises = false,
            restSource = (if (tuyo != null) RestDefaults.Source.YOURS else RestDefaults.Source.DEFAULT).name
        )
    }

    /**
     * «Terminar ejercicio»: cierra el ejercicio en curso y da el descanso entre ejercicios.
     *
     * Lo normal es pulsarlo descansando tras la última serie: entonces el descanso que ya corre
     * pasa a ser el largo, contado desde el mismo instante (y con lo que sume cómo ha ido la
     * serie). Si se pulsa con la serie delante, el descanso empieza ahora.
     */
    fun finishExercise(s: ActiveSession, now: Long, restBase: RestDefaults.Base = RestDefaults.Base()): ActiveSession {
        if (s.phase != SessionPhase.FREE || s.freeCurrent < 0) return s
        return when (s.freeStep) {
            FreeStep.REST -> {
                if (s.restBetweenExercises) return s
                val esfuerzo = RestDefaults.forEffort(
                    s.freeLog.getOrNull(s.freeCurrent)?.sets?.lastOrNull()?.rir.orEmpty()
                )
                s.copy(
                    freeCurrent = -1,
                    restBetweenExercises = true,
                    restTargetSeconds = maxOf(restBase.exercises, s.restTargetSeconds - esfuerzo) + esfuerzo,
                    restSource = RestDefaults.Source.DEFAULT.name
                )
            }
            FreeStep.WORK -> s.copy(
                freeCurrent = -1,
                freeStep = FreeStep.REST,
                restStartMillis = now,
                restTargetSeconds = restBase.exercises,
                restBetweenExercises = true,
                restSource = RestDefaults.Source.DEFAULT.name
            )
            FreeStep.WARMUP -> s
        }
    }

    /** Fin del descanso: a por la siguiente serie (del mismo ejercicio, o de uno nuevo). */
    fun endRest(s: ActiveSession): ActiveSession {
        if (!s.isFree(FreeStep.REST)) return s
        return s.copy(freeStep = FreeStep.WORK, restTargetSeconds = 0)
    }

    /**
     * Le pone nombre al ejercicio en curso. Si ese ejercicio ya se había hecho hoy (press, luego
     * curl, y otra vez press), las series se juntan con las de entonces en vez de salir dos
     * veces. La serie que estaba sin datos se rellena con los de la última vez.
     */
    fun nameCurrent(
        s: ActiveSession,
        name: String,
        weights: Map<String, String> = emptyMap(),
        reps: Map<String, String> = emptyMap()
    ): ActiveSession {
        val nombre = name.trim()
        val actual = s.freeLog.getOrNull(s.freeCurrent) ?: return s
        if (nombre.isEmpty()) return s
        val series = actual.sets.map {
            if (it.isEmpty) it.copy(weight = weights[nombre].orEmpty(), reps = reps[nombre].orEmpty()) else it
        }
        val log = s.freeLog.toMutableList()
        val previo = log.indexOfFirst { it.name.equals(nombre, ignoreCase = true) }
        return if (previo >= 0 && previo != s.freeCurrent) {
            log[previo] = log[previo].copy(sets = log[previo].sets + series)
            log.removeAt(s.freeCurrent)
            s.copy(freeLog = log, freeCurrent = if (previo > s.freeCurrent) previo - 1 else previo)
        } else {
            log[s.freeCurrent] = actual.copy(name = nombre, sets = series)
            s.copy(freeLog = log)
        }
    }

    /** Corrige los kilos y las repeticiones de la última serie del ejercicio en curso. */
    fun editLastSet(s: ActiveSession, weight: String, reps: String): ActiveSession {
        val actual = s.freeLog.getOrNull(s.freeCurrent) ?: return s
        val ultima = actual.sets.lastOrNull() ?: return s
        val log = s.freeLog.toMutableList()
        log[s.freeCurrent] = actual.copy(
            sets = actual.sets.dropLast(1) + ultima.copy(weight = weight.trim(), reps = reps.trim())
        )
        return s.copy(freeLog = log)
    }

    /**
     * Cómo ha ido la última serie. Como en el guiado, alarga el descanso en marcha: se suma lo
     * que pide el nuevo y se quita lo que había sumado el anterior.
     */
    fun setLastRir(s: ActiveSession, rir: String): ActiveSession {
        val actual = s.freeLog.getOrNull(s.freeCurrent) ?: return s
        val ultima = actual.sets.lastOrNull() ?: return s
        val log = s.freeLog.toMutableList()
        log[s.freeCurrent] = actual.copy(sets = actual.sets.dropLast(1) + ultima.copy(rir = rir.trim()))
        val delta = RestDefaults.forEffort(rir) - RestDefaults.forEffort(ultima.rir)
        val descansando = s.isFree(FreeStep.REST)
        return s.copy(
            freeLog = log,
            restTargetSeconds = if (descansando) (s.restTargetSeconds + delta).coerceIn(0, 600)
            else s.restTargetSeconds
        )
    }

    /** ±30 s al descanso en marcha. */
    fun adjustRest(s: ActiveSession, deltaSeconds: Int): ActiveSession {
        if (!s.isFree(FreeStep.REST)) return s
        return s.copy(
            restTargetSeconds = (s.restTargetSeconds + deltaSeconds).coerceIn(0, 600),
            restSource = RestDefaults.Source.YOURS.name
        )
    }

    /** ±1 min al calentamiento. */
    fun adjustWarmup(s: ActiveSession, deltaSeconds: Int): ActiveSession {
        if (!s.isFree(FreeStep.WARMUP)) return s
        return s.copy(warmupTargetSeconds = (s.warmupTargetSeconds + deltaSeconds).coerceIn(0, 3600))
    }
}
