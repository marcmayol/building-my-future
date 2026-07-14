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
    val restSeconds: Int = 0
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
    val timedPaused: Boolean = false
) {
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
    val extra: Boolean = false
) {
    val durationSeconds: Int get() = ((endMillis - startMillis) / 1000).toInt().coerceAtLeast(0)
}

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
 * Repeticiones objetivo indicadas en el esquema, para usarlas por defecto.
 * Ej: "3 x 12" -> "12", "4 x 8-10" -> "8-10", "3 vueltas" -> "".
 */
fun repsFromScheme(scheme: String): String {
    val m = Regex("""x\s*(\d+\s*-?\s*\d*)""").find(scheme.lowercase()) ?: return ""
    return m.groupValues[1].replace(" ", "").trimEnd('-')
}
