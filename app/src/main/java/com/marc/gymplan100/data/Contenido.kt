package com.marc.gymplan100.data

import android.content.Context
import com.marc.gymplan100.R
import kotlinx.serialization.json.Json

/**
 * El contenido de serie (nombres de ejercicio, planes, fichas, entrenamientos especiales) en
 * el idioma de la app.
 *
 * Todo ese contenido está escrito en castellano y **el castellano sigue siendo la clave**: el
 * progreso, los pesos, los dibujos, los alias y las sesiones a medias guardan «Prensa de
 * piernas», no «Leg press». Traducir los datos rompería el historial de quien ya entrena, así
 * que aquí solo se traduce al pintar: [t] recibe el texto tal y como está en el plan y
 * devuelve lo que se enseña. En castellano devuelve lo mismo.
 *
 * Las traducciones viven en `assets/en/contenido.json` (castellano → inglés), fichas del
 * catálogo incluidas: las fichas se siguen leyendo en castellano porque las alternativas se
 * enlazan con el catálogo por sus palabras, y se traducen al pintar. Lo que no está en el
 * diccionario —un plan que se ha escrito el usuario, un ejercicio suyo— sale como lo escribió.
 *
 * Es un objeto global, como [ExerciseAliases], porque lo consultan sitios sin contexto. Se
 * pone en hora con [sync] cada vez que se envuelve un contexto con el idioma de la app.
 */
object Contenido {

    @Volatile
    var ingles: Boolean = false
        private set

    @Volatile
    private var dic: Map<String, String>? = null

    private val json = Json { ignoreUnknownKeys = true }

    /** Lee el idioma resuelto de [context] (el ya envuelto con el de la app). */
    fun sync(context: Context) {
        val en = runCatching { context.getString(R.string.idioma_codigo) == "en" }.getOrDefault(false)
        if (en) cargar(context)
        ingles = en
    }

    private fun cargar(context: Context) {
        if (dic != null) return
        synchronized(this) {
            if (dic != null) return
            fun leer(fichero: String): Map<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(
                    context.applicationContext.assets.open(fichero).bufferedReader().use { it.readText() }
                )
            }.getOrElse { emptyMap() }
            inverso = leer("en/inverso.json").mapKeys { it.key.lowercase() }
            dic = leer("en/contenido.json")
        }
    }

    /** Para los tests, que no tienen assets. */
    fun set(ingles: Boolean, dic: Map<String, String>, inverso: Map<String, String> = emptyMap()) {
        this.dic = dic
        this.inverso = inverso.mapKeys { it.key.lowercase() }
        this.ingles = ingles
    }

    /** ¿Hay traducción de [s]? Para el test de cobertura. */
    fun traducible(s: String): Boolean = dic?.containsKey(s.trim()) == true || patron(s.trim()) != null

    /**
     * El texto de contenido en el idioma de la app. Vale para nombres de ejercicio, de plan y
     * de fase, títulos, calentamientos, notas… En castellano, o si no hay traducción, el
     * mismo texto.
     */
    fun t(s: String): String {
        if (!ingles || s.isBlank()) return s
        val limpio = s.trim()
        return dic?.get(limpio) ?: patron(limpio) ?: s
    }

    @Volatile
    private var inverso: Map<String, String>? = null

    /**
     * La clave en castellano de un nombre de ejercicio que se enseña traducido, para lo que el
     * usuario escribe en un campo pintado en inglés (el entreno libre, el editor de planes):
     * «Leg press» se guarda como «Prensa de piernas» y cuenta para su historial. Lo que no es
     * una traducción conocida se guarda tal cual.
     *
     * Sale de `assets/en/inverso.json` y no de dar la vuelta al diccionario: hay claves que
     * comparten traducción («Remo sentado en polea» y «Remo en polea sentado» son «Seated
     * cable row»), y de las dos tiene que ganar la que usan los planes, que es la que tiene
     * historial.
     */
    fun clave(texto: String): String {
        if (!ingles || texto.isBlank()) return texto
        // Sin distinguir mayúsculas: «leg press» también es «Prensa de piernas».
        return inverso?.get(texto.trim().lowercase()) ?: texto
    }

    /** Lo que se genera al leer un plan («Día 3», «Semanas 1-4»…), que no está en ningún fichero. */
    private fun patron(s: String): String? {
        Regex("""Día (\d+)""").matchEntire(s)?.let { return "Day ${it.groupValues[1]}" }
        Regex("""Días (\d+) al (\d+)""").matchEntire(s)?.let {
            return "Days ${it.groupValues[1]} to ${it.groupValues[2]}"
        }
        Regex("""Semana (\d+)""").matchEntire(s)?.let { return "Week ${it.groupValues[1]}" }
        Regex("""Semanas (\d+)-(\d+)""").matchEntire(s)?.let {
            return "Weeks ${it.groupValues[1]}-${it.groupValues[2]}"
        }
        return null
    }

    private val reglasEsquema: List<Pair<Regex, String>> = listOf(
        Regex("""(?i)\bpor lado\b""") to "per side",
        Regex("""(?i)\bpor pierna\b""") to "per leg",
        Regex("""(?i)\bpor brazo\b""") to "per arm",
        Regex("""(?i)\bde cada\b""") to "each",
        Regex("""(?i)\bm[áa]ximo tiempo\b""") to "max time",
        Regex("""(?i)\bm[áa]ximo posible\b""") to "max reps",
        Regex("""(?i)\bvueltas\b""") to "rounds",
        Regex("""(?i)\bvuelta\b""") to "round",
        Regex("""(?i)\bseries\b""") to "sets",
        Regex("""(?i)\bserie\b""") to "set",
        Regex("""(?i)\btrabajo\b""") to "work",
        Regex("""(?i)\bdescanso\b""") to "rest",
        Regex("""(?i)\bpasos\b""") to "steps",
        Regex("""(?i)\balternando\b""") to "alternating",
    )

    /**
     * El esquema («3 x 12 por lado», «4 vueltas») en el idioma de la app. El del plan se queda
     * como está porque el motor de la sesión lo lee para contar series y lados.
     */
    fun esquema(s: String): String {
        if (!ingles || s.isBlank()) return s
        dic?.get(s.trim())?.let { return it }
        return reglasEsquema.fold(s) { acc, (re, en) -> re.replace(acc, en) }
    }

    /** Una ficha con sus textos traducidos, solo para pintarla. */
    fun ficha(g: ExerciseGuide): ExerciseGuide {
        if (!ingles) return g
        return ExerciseGuide(
            muscles = t(g.muscles),
            howTo = t(g.howTo),
            mistakes = g.mistakes.map(::t),
            alternatives = g.alternatives.map(::t),
            videoQuery = t(g.videoQuery)
        )
    }
}
