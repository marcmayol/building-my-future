package com.marc.gymplan100.data

/**
 * Grupos musculares que resalta el mapa para cada ejercicio, separados en principales y
 * secundarios (para pintarlos con distinta intensidad). Los valores son "slugs" de
 * [MuscleMapData] (frente y espalda mezclados). El reparto principal/secundario está validado
 * con el dataset de GoFitnessPlan, pero mapeado a los slugs anatómicamente correctos
 * (p. ej. el dorsal del jalón va a `upper-back`, no a la zona lumbar).
 */
data class MuscleTarget(
    val primary: Set<String>,
    val secondary: Set<String> = emptySet()
)

object MuscleTargets {

    // Conjuntos base reutilizados por las variantes del mismo movimiento.
    private val chestPress = MuscleTarget(setOf("chest"), setOf("deltoids", "triceps"))
    private val pecDeck = MuscleTarget(setOf("chest"), setOf("deltoids"))
    private val lat = MuscleTarget(setOf("upper-back"), setOf("biceps"))
    private val row = MuscleTarget(setOf("upper-back", "trapezius"), setOf("biceps"))
    private val shoulderPress = MuscleTarget(setOf("deltoids"), setOf("triceps"))
    private val biceps = MuscleTarget(setOf("biceps"), setOf("forearm"))
    private val triceps = MuscleTarget(setOf("triceps"))
    private val tricepsCompound = MuscleTarget(setOf("triceps"), setOf("chest", "deltoids"))
    private val lateralRaise = MuscleTarget(setOf("deltoids"))
    private val facePull = MuscleTarget(setOf("deltoids", "trapezius"), setOf("upper-back"))
    private val armSuperset = MuscleTarget(setOf("biceps", "triceps"))
    private val legPress = MuscleTarget(setOf("quadriceps", "gluteal"), setOf("hamstring", "calves"))
    private val hamstring = MuscleTarget(setOf("hamstring"), setOf("gluteal"))
    private val quads = MuscleTarget(setOf("quadriceps"))
    private val glutes = MuscleTarget(setOf("gluteal"), setOf("hamstring"))
    private val calves = MuscleTarget(setOf("calves"))
    private val boxSquat = MuscleTarget(setOf("quadriceps", "gluteal"), setOf("hamstring"))
    private val gobletSquat = MuscleTarget(setOf("quadriceps", "gluteal"), setOf("abs"))
    private val core = MuscleTarget(setOf("abs"), setOf("obliques", "lower-back"))
    private val coreSide = MuscleTarget(setOf("obliques"), setOf("abs"))
    private val coreCircuit = MuscleTarget(setOf("abs", "obliques"), setOf("lower-back"))
    private val deepCore = MuscleTarget(setOf("abs"), setOf("lower-back"))

    private val map: Map<String, MuscleTarget> = mapOf(
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
        "Sentadilla a banco/cajón" to boxSquat,
        "Sentadilla goblet con mancuerna" to gobletSquat,
        "Sentadilla goblet o en máquina" to gobletSquat,
        "Sentadilla goblet" to gobletSquat,
        "Plancha" to core,
        "Plancha lateral + dead bug" to coreSide,
        "Plancha lateral" to coreSide,
        "Dead bug" to deepCore,
        "Circuito core (plancha, rodillas, plancha lateral)" to coreCircuit,
        "Circuito core (plancha, rueda, elevación piernas)" to coreCircuit,
        "Circuito core (plancha, plancha lateral, dead bug)" to coreCircuit,
        "Finisher: press de pecho ligero al fallo" to chestPress,
    )

    fun forName(name: String): MuscleTarget? = map[name]
}
