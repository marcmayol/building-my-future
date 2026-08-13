package com.marc.gymplan100.data

import android.content.Context

/**
 * Los planes que vienen con la app además del reto de 100 días: Arranque, Hipertrofia,
 * Fuerza Máxima, Mantenimiento, Calistenia, Cardio y los cinco bloques extra (piernas,
 * brazos, core, postura y movilidad).
 *
 * Viven en `assets/planes/` con el **mismo formato con el que se importa un plan propio**
 * ([PlanDto]), así que pasan por la misma validación que cualquier otro y no hay un segundo
 * camino que mantener. Se marcan como `builtin` al salir: se pueden activar, pero no borrar.
 *
 * El reto de 100 días NO está aquí: sigue siendo [BuiltinPlan], porque su id es el que cuelga
 * del progreso que ya tiene el usuario.
 */
object BuiltinPlans {

    private const val DIR = "planes"
    private const val INDEX = "$DIR/index.json"

    /** Se leen una vez: son ficheros del propio APK y no cambian en caliente. */
    @Volatile
    private var cache: List<TrainingPlan>? = null

    fun all(context: Context): List<TrainingPlan> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val loaded = load(context.applicationContext)
            cache = loaded
            return loaded
        }
    }

    fun byId(context: Context, id: String): TrainingPlan? = all(context).firstOrNull { it.id == id }

    private fun load(context: Context): List<TrainingPlan> {
        val ids = runCatching {
            val raw = context.assets.open(INDEX).bufferedReader().use { it.readText() }
            PlanCodec.decodeIdList(raw)
        }.getOrNull().orEmpty()

        return ids.mapNotNull { id ->
            runCatching {
                val raw = context.assets.open("$DIR/$id.json").bufferedReader().use { it.readText() }
                // importedAt = 0: son de serie, no llevan fecha de importación en la ficha.
                val ok = PlanCodec.parseJson(raw, now = 0L) as? PlanImport.Ok ?: return@runCatching null
                ok.plan.copy(builtin = true, source = PlanSource.BUILTIN, importedAt = 0L)
            }.getOrNull()
        }
    }
}
