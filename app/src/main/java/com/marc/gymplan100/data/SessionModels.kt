package com.marc.gymplan100.data

import kotlinx.serialization.Serializable

/** Fase en la que se encuentra una sesión de entrenamiento en curso. */
enum class SessionPhase {
    /** Calentamiento inicial con temporizador (cuenta atrás) y pausa/play. */
    WARMUP,

    /** Haciendo una serie: el usuario introduce peso y marca "serie hecha". */
    WORKING,

    /** Haciendo una serie por tiempo (planchas, isométricos): cuenta atrás con aviso al terminar. */
    TIMED_SET,

    /** Descansando entre series o entre ejercicios, con cuenta atrás. */
    RESTING,

    /** Entrenamiento especial/libre: solo cronómetro corriendo hasta pulsar "Finalizar". */
    FREE,

    /** Todas las series del día hechas: se muestra el resumen final. */
    FINISHED
}

/** Una serie ya realizada dentro de una sesión. */
@Serializable
data class CompletedSet(
    val exerciseIndex: Int,
    val setNumber: Int,
    val weight: String = "",
    /** Descanso real (segundos) tomado tras esta serie. 0 si fue la última. */
    val restSeconds: Int = 0,
    /**
     * Repeticiones que se hicieron de verdad en esta serie (número, texto o AMRAP).
     *
     * En el entreno guiado se rellenan solas con lo que pide el plan y solo se tocan cuando el
     * día no sale: marcar "serie hecha" es decir "he hecho lo que ponía". Sin esto, tres semanas
     * subiendo de 8 a 12 repeticiones con los mismos kilos eran tres semanas de línea plana.
     */
    val reps: String = "",
    /** Serie de aproximación: se apunta, pero no cuenta como trabajo. */
    val warmup: Boolean = false,
    /** Repeticiones en reserva (RIR) apuntadas para esta serie; vacío si no se apuntó. */
    val rir: String = ""
)

