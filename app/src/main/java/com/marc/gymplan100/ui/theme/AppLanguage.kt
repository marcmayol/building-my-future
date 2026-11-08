package com.marc.gymplan100.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

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
 * Idioma de la app.
 *
 * Los textos viven en `res/values` (inglés, el que sale por defecto) y `res/values-es`
 * (español). En automático manda el móvil: cualquier variante de español ve la app en
 * español y cualquier otro idioma (catalán incluido) en inglés, que es lo que pidió Marc.
 *
 * Elegir uno fijo no pasa por las APIs de idioma por app de Android 13 ni por AppCompat: la
 * actividad es una ComponentActivity y la app arranca en Android 8. Se guarda en
 * SharedPreferences, como el tema, porque hay que saberlo antes de crear la actividad, y
 * se aplica envolviendo el contexto ([wrap]). Lo que se pinta fuera de la actividad
 * (notificaciones, el aviso de descanso, el reloj) pide sus textos a [wrap] también, porque
 * el contexto de la aplicación no se entera del cambio hasta que muere el proceso.
 */
object AppLanguage {

    private const val PREFS = "gym_idioma"
    private const val KEY = "idioma"

    fun mode(context: Context): LanguageMode = LanguageMode.from(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    )

    /** Guarda el idioma. Quien llama recrea la actividad para que se vea. */
    fun set(context: Context, nuevo: LanguageMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, nuevo.id)
            .commit() // síncrono: justo después se recrea la actividad y lo vuelve a leer
    }

    /**
     * El contexto con el idioma elegido. En automático devuelve el mismo, que ya sigue al
     * sistema.
     */
    fun wrap(context: Context): Context {
        val tag = mode(context).tag ?: return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    /** El idioma con el que se está pintando: para fechas y números escritos. */
    fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    /** El idioma elegido, para la copia de seguridad. */
    fun export(context: Context): Map<String, String> = mapOf(KEY to mode(context).id)

    /** Restaura el idioma de una copia. Sin dato, no se toca. Devuelve si ha cambiado. */
    fun import(context: Context, values: Map<String, String>): Boolean {
        val id = values[KEY] ?: return false
        val nuevo = LanguageMode.from(id)
        if (nuevo == mode(context)) return false
        set(context, nuevo)
        return true
    }
}
