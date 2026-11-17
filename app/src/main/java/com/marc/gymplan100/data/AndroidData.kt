package com.marc.gymplan100.data

import android.content.Context
import androidx.annotation.StringRes
import com.marc.gymplan100.R

/**
 * La parte Android de lo que vive en :shared: leer los assets con un Context y traducir a
 * recursos de Android. Son extensiones con el mismo nombre que antes, así que las llamadas
 * (`ExerciseCatalog.load(context)`) no cambian.
 */

fun Context.leerAsset(ruta: String): String =
    applicationContext.assets.open(ruta).bufferedReader().use { it.readText() }

fun ExerciseCatalog.load(context: Context) {
    if (cargado) return
    load(runCatching { context.leerAsset("catalogo.json") }.getOrElse { "[]" })
}

/** Nombre del grupo en el idioma de la app, para los títulos de sección. */
@StringRes
fun ExerciseCatalog.groupLabelRes(grupo: String): Int = when (grupo) {
    "chest" -> R.string.ses_group_chest
    "back" -> R.string.ses_group_back
    "shoulders" -> R.string.ses_group_shoulders
    "arms" -> R.string.ses_group_arms
    "legs" -> R.string.ses_group_legs
    "core" -> R.string.ses_group_core
    "cardio" -> R.string.ses_group_cardio
    else -> R.string.ses_group_other
}

fun SpecialWorkoutsLoader.load(context: Context): SpecialWorkoutsData = load()

fun BuiltinPlans.all(context: Context): List<TrainingPlan> = all()

fun BuiltinPlans.byId(context: Context, id: String): TrainingPlan? = byId(id)

fun ExerciseGuides.forName(context: Context, name: String): ExerciseGuide? = forNameConCatalogo(name)

/** Lee el idioma resuelto de [context] (el ya envuelto con el de la app). */
fun Contenido.sync(context: Context) =
    sync(runCatching { context.getString(R.string.idioma_codigo) == "en" }.getOrDefault(false))

private fun dibujo(context: Context, slug: String, female: Boolean): Int =
    context.resources.getIdentifier(ExerciseImages.nombreDibujo(slug, female), "drawable", context.packageName)

/**
 * Drawable de la ilustración del ejercicio para el género indicado (femenino si [female],
 * masculino en caso contrario), o null si el ejercicio no tiene imagen.
 */
fun ExerciseImages.forName(context: Context, rawName: String, female: Boolean): Int? {
    slugPropio(rawName)?.let { slug ->
        val id = dibujo(context, slug, female)
        if (id != 0) return id
    }
    // Un compuesto no tiene imagen propia. Vale la del primer movimiento para las
    // miniaturas de 64 dp (la lista del día), donde no cabe nada más; donde hay sitio se
    // pintan todos con [circuitMoves], que para eso el circuito son tres cosas.
    return circuitMoves(context, rawName, female).firstOrNull()?.drawable
}

/**
 * Movimientos de un ejercicio compuesto ya resueltos a drawable, en orden y solo los que
 * tienen ilustración. Vacío si el nombre no es un compuesto conocido.
 */
fun ExerciseImages.circuitMoves(context: Context, rawName: String, female: Boolean): List<ExerciseImages.ResolvedMove> =
    movimientos(rawName).mapNotNull { move ->
        val id = dibujo(context, move.slug, female)
        if (id != 0) ExerciseImages.ResolvedMove(move.label, move.dose, id) else null
    }
