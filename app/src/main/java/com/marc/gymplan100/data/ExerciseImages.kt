package com.marc.gymplan100.data

import android.content.Context

/**
 * Ilustración de referencia de cada ejercicio, en versión masculina (`_m`) y femenina (`_f`).
 * Son ilustraciones propias, en el estilo de la app: dos fotogramas, camiseta naranja, contorno
 * negro, fondo blanco. Salen del catálogo abierto <https://marcmayol.com/exercise-api>, que es
 * donde se generan y se mantienen; aquí viven copiadas para que funcionen sin conexión.
 *
 * Se resuelven por nombre de recurso con getIdentifier (`ex_<slug>_m` / `ex_<slug>_f`) para no
 * tener que enumerar los drawables a mano.
 *
 * El mapa es grande porque los planes escriben el mismo movimiento de muchas maneras
 * ("Sentadilla hack", "Sentadilla hack o goblet", "Sentadilla en máquina hack o goblet"): todas
 * apuntan al mismo dibujo.
 */
object ExerciseImages {

    /** Nombre del ejercicio en el plan -> slug base de su ilustración. */
    private val nameToSlug: Map<String, String> = mapOf(
        // --- Pecho ---
        "Empuje (escalera)" to "push-ups",
        "Empuje (nivel actual)" to "push-ups",
        "Empuje con manos elevadas" to "push-ups",
        "Empuje con manos juntas" to "diamond-push-up",
        "Estiramiento de pecho en marco de puerta" to "doorway-chest-stretch",
        "Finisher: press de pecho ligero al fallo" to "seated-chest-press",
        "Flexión con pies elevados o pseudo planche" to "push-ups",
        "Flexión en diamante" to "diamond-push-up",
        "Pec deck" to "butterflies",
        "Pec deck (aperturas en máquina)" to "butterflies",
        "Pec deck o aperturas en polea" to "butterflies",
        "Press de banca (barra, mancuernas o máquina)" to "barbell-bench-press",
        "Press de banca (tu variante)" to "barbell-bench-press",
        "Press de banca, respaldo" to "barbell-bench-press",
        "Press de pecho" to "seated-chest-press",
        "Press de pecho en máquina" to "seated-chest-press",
        "Press de pecho en máquina o mancuernas" to "seated-chest-press",
        "Press inclinado" to "incline-chest-press-machine",
        "Press inclinado con mancuernas" to "incline-dumbbell-press",
        "Press inclinado o pec deck" to "incline-chest-press-machine",
        // --- Espalda ---
        "Apertura de pecho tumbado (libro abierto)" to "open-book-stretch",
        "Chin tuck (retracción de barbilla)" to "chin-tuck",
        "Colgarse de la barra" to "dead-hang",
        "Colgarse de la barra (activo o pasivo)" to "dead-hang",
        "Dominada (asistida o completa)" to "pull-ups",
        "Extensión torácica sobre banco o foam roller" to "thoracic-extension-bench",
        "Extensión torácica sobre banco o silla" to "thoracic-extension-bench",
        "Jalón al pecho" to "chest-pulldown",
        "Jalón al pecho (polea)" to "chest-pulldown",
        "Jalón al pecho agarre ancho" to "chest-pulldown",
        "Jalón al pecho agarre neutro" to "close-grip-chest-pulldown",
        "Jalón al pecho en polea" to "chest-pulldown",
        "Jalón al pecho o dominada" to "chest-pulldown",
        "Jalón al pecho o dominada asistida" to "chest-pulldown",
        "Negativas de dominada (bajar en 5 s)" to "pull-ups",
        "Pullover en polea" to "cable-pullover",
        "Remo con barra o en máquina" to "barbell-row",
        "Remo con mancuerna" to "dumbbell-row",
        "Remo con mancuerna a una mano" to "dumbbell-row",
        "Remo en máquina" to "seated-row-machine",
        "Remo en máquina o con mancuerna" to "seated-row-machine",
        "Remo invertido" to "inverted-row",
        "Remo invertido agarre supino" to "inverted-row",
        "Remo invertido con pies elevados" to "inverted-row",
        "Remo sentado" to "seated-cable-back-rows",
        "Remo sentado con pausa de 2 s atrás" to "seated-row-machine",
        "Remo sentado en máquina" to "seated-row-machine",
        "Remo sentado en polea" to "seated-cable-back-rows",
        "Rotación torácica en cuadrupedia (mano a la nuca)" to "quadruped-thoracic-rotation",
        "Tirón (escalera)" to "pull-ups",
        "Tirón (nivel actual)" to "pull-ups",
        // --- Hombros ---
        "Deslizamientos de brazos en pared (wall slides)" to "wall-angels",
        "Elevaciones laterales" to "dumbbell-lateral-raises",
        "Elevaciones laterales en polea a una mano" to "cable-lateral-raises",
        "Face pull" to "face-pull",
        "Face pull en polea" to "face-pull",
        "Pino contra la pared" to "wall-handstand",
        "Press de hombros" to "seated-dumbbell-overhead-shoulder-press",
        "Press de hombros en máquina" to "machine-shoulder-press",
        "Press de hombros en máquina (peso mínimo)" to "machine-shoulder-press",
        "Press de hombros sentado" to "seated-dumbbell-overhead-shoulder-press",
        "Press militar" to "barbell-overhead-press",
        "Press militar sentado" to "barbell-overhead-press",
        "Pájaros con mancuernas o pec deck invertido" to "reverse-fly",
        "Remo en polea a la cara con cuerda" to "face-pull",
        "Y-T-W en banco inclinado (sin peso o muy ligero)" to "y-t-w-raise",
        // --- Brazos ---
        "Curl con barra o mancuernas" to "barbell-biceps-curl",
        "Curl con barra Z" to "barbell-biceps-curl",
        "Curl de bíceps" to "alternating-dumbbell-biceps-curl",
        "Curl de bíceps + curl martillo" to "dumbbell-hammer-biceps-curl",
        "Curl de bíceps con mancuernas" to "alternating-dumbbell-biceps-curl",
        "Curl de bíceps con mancuernas ligeras" to "alternating-dumbbell-biceps-curl",
        "Curl inclinado con mancuernas" to "incline-dumbbell-curl",
        "Curl martillo" to "dumbbell-hammer-biceps-curl",
        "Extensión de tríceps en polea" to "triceps-pushdown",
        "Extensión de tríceps en polea alta" to "triceps-pushdown",
        "Extensión de tríceps sobre la cabeza" to "overhead-triceps-extension",
        "Extensión sobre la cabeza con mancuerna" to "overhead-triceps-extension",
        "Fondos" to "dips-machine",
        "Fondos en máquina asistida" to "dips-machine",
        "Fondos en máquina asistida o press cerrado" to "dips-machine",
        "Fondos en máquina o press francés" to "dips-machine",
        "Fondos entre sillas o en banco" to "bench-dips",
        "Superserie curl + extensión" to "superset-curl-triceps",
        "Superserie curl + extensión de tríceps" to "superset-curl-triceps",
        // --- Piernas ---
        "Abducción de cadera en máquina" to "hip-abduction",
        "Bisagra de cadera con palo o escoba (espalda recta)" to "hip-hinge-dowel",
        "Bisagra, respaldo" to "romanian-deadlift",
        "Curl de piernas" to "lying-leg-curls",
        "Curl de piernas (máquina)" to "seated-leg-curl",
        "Curl de piernas en máquina" to "seated-leg-curl",
        "Curl de piernas sentado" to "seated-leg-curl",
        "Curl de piernas tumbado" to "lying-leg-curls",
        "Dorsiflexión de tobillo contra pared" to "ankle-dorsiflexion-wall",
        "Elevación de gemelos" to "seated-calf-raises",
        "Elevación de gemelos de pie" to "standing-calf-raises",
        "Elevación de gemelos sentado" to "seated-calf-raises",
        "Elevación de talones" to "standing-calf-raises",
        "Elevación de talones a una pierna" to "standing-calf-raises",
        "Estiramiento de flexores (zancada baja, brazo arriba)" to "hip-flexor-stretch",
        "Estiramiento de flexores de cadera (zancada baja)" to "hip-flexor-stretch",
        "Estiramiento de gemelos en escalón o pared" to "calf-stretch-wall",
        "Extensión de piernas" to "seated-leg-extensions",
        "Flexión adelante sentado, espalda larga" to "seated-forward-fold",
        "Glúteo y piriforme (figura 4 tumbado)" to "figure-4-stretch",
        "Hip thrust" to "barbell-hip-thrust",
        "Hip thrust o peso muerto rumano" to "romanian-deadlift",
        "Hip thrust o puente de glúteos" to "barbell-hip-thrust",
        "Hip thrust o puente de glúteos con pausa" to "barbell-hip-thrust",
        "Isquios con correa o toalla, tumbado" to "supine-hamstring-stretch",
        "Peso muerto a una pierna" to "deadlift",
        "Peso muerto rumano" to "romanian-deadlift",
        "Peso muerto rumano con mancuernas" to "romanian-deadlift",
        "Peso muerto rumano con mancuernas o barra" to "romanian-deadlift",
        "Peso muerto rumano o hip thrust" to "romanian-deadlift",
        "Peso muerto rumano o hip thrust pesado" to "romanian-deadlift",
        "Postura 90/90 con cambios" to "hip-90-90",
        "Prensa de piernas" to "leg-press",
        "Prensa de piernas (recorrido corto)" to "leg-press",
        "Prensa de piernas pies altos" to "leg-press",
        "Prensa de piernas pies altos y anchos" to "leg-press",
        "Prensa pies altos" to "leg-press",
        "Puente a una pierna" to "glute-bridge",
        "Puente de glúteos" to "glute-bridge",
        "Puente de glúteos a una pierna" to "glute-bridge",
        "Sentadilla (barra, hack o multipower)" to "barbell-squat",
        "Sentadilla (escalera)" to "bodyweight-squat",
        "Sentadilla (nivel actual)" to "bodyweight-squat",
        "Sentadilla (tu variante)" to "barbell-squat",
        "Sentadilla a banco/cajón" to "box-squat",
        "Sentadilla a una pierna asistida" to "assisted-pistol-squat",
        "Sentadilla búlgara" to "bulgarian-split-squat",
        "Sentadilla en máquina hack o goblet" to "hack-squat",
        "Sentadilla goblet" to "goblet-squats",
        "Sentadilla goblet con mancuerna" to "goblet-squats",
        "Sentadilla goblet o en máquina" to "goblet-squats",
        "Sentadilla goblet o hack en máquina" to "goblet-squats",
        "Sentadilla goblet o prensa" to "goblet-squats",
        "Sentadilla hack" to "hack-squat",
        "Sentadilla hack o goblet" to "hack-squat",
        "Sentadilla profunda asistida (agarrado a algo)" to "deep-squat-hold",
        "Sentadilla, series de respaldo" to "barbell-squat",
        "Zancada baja con rotación hacia arriba" to "low-lunge-rotation",
        "Zancada caminando" to "dumbbell-lunges",
        "Zancadas con mancuernas o en multipower" to "dumbbell-lunges",
        "Zancadas estáticas" to "dumbbell-lunges",
        // --- Core ---
        "Bird dog" to "bird-dog",
        "Bird dog lento (5 s por repetición)" to "bird-dog",
        "Dead bug" to "dead-bug",
        "Dead bug con banda" to "dead-bug",
        "Dead bug con brazos" to "dead-bug",
        "Dead bug lento" to "dead-bug",
        "Elevación de piernas colgado" to "hanging-leg-raise",
        "Elevación de piernas colgado o en banco" to "hanging-leg-raise",
        "Elevación de piernas colgado o tumbado" to "hanging-leg-raise",
        "Elevación de piernas tumbado" to "lying-leg-raise",
        "Elevación de piernas tumbado (rodillas flexionadas)" to "lying-leg-raise",
        "Elevación de rodillas colgado" to "hanging-leg-raise",
        "Elevación de rodillas colgado con giro" to "hanging-leg-raise",
        "Hollow hold" to "hollow-hold",
        "Hollow hold con rodillas flexionadas" to "hollow-hold",
        "Hollow rock" to "hollow-rock",
        "Lanzamiento de balón medicinal (o rotación explosiva con banda)" to "cable-woodchopper",
        "Paseo del granjero a una mano" to "farmers-walk",
        "Plancha" to "plank",
        "Plancha (rodillas o banco)" to "plank",
        "Plancha (rodillas o completa)" to "plank",
        "Plancha (tu nivel)" to "plank",
        "Plancha apoyada en banco o pared" to "plank",
        "Plancha con deslizamiento de brazos" to "plank",
        "Plancha con peso" to "plank",
        "Plancha con peso o pies elevados" to "plank",
        "Plancha con toque de hombro" to "plank",
        "Plancha lateral" to "side-plank",
        "Plancha lateral + dead bug" to "side-plank",
        "Plancha lateral completa" to "side-plank",
        "Plancha lateral con elevación de cadera" to "side-plank",
        "Plancha lateral con pies elevados" to "side-plank",
        "Plancha lateral con rodillas" to "side-plank",
        "Press Pallof con banda" to "pallof-press",
        "Press Pallof con banda (o isométrico en pared)" to "pallof-press",
        "Press Pallof en rodillas" to "pallof-press",
        "Rotación con banda de pie" to "cable-woodchopper",
        "Rueda abdominal desde rodillas" to "ab-wheel-rollout",
        "Rueda abdominal desde rodillas (o toalla)" to "ab-wheel-rollout",
    )

