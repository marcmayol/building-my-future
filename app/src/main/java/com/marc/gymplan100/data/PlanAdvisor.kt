package com.marc.gymplan100.data

/**
 * Elige qué plan proponerle a quien abre la app por primera vez.
 *
 * Las respuestas del asistente **no se guardan en ninguna parte**: entran aquí, sale una
 * recomendación y se olvidan. Lo único que queda es el plan que se acabe activando.
 *
 * La recomendación no obliga a nada: al lado siempre están el catálogo entero y el editor.
 * Por eso el criterio es "acertar el 80 % de las veces con cuatro preguntas", no ser exacto.
 */
object PlanAdvisor {

    /** Con cuántos días a la semana cuenta la persona. */
    enum class Days(val n: Int) { TWO(2), THREE(3), FOUR(4), FIVE(5) }

    /** De dónde parte: es lo que evita mandar a alguien parado a un plan de intermedio. */
    enum class Shape {
        /** Parado desde hace tiempo, o volviendo de una lesión ya recuperada. */
        STOPPED,

        /** Entrena de vez en cuando, sin constancia. */
        SOMETIMES,

        /** Entrena con regularidad. */
        REGULAR
    }

    /** Dónde va a entrenar. */
    enum class Place { GYM, HOME }

    data class Answers(
        val goal: PlanGoal,
        val shape: Shape,
        val days: Days,
        val place: Place
    )

    /**
     * Un plan propuesto y **por qué**. El motivo se enseña tal cual: una recomendación sin
     * explicación es un oráculo, y aquí lo que se busca es que la persona pueda decir "no,
     * eso no es lo mío" con conocimiento de causa.
     */
    data class Suggestion(val plan: TrainingPlan, val reason: String)

    data class Result(
        val best: Suggestion?,
        val alternatives: List<Suggestion>,
        /** Bloques que se pueden montar encima del plan elegido, si pegan con el objetivo. */
        val addOns: List<TrainingPlan>
    )

    /**
     * Puntúa cada plan contra las respuestas. Sin puntos suficientes no se propone nada:
     * es mejor mandar al catálogo que recomendar cualquier cosa.
     */
    fun recommend(plans: List<TrainingPlan>, a: Answers): Result {
        val principales = plans.filter { !it.addOn }
        val puntuados = principales
            .map { it to score(it, a) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val sugerencias = puntuados.map { (plan, _) -> Suggestion(plan, reason(plan, a)) }

        return Result(
            best = sugerencias.firstOrNull(),
            alternatives = sugerencias.drop(1).take(2),
            addOns = addOnsFor(plans, a)
        )
    }

    private fun score(plan: TrainingPlan, a: Answers): Int {
        var s = 0

        // Lo que no se puede hacer, no se propone: sin gimnasio, un plan de máquinas sobra.
        if (a.place == Place.HOME && plan.equipment == PlanEquipment.GYM) return 0

        // Quien lleva tiempo parado empieza por el principio, diga lo que diga su objetivo.
        if (a.shape == Shape.STOPPED) {
            if (plan.level == PlanLevel.INTERMEDIATE) return 0
            if (plan.goal == PlanGoal.START) s += 10
        }
        if (a.shape == Shape.REGULAR && plan.goal == PlanGoal.START) return 0

        if (plan.goal == a.goal) s += 6
        // Perder grasa y ganar músculo se solapan bastante en la práctica.
        if (parientes(plan.goal, a.goal)) s += 3

        s += when (plan.level) {
            PlanLevel.ANY -> 2
            PlanLevel.ZERO -> if (a.shape == Shape.STOPPED) 3 else 0
            PlanLevel.BEGINNER -> if (a.shape != Shape.REGULAR) 3 else 1
            PlanLevel.INTERMEDIATE -> if (a.shape == Shape.REGULAR) 3 else 0
        }

        // Los días son un límite real, no una preferencia: un plan de 5 días con 3 disponibles
        // no se sostiene, así que ni se ofrece como alternativa.
        val diff = plan.daysPerWeek - a.days.n
        when {
            plan.daysPerWeek == 0 -> Unit
            diff <= 0 -> s += 2     // cabe de sobra
            diff == 1 -> Unit       // un día de más: se puede estirar la semana
            else -> return 0        // dos o más: no cuadra
        }

        return s.coerceAtLeast(0)
    }

    private fun parientes(a: PlanGoal?, b: PlanGoal): Boolean {
        if (a == null) return false
        val pares = setOf(
            setOf(PlanGoal.LOSE_FAT, PlanGoal.MUSCLE),
            setOf(PlanGoal.MUSCLE, PlanGoal.STRENGTH),
            setOf(PlanGoal.MAINTAIN, PlanGoal.MUSCLE),
        )
        return setOf(a, b) in pares
    }

    /** Bloques extra que pegan con lo que ha pedido, para ofrecerlos encima del plan. */
    private fun addOnsFor(plans: List<TrainingPlan>, a: Answers): List<TrainingPlan> {
        val extras = plans.filter { it.addOn }
        return when {
            a.goal == PlanGoal.MOBILITY -> extras.filter { it.goal == PlanGoal.MOBILITY }
            a.shape == Shape.STOPPED -> extras.filter { it.goal == PlanGoal.MOBILITY }
            a.days == Days.TWO -> emptyList()   // si va justo de días, no se le carga más
            else -> extras
        }
    }

    /** El porqué, en una frase y en cristiano. */
    private fun reason(plan: TrainingPlan, a: Answers): String {
        val trozos = mutableListOf<String>()
        trozos += when (a.shape) {
            Shape.STOPPED -> "vienes de estar parado"
            Shape.SOMETIMES -> "entrenas de vez en cuando"
            Shape.REGULAR -> "ya entrenas con constancia"
        }
        trozos += "${a.days.n} días por semana"
        if (a.place == Place.HOME) trozos += "sin material"

        val cola = when (plan.goal) {
            PlanGoal.START -> "esto es para coger el hábito sin machacarte"
            PlanGoal.LOSE_FAT -> "aquí se mueve mucho y se suda"
            PlanGoal.MUSCLE -> "el volumen está repartido para crecer"
            PlanGoal.STRENGTH -> "pocas repeticiones y peso de verdad"
            PlanGoal.MAINTAIN -> "lo justo para no perder lo ganado"
            PlanGoal.MOBILITY -> "sesiones cortas para recuperar rango"
            null -> "encaja con lo que has pedido"
        }
        return trozos.joinToString(", ") + ": " + cola + "."
    }
}
