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
    val reps: String = ""
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
    val useAlternative: Boolean = false
) {
    /** Es una rutina especial (militar o quema grasa), no una sesión del plan de 100 días. */
    val isRoutine: Boolean get() = routineId != null
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

/** Objetivos de descanso por defecto (segundos). */
object RestDefaults {
    const val BETWEEN_SETS = 90
    const val BETWEEN_EXERCISES = 120
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
