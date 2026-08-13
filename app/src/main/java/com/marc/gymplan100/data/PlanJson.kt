@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.marc.gymplan100.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames

/**
 * Formato de intercambio de un plan.
 *
 * Es a la vez el formato que se importa y el que se guarda en el móvil, así que un plan
 * siempre pasa por la misma validación: da igual que venga de un archivo, del parser de
 * Markdown o del editor de la app.
 *
 * Los nombres van en español (como `entrenamientos_especiales.json`), pero se aceptan también
 * en inglés porque estos JSON se escriben a mano o se le piden a una IA y ahí el inglés se
 * cuela solo.
 */
@Serializable
data class PlanDto(
    /**
     * Identificador estable. Si el archivo trae uno y ya existe un plan con ese id, la
     * importación LO SUSTITUYE conservando su progreso: así se corrige un plan sin perder
     * los días hechos. Sin id, cada importación crea un plan nuevo y vacío de progreso.
     */
    val id: String? = null,
    @JsonNames("name", "titulo")
    val nombre: String = "",
    @JsonNames("description")
    val descripcion: String = "",
    @JsonNames("phases")
    val fases: List<FaseDto> = emptyList(),
    /** Plan sin fases: los días cuelgan directamente de aquí. */
    @JsonNames("days", "entrenamientos")
    val dias: List<DiaDto> = emptyList(),
    /** "json", "markdown" o "editor". Solo informativo. */
    val origen: String = "json",
    val importadoEn: Long = 0L
)

@Serializable
data class FaseDto(
    @JsonNames("name", "titulo")
    val nombre: String = "",
    /** Cuántas semanas se repiten los días de esta fase. */
    @JsonNames("weeks")
    val semanas: Int = 1,
    @JsonNames("description")
    val descripcion: String = "",
    @JsonNames("progression")
    val progresion: String = "",
    @JsonNames("days", "entrenamientos", "plantillas")
    val dias: List<DiaDto> = emptyList()
)

@Serializable
data class DiaDto(
    /** Cómo se llama el día ("Lunes", "Día A"…). Si falta, se numera solo. */
    @JsonNames("weekday", "nombre")
    val dia: String = "",
    @JsonNames("title")
    val titulo: String = "",
    @JsonNames("warmup")
    val calentamiento: String = "",
    @JsonNames("cooldown", "vuelta_a_la_calma")
    val estiramientos: String = "",
    @JsonNames("exercises")
    val ejercicios: List<EjercicioDto> = emptyList()
)

@Serializable
data class EjercicioDto(
    @JsonNames("name")
    val nombre: String = "",
    /** Series y repeticiones en texto: "4 x 8", "3 x 30 s", "3 vueltas". */
    @JsonNames("scheme", "series")
    val esquema: String = "",
    @JsonNames("note")
    val nota: String = ""
)

/** Resultado de leer un plan: o sale un plan usable, o un motivo concreto por el que no. */
sealed interface PlanImport {
    data class Ok(val dto: PlanDto, val plan: TrainingPlan) : PlanImport
    data class Error(val message: String) : PlanImport
}

/**
 * Lee y valida planes. Es deliberadamente estricto con lo que rompería un entrenamiento
 * (un día sin ejercicios, un ejercicio sin series) y tolerante con lo cosmético (un día sin
 * título se numera solo), porque un error a media sesión en el gimnasio no tiene arreglo.
 */
object PlanCodec {

    /** Tope de sanidad: un "semanas: 9999" no debe generar un plan de miles de días. */
    private const val MAX_DAYS = 500
    private const val MAX_WEEKS = 52
    private const val MAX_EXERCISES = 30

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * ¿Esto es un archivo de texto? El selector del sistema enseña todos los archivos (filtrar
     * por tipo deja en gris los .md y los .json que el gestor no etiqueta bien), así que se
     * puede elegir una foto por error. Decodificada como texto, una foto es ruido: sin esta
     * comprobación el usuario recibiría un "falta el nombre del plan" que no le dice nada.
     */
    fun isProbablyText(text: String): Boolean {
        val sample = text.take(2000)
        if (sample.isBlank()) return false
        val noise = sample.count { c ->
            c == '�' || (c.isISOControl() && c != '\n' && c != '\r' && c != '\t')
        }
        return noise * 100 / sample.length < 2
    }

    /** Lee el texto de un archivo .json y lo convierte en plan. */
    fun parseJson(text: String, now: Long = System.currentTimeMillis()): PlanImport {
        if (text.isBlank()) return PlanImport.Error("El archivo está vacío.")
        val dto = runCatching { json.decodeFromString<PlanDto>(text) }.getOrElse {
            return PlanImport.Error(
                "El archivo no es un JSON válido. Revisa que no falte una coma o una comilla."
            )
        }
        return fromDto(dto, now)
    }

