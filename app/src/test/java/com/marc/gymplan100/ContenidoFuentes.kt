package com.marc.gymplan100

import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.CatalogEntry
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.ExerciseGuide
import com.marc.gymplan100.data.ExerciseGuides
import com.marc.gymplan100.data.ExerciseImages
import com.marc.gymplan100.data.MuscleTargets
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.TrainingPlan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * Todos los textos de contenido de serie que se enseñan en castellano y deben salir en inglés
 * con la app en inglés: nombres de ejercicio, textos de los planes, fichas escritas a mano y
 * entrenamientos especiales. Los esquemas («3 x 12 por lado») van aparte: se traducen con
 * reglas, no con diccionario.
 *
 * Lo usa el test de cobertura. Las fichas de `assets/fichas.json` se traducen con el mismo
 * diccionario y el test las recorre aparte.
 */
object ContenidoFuentes {

    val assets = File("src/main/assets")

    private val json = Json { ignoreUnknownKeys = true }

    fun catalogo(): List<CatalogEntry> =
        json.decodeFromString(File(assets, "catalogo.json").readText())

    fun planesDeSerie(): List<TrainingPlan> {
        val ids = PlanCodec.decodeIdList(File(assets, "planes/index.json").readText())
        return ids.map { id ->
            val ok = PlanCodec.parseJson(File(assets, "planes/$id.json").readText(), now = 0L)
            (ok as PlanImport.Ok).plan
        } + BuiltinPlan.plan
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> privado(obj: Any, campo: String): T {
        val f = obj.javaClass.getDeclaredField(campo)
        f.isAccessible = true
        return f.get(obj) as T
    }

    fun fichasAMano(): Map<String, ExerciseGuide> = privado(ExerciseGuides, "map")

    /** Nombres de ejercicio de serie (las claves con las que se guarda el progreso). */
    fun nombres(): Set<String> {
        ExerciseCatalog.set(catalogo())
        val out = linkedSetOf<String>()
        out += ExerciseCatalog.all.map { it.nombre }
        out += privado<Map<String, String>>(ExerciseImages, "nameToSlug").keys
        val compuestos: Map<String, List<ExerciseImages.CompoundMove>> =
            privado(ExerciseImages, "nameToCompoundMoves")
        out += compuestos.keys
        out += compuestos.values.flatten().map { it.label }
        out += fichasAMano().keys
        out += privado<Map<String, Any>>(MuscleTargets, "map").keys
        planesDeSerie().forEach { p -> out += p.exerciseNames }
        especiales(soloNombres = true).forEach { out += it }
        return out.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /** Nombres de los pasos y ejercicios de los entrenamientos especiales. */
    fun nombresDeEspeciales(): Set<String> = especiales(soloNombres = true).map { it.trim() }.toSet()

    /** Textos de contenido que no son nombres de ejercicio ni esquemas. */
    fun textos(): Set<String> {
        val out = linkedSetOf<String>()
        planesDeSerie().forEach { p ->
            out += p.name
            out += p.description
            p.phases.forEach { f ->
                out += f.name
                out += f.description
                out += f.progression
                f.templates.forEach { t ->
                    out += t.weekday
                    out += t.title
                    out += t.warmup
                    out += t.cooldown
                    t.exercises.forEach { e -> out += e.note }
                }
            }
        }
        fichasAMano().values.forEach { g ->
            out += g.muscles
            out += g.howTo
            out += g.mistakes
            out += g.alternatives
            out += g.videoQuery
        }
        val compuestos: Map<String, List<ExerciseImages.CompoundMove>> =
            privado(ExerciseImages, "nameToCompoundMoves")
        compuestos.values.flatten().mapNotNull { it.dose }.forEach { out += it }
        out += especiales(soloNombres = false)
        return out.map { it.trim() }.filter { it.isNotEmpty() && it.any(Char::isLetter) }.toSet() - nombres()
    }

    fun esquemas(): Set<String> =
        planesDeSerie().flatMap { p -> p.days.flatMap { d -> d.template.exercises.map { it.scheme } } }.toSet()

    private val camposNombre = setOf("nombre")
    private val camposTexto = setOf(
        "descripcion", "progresion", "aviso_frecuencia", "nota", "notas_forma", "condicion",
        "progresiones", "reps"
    )

    private fun especiales(soloNombres: Boolean): List<String> {
        val raiz = Json.parseToJsonElement(File(assets, "entrenamientos_especiales.json").readText())
        val rutinas = (raiz as JsonObject)["rutinas"] ?: return emptyList()
        val out = mutableListOf<String>()
        fun walk(e: JsonElement, clave: String?) {
            when (e) {
                is JsonObject -> e.forEach { (k, v) -> walk(v, k) }
                is JsonArray -> e.forEach { walk(it, clave) }
                is JsonPrimitive -> if (e.isString) {
                    val campos = if (soloNombres) camposNombre else camposTexto
                    if (clave in campos) out += e.content
                }
            }
        }
        walk(rutinas, null)
        return out
    }
}
