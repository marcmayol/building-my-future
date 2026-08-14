package com.marc.gymplan100.data

/**
 * Para qué entrena la gente. Es la primera pregunta del asistente y lo que más estrecha el
 * catálogo: quien quiere levantar más y quien quiere moverse mejor no comparten plan.
 */
enum class PlanGoal { LOSE_FAT, MUSCLE, STRENGTH, MAINTAIN, MOBILITY, START }

/** Punto de partida de quien lo va a hacer. `ANY` = le vale a cualquiera. */
enum class PlanLevel { ZERO, BEGINNER, INTERMEDIATE, ANY }

/** Qué hace falta para poder hacerlo. */
enum class PlanEquipment {
    /** Máquinas y peso libre. */
    GYM,

    /** Una barra de dominadas y suelo. */
    BAR,

    /** Nada: el propio cuerpo, una pared, una toalla. */
    NONE
}

/** De dónde salió un plan. Se enseña en la lista de planes y decide si se puede borrar. */
enum class PlanSource {
    /** Viene con la app (el reto de 100 días). */
    BUILTIN,
    JSON,
    MARKDOWN,
    /** Creado o modificado dentro de la app. */
    EDITOR
}

/**
 * Un plan de entrenamiento completo: las fases de las que salen los días numerados.
 *
 * Hasta la v1.8 solo existía el de 100 días y vivía como datos fijos dentro de `PlanData`. Al
 * poder importar planes propios, el plan pasa a ser un valor corriente: [BuiltinPlan] es uno
 * más y [PlanData] mira al que esté activo.
 *
 * La duración es libre: los días salen de repetir las plantillas de cada fase tantas semanas
 * como diga la fase, así que el total es "plantillas x semanas" sumado, no necesariamente 100.
 */
data class TrainingPlan(
    val id: String,
    val name: String,
    val description: String = "",
    val phases: List<Phase>,
    /** Viene con la app: no se puede borrar. */
    val builtin: Boolean = false,
    val source: PlanSource = if (builtin) PlanSource.BUILTIN else PlanSource.JSON,
    /** Cuándo se importó, para ordenar la lista de planes. 0 en el integrado. */
    val importedAt: Long = 0L,
    /** Para qué sirve, para poder recomendarlo. Vacío en un plan propio que no lo diga. */
    val goal: PlanGoal? = null,
    val level: PlanLevel = PlanLevel.ANY,
    val daysPerWeek: Int = 0,
    val equipment: PlanEquipment = PlanEquipment.GYM,
    /** Se monta sobre otro plan en vez de sustituirlo (los bloques de 2-3 días extra). */
    val addOn: Boolean = false
) {
    /** Los días numerados, repitiendo las plantillas de cada fase por cada una de sus semanas. */
    val days: List<TrainingDay> by lazy {
        val result = mutableListOf<TrainingDay>()
        var n = 1
        for (phase in phases) {
            repeat(phase.weeksCount) {
                for (template in phase.templates) {
                    result.add(TrainingDay(n, phase, template))
                    n++
                }
            }
        }
        result
    }

    /** Días que dura el plan. */
    val totalDays: Int get() = days.size

    /** Nombres de ejercicio únicos de todo el plan, ordenados alfabéticamente. */
    val exerciseNames: List<String> by lazy {
        days.flatMap { it.template.exercises }.map { it.name }.distinct().sortedBy { it.lowercase() }
    }

    fun dayByNumber(n: Int): TrainingDay? = days.getOrNull(n - 1)

    fun daysOfPhase(phaseNumber: Int): List<TrainingDay> =
        days.filter { it.phase.number == phaseNumber }

    fun weekWithinPhase(day: TrainingDay): Int {
        val phaseDays = daysOfPhase(day.phase.number)
        val index = phaseDays.indexOfFirst { it.number == day.number }
        val perWeek = day.phase.templates.size
        return if (index >= 0 && perWeek > 0) index / perWeek + 1 else 1
    }
}
