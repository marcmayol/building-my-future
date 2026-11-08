package com.marc.gymplan100.data

/**
 * Un logro del reto. No lleva texto: el título y la descripción los pone la interfaz a partir
 * del [id] (ver ui/AchievementsScreen.kt), en el idioma que toque.
 */
data class Achievement(
    val id: String,
    val emoji: String,
    /** Día del plan en el que cae. Sirve para decir cuánto falta para el siguiente. */
    val atDay: Int = 0,
    /** Logros de fase: número y nombre de la fase (el nombre es contenido del plan). */
    val phaseNumber: Int = 0,
    val phaseName: String = ""
)

/**
 * Logros del reto y lógica de desbloqueo (derivada del progreso, sin estado extra).
 *
 * Las reglas se calculan sobre el plan ACTIVO, no sobre un 100 fijo: los cuartos son cuartos de
 * su duración y hay un logro por cada fase que tenga. En el plan de 100 días eso da exactamente
 * los mismos hitos de siempre (25, 50, 75 y 100 días, y las cuatro fases con su nombre).
 */
object Achievements {

    private data class Rule(
        val achievement: Achievement,
        /** Día del plan en el que se consigue; solo para ordenar la lista de menor a mayor. */
        val atDay: Int,
        val unlocked: (ProgressState) -> Boolean
    )

    /** Emojis de las fases del plan de la app; los planes con más fases repiten el último. */
    private val PHASE_EMOJIS = listOf("🛡️", "🏗️", "📈", "🏆")

    private fun byDays(id: String, emoji: String, days: Int) =
        Rule(Achievement(id, emoji, atDay = days), days) { state ->
            state.completedDays.count { it in 1..PlanData.TOTAL_DAYS } >= days
        }

    private fun byPhase(phase: Phase, lastDay: Int): Rule {
        val emoji = PHASE_EMOJIS.getOrElse(phase.number - 1) { PHASE_EMOJIS.last() }
        return Rule(
            Achievement(
                id = "phase${phase.number}",
                emoji = emoji,
                atDay = lastDay,
                phaseNumber = phase.number,
                phaseName = phase.name
            ),
            atDay = lastDay
        ) { state ->
            val total = PlanData.daysOfPhase(phase.number).size
            total > 0 && state.completedInPhase(phase.number) >= total
        }
    }

    /** Días necesarios para un porcentaje del plan (al menos 1, nunca más que el total). */
    private fun atPercent(percent: Int): Int {
        val total = PlanData.TOTAL_DAYS
        return (total * percent / 100).coerceIn(1, total.coerceAtLeast(1))
    }

    private fun rules(): List<Rule> {
        val total = PlanData.TOTAL_DAYS
        val quarter = atPercent(25)
        val half = atPercent(50)
        val threeQuarters = atPercent(75)

        val milestones = buildList {
            add(byDays("first", "🥇", 1))
            if (total >= 5) add(byDays("week", "📅", 5))
            if (total >= 10) add(byDays("ten", "🔥", 10))
            if (quarter > 1) add(byDays("quarter", "⚡", quarter))
            if (half > quarter) add(byDays("half", "🏔️", half))
            if (threeQuarters > half) add(byDays("threequarter", "🚀", threeQuarters))
            if (total > 1) add(byDays("complete", "👑", total))
        }

        val phaseRules = mutableListOf<Rule>()
        var lastDay = 0
        for (phase in PlanData.phases) {
            lastDay += PlanData.daysOfPhase(phase.number).size
            phaseRules.add(byPhase(phase, lastDay))
        }

        // Ordenados por dificultad, con los de fase intercalados donde toca. Cuando una fase
        // termina justo en un hito (en el plan de la app pasa en el 50, el 75 y el 100), va
        // primero la fase: es el orden que la lista de logros ha tenido siempre.
        return (milestones + phaseRules).sortedWith(
            compareBy({ it.atDay }, { if (it.achievement.id.startsWith("phase")) 0 else 1 })
        )
    }

    /** Logros del plan activo, del más fácil al más difícil. */
    val all: List<Achievement> get() = rules().map { it.achievement }

    fun unlockedIds(state: ProgressState): Set<String> =
        rules().filter { it.unlocked(state) }.map { it.achievement.id }.toSet()

    /** Logros que están desbloqueados en [after] pero no lo estaban en [before]. */
    fun newlyUnlocked(before: ProgressState, after: ProgressState): List<Achievement> {
        val prev = unlockedIds(before)
        return rules().filter { it.unlocked(after) && it.achievement.id !in prev }.map { it.achievement }
    }
}

/**
 * Mensajes de enhorabuena al completar un día.
 *
 * Devuelve claves, no frases: el texto lo pone el diálogo de celebración en el idioma que toque
 * (ui/CelebrationDialog.kt). Las claves solo viven en memoria; no se guardan.
 */
object Motivation {

    const val FIRST = "first"
    const val QUARTER = "quarter"
    const val HALF = "half"
    const val THREE_QUARTERS = "threequarter"
    const val FINAL = "final"

    /** Frases genéricas, por clave. */
    val generic = (0 until 8).map { "g$it" }

    /** Frase genérica por defecto para [totalCompleted] (sin hito). */
    fun genericFor(totalCompleted: Int): String = generic[totalCompleted % generic.size]

    /**
     * Frase del hito, si [totalCompleted] cae en uno. Los cuartos son del plan activo, así que
     * en el de 100 días siguen siendo 25, 50, 75 y 100.
     */
    fun message(totalCompleted: Int): String {
        val total = PlanData.TOTAL_DAYS
        return when (totalCompleted) {
            1 -> FIRST
            total * 25 / 100 -> QUARTER
            total * 50 / 100 -> HALF
            total * 75 / 100 -> THREE_QUARTERS
            total -> FINAL
            else -> genericFor(totalCompleted)
        }
    }
}

/** Evento de celebración tras completar un día. */
data class Celebration(
    val dayNumber: Int,
    val totalCompleted: Int,
    /** Clave de [Motivation]; el texto lo resuelve la interfaz. */
    val message: String,
    val newAchievements: List<Achievement>,
    /** Cierto al completar el último día: el plan está terminado. */
    val isFinalVictory: Boolean = false,
    /**
     * Ofrecer el himno de Queen. Solo en el reto de 100 días, que es de lo que venía: al
     * acabar un bloque de movilidad de 12 sesiones, "We Are The Champions" sobra.
     */
    val offerAnthem: Boolean = false
)
