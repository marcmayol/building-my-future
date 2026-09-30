package com.marc.gymplan100.data

import java.text.Normalizer

/**
 * A qué grupo muscular va cada ejercicio en «Mis pesos», para no tener que buscar entre
 * cuarenta filas seguidas.
 *
 * No sale de [MuscleTargets]: ese mapa solo conoce los ejercicios del plan de 100 días, y en
 * los otros once planes dejaría sin grupo más de la mitad. Aquí basta con el nombre, porque el
 * gimnasio ya pone el músculo en él: un «jalón» es espalda y una «sentadilla» es pierna, sea
 * cual sea la variante. [MuscleTargets] queda de respaldo para lo que el nombre no delata.
 *
 * Un solo grupo por ejercicio, el del músculo que manda: un press de banca también mueve
 * tríceps, pero nadie lo buscaría ahí.
 */
enum class ExerciseGroup(val label: String) {
    PECHO("Pecho"),
    ESPALDA("Espalda"),
    HOMBROS("Hombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    PIERNA("Pierna"),
    CORE("Core"),
    CARDIO("Cardio"),
    MOVILIDAD("Movilidad"),
    OTROS("Otros")
}

object ExerciseGroups {

    /**
     * Las reglas van EN ORDEN y gana la primera: el orden es lo que resuelve los nombres que
     * tocan dos grupos. «Curl de piernas» es pierna antes que bíceps, «Jalón al pecho» es
     * espalda antes que pecho, «Rodillas al pecho» es core, y «Apertura de pecho tumbado» es
     * un estiramiento, no un ejercicio de pecho.
     */
    private val rules: List<Pair<ExerciseGroup, List<String>>> = listOf(
        ExerciseGroup.CARDIO to listOf(" min ", " min)", "bloque", "continuo", "alternar", "piramide"),
        ExerciseGroup.MOVILIDAD to listOf(
            "estiramiento", "postura 90", "toracica", "chin tuck", "dorsiflexion",
            "flexion adelante", "wall slides", "deslizamientos de brazos", "apertura de pecho tumbado",
            "piriforme", "isquios con correa", "zancada baja", "bisagra de cadera con palo"
        ),
        ExerciseGroup.CORE to listOf(
            "plancha", "dead bug", "hollow", "pallof", "bird dog", "rueda abdominal", "core",
            "elevacion de piernas", "elevacion de rodillas", "rodillas al pecho",
            "rotacion con banda", "lanzamiento de balon", "paseo del granjero"
        ),
        ExerciseGroup.PIERNA to listOf(
            "piernas", "sentadilla", "prensa", "zancada", "peso muerto", "hip thrust", "puente",
            "gemelos", "talones", "abduccion", "gluteo", "bisagra"
        ),
        ExerciseGroup.TRICEPS to listOf(
            "triceps", "fondos", "diamante", "sobre la cabeza", "press cerrado", "press frances"
        ),
        ExerciseGroup.BICEPS to listOf("curl"),
        ExerciseGroup.HOMBROS to listOf(
            "hombro", "militar", "lateral", "pajaros", "face pull", "a la cara", "pino", "y-t-w"
        ),
        ExerciseGroup.ESPALDA to listOf("jalon", "remo", "dominada", "pullover", "tiron", "colgarse"),
        ExerciseGroup.PECHO to listOf(
            "pecho", "banca", "pec deck", "aperturas", "inclinado", "flexion", "empuje"
        )
    )

    /** Qué slug del mapa muscular va a qué grupo, para los ejercicios que el nombre no delata. */
    private val bySlug: Map<String, ExerciseGroup> = mapOf(
        "chest" to ExerciseGroup.PECHO,
        "upper-back" to ExerciseGroup.ESPALDA,
        "trapezius" to ExerciseGroup.ESPALDA,
        "lower-back" to ExerciseGroup.ESPALDA,
        "deltoids" to ExerciseGroup.HOMBROS,
        "biceps" to ExerciseGroup.BICEPS,
        "forearm" to ExerciseGroup.BICEPS,
        "triceps" to ExerciseGroup.TRICEPS,
        "quadriceps" to ExerciseGroup.PIERNA,
        "hamstring" to ExerciseGroup.PIERNA,
        "gluteal" to ExerciseGroup.PIERNA,
        "calves" to ExerciseGroup.PIERNA,
        "adductors" to ExerciseGroup.PIERNA,
        "abs" to ExerciseGroup.CORE,
        "obliques" to ExerciseGroup.CORE
    )

    fun of(name: String): ExerciseGroup {
        // Un ejercicio con alias se agrupa como aquel al que se parece: si lo has llamado a tu
        // manera, sigue estando donde lo buscarías.
        val resolved = ExerciseAliases.resolve(name)
        val folded = " " + fold(resolved) + " "
        rules.firstOrNull { (_, keys) -> keys.any { it in folded } }?.let { return it.first }
        val slug = MuscleTargets.forName(resolved)?.primary?.firstOrNull()
        return slug?.let { bySlug[it] } ?: ExerciseGroup.OTROS
    }

    /** Sin tildes y en minúsculas: «Jalón» y «jalon» tienen que ser lo mismo. */
    private fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
