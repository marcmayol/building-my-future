package com.marc.gymplan100.data

import android.content.Context

/**
 * Ilustración de referencia de cada ejercicio, en versión masculina (`_m`) y femenina (`_f`).
 * Son ilustraciones propias (generadas con gpt-image a partir de la referencia de pose/máquina),
 * en el estilo de la app: dos fotogramas, camiseta naranja, contorno negro, fondo blanco.
 *
 * Se resuelven por nombre de recurso con getIdentifier (`ex_<slug>_m` / `ex_<slug>_f`) para no
 * tener que enumerar 52 drawables a mano.
 */
object ExerciseImages {

    /** Nombre del ejercicio en el plan -> slug base de su ilustración. */
    private val nameToSlug: Map<String, String> = mapOf(
        "Press de pecho en máquina" to "seated-chest-press",
        "Press de pecho" to "seated-chest-press",
        "Press inclinado o pec deck" to "incline-chest-press-machine",
        "Press inclinado" to "incline-chest-press-machine",
        "Pec deck (aperturas en máquina)" to "butterflies",
        "Remo sentado en máquina" to "seated-row-machine",
        "Remo sentado" to "seated-cable-back-rows",
        "Remo en máquina" to "seated-row-machine",
        "Remo en máquina o con mancuerna" to "seated-row-machine",
        "Curl de bíceps con mancuernas" to "alternating-dumbbell-biceps-curl",
        "Curl de bíceps" to "alternating-dumbbell-biceps-curl",
        "Curl de bíceps + curl martillo" to "dumbbell-hammer-biceps-curl",
        "Curl martillo" to "dumbbell-hammer-biceps-curl",
        "Extensión de tríceps en polea" to "triceps-pushdown",
        "Fondos en máquina o press francés" to "dips-machine",
        "Elevaciones laterales" to "dumbbell-lateral-raises",
        "Jalón al pecho (polea)" to "chest-pulldown",
        "Jalón al pecho" to "chest-pulldown",
        "Jalón al pecho agarre neutro" to "close-grip-chest-pulldown",
        "Press de hombros en máquina" to "machine-shoulder-press",
        "Press de hombros" to "seated-dumbbell-overhead-shoulder-press",
        "Face pull en polea" to "face-pull",
        "Face pull" to "face-pull",
        "Plancha" to "plank",
        "Plancha lateral + dead bug" to "side-plank",
        "Plancha lateral" to "side-plank",
        "Dead bug" to "dead-bug",
        "Hip thrust o puente de glúteos" to "barbell-hip-thrust",
        "Hip thrust" to "barbell-hip-thrust",
        "Prensa de piernas" to "leg-press",
        "Curl de piernas (máquina)" to "seated-leg-curl",
        "Curl de piernas" to "lying-leg-curls",
        "Extensión de piernas" to "seated-leg-extensions",
        "Elevación de gemelos" to "seated-calf-raises",
        "Sentadilla a banco/cajón" to "box-squat",
        "Sentadilla goblet con mancuerna" to "goblet-squats",
        "Sentadilla goblet o en máquina" to "goblet-squats",
        "Sentadilla goblet" to "goblet-squats",
        "Finisher: press de pecho ligero al fallo" to "seated-chest-press",
    )

    /**
     * Movimientos ilustrables de cada circuito, en orden (etiqueta -> slug de imagen).
     * Un circuito no tiene una sola imagen: mostramos la de cada sub-ejercicio que tengamos.
     * Los slugs sin drawable (p. ej. escalador, rueda) simplemente no aparecen.
     */
    private val nameToCircuitMoves: Map<String, List<Pair<String, String>>> = mapOf(
        "Circuito core (plancha, rodillas, plancha lateral)" to listOf(
            "Plancha" to "plank",
            "Rodillas al pecho" to "mountain-climber",
            "Plancha lateral" to "side-plank",
        ),
        "Circuito core (plancha, rueda, elevación piernas)" to listOf(
            "Plancha" to "plank",
            "Rueda abdominal" to "ab-wheel",
            "Elevación de piernas" to "lying-leg-raise",
        ),
        "Circuito core (plancha, plancha lateral, dead bug)" to listOf(
            "Plancha" to "plank",
            "Plancha lateral" to "side-plank",
            "Dead bug" to "dead-bug",
        ),
    )

    private fun drawableFor(context: Context, slug: String, female: Boolean): Int {
        val res = "ex_" + slug.replace('-', '_') + if (female) "_f" else "_m"
        return context.resources.getIdentifier(res, "drawable", context.packageName)
    }

    /**
     * Drawable de la ilustración del ejercicio para el género indicado (femenino si [female],
     * masculino en caso contrario), o null si el ejercicio no tiene imagen.
     */
    fun forName(context: Context, name: String, female: Boolean): Int? {
        val slug = nameToSlug[name] ?: return null
        val id = drawableFor(context, slug, female)
        return if (id != 0) id else null
    }

    /**
     * Ilustraciones de los movimientos de un circuito (etiqueta -> drawable), solo las que existen.
     * Vacío si el nombre no es un circuito conocido.
     */
    fun circuitMoves(context: Context, name: String, female: Boolean): List<Pair<String, Int>> {
        val moves = nameToCircuitMoves[name] ?: return emptyList()
        return moves.mapNotNull { (label, slug) ->
            val id = drawableFor(context, slug, female)
            if (id != 0) label to id else null
        }
    }
}
