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
    val exerciseWeights: Map<String, String> = emptyMap()
) {
    fun completedInPhase(phaseNumber: Int): Int =
        PlanData.daysOfPhase(phaseNumber).count { it.number in completedDays }
}

/**
 * Datos personales del usuario. Se usan para estimar las calorías activas de cada
 * entrenamiento (que es lo que el coach de Google Health necesita para contarlo) y
 * pueden rellenarse manualmente con las ruedas de Configuración o importarse desde
 * Google Health. Un valor a 0 / vacío significa "sin definir".
 */
@Serializable
data class UserProfile(
    val weightKg: Int = 0,
    val heightCm: Int = 0,
    val gender: String = "",
    // El usuario lleva reloj/pulsómetro (Wear OS): Google Health ya obtiene calorías reales
    // de su pulso, así que NO escribimos la estimación MET para no contar el doble.
    val usesWatch: Boolean = false
) {
    val isWeightSet: Boolean get() = weightKg > 0
    val isHeightSet: Boolean get() = heightCm > 0
}
