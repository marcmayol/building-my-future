package com.marc.gymplan100.data

/**
 * Grupos musculares que resalta el mapa para cada ejercicio del plan.
 * Los valores son "slugs" de [MuscleMapData] (frente y espalda mezclados): el mapa resalta
 * cualquier trazado, de frente o de espalda, cuyo slug esté en el conjunto.
 * Mismo criterio de agrupación que las guías (las variantes comparten músculos).
 */
object MuscleTargets {

    // Conjuntos base reutilizados por las variantes del mismo movimiento.
    private val chestPress = setOf("chest", "deltoids", "triceps")
    private val pecDeck = setOf("chest", "deltoids")
    private val lat = setOf("upper-back", "biceps")
    private val row = setOf("upper-back", "trapezius", "biceps")
    private val shoulderPress = setOf("deltoids", "triceps")
    private val biceps = setOf("biceps", "forearm")
    private val triceps = setOf("triceps")
    private val tricepsCompound = setOf("triceps", "chest", "deltoids")
    private val lateralRaise = setOf("deltoids")
    private val facePull = setOf("deltoids", "trapezius", "upper-back")
    private val armSuperset = setOf("biceps", "triceps")
    private val legPress = setOf("quadriceps", "gluteal", "hamstring")
    private val hamstring = setOf("hamstring")
    private val quads = setOf("quadriceps")
    private val glutes = setOf("gluteal", "hamstring")
    private val calves = setOf("calves")
    private val gobletSquat = setOf("quadriceps", "gluteal", "abs")
    private val core = setOf("abs", "obliques", "lower-back")
    private val coreSide = setOf("obliques", "deltoids")
    private val deepCore = setOf("abs", "lower-back")

    private val map: Map<String, Set<String>> = mapOf(
        "Press de pecho en máquina" to chestPress,
        "Press de pecho" to chestPress,
        "Press inclinado o pec deck" to chestPress,
        "Press inclinado" to chestPress,
        "Pec deck (aperturas en máquina)" to pecDeck,
        "Jalón al pecho (polea)" to lat,
        "Jalón al pecho" to lat,
        "Jalón al pecho agarre neutro" to lat,
        "Remo sentado en máquina" to row,
        "Remo sentado" to row,
        "Remo en máquina" to row,
        "Remo en máquina o con mancuerna" to row,
        "Press de hombros en máquina" to shoulderPress,
        "Press de hombros" to shoulderPress,
        "Curl de bíceps con mancuernas" to biceps,
        "Curl de bíceps + curl martillo" to biceps,
        "Curl martillo" to biceps,
        "Curl de bíceps" to biceps,
        "Extensión de tríceps en polea" to triceps,
        "Fondos en máquina o press francés" to tricepsCompound,
        "Elevaciones laterales" to lateralRaise,
        "Face pull en polea" to facePull,
        "Face pull" to facePull,
        "Superserie curl + extensión de tríceps" to armSuperset,
        "Superserie curl + extensión" to armSuperset,
        "Prensa de piernas" to legPress,
        "Curl de piernas (máquina)" to hamstring,
        "Curl de piernas" to hamstring,
        "Extensión de piernas" to quads,
        "Hip thrust o puente de glúteos" to glutes,
        "Hip thrust" to glutes,
        "Elevación de gemelos" to calves,
        "Sentadilla a banco/cajón" to legPress,
        "Sentadilla goblet con mancuerna" to gobletSquat,
        "Sentadilla goblet o en máquina" to gobletSquat,
        "Sentadilla goblet" to gobletSquat,
        "Plancha" to core,
        "Plancha lateral + dead bug" to coreSide,
        "Plancha lateral" to coreSide,
        "Dead bug" to deepCore,
        "Circuito core (plancha, rodillas, plancha lateral)" to core,
        "Circuito core (plancha, rueda, elevación piernas)" to core,
        "Circuito core (plancha, plancha lateral, dead bug)" to core,
        "Finisher: press de pecho ligero al fallo" to chestPress,
    )

    fun forName(name: String): Set<String> = map[name] ?: emptySet()
}
