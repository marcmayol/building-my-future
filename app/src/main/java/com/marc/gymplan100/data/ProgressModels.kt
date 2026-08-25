package com.marc.gymplan100.data

import kotlinx.serialization.Serializable

/**
 * Una serie tal y como quedó apuntada: los kilos que se movieron y, si se anotaron, las
 * repeticiones que se hicieron de verdad.
 *
 * Existe porque las tres series de un ejercicio casi nunca son iguales —la primera con 10, la
 * segunda con 12 y la tercera con 11 es lo normal—, y hasta la v2.3 la app se quedaba solo con
 * la última y tiraba las otras dos al cerrar el día.
 */
@Serializable
data class SetLog(
    val weight: String = "",
    val reps: String = ""
) {
    val isEmpty: Boolean get() = weight.isBlank() && reps.isBlank()
}

/** Registro del usuario para un ejercicio concreto de un día. */
@Serializable
data class ExerciseLog(
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false,
    /**
     * Desglose serie a serie. Vacío en los días de siempre (y en los que se apuntan de un
     * plumazo): entonces [weight] y [reps] son todo lo que hay, y se leen igual que antes.
     *
     * [weight] no desaparece por esto: sigue siendo el peso de referencia del ejercicio ese
     * día —el MÁS ALTO de sus series—, que es lo que mira "Mis pesos" y lo que se sugiere la
     * próxima vez.
     */
    val sets: List<SetLog> = emptyList()
) {
    /** Series con algo apuntado; las de relleno (vacías del todo) no cuentan. */
    val filledSets: List<SetLog> get() = sets.filter { !it.isEmpty }
}

/**
 * El desglose de pesos en una línea: "10 · 12 · 11 kg" cuando cada serie llevó lo suyo, y
 * "12 kg ×3" cuando las tres fueron iguales (repetir el mismo número tres veces no dice nada).
 *
 * Una serie sin peso en medio se marca con un guion ("10 · — · 11 kg") para no desplazar a las
 * demás; las que se quedan sin apuntar al final se recortan. Devuelve "" si no hay ni un peso.
 */
fun weightsSummary(sets: List<SetLog>): String {
    val weights = sets.map { it.weight.trim() }.dropLastWhile { it.isBlank() }
    if (weights.none { it.isNotBlank() }) return ""
    val distinct = weights.filter { it.isNotBlank() }.distinct()
    if (distinct.size == 1 && weights.size > 1 && weights.all { it.isNotBlank() }) {
        return "${distinct.first()} kg ×${weights.size}"
    }
    return weights.joinToString(" · ") { it.ifBlank { "—" } } + " kg"
}

/**
 * Las series con kilos Y repeticiones, para lo que se apunta en el entreno libre:
 * "10 kg × 12 · 12 kg × 12 · 11 kg × 10". Si ninguna trae repeticiones, se lee como el
 * desglose de pesos de siempre.
 */
fun setsSummary(sets: List<SetLog>): String {
    val llenas = sets.filter { !it.isEmpty }
    if (llenas.isEmpty()) return ""
    if (llenas.none { it.reps.isNotBlank() }) return weightsSummary(llenas)
    return llenas.joinToString(" · ") { serie ->
        val kg = serie.weight.trim()
        val reps = serie.reps.trim()
        when {
            kg.isNotBlank() && reps.isNotBlank() -> "$kg kg × $reps"
            kg.isNotBlank() -> "$kg kg"
            else -> "× $reps"
        }
    }
}

