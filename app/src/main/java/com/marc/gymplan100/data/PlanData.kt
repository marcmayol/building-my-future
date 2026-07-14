package com.marc.gymplan100.data

/** Contenido completo del plan de 100 días y construcción de los días numerados. */
object PlanData {

    private val phase1 = Phase(
        number = 1,
        name = "Adaptación",
        range = "Días 1 al 20",
        weeks = "Semanas 1-4",
        weeksCount = 4,
        description = "Aprender los movimientos con máquinas guiadas y volumen moderado. " +
            "Split tren superior / tren inferior.",
        progression = "Semana 1: conoce las máquinas y ajusta pesos cómodos. " +
            "De la semana 2 a la 4, sube algo el peso cuando 12 repeticiones se queden fáciles.",
        templates = listOf(
            WorkoutTemplate(
                weekday = "Lunes",
                title = "Tren superior A",
                warmup = "5-8 min bici suave + movilidad de hombros",
                exercises = listOf(
                    Exercise("Press de pecho en máquina", "3 x 12"),
                    Exercise("Jalón al pecho (polea)", "3 x 12"),
                    Exercise("Remo sentado en máquina", "3 x 12"),
                    Exercise("Press de hombros en máquina", "2 x 12"),
                    Exercise("Curl de bíceps con mancuernas", "2 x 12"),
                    Exercise("Extensión de tríceps en polea", "2 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Martes",
                title = "Tren inferior A",
                warmup = "5-8 min bici + movilidad de cadera",
                exercises = listOf(
                    Exercise("Prensa de piernas", "3 x 12"),
                    Exercise("Curl de piernas (máquina)", "3 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Hip thrust o puente de glúteos", "2 x 12"),
                    Exercise("Elevación de gemelos", "3 x 15"),
                    Exercise("Plancha", "3 x 20-30 s"),
                )
            ),
            WorkoutTemplate(
                weekday = "Miércoles",
                title = "Tren superior B",
                warmup = "5-8 min + movilidad de hombros",
                exercises = listOf(
                    Exercise("Pec deck (aperturas en máquina)", "3 x 12"),
                    Exercise("Remo en máquina", "3 x 12"),
                    Exercise("Jalón al pecho agarre neutro", "3 x 12"),
                    Exercise("Press de hombros en máquina", "2 x 12"),
                    Exercise("Elevaciones laterales", "2 x 12"),
                    Exercise("Superserie curl + extensión de tríceps", "2 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Jueves",
                title = "Tren inferior B",
                warmup = "5-8 min + movilidad de cadera",
                exercises = listOf(
                    Exercise("Prensa de piernas", "3 x 12"),
                    Exercise("Sentadilla a banco/cajón", "2 x 10"),
                    Exercise("Curl de piernas", "3 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Elevación de gemelos", "3 x 15"),
                    Exercise("Plancha lateral", "2 x 20 s"),
                    Exercise("Dead bug", "2 x 10"),
                )
            ),
            WorkoutTemplate(
                weekday = "Viernes",
                title = "Cuerpo completo (técnica)",
                warmup = "5-8 min bici suave",
                exercises = listOf(
                    Exercise("Prensa de piernas", "2 x 12"),
                    Exercise("Press de pecho en máquina", "2 x 12"),
                    Exercise("Jalón al pecho", "2 x 12"),
                    Exercise("Press de hombros", "2 x 12"),
                    Exercise("Curl de piernas", "2 x 12"),
                    Exercise("Plancha", "3 x 30 s"),
                )
            ),
        )
    )

    private val phase2 = Phase(
        number = 2,
        name = "Construcción",
        range = "Días 21 al 50",
        weeks = "Semanas 5-10",
        weeksCount = 6,
        description = "Mismo split superior/inferior, subir a 3-4 series e introducir algún peso libre básico.",
        progression = "Sube peso cuando completes todas las series con técnica limpia. " +
            "Deja un par de repeticiones en reserva en cada serie.",
        templates = listOf(
            WorkoutTemplate(
                weekday = "Lunes",
                title = "Tren superior A",
                warmup = "6 min + movilidad de hombros",
                exercises = listOf(
                    Exercise("Press de pecho en máquina", "4 x 10"),
                    Exercise("Jalón al pecho", "4 x 10"),
                    Exercise("Remo sentado", "3 x 12"),
                    Exercise("Press de hombros", "3 x 10"),
                    Exercise("Curl de bíceps con mancuernas", "3 x 12"),
                    Exercise("Extensión de tríceps en polea", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Martes",
                title = "Tren inferior A",
                warmup = "6 min + movilidad de cadera",
                exercises = listOf(
                    Exercise("Prensa de piernas", "4 x 10"),
                    Exercise("Curl de piernas", "3 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Hip thrust", "3 x 12"),
                    Exercise("Elevación de gemelos", "4 x 15"),
                    Exercise("Plancha", "3 x 40 s"),
                )
            ),
            WorkoutTemplate(
                weekday = "Miércoles",
                title = "Tren superior B",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Press inclinado o pec deck", "4 x 10"),
                    Exercise("Remo en máquina", "4 x 10"),
                    Exercise("Jalón al pecho agarre neutro", "3 x 12"),
                    Exercise("Elevaciones laterales", "3 x 12"),
                    Exercise("Face pull en polea", "3 x 15"),
                    Exercise("Superserie curl + extensión", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Jueves",
                title = "Tren inferior B",
                warmup = "6 min + movilidad de cadera",
                exercises = listOf(
                    Exercise("Sentadilla goblet con mancuerna", "3 x 10"),
                    Exercise("Prensa de piernas", "3 x 12"),
                    Exercise("Curl de piernas", "3 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Elevación de gemelos", "4 x 15"),
                    Exercise("Plancha lateral", "3 x 25 s"),
                    Exercise("Dead bug", "3 x 10"),
                )
            ),
            WorkoutTemplate(
                weekday = "Viernes",
                title = "Cuerpo completo + core",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Prensa de piernas", "3 x 12"),
                    Exercise("Press de pecho", "3 x 12"),
                    Exercise("Remo sentado", "3 x 12"),
                    Exercise("Press de hombros", "3 x 10"),
                    Exercise("Circuito core (plancha, rodillas, plancha lateral)", "3 vueltas"),
                )
            ),
        )
    )

    private val phase3 = Phase(
        number = 3,
        name = "Progresión",
        range = "Días 51 al 80",
        weeks = "Semanas 11-16",
        weeksCount = 6,
        description = "Rutina empuje / tirón / piernas / superior / inferior. Más carga y ejercicios algo más exigentes.",
        progression = "Prioriza subir carga en los básicos (prensa, press de pecho, jalón, remo). La técnica siempre primero.",
        templates = listOf(
            WorkoutTemplate(
                weekday = "Lunes",
                title = "Empuje (pecho, hombro, tríceps)",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Press de pecho en máquina", "4 x 8-10"),
                    Exercise("Press inclinado o pec deck", "3 x 12"),
                    Exercise("Press de hombros", "3 x 10"),
                    Exercise("Elevaciones laterales", "3 x 12"),
                    Exercise("Fondos en máquina o press francés", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Martes",
                title = "Tirón (espalda, bíceps)",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Jalón al pecho", "4 x 10"),
                    Exercise("Remo sentado", "4 x 10"),
                    Exercise("Remo en máquina o con mancuerna", "3 x 12"),
                    Exercise("Face pull", "3 x 15"),
                    Exercise("Curl de bíceps", "3 x 12"),
                    Exercise("Curl martillo", "2 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Miércoles",
                title = "Piernas",
                warmup = "6 min + movilidad de cadera",
                exercises = listOf(
                    Exercise("Prensa de piernas", "4 x 12"),
                    Exercise("Sentadilla goblet o en máquina", "3 x 10"),
                    Exercise("Curl de piernas", "3 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Hip thrust", "3 x 12"),
                    Exercise("Elevación de gemelos", "4 x 15"),
                )
            ),
            WorkoutTemplate(
                weekday = "Jueves",
                title = "Tren superior completo",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Press de pecho", "3 x 10"),
                    Exercise("Remo sentado", "3 x 10"),
                    Exercise("Press de hombros", "3 x 10"),
                    Exercise("Jalón al pecho", "3 x 12"),
                    Exercise("Superserie curl + extensión", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Viernes",
                title = "Tren inferior + core",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Prensa de piernas", "4 x 10"),
                    Exercise("Curl de piernas", "4 x 12"),
                    Exercise("Extensión de piernas", "3 x 12"),
                    Exercise("Hip thrust", "3 x 12"),
                    Exercise("Elevación de gemelos", "4 x 15"),
                    Exercise("Circuito core (plancha, rueda, elevación piernas)", "3 vueltas"),
                )
            ),
        )
    )

    private val phase4 = Phase(
        number = 4,
        name = "Consolidación",
        range = "Días 81 al 100",
        weeks = "Semanas 17-20",
        weeksCount = 4,
        description = "Consolidar fuerza con intensidad alta y algún finisher. " +
            "Mismo split empuje / tirón / piernas / superior / inferior.",
        progression = "Carga alta con buena técnica. Si te sobra energía añade el finisher; " +
            "si vienes cansado, prioriza completar las series principales.",
        templates = listOf(
            WorkoutTemplate(
                weekday = "Lunes",
                title = "Empuje",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Press de pecho en máquina", "4 x 8"),
                    Exercise("Press inclinado", "4 x 10"),
                    Exercise("Press de hombros", "4 x 10"),
                    Exercise("Elevaciones laterales", "3 x 15"),
                    Exercise("Extensión de tríceps en polea", "3 x 12"),
                    Exercise("Finisher: press de pecho ligero al fallo", "1 serie"),
                )
            ),
            WorkoutTemplate(
                weekday = "Martes",
                title = "Tirón",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Jalón al pecho", "4 x 8"),
                    Exercise("Remo sentado", "4 x 10"),
                    Exercise("Remo en máquina", "3 x 12"),
                    Exercise("Face pull", "3 x 15"),
                    Exercise("Curl de bíceps + curl martillo", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Miércoles",
                title = "Piernas",
                warmup = "6 min + movilidad de cadera",
                exercises = listOf(
                    Exercise("Prensa de piernas", "5 x 10"),
                    Exercise("Sentadilla goblet", "3 x 10"),
                    Exercise("Curl de piernas", "4 x 12"),
                    Exercise("Extensión de piernas", "3 x 15"),
                    Exercise("Hip thrust", "4 x 12"),
                    Exercise("Elevación de gemelos", "4 x 20"),
                )
            ),
            WorkoutTemplate(
                weekday = "Jueves",
                title = "Tren superior completo",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Press de pecho", "4 x 8"),
                    Exercise("Remo sentado", "4 x 8"),
                    Exercise("Press de hombros", "3 x 10"),
                    Exercise("Jalón al pecho", "3 x 10"),
                    Exercise("Superserie curl + extensión", "3 x 12"),
                )
            ),
            WorkoutTemplate(
                weekday = "Viernes",
                title = "Tren inferior + core",
                warmup = "6 min de calentamiento",
                exercises = listOf(
                    Exercise("Prensa de piernas", "4 x 10"),
                    Exercise("Curl de piernas", "4 x 12"),
                    Exercise("Extensión de piernas", "4 x 12"),
                    Exercise("Hip thrust", "4 x 12"),
                    Exercise("Elevación de gemelos", "4 x 20"),
                    Exercise("Circuito core (plancha, plancha lateral, dead bug)", "4 vueltas"),
                )
            ),
        )
    )

    val phases: List<Phase> = listOf(phase1, phase2, phase3, phase4)

    /** Los 100 días numerados, generados repitiendo las plantillas por cada semana de su fase. */
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

    const val TOTAL_DAYS = 100

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
        return if (index >= 0) index / day.phase.templates.size + 1 else 1
    }
}