/** Sesión de entrenamiento en curso. Se persiste para poder reanudarla. */
@Serializable
data class ActiveSession(
    val dayNumber: Int,
    val startMillis: Long,
    val exerciseIndex: Int = 0,
    val setNumber: Int = 1,
    val phase: SessionPhase = SessionPhase.WORKING,
    /** Entrenamiento especial/libre (p. ej. guiado por su tío): sin series, solo cronómetro. */
    val special: Boolean = false,
    /** Entrenamiento EXTRA: como el especial, pero NO cuenta como día del plan (es un bonus). */
    val extra: Boolean = false,
    /**
     * La sesión se inició o se controló desde el reloj (Wear OS). Como el reloj mide el pulso,
     * Google Health ya tiene calorías reales, así que al finalizar NO estimamos las nuestras
     * (evita el doble conteo). Se activa solo: es el "interruptor automático".
     */
    val watchControlled: Boolean = false,
    val restStartMillis: Long = 0L,
    val restTargetSeconds: Int = 0,
    val restBetweenExercises: Boolean = false,
    /** De dónde sale el descanso en curso ([RestDefaults.Source], por nombre), para explicarlo. */
    val restSource: String = "",
    /**
     * Peso (kg) preparado durante el descanso para la próxima serie: permite dejar la máquina
     * lista antes de continuar. Si no está en blanco, precarga el campo de peso de la siguiente
     * serie. Se limpia al iniciar cada descanso para recalcular la sugerencia.
     */
    val plannedWeight: String = "",
    /**
     * Repeticiones apuntadas para la serie EN CURSO, persistidas por el mismo motivo que el
     * peso: si Android mata la app a mitad de la serie, lo escrito no se pierde. Se limpia al
     * cerrar cada serie y al cambiar de máquina, porque la siguiente vuelve a partir del plan.
     */
    val currentReps: String = "",
    val completedSets: List<CompletedSet> = emptyList(),
    /** Orden en que se realizan los ejercicios (puede reordenarse al saltar uno). */
    val order: List<Int> = emptyList(),
    /**
     * Cambios de máquina seguidos por "máquina ocupada" sin completar ninguna serie entre medias.
     * Con él la UI rotula el botón ("Máquina ocupada" vs "Esta también está ocupada").
     * Se pone a 0 al completar una serie (ya conseguiste máquina).
     */
    val occupiedSkips: Int = 0,
    // --- Calentamiento (fase WARMUP) ---
    /** Duración objetivo del calentamiento (segundos), deducida del plan y ajustable. */
    val warmupTargetSeconds: Int = 0,
    /** Inicio del tramo en marcha del calentamiento; sin sentido si está en pausa. */
    val warmupStartMillis: Long = 0L,
    /** Segundos ya transcurridos acumulados antes del tramo en marcha actual. */
    val warmupElapsedBeforePause: Int = 0,
    /** Si el temporizador de calentamiento está pausado. */
    val warmupPaused: Boolean = false,
    // --- Serie por tiempo (fase TIMED_SET) ---
    /** Duración objetivo de la serie por tiempo (segundos), deducida del esquema y ajustable. */
    val timedTargetSeconds: Int = 0,
    /** Inicio del tramo en marcha de la serie por tiempo; sin sentido si está en pausa. */
    val timedStartMillis: Long = 0L,
    /** Segundos ya transcurridos acumulados antes del tramo en marcha actual. */
    val timedElapsedBeforePause: Int = 0,
    /** Si el temporizador de la serie por tiempo está pausado. */
    val timedPaused: Boolean = false,
    // --- Rutinas especiales (militar / quema grasa) ---
    /**
     * Identificador de la rutina especial en curso (ver `entrenamientos_especiales.json`):
     * "militar_basica" o "quema_grasa". Null = sesión normal del plan de 100 días.
     */
    val routineId: String? = null,
    /** Ejercicio del catálogo de quema grasa en curso (null en la militar). */
    val exerciseId: String? = null,
    /** Nombre del protocolo elegido para el ejercicio de quema grasa. */
    val protocolName: String? = null,
    /** Índice del paso actual (militar) o de la ronda/serie actual (quema grasa), base 0. */
    val stepIndex: Int = 0,
    /** Total de pasos (militar) o de rondas/series (quema grasa). */
    val totalUnits: Int = 0,
    /** En el paso con alternativa (burpees/jumping jacks), si se eligió la alternativa. */
    val useAlternative: Boolean = false,
    /**
     * Entreno libre: lo que se va apuntando sobre la marcha, antes de finalizar. Vive en la
     * sesión (y no en la pantalla) para que sobreviva a que Android mate la app entre serie y
     * serie; al finalizar se enseña para repasarlo en vez de pedirlo todo de memoria.
     */
    val freeLog: List<LoggedExercise> = emptyList(),
    /**
     * Lo que se hizo de verdad en lugar del ejercicio del plan (clave: índice en el día).
     *
     * Sin esto, quien no tiene pec deck y hace aperturas con mancuernas no tenía forma de
     * apuntarlo: la serie quedaba registrada como pec deck, con sus kilos mezclados con los de
     * una máquina que no ha tocado. El cambio manda en todo lo que lee el nombre: dibujo, ficha,
     * pesos y progresión.
     */
    val swaps: Map<Int, String> = emptyMap(),
    /**
     * Tope de series por ejercicio del entreno exprés (0 = las del plan). Lo que se quita del
     * todo va fuera de [order]; esto solo recorta.
     */
    val maxSets: Int = 0
) {
    /** Hay al menos un ejercicio apuntado con alguna serie en el entreno libre. */
    val hasFreeLog: Boolean
        get() = freeLog.any { it.name.isNotBlank() && it.setsOrSingle.isNotEmpty() }

    /** Es una rutina especial (militar o quema grasa), no una sesión del plan de 100 días. */
    val isRoutine: Boolean get() = routineId != null

    /** Es un entreno exprés: con menos ejercicios o menos series que el día del plan. */
    val isExpress: Boolean
        get() = maxSets > 0 ||
            (order.isNotEmpty() && order.size < (PlanData.dayByNumber(dayNumber)?.template?.exercises?.size ?: 0))

    /**
     * El ejercicio [index] del día tal y como se está haciendo HOY: el del plan, o el que se
     * eligió en su lugar. Todo lo que dependa del nombre (pesos, ficha, dibujo) pasa por aquí.
     */
    fun exerciseAt(index: Int): Exercise? {
        val delPlan = PlanData.dayByNumber(dayNumber)?.template?.exercises?.getOrNull(index) ?: return null
        val otro = swaps[index]?.trim().orEmpty()
        return if (otro.isEmpty()) delPlan else delPlan.copy(name = otro)
    }

    /** El nombre que trae el plan para [index], aunque hoy se esté haciendo otro. */
    fun planNameAt(index: Int): String? =
        PlanData.dayByNumber(dayNumber)?.template?.exercises?.getOrNull(index)?.name

    /** Series que tocan hoy en [exercise]: las del plan, recortadas si es exprés. */
    fun setsFor(exercise: Exercise): Int {
        val delPlan = setCountFromScheme(exercise.scheme)
        return if (maxSets > 0) minOf(delPlan, maxSets) else delPlan
    }
    /** Segundos de calentamiento transcurridos en el instante [now]. */
    fun warmupElapsed(now: Long): Int =
        if (warmupPaused) warmupElapsedBeforePause
        else warmupElapsedBeforePause + ((now - warmupStartMillis) / 1000).toInt().coerceAtLeast(0)

    /** Segundos transcurridos de la serie por tiempo en el instante [now]. */
    fun timedElapsed(now: Long): Int =
        if (timedPaused) timedElapsedBeforePause
        else timedElapsedBeforePause + ((now - timedStartMillis) / 1000).toInt().coerceAtLeast(0)
}

