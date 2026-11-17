package com.marc.gymplan100.ui.theme

import com.marc.gymplan100.data.PlatformContext
import com.marc.gymplan100.data.preferencias

/** En qué idioma se ve la app: el del móvil, o uno fijo. */
enum class LanguageMode(val id: String, val tag: String?) {
    SISTEMA("sistema", null),
    ES("es", "es"),
    EN("en", "en");

    companion object {
        fun from(id: String?): LanguageMode = entries.firstOrNull { it.id == id } ?: SISTEMA
    }
}

/**
 * Idioma de la app: qué ha elegido el usuario. Cómo se aplica depende de la plataforma
 * (en Android, envolviendo el contexto: ver AppLanguage.wrap en :app).
 */
object AppLanguage {

    const val PREFS = "gym_idioma"
    const val KEY = "idioma"

    fun mode(context: PlatformContext): LanguageMode =
        LanguageMode.from(context.preferencias(PREFS).getString(KEY))

    /** Guarda el idioma. Quien llama recrea la pantalla para que se vea. */
    fun set(context: PlatformContext, nuevo: LanguageMode) {
        // síncrono: justo después se recrea la actividad y lo vuelve a leer
        context.preferencias(PREFS).editar(sincrono = true) { putString(KEY, nuevo.id) }
    }

    /** El idioma elegido, para la copia de seguridad. */
    fun export(context: PlatformContext): Map<String, String> = mapOf(KEY to mode(context).id)

    /** Restaura el idioma de una copia. Sin dato, no se toca. Devuelve si ha cambiado. */
    fun import(context: PlatformContext, values: Map<String, String>): Boolean {
        val id = values[KEY] ?: return false
        val nuevo = LanguageMode.from(id)
        if (nuevo == mode(context)) return false
        set(context, nuevo)
        return true
    }
}
