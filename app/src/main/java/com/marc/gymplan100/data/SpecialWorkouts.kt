package com.marc.gymplan100.data

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Modelos tipados de `assets/entrenamientos_especiales.json`: las dos rutinas nuevas de la
 * sección de entrenamientos especiales (Rutina Militar y Rutina Quema Grasa), con sus pasos,
 * ejercicios, protocolos, frecuencias y textos de aviso.
 *
 * No hay base de datos (la app persiste con DataStore), así que la definición se carga como
 * asset y se parsea a estos modelos. Algunos campos del JSON son "flexibles" (un número o un
 * texto, p. ej. `reps: 15` o `reps: "AMRAP"`): se leen con [FlexString].
 */

/** Lee un campo JSON que puede venir como número o como texto y lo entrega siempre como String. */
object FlexString : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return (element as? JsonPrimitive)?.content ?: element.toString()
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

@Serializable
data class FrecuenciaSemanal(
    val min: Int = 0,
    val max: Int = 0,
    val fuente: String = ""
)

/** Límite de sesiones por día (rutinas de periodicidad diaria, p. ej. Altura y Postura). */
@Serializable
data class FrecuenciaDiaria(val max: Int = 0)

/** Rango de segundos (mín/máx) usado para descansos configurables. */
@Serializable
data class RangoSeg(val min: Int = 0, val max: Int = 0)

/** Alternativa que el usuario puede elegir para un paso (p. ej. jumping jacks en vez de burpees). */
@Serializable
data class Alternativa(
    val nombre: String = "",
    val tipo: String = "",
    val duracion_seg: Int = 0,
    val condicion: String = ""
)

/**
 * Un paso de una secuencia fija (Rutina Militar o Rutina Altura y Postura). Es "tiempo" o
 * "repeticiones". La militar no usa series (1 por paso); la de altura tiene 2-3 series con
 * descanso entre ellas y rangos de repeticiones.
 */
@Serializable
data class PasoMilitar(
    val orden: Int = 0,
    val nombre: String = "",
    val tipo: String = "",
    val duracion_seg: Int = 0,
    val duracion_seg_min: Int = 0,
    val duracion_seg_max: Int = 0,
    @Serializable(with = FlexString::class) val reps: String = "",
    val reps_min: Int = 0,
    val reps_max: Int = 0,
    val series_min: Int = 0,
    val series_max: Int = 0,
    val descanso_entre_series_seg: Int = 0,
    val descanso_entre_series_seg_min: Int = 0,
    val descanso_entre_series_seg_max: Int = 0,
    val registro: String = "",
    val nota: String = "",
    val notas_forma: String = "",
    val alternativa: Alternativa? = null
) {
    val esTiempo: Boolean get() = tipo == "tiempo"

    /** Nº de series del paso: el máximo del rango (objetivo), o 1 si no hay series (militar). */
    val numSeries: Int
        get() = when {
            series_max > 0 -> series_max
            series_min > 0 -> series_min
            else -> 1
        }

    /** Mínimo de series recomendado (a partir de aquí se puede terminar el ejercicio antes). */
    val minSeries: Int get() = if (series_min > 0) series_min else numSeries

    /** Descanso entre series (segundos): valor exacto o mínimo del rango; 0 si no hay. */
    val descansoEntreSeriesSeg: Int
        get() = when {
            descanso_entre_series_seg > 0 -> descanso_entre_series_seg
            descanso_entre_series_seg_min > 0 -> descanso_entre_series_seg_min
            else -> 0
        }

    /** Objetivo de tiempo del paso: el valor exacto o el mínimo de un rango. 0 si no aplica. */
    val objetivoSeg: Int
        get() = when {
            duracion_seg > 0 -> duracion_seg
            duracion_seg_min > 0 -> duracion_seg_min
            else -> 0
        }

    /** Etiqueta legible del objetivo de tiempo (para rangos muestra "30-45 s"). */
    val etiquetaTiempo: String
        get() = when {
            duracion_seg > 0 -> "$duracion_seg s"
            duracion_seg_min > 0 && duracion_seg_max > 0 -> "$duracion_seg_min-$duracion_seg_max s"
            duracion_seg_min > 0 -> "$duracion_seg_min s"
            else -> ""
        }

    /** Repeticiones objetivo como texto: valor fijo ("15"/"AMRAP") o rango ("10-15"). */
    val repsObjetivo: String
        get() = when {
            reps.isNotBlank() -> reps
            reps_min > 0 && reps_max > 0 -> "$reps_min-$reps_max"
            reps_min > 0 -> "$reps_min"
            else -> ""
        }

    /** Notas a mostrar durante la ejecución: prioriza notas_forma (altura) sobre nota (militar). */
    val notas: String get() = notas_forma.ifBlank { nota }
}

