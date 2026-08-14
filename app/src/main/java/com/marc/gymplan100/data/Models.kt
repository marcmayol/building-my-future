package com.marc.gymplan100.data

/**
 * Qué se registra de un ejercicio, que es lo que decide qué campos pide la sesión.
 *
 * Hasta ahora se adivinaba leyendo el esquema ("3 x 30 s" es por tiempo, "3 vueltas" es sin
 * peso), y con los planes nuevos eso ya no basta: una dominada y un press de banca se escriben
 * igual ("3 x 8-12") y solo en uno tiene sentido preguntar kilos.
 */
enum class ExerciseKind {
    /** Peso y repeticiones: el gimnasio de siempre. */
    STRENGTH,

    /** El peso es tu cuerpo: repeticiones sí, kilos no. */
    BODYWEIGHT,

    /** Segundos: planchas, holds, colgarse, estiramientos. */
    TIME,

    /** Minutos en una máquina o corriendo: ni series ni kilos. */
    CARDIO
}

/** Un ejercicio dentro de una sesión. */
data class Exercise(
    val name: String,
    val scheme: String,
    val note: String = "",
    val kind: ExerciseKind = ExerciseKind.STRENGTH
) {
    /**
     * No tiene sentido pedirle kilos: es tu cuerpo, es un estiramiento o una plancha, o son
     * minutos en una máquina. Solo los ejercicios de fuerza llevan carga.
     */
    val withoutWeight: Boolean
        get() = kind != ExerciseKind.STRENGTH || isBodyweightScheme(scheme)
}

/** Plantilla de un día de entrenamiento (lunes a viernes). */
data class WorkoutTemplate(
    val weekday: String,
    val title: String,
    val warmup: String,
    val exercises: List<Exercise>,
    val cooldown: String = "Estiramientos: 5 min"
)

/** Una fase del plan. */
data class Phase(
    val number: Int,
    val name: String,
    val range: String,
    val weeks: String,
    val weeksCount: Int,
    val description: String,
    val progression: String,
    val templates: List<WorkoutTemplate>
)

/** Un día concreto numerado dentro del plan. */
data class TrainingDay(
    val number: Int,
    val phase: Phase,
    val template: WorkoutTemplate
)