/** Registro histórico de una sesión finalizada. */
@Serializable
data class SessionRecord(
    val dayNumber: Int,
    val startMillis: Long,
    val endMillis: Long,
    val totalSets: Int,
    val totalRestSeconds: Int,
    /** Entrenamiento especial/libre (cronómetro guiado, sin series). */
    val special: Boolean = false,
    /** Entrenamiento EXTRA (bonus que no cuenta como día del plan). */
    val extra: Boolean = false,
    /** Rutina especial de la que proviene la sesión ("militar_basica"/"quema_grasa"), o null. */
    val routineId: String? = null,
    /** Ejercicio del catálogo de quema grasa (null en militar o sesiones normales). */
    val exerciseId: String? = null,
    /** Solo militar: la sesión llegó hasta el último paso (cuenta para la frecuencia semanal). */
    val routineCompleted: Boolean = false,
    /**
     * Lo que se apuntó a mano al terminar un entrenamiento libre. En el guiado no hace falta
     * (cada serie se registra sobre la marcha), pero entrenando a tu aire el cronómetro solo
     * guardaba el tiempo y lo demás se perdía.
     */
    val logged: List<LoggedExercise> = emptyList()
) {
    val durationSeconds: Int get() = ((endMillis - startMillis) / 1000).toInt().coerceAtLeast(0)
}

/**
 * Un ejercicio apuntado a mano al acabar un entreno libre: el nombre (del plan, del catálogo
 * o escrito por ti) con el peso y las repeticiones que hayas hecho.
 *
 * [sets] guarda serie a serie —10 kg × 12, 12 kg × 12, 11 kg × 10—, que es como se entrena de
 * verdad. [weight] y [reps] siguen ahí como resumen del ejercicio (el peso más alto y las
 * repeticiones de esa serie), y son lo único que traen los entrenos apuntados antes de la 2.4.
 */
@Serializable
data class LoggedExercise(
    val name: String,
    val weight: String = "",
    val reps: String = "",
    val sets: List<SetLog> = emptyList()
) {
    /** Las series apuntadas; si es un registro viejo (sin desglose), el resumen hace de serie única. */
    val setsOrSingle: List<SetLog>
        get() = sets.filter { !it.isEmpty }.ifEmpty {
            if (weight.isBlank() && reps.isBlank()) emptyList() else listOf(SetLog(weight, reps))
        }
}

/**
 * Lo que sube o baja el peso con un toque del stepper.
 *
 * 0,5 kg y no 2,5: en un gimnasio no todo son discos de 2,5. Hay mancuernas de 1 en 1, discos
 * de medio kilo y máquinas con placas que no van de dos en dos, y con el paso grande no había
 * manera de escribir el peso real de la serie. Para los saltos gordos están la rueda, escribir
 * el número a mano y mantener pulsado el botón, que acelera.
 */
const val WEIGHT_STEP = 0.5

