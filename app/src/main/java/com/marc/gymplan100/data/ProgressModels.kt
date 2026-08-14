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
     * Cuándo se completó el último día del plan (0 = sin terminar). Lo que dispara la
     * pantalla de cierre; se pone a 0 al empezar otra vuelta.
     */
    val finishedAt: Long = 0L,
    /**
     * Vueltas ya terminadas. Mantenimiento y Movilidad están pensados para darlas: al
     * empezar otra, los días se vacían pero esto sube y el historial de entrenos se queda.
     */
    val rounds: Int = 0
) {
    fun completedInPhase(phaseNumber: Int): Int =
        PlanData.daysOfPhase(phaseNumber).count { it.number in completedDays }

    /** El plan está acabado y todavía no se ha decidido qué hacer después. */
    val isFinished: Boolean get() = finishedAt > 0L
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
