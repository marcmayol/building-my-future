package com.marc.gymplan100.data

import kotlinx.serialization.Serializable

/** Registro del usuario para un ejercicio concreto de un día. */
@Serializable
data class ExerciseLog(
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false
)

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