/** Un protocolo de ejecución de un ejercicio de quema grasa (Tabata, intervalos, series…). */
@Serializable
data class Protocolo(
    val nombre: String = "",
    val trabajo_seg: Int = 0,
    val trabajo_seg_min: Int = 0,
    val trabajo_seg_max: Int = 0,
    val descanso_seg: Int = 0,
    val descanso_seg_min: Int = 0,
    val descanso_seg_max: Int = 0,
    val rondas: Int = 0,
    val rondas_min: Int = 0,
    val rondas_max: Int = 0,
    val series: Int = 0,
    val series_min: Int = 0,
    val series_max: Int = 0,
    @Serializable(with = FlexString::class) val reps: String = "",
    val reps_min: Int = 0,
    val reps_max: Int = 0,
    val duracion_inicial_seg: Int = 0,
    val progresion: String = "",
    val esfuerzo_pct: String = "",
    @Serializable(with = FlexString::class) val duracion_total_min: String = "",
    val nota: String = ""
) {
    val trabajoSeg: Int
        get() = when {
            trabajo_seg > 0 -> trabajo_seg
            trabajo_seg_min > 0 -> trabajo_seg_min
            else -> 0
        }

    val descansoSeg: Int
        get() = when {
            descanso_seg > 0 -> descanso_seg
            descanso_seg_min > 0 -> descanso_seg_min
            else -> 0
        }

    val numRondas: Int
        get() = when {
            rondas > 0 -> rondas
            rondas_min > 0 -> rondas_min
            else -> 0
        }

    val numSeries: Int
        get() = when {
            series > 0 -> series
            series_min > 0 -> series_min
            else -> 0
        }

    /** Repeticiones objetivo como texto (número, rango "6-8" o "AMRAP"). Vacío si es por tiempo. */
    val repsLabel: String
        get() = when {
            reps.isNotBlank() -> reps
            reps_min > 0 && reps_max > 0 -> "$reps_min-$reps_max"
            reps_min > 0 -> "$reps_min"
            else -> ""
        }

    /** Intervalos de tiempo: trabajo con cuenta atrás + descanso, repetido N rondas. */
    val esIntervalos: Boolean get() = trabajoSeg > 0 && numRondas > 0

    /** Un único bloque por tiempo (p. ej. plancha: aguantar la duración inicial). */
    val esTiempoUnico: Boolean get() = !esIntervalos && duracion_inicial_seg > 0

    /** Series de repeticiones con descanso entre ellas. */
    val esSeries: Boolean get() = !esIntervalos && numSeries > 0
}

/** Un ejercicio del catálogo de quema grasa, con su frecuencia y sus protocolos. */
@Serializable
data class EjercicioCatalogo(
    val id: String = "",
    val nombre: String = "",
    val frecuencia_semanal: FrecuenciaSemanal = FrecuenciaSemanal(),
    val protocolos: List<Protocolo> = emptyList(),
    val registro: String = "",
    val notas_forma: String = "",
    val aviso_frecuencia: String = "",
    val progresiones: List<String> = emptyList(),
    val calentamiento_obligatorio_min: Int = 0
)

/** Una rutina de la sección de especiales: secuencia fija (militar) o catálogo (quema grasa). */
@Serializable
data class Rutina(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val duracion_estimada_min: Int = 0,
    val frecuencia_semanal: FrecuenciaSemanal = FrecuenciaSemanal(),
    val frecuencia_diaria: FrecuenciaDiaria = FrecuenciaDiaria(),
    val periodicidad_aviso: String = "",
    val descanso_minimo_entre_sesiones_dias: Int = 0,
    val descanso_entre_ejercicios_seg: RangoSeg = RangoSeg(),
    val progresion: String = "",
    val aviso_frecuencia: String = "",
    val pasos: List<PasoMilitar> = emptyList(),
    val ejercicios: List<EjercicioCatalogo> = emptyList()
) {
    val esSecuenciaFija: Boolean get() = tipo == "secuencia_fija"
    val esCatalogo: Boolean get() = tipo == "catalogo_ejercicios"
    val pasosOrdenados: List<PasoMilitar> get() = pasos.sortedBy { it.orden }

    /** El aviso de frecuencia es diario (rutina apta a diario) en vez de semanal. */
    val esDiaria: Boolean get() = periodicidad_aviso == "diaria"

    /** Descanso entre ejercicios (segundos): mínimo del rango; 0 si no aplica (militar). */
    val descansoEntreEjerciciosSeg: Int get() = descanso_entre_ejercicios_seg.min
}

@Serializable
data class SpecialWorkoutsData(
    val version: Int = 1,
    val rutinas: List<Rutina> = emptyList()
) {
    val militar: Rutina? get() = rutinas.firstOrNull { it.id == SpecialWorkoutsLoader.MILITAR_ID }
    val altura: Rutina? get() = rutinas.firstOrNull { it.id == SpecialWorkoutsLoader.ALTURA_ID }
    val quemaGrasa: Rutina? get() = rutinas.firstOrNull { it.esCatalogo }

    fun rutina(id: String): Rutina? = rutinas.firstOrNull { it.id == id }
    fun ejercicio(exerciseId: String): EjercicioCatalogo? =
        quemaGrasa?.ejercicios?.firstOrNull { it.id == exerciseId }
}

/** Carga y parsea la definición de entrenamientos especiales desde el asset. */
object SpecialWorkoutsLoader {

    private val json = Json { ignoreUnknownKeys = true }

    const val MILITAR_ID = "militar_basica"
    const val ALTURA_ID = "altura_postura"
    const val ASSET = "entrenamientos_especiales.json"

    /** Parsea el contenido del JSON. Separado de [load] para poder testearlo sin Context. */
    fun parse(text: String): SpecialWorkoutsData = json.decodeFromString(text)

    /** Lee el asset del APK y lo parsea. */
    fun load(context: Context): SpecialWorkoutsData =
        context.assets.open(ASSET).bufferedReader().use { parse(it.readText()) }
}
