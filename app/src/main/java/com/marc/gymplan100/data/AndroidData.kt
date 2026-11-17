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


/** Lee el idioma resuelto de [context] (el ya envuelto con el de la app). */
fun Contenido.sync(context: Context) =
    sync(runCatching { context.getString(R.string.idioma_codigo) == "en" }.getOrDefault(false))