/** "12,5" o "12.5" -> 12.5. Null si no hay número. */
fun parseKg(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

/** 12.5 -> "12,5"; 40.0 -> "40". Coma decimal, que es como se escribe aquí. */
fun formatKg(value: Double): String {
    val v = value.coerceIn(0.0, 500.0)
    return if (v % 1.0 == 0.0) v.toInt().toString()
    else "%.1f".format(v).replace('.', ',')
}

/**
 * El más pesado de unos kilos escritos, comparando por número y no por texto ("9" pesa menos
 * que "10", aunque alfabéticamente vaya después). Devuelve "" si ninguno trae un número.
 *
 * Es lo que se guarda como peso de referencia del ejercicio: de una serie a otra se sube y se
 * baja —10, 12, 11— y lo que cuenta como "lo que levantas" es el tope, no lo último que tocó.
 */
fun heaviestWeight(weights: List<String>): String =
    weights.filter { parseKg(it) != null }.maxByOrNull { parseKg(it)!! }?.trim().orEmpty()

/**
 * Cuánto se descansa entre series, y por qué.
 *
 * Hasta la 2.17 eran 90 s para todo: una sentadilla a 5 repeticiones descansaba lo mismo que un
 * curl a 15, y el plan de fuerza escribía «3 min de descanso» en la nota mientras la cuenta
 * atrás sonaba al minuto y medio. Ahora el descanso sale, por este orden, de:
 *
 * 1. lo que tú pusiste la última vez en ese ejercicio (si lo alargaste, es que lo necesitas);
 * 2. lo que diga la nota del plan («3 min de descanso», «descansa 90 s»);
 * 3. las repeticiones: a pocas repeticiones —fuerza— el músculo tarda más en recuperarse, y
 *    menos de 3 minutos en un básico pesado es quedarse corto en la serie siguiente.
 */
object RestDefaults {
    const val BETWEEN_SETS = 90
    const val BETWEEN_EXERCISES = 120

    /** Descanso largo de fuerza: básicos a pocas repeticiones. */
    const val HEAVY = 180

    /** Descanso intermedio: aislados a pocas repeticiones. */
    const val MEDIUM = 120

    /** De dónde sale el descanso, para poder decirlo en pantalla. */
    enum class Source { YOURS, PLAN, HEAVY, DEFAULT }

    data class Rest(val seconds: Int, val source: Source)

    /**
     * Descanso entre series de [exercise]. [repsDone] son las repeticiones de la serie que se
     * acaba de cerrar (manda sobre el esquema: si el plan dice 12 y hoy has hecho 6 con más
     * peso, descansas como en una serie de 6). [yours] son los descansos que el usuario dejó
     * puestos, por nombre de ejercicio.
     */
    fun between(exercise: Exercise, repsDone: String = "", yours: Map<String, Int> = emptyMap()): Rest {
        yours[exercise.name]?.takeIf { it > 0 }?.let { return Rest(it, Source.YOURS) }
        fromNote(exercise.note)?.let { return Rest(it, Source.PLAN) }
        if (exercise.kind != ExerciseKind.STRENGTH || isBodyweightScheme(exercise.scheme) ||
            secondsPerSetFromScheme(exercise.scheme) != null
        ) return Rest(BETWEEN_SETS, Source.DEFAULT)
        val reps = parseReps(repsDone) ?: repsRangeFromScheme(exercise.scheme)?.last
            ?: return Rest(BETWEEN_SETS, Source.DEFAULT)
        return when {
            reps <= 5 -> Rest(HEAVY, Source.HEAVY)
            reps <= 8 -> if (isBigLift(exercise.name)) Rest(HEAVY, Source.HEAVY) else Rest(MEDIUM, Source.HEAVY)
            else -> Rest(BETWEEN_SETS, Source.DEFAULT)
        }
    }

    /** Entre un ejercicio y el siguiente: como poco lo de siempre, y nunca menos que entre series. */
    fun betweenExercises(between: Rest): Rest =
        if (between.seconds > BETWEEN_EXERCISES) between else Rest(BETWEEN_EXERCISES, between.source)

    /**
     * Los segundos de descanso que pide una nota del plan, o null si no habla de descanso.
     * «3 min de descanso» -> 180, «3-4 min de descanso» -> 180 (el suelo: el resto lo decides
     * tú con +30 s), «descansa 90 s» -> 90.
     */
    fun fromNote(note: String): Int? {
        val t = note.lowercase()
        if (!t.contains("descans")) return null
        Regex("""(\d+(?:[.,]\d+)?)\s*(?:-\s*\d+(?:[.,]\d+)?\s*)?min""").find(t)?.let {
            val min = it.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            return (min * 60).toInt().coerceIn(15, 600)
        }
        Regex("""(\d+)\s*(?:-\s*\d+\s*)?(?:s|seg|segundos)\b""").find(t)?.let {
            return it.groupValues[1].toInt().coerceIn(15, 600)
        }
        return null
    }

    /**
     * Un básico que mueve mucho músculo: sentadillas, pesos muertos, prensas, presses, remos,
     * jalones y dominadas. Lo contrario son los aislados (curl, extensión, apertura,
     * elevación), que a las mismas repeticiones se recuperan antes.
     */
    fun isBigLift(name: String): Boolean {
        val n = java.text.Normalizer.normalize(name.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
        val aislado = listOf("curl", "extension", "apertura", "elevacion", "pec deck", "patada",
            "pajaro", "face pull", "gemelo", "abduc", "aduc", "encogimiento", "frances")
        if (aislado.any { n.contains(it) }) return false
        val basico = listOf("sentadilla", "peso muerto", "prensa", "press", "remo", "jalon",
            "dominada", "hip thrust", "zancada", "fondo", "buenos dias", "hack")
        return basico.any { n.contains(it) }
    }
}

/**
 * Deduce la duración del calentamiento (segundos) del texto del plan.
 * Toma los minutos indicados; si es un rango ("5-8 min") usa el valor mayor.
 * Ej: "5-8 min bici" -> 8 min, "6 min de calentamiento" -> 6 min. Por defecto 6 min.
 */
fun warmupSecondsFromText(warmup: String): Int {
    val head = warmup.substringBefore("min", warmup)
    val minutes = Regex("""\d+""").findAll(head)
        .map { it.value.toInt() }
        .maxOrNull() ?: 6
    return minutes.coerceIn(1, 30) * 60
}

/**
 * Deduce el número de series de un esquema textual.
 * Ej: "3 x 12" -> 3, "4 x 8-10" -> 4, "3 vueltas" -> 3, "1 serie" -> 1.
 */
fun setCountFromScheme(scheme: String): Int {
    val s = scheme.lowercase()
    Regex("""(\d+)\s*vuelta""").find(s)?.let { return it.groupValues[1].toInt() }
    Regex("""(\d+)\s*serie""").find(s)?.let { return it.groupValues[1].toInt() }
    Regex("""(\d+)\s*x""").find(s)?.let { return it.groupValues[1].toInt() }
    return 3
}

/**
 * Si el ejercicio se mide SOLO por tiempo (planchas, isométricos), devuelve los
 * segundos objetivo de cada serie; si lleva repeticiones, peso o es mixto, devuelve null.
 * Ej: "3 x 30 s" -> 30, "3 x 20-30 s" -> 30 (usa el mayor), "3 x 40 s" -> 40.
 *     "3 x 12" -> null, "3 vueltas" -> null, "2 x 20 s / 2 x 10" -> null (mixto).
 */
fun secondsPerSetFromScheme(scheme: String): Int? {
    val s = scheme.lowercase().trim()
    val m = Regex("""^\d+\s*x\s*(\d+)\s*(?:-\s*(\d+))?\s*(?:s|seg|segundos)\b\.?$""").find(s) ?: return null
    val low = m.groupValues[1].toInt()
    val high = m.groupValues[2].toIntOrNull()
    return (high ?: low).coerceIn(1, 600)
}

/**
 * Ejercicio de peso corporal sin carga que registrar (circuitos "por vueltas"):
 * no tiene sentido pedir kilos. Ej: "3 vueltas" -> true, "3 x 12" -> false.
 */
fun isBodyweightScheme(scheme: String): Boolean =
    Regex("""\d+\s*vuelta""").containsMatchIn(scheme.lowercase())

/**
 * Repeticiones objetivo indicadas en el esquema, para usarlas por defecto.
 * Ej: "3 x 12" -> "12", "4 x 8-10" -> "8-10", "3 vueltas" -> "".
 */
fun repsFromScheme(scheme: String): String {
    val m = Regex("""x\s*(\d+\s*-?\s*\d*)""").find(scheme.lowercase()) ?: return ""
    return m.groupValues[1].replace(" ", "").trimEnd('-')
}

/** Lo que sube o baja el contador de repeticiones con un toque. */
const val REPS_STEP = 1

/** "12" -> 12. Null si no hay un número de repeticiones utilizable. */
fun parseReps(raw: String): Int? =
    Regex("""\d+""").find(raw.trim())?.value?.toIntOrNull()?.takeIf { it in 1..999 }

/**
 * Rango de repeticiones que pide el esquema: "3 x 8-12" -> 8..12, "3 x 12" -> 12..12.
 *
 * Null cuando el ejercicio no se cuenta por repeticiones —una plancha va por segundos y un
 * circuito por vueltas—, que es la señal para no preguntarlas siquiera.
 */
fun repsRangeFromScheme(scheme: String): IntRange? {
    if (secondsPerSetFromScheme(scheme) != null) return null
    if (isBodyweightScheme(scheme)) return null
    val texto = repsFromScheme(scheme)
    if (texto.isBlank()) return null
    val partes = texto.split('-').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..999 }
    if (partes.isEmpty()) return null
    return partes.min()..partes.max()
}

/**
 * Repeticiones con las que se llega a una serie por primera vez: el suelo del rango.
 *
 * En un "3 x 8-12" se entra por 8 y se sube desde ahí; dar por hecho el 12 sería apuntarse un
 * resultado que aún no ha pasado. Con un número fijo ("3 x 12") el suelo y el techo coinciden.
 */
fun defaultRepsFromScheme(scheme: String): String =
    repsRangeFromScheme(scheme)?.first?.toString().orEmpty()
