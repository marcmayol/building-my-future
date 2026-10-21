package com.marc.gymplan100.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** Un ejercicio del catálogo de exercise-api, tal y como viene en `assets/catalogo.json`. */
@Serializable
data class CatalogEntry(
    val slug: String,
    val nombre: String,
    /** chest, back, shoulders, arms, legs, core, cardio. */
    val grupo: String,
    /** machine, cable, barbell, dumbbell, bodyweight, cardio. */
    val material: String = "",
    val principales: List<String> = emptyList(),
    val secundarios: List<String> = emptyList()
)

/**
 * El catálogo de ejercicios con dibujo y ficha (los 151 de marcmayol.com/exercise-api).
 *
 * Sirve para dos cosas: ofrecer con qué cambiar un ejercicio cuando haces otro («no tengo pec
 * deck, he hecho aperturas con mancuernas») y que lo que elijas tenga dibujo, ficha y músculos
 * aunque ningún plan lo nombre. Se carga una vez al arrancar ([load]) porque lo consultan
 * objetos sin contexto, como [MuscleTargets].
 */
object ExerciseCatalog {

    @Volatile
    private var entries: List<CatalogEntry> = emptyList()

    @Volatile
    private var byName: Map<String, CatalogEntry> = emptyMap()

    fun load(context: Context) {
        if (entries.isNotEmpty()) return
        val leido = runCatching {
            val raw = context.applicationContext.assets.open("catalogo.json")
                .bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<List<CatalogEntry>>(raw)
        }.getOrElse { emptyList() }
        set(leido)
    }

    /** Para los tests, que no tienen assets. */
    fun set(values: List<CatalogEntry>) {
        entries = values
        byName = values.associateBy { it.nombre }
    }

    val all: List<CatalogEntry> get() = entries

    fun forName(name: String): CatalogEntry? = byName[name.trim()]

    fun forSlug(slug: String): CatalogEntry? = entries.firstOrNull { it.slug == slug }

    /** Nombre del grupo en español, para los títulos de sección. */
    fun groupLabel(grupo: String): String = when (grupo) {
        "chest" -> "Pecho"
        "back" -> "Espalda"
        "shoulders" -> "Hombros"
        "arms" -> "Brazos"
        "legs" -> "Piernas"
        "core" -> "Core"
        "cardio" -> "Cardio"
        else -> "Otros"
    }

    /**
     * El ejercicio del catálogo al que se refiere una alternativa escrita en una ficha, o null
     * si la frase no es un ejercicio («Hazlas sentado para más estabilidad»).
     *
     * Las alternativas están escritas para leerlas, no para enlazarlas: «Aperturas con
     * mancuernas en banco», «Dominada asistida en máquina o con goma». Se busca el ejercicio
     * del catálogo cuyas palabras estén TODAS en la frase y, entre los que cumplan, el de más
     * palabras: «Press de banca con mancuernas» gana a «Press de banca» si la frase habla de
     * mancuernas. Las palabras se comparan sin tildes, sin plurales y sin la vocal final
     * («banco» y «banca», «polea» y «poleas» son la misma), que es como varían en las fichas.
     */
    fun matchAlternative(text: String): CatalogEntry? {
        val frase = tokens(text)
        if (frase.isEmpty()) return null
        return entries
            .map { it to tokens(it.nombre.substringBefore(" (")) }
            .filter { (_, palabras) -> palabras.isNotEmpty() && frase.containsAll(palabras) }
            .maxWithOrNull(compareBy<Pair<CatalogEntry, Set<String>>> { it.second.size }
                .thenBy { -it.first.nombre.length })
            ?.first
    }

    private val vacias = setOf(
        "de", "del", "la", "el", "los", "las", "en", "con", "a", "al", "o", "y", "u", "un", "una",
        "si", "sin", "por", "para", "que", "se", "tu", "lo", "mas", "muy", "hay", "no", "es"
    )

    internal fun tokens(text: String): Set<String> =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .split(Regex("""[^a-z0-9]+"""))
            .filter { it.isNotBlank() && it !in vacias }
            .map { raiz(it) }
            .toSet()

    private fun raiz(palabra: String): String {
        var p = palabra
        if (p.length > 3 && p.endsWith("s")) p = p.dropLast(1)
        if (p.length > 3 && p.last() in "aeo") p = p.dropLast(1)
        return p
    }
}