/** Estado completo del progreso, persistido como JSON. */
@Serializable
data class ProgressState(
    val completedDays: Set<Int> = emptySet(),
    // Clave: "<numeroDia>-<indiceEjercicio>"
    val logs: Map<String, ExerciseLog> = emptyMap(),
    // El usuario ya cerró el aviso de optimización de batería.
    val batteryHintDismissed: Boolean = false,
    // Último peso usado por ejercicio (clave: nombre del ejercicio).
    val exerciseWeights: Map<String, String> = emptyMap(),
    // En Resultados, el día más reciente arriba (con 30+ días completados, bajar hasta
    // el último era un scroll interminable).
    val resultsNewestFirst: Boolean = true,
    /**
     * Cuándo se empezó este plan (0 = no se sabe, planes de antes de guardarlo).
     *
     * Se guarda al activarlo y, por si acaso, al marcar el primer día. Antes se deducía del
     * primer entreno del historial, pero eso solo funciona si se usa el entrenamiento guiado:
     * quien marca los días a mano se quedaba sin fecha.
     */
    val startedAt: Long = 0L,
    /**
     * Cuándo se terminó el plan por última vez (0 = nunca).
     *
     * La fecha **no se borra**: ni al cerrar el aviso ni al empezar otra vuelta. La portada y
     * la lista dicen "Terminado el 14 de agosto", y esa frase no puede depender de si se cerró
     * un aviso ni desaparecer por seguir entrenando. Lo que decide si el cierre sale solo es
     * [finishedSeen], no esto.
     */
    val finishedAt: Long = 0L,
    /** El cierre del plan ya se ha enseñado: no vuelve a salir solo. */
    val finishedSeen: Boolean = false,
    /**
     * Vueltas ya terminadas. Mantenimiento y Movilidad están pensados para darlas: al
     * empezar otra, los días se vacían pero esto sube y el historial de entrenos se queda.
     */
    val rounds: Int = 0
) {
    fun completedInPhase(phaseNumber: Int): Int =
        PlanData.daysOfPhase(phaseNumber).count { it.number in completedDays }

    /** El plan está acabado y todavía no se ha decidido qué hacer después. */
    val isFinished: Boolean get() = finishedAt > 0L && !finishedSeen

    /** Acabado alguna vez, se haya visto el cierre o no. */
    val everFinished: Boolean get() = finishedAt > 0L

    /**
     * Cierra un día del entrenamiento guiado: apunta en cada ejercicio el peso con el que se
     * quedó y lo deja también como peso de referencia en "Mis pesos".
     *
     * Los pesos se rehacen aquí, y no solo serie a serie, porque las series marcadas desde el
     * reloj no pasan por el ViewModel: al cerrar el día es donde se sabe de verdad qué se
     * levantó. Un ejercicio sin ninguna serie con peso (una plancha) no toca nada.
     *
     * Aquí queda también el desglose entero (10 · 12 · 11), que antes se perdía: de las tres
     * series solo sobrevivía la última, y con ella la idea falsa de que el ejercicio se hizo
     * con un único peso.
     */
    fun withFinishedDay(dayNumber: Int, completedSets: List<CompletedSet>): ProgressState {
        val nuevosLogs = logs.toMutableMap()
        val nuevosPesos = exerciseWeights.toMutableMap()
        completedSets.groupBy { it.exerciseIndex }.forEach { (idx, seriesDelEjercicio) ->
            // El peso del ejercicio es el MÁS ALTO de sus series, no el de la última: si hoy
            // fueron 10, 12 y 11, lo que levantas son 12. El detalle vive en `sets`.
            val pesoTope = heaviestWeight(seriesDelEjercicio.map { it.weight })
                .ifBlank { null }
            val clave = "$dayNumber-$idx"
            val previo = nuevosLogs[clave] ?: ExerciseLog()
            // En el orden en que se hicieron, que es el que se lee luego en el resumen.
            val desglose = seriesDelEjercicio
                .sortedBy { it.setNumber }
                .map { SetLog(weight = it.weight.trim(), reps = it.reps.trim()) }
            nuevosLogs[clave] = previo.copy(
                weight = pesoTope ?: previo.weight,
                done = true,
                sets = if (desglose.any { !it.isEmpty }) desglose else previo.sets
            )
            val nombre = PlanData.dayByNumber(dayNumber)?.template?.exercises?.getOrNull(idx)?.name
            if (pesoTope != null && !nombre.isNullOrBlank()) nuevosPesos[nombre] = pesoTope
        }
        return copy(
            completedDays = completedDays + dayNumber,
            logs = nuevosLogs,
            exerciseWeights = nuevosPesos
        )
    }

    /**
     * Otra vuelta al plan entero: los días vuelven a cero y la vuelta queda contada.
     *
     * Lo que **no** se toca: el historial de entrenos (vive aparte) ni los pesos por ejercicio,
     * que son lo que has aprendido a levantar y no se pierde por repetir el plan.
     */
    fun restartedFromScratch(now: Long): ProgressState = copy(
        completedDays = emptySet(),
        logs = emptyMap(),
        startedAt = now,
        finishedSeen = true,
        rounds = rounds + 1
    )

    /**
     * Repetir solo unos días concretos (la última fase). El resto del progreso se queda: es
     * la diferencia entre volver al día 1 de 100 y repetir los 20 últimos.
     */
    fun restartedDays(days: Set<Int>, now: Long): ProgressState = copy(
        completedDays = completedDays - days,
        logs = logs.filterKeys { it.substringBefore("-").toIntOrNull() !in days },
        startedAt = now,
        finishedSeen = true,
        rounds = rounds + 1
    )
}

/**
 * Datos personales del usuario, y cada uno con su porqué:
 *  - [weightKg] estima las calorías activas del entreno (fórmula MET) para Google Health.
 *  - [gender] elige si las ilustraciones de los ejercicios son masculinas o femeninas.
 *  - [usesWatch] decide si estimamos calorías o dejamos que las ponga el pulso del reloj.
 *
 * Hubo también una altura: se pedía "para las calorías" pero no entraba en ningún cálculo,
 * así que se quitó en la v2.2. El perfil guardado que aún la traiga se lee igual, porque el
 * `Json` del repositorio ignora las claves desconocidas.
 *
 * Un valor a 0 / vacío significa "sin definir".
 */
@Serializable
data class UserProfile(
    val weightKg: Int = 0,
    val gender: String = "",
    // El usuario lleva reloj/pulsómetro (Wear OS): Google Health ya obtiene calorías reales
    // de su pulso, así que NO escribimos la estimación MET para no contar el doble.
    val usesWatch: Boolean = false
) {
    val isWeightSet: Boolean get() = weightKg > 0
}
