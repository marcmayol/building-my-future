package com.marc.gymplan100.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marc.gymplan100.R

/** Claro, oscuro, o lo que diga el móvil. La etiqueta es un recurso: la resuelve la pantalla. */
enum class ThemeMode(val id: String, @StringRes val label: Int) {
    SISTEMA("sistema", R.string.ini_theme_system),
    CLARO("claro", R.string.ini_theme_light),
    OSCURO("oscuro", R.string.ini_theme_dark);

    companion object {
        fun from(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SISTEMA
    }
}

/**
 * Qué tema usa la app.
 *
 * Hasta ahora seguía al del sistema y punto, que está bien hasta el día que entrenas de noche
 * con el móvil en claro, o al revés. Ahora se puede fijar.
 *
 * Se guarda en SharedPreferences y no en el DataStore del perfil por una razón concreta: hay
 * que saber el tema **antes** de pintar el primer fotograma. Leerlo de un flujo asíncrono
 * significaría arrancar con el tema del sistema y cambiar a mitad de arranque, y ese parpadeo
 * blanco en un gimnasio a oscuras se nota más que cualquier ajuste.
 *
 * El valor vive en un estado de Compose para que cambiarlo en Ajustes repinte la app entera al
 * instante, sin reiniciar ni volver atrás.
 */
object AppTheme {

    private const val PREFS = "gym_tema"
    private const val KEY = "modo"

    var mode by mutableStateOf(ThemeMode.SISTEMA)
        private set

    /** Lee el tema guardado. Se llama al arrancar, antes de pintar nada. */
    fun load(context: Context) {
        mode = ThemeMode.from(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        )
    }

    /** Cambia el tema y lo deja guardado. La app se repinta sola. */
    fun set(context: Context, nuevo: ThemeMode) {
        mode = nuevo
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, nuevo.id)
            .apply()
    }

    /** El tema elegido, para la copia de seguridad. */
    fun export(): Map<String, String> = mapOf(KEY to mode.id)

    /** Restaura el tema de una copia. Sin dato, se vuelve al del móvil. */
    fun import(context: Context, values: Map<String, String>) {
        set(context, ThemeMode.from(values[KEY]))
    }

    /** Si toca pintar en oscuro, mirando el sistema solo cuando no hay elección propia. */
    @Composable
    fun isDark(): Boolean = when (mode) {
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
    }
}
