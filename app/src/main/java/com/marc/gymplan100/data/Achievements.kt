package com.marc.gymplan100.data

/** Un logro del reto. */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

/** Logros del reto y lógica de desbloqueo (derivada del progreso, sin estado extra). */
object Achievements {

    private data class Rule(val achievement: Achievement, val unlocked: (ProgressState) -> Boolean)

    private fun byDays(id: String, title: String, desc: String, emoji: String, days: Int) =
        Rule(Achievement(id, title, desc, emoji)) { state ->
            state.completedDays.count { it in 1..PlanData.TOTAL_DAYS } >= days
        }

    private fun byPhase(id: String, title: String, emoji: String, phase: Int) =
        Rule(
            Achievement(id, title, "Termina la fase ${phase}.", emoji)
        ) { state ->
            val total = PlanData.daysOfPhase(phase).size
            total > 0 && state.completedInPhase(phase) >= total
        }

    private val rules: List<Rule> = listOf(
        byDays("first", "Primer paso", "Completa tu primer día.", "🥇", 1),
        byDays("week", "Primera semana", "Completa 5 días.", "📅", 5),
        byDays("ten", "Cogiendo ritmo", "Completa 10 días.", "🔥", 10),
        byPhase("phase1", "Adaptación superada", "🛡️", 1),
        byDays("quarter", "Un cuarto del reto", "Completa 25 días.", "⚡", 25),
        byPhase("phase2", "Construcción superada", "🏗️", 2),
        byDays("half", "Mitad del camino", "Completa 50 días.", "🏔️", 50),
        byPhase("phase3", "Progresión superada", "📈", 3),
        byDays("threequarter", "Recta final", "Completa 75 días.", "🚀", 75),
        byPhase("phase4", "Consolidación superada", "🏆", 4),
        byDays("complete", "¡Reto completado!", "Completa los 100 días.", "👑", 100),
    )

    val all: List<Achievement> = rules.map { it.achievement }

    fun unlockedIds(state: ProgressState): Set<String> =
        rules.filter { it.unlocked(state) }.map { it.achievement.id }.toSet()

    /** Logros que están desbloqueados en [after] pero no lo estaban en [before]. */
    fun newlyUnlocked(before: ProgressState, after: ProgressState): List<Achievement> {
        val prev = unlockedIds(before)
        return rules.filter { it.unlocked(after) && it.achievement.id !in prev }.map { it.achievement }
    }
}

/** Mensajes de enhorabuena al completar un día. */
object Motivation {

    val generic = listOf(
        "¡Un día más cerca de tu objetivo!",
        "Tu yo del futuro te lo agradece.",
        "La constancia es tu superpoder.",
        "Hoy has elegido ser mejor. 💪",
        "Ladrillo a ladrillo, estás construyendo tu futuro.",
        "Cada sesión cuenta. ¡Sigue así!",
        "El esfuerzo de hoy es el orgullo de mañana.",
        "Has aparecido y lo has hecho. Eso es lo que gana.",
    )

    /** Frase genérica por defecto para [totalCompleted] (sin hito). */
    fun genericFor(totalCompleted: Int): String = generic[totalCompleted % generic.size]

    fun message(totalCompleted: Int): String = when (totalCompleted) {
        1 -> "¡Has empezado! El primer paso es el más importante."
        25 -> "Un cuarto del reto hecho. Vas en serio."
        50 -> "¡Mitad del camino! Ya no hay vuelta atrás."
        75 -> "Recta final. Lo que viene es para nota."
        100 -> "¡LO HAS LOGRADO! 100 días. Eres otra persona."
        else -> genericFor(totalCompleted)
    }
}

/** Evento de celebración tras completar un día. */
data class Celebration(
    val dayNumber: Int,
    val totalCompleted: Int,
    val message: String,
    val newAchievements: List<Achievement>,
    /** Cierto al completar el día 100: el reto está terminado. */
    val isFinalVictory: Boolean = false
)