    /** Serializa un plan ya validado para guardarlo o exportarlo. */
    fun toJson(dto: PlanDto): String = json.encodeToString(dto)

    fun decodeStored(text: String): PlanDto? =
        runCatching { json.decodeFromString<PlanDto>(text) }.getOrNull()

    /** Lista de ids (el índice de los planes de serie en assets). */
    fun decodeIdList(text: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(text) }.getOrElse { emptyList() }

    /**
     * Convierte y valida. Las fases son opcionales: si el plan trae días sueltos, se envuelven
     * en una fase única para que las pantallas de fases, estadísticas y logros sigan valiendo.
     */
    fun fromDto(dto: PlanDto, now: Long = System.currentTimeMillis()): PlanImport {
        val name = dto.nombre.trim()
        if (name.isEmpty()) return PlanImport.Error("El plan no tiene nombre.")

        val rawPhases = when {
            dto.fases.isNotEmpty() -> dto.fases
            dto.dias.isNotEmpty() -> listOf(FaseDto(nombre = name, semanas = 1, dias = dto.dias))
            else -> return PlanImport.Error("El plan no tiene ningún día de entrenamiento.")
        }

        val phases = mutableListOf<Phase>()
        var dayCursor = 1
        var weekCursor = 1
        rawPhases.forEachIndexed { index, fase ->
            val number = index + 1
            val label = fase.nombre.trim().ifEmpty { "Fase $number" }
            if (fase.dias.isEmpty()) {
                return PlanImport.Error("La fase «$label» no tiene ningún día.")
            }
            val weeks = fase.semanas.coerceIn(1, MAX_WEEKS)

            val templates = mutableListOf<WorkoutTemplate>()
            fase.dias.forEachIndexed { dayIndex, dia ->
                val dayLabel = dia.dia.trim().ifEmpty { "Día ${dayIndex + 1}" }
                if (dia.ejercicios.isEmpty()) {
                    return PlanImport.Error("«$dayLabel» de la fase «$label» no tiene ejercicios.")
                }
                if (dia.ejercicios.size > MAX_EXERCISES) {
                    return PlanImport.Error(
                        "«$dayLabel» tiene ${dia.ejercicios.size} ejercicios; el máximo es $MAX_EXERCISES."
                    )
                }
                val exercises = dia.ejercicios.map { ex ->
                    val exName = ex.nombre.trim()
                    if (exName.isEmpty()) {
                        return PlanImport.Error("Hay un ejercicio sin nombre en «$dayLabel».")
                    }
                    val scheme = ex.esquema.trim()
                    if (scheme.isEmpty()) {
                        return PlanImport.Error(
                            "«$exName» ($dayLabel) no dice las series. Escríbelas como \"4 x 8\", " +
                                "\"3 x 30 s\" o \"3 vueltas\"."
                        )
                    }
                    Exercise(name = exName, scheme = scheme, note = ex.nota.trim())
                }
                templates.add(
                    WorkoutTemplate(
                        weekday = dayLabel,
                        title = dia.titulo.trim().ifEmpty { dayLabel },
                        warmup = dia.calentamiento.trim().ifEmpty { "6 min de calentamiento" },
                        exercises = exercises,
                        cooldown = dia.estiramientos.trim().ifEmpty { "Estiramientos: 5 min" }
                    )
                )
            }

            val daysInPhase = templates.size * weeks
            val lastDay = dayCursor + daysInPhase - 1
            val lastWeek = weekCursor + weeks - 1
            phases.add(
                Phase(
                    number = number,
                    name = label,
                    range = if (daysInPhase == 1) "Día $dayCursor" else "Días $dayCursor al $lastDay",
                    weeks = if (weeks == 1) "Semana $weekCursor" else "Semanas $weekCursor-$lastWeek",
                    weeksCount = weeks,
                    description = fase.descripcion.trim(),
                    progression = fase.progresion.trim(),
                    templates = templates
                )
            )
            dayCursor = lastDay + 1
            weekCursor = lastWeek + 1
        }

        val total = dayCursor - 1
        if (total > MAX_DAYS) {
            return PlanImport.Error("El plan sale de $total días y el máximo son $MAX_DAYS.")
        }

        val id = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "plan-$now"
        if (id == BuiltinPlan.ID) {
            return PlanImport.Error("Ese id está reservado para el plan que viene con la app.")
        }
        val source = when (dto.origen.lowercase()) {
            "markdown", "md" -> PlanSource.MARKDOWN
            "editor" -> PlanSource.EDITOR
            else -> PlanSource.JSON
        }
        val importedAt = dto.importadoEn.takeIf { it > 0L } ?: now
        return PlanImport.Ok(
            dto = dto.copy(id = id, nombre = name, importadoEn = importedAt),
            plan = TrainingPlan(
                id = id,
                name = name,
                description = dto.descripcion.trim(),
                phases = phases,
                builtin = false,
                source = source,
                importedAt = importedAt
            )
        )
    }
}