    /**
     * Un movimiento dentro de un ejercicio compuesto: cómo se llama, con qué se ilustra y
     * cuánto toca hacer. La dosis es la del propio circuito (la que cuenta la ficha), no la
     * del esquema del ejercicio: el esquema dice cuántas *vueltas*, no cuánto dura cada cosa.
     */
    data class CompoundMove(val label: String, val slug: String, val dose: String? = null)

    /** Un movimiento ya resuelto a drawable, listo para pintar. */
    data class ResolvedMove(val label: String, val dose: String?, val drawable: Int)

    /**
     * Movimientos ilustrables de cada ejercicio **compuesto** (circuitos y superseries), en
     * orden. No tienen una sola imagen: se muestran todos, porque enseñar solo el primero
     * hacía pasar un circuito de tres cosas por una plancha a secas.
     */
    private val nameToCompoundMoves: Map<String, List<CompoundMove>> = mapOf(
        "Circuito core (plancha, plancha lateral, dead bug)" to listOf(
            CompoundMove("Plancha", "plank", "~30 s"),
            CompoundMove("Plancha lateral", "side-plank", "~20 s por lado"),
            CompoundMove("Dead bug", "dead-bug", "~10 reps alternando"),
        ),
        "Circuito core (plancha, rodillas, plancha lateral)" to listOf(
            CompoundMove("Plancha", "plank", "~30 s"),
            CompoundMove("Rodillas al pecho", "mountain-climbers", "~20-30 s"),
            CompoundMove("Plancha lateral", "side-plank", "~20 s por lado"),
        ),
        "Circuito core (plancha, rueda, elevación piernas)" to listOf(
            CompoundMove("Plancha", "plank", "~30 s"),
            CompoundMove("Rueda abdominal", "ab-wheel-rollout", "~8-10 reps"),
            CompoundMove("Elevación de piernas", "lying-leg-raise", "~10-12 reps"),
        ),
        "Circuito de core (plancha, hollow, plancha lateral)" to listOf(
            CompoundMove("Plancha", "plank", "~30 s"),
            CompoundMove("Hollow hold", "hollow-hold", "~20-30 s"),
            CompoundMove("Plancha lateral", "side-plank", "~20 s por lado"),
        ),
        "Circuito: plancha, hollow hold, plancha lateral dcha, plancha lateral izq, dead bug" to listOf(
            CompoundMove("Plancha", "plank", "~30 s"),
            CompoundMove("Hollow hold", "hollow-hold", "~20-30 s"),
            CompoundMove("Plancha lateral", "side-plank", "~20 s por lado"),
            CompoundMove("Dead bug", "dead-bug", "~10 reps alternando"),
        ),
        "Superserie curl + extensión" to listOf(
            CompoundMove("Curl de bíceps alterno con mancuernas", "alternating-dumbbell-biceps-curl"),
            CompoundMove("Extensión de tríceps en polea", "triceps-pushdown"),
        ),
        "Superserie curl + extensión de tríceps" to listOf(
            CompoundMove("Curl de bíceps alterno con mancuernas", "alternating-dumbbell-biceps-curl"),
            CompoundMove("Extensión de tríceps en polea", "triceps-pushdown"),
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
        nameToSlug[name]?.let { slug ->
            val id = drawableFor(context, slug, female)
            if (id != 0) return id
        }
        // Un compuesto no tiene imagen propia. Vale la del primer movimiento para las
        // miniaturas de 64 dp (la lista del día), donde no cabe nada más; donde hay sitio se
        // pintan todos con [circuitMoves], que para eso el circuito son tres cosas.
        return circuitMoves(context, name, female).firstOrNull()?.drawable
    }

    /**
     * Movimientos de un ejercicio compuesto ya resueltos a drawable, en orden y solo los que
     * tienen ilustración. Vacío si el nombre no es un compuesto conocido.
     */
    fun circuitMoves(context: Context, name: String, female: Boolean): List<ResolvedMove> {
        val moves = nameToCompoundMoves[name] ?: return emptyList()
        return moves.mapNotNull { move ->
            val id = drawableFor(context, move.slug, female)
            if (id != 0) ResolvedMove(move.label, move.dose, id) else null
        }
    }

    /** ¿Es un circuito o una superserie, es decir, varios movimientos en un solo ejercicio? */
    fun isCompound(name: String): Boolean = nameToCompoundMoves.containsKey(name)

    /**
     * El nombre para un titular grande. A un compuesto se le quita el paréntesis con la lista
     * de movimientos ("Circuito core (plancha, rodillas, plancha lateral)" -> "Circuito core"):
     * en la pantalla de la serie no cabía y se cortaba a media enumeración, y además ahora los
     * movimientos van escritos debajo, uno por uno y con su dibujo.
     */
    fun headlineName(name: String): String =
        if (isCompound(name)) name.substringBefore(" (").trim() else name

    /** ¿Sabemos con qué ilustrar este ejercicio? Slug propio o movimientos de un compuesto. */
    fun hasVisual(name: String): Boolean =
        nameToSlug.containsKey(name) || nameToCompoundMoves.containsKey(name)

    /**
     * Slug del catálogo al que corresponde este nombre, o el del primer movimiento si es un
     * compuesto. Lo usa [ExerciseGuides] para encontrar la ficha del ejercicio.
     */
    fun slugFor(name: String): String? =
        nameToSlug[name] ?: nameToCompoundMoves[name]?.firstOrNull()?.slug
}
