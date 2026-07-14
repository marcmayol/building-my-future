package com.marc.gymplan100.data

/** Un ejercicio dentro de una sesión. */
data class Exercise(
    val name: String,
    val scheme: String,
    val note: String = ""
)

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

/** Un día concreto numerado dentro del programa de 100 días. */
data class TrainingDay(
    val number: Int,
    val phase: Phase,
    val template: WorkoutTemplate
)
