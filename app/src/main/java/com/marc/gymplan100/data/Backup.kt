package com.marc.gymplan100.data

import android.content.Context
import androidx.annotation.StringRes
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Copia de seguridad de TODO lo que la app guarda de ti.
 *
 * Existe por lo que no se puede arreglar después: los días, los pesos, el historial y los planes
 * viven solo en este teléfono. Sin nube y sin cuenta —que es como queremos la app—, perder el
 * móvil era perderlo todo, y el respaldo automático de Android no es algo que se pueda mirar ni
 * comprobar. Esto sí: un archivo tuyo, que ves, que copias a donde quieras y que puedes abrir.
 *
 * Se copia el almacén ENTERO, clave por clave, en vez de ir campo por campo. Así una versión
 * futura que guarde algo nuevo entra sola en la copia: lo contrario —una lista de campos que
 * alguien se olvida de actualizar— es exactamente como se pierden los datos que creías salvados.
 */
@Serializable
data class BackupFile(
    /** Sube solo si el formato deja de poder leerse hacia atrás. */
    val formato: Int = FORMATO_ACTUAL,
    val app: String = APP,
    /** Cuándo se hizo la copia (epoch millis). */
    val creado: Long = 0L,
    /** Versión de la app que la hizo, para saber de dónde salió si algo no cuadra. */
    val version: String = "",
    /** Todo el progreso: días, registros, historial, sesión activa y perfil. */
    val progreso: Map<String, String> = emptyMap(),
    /** Los planes guardados y cuál está activo. */
    val planes: Map<String, String> = emptyMap(),
    /**
     * Ajustes de la app que no viven en el progreso ni en los planes (de momento, el tema).
     *
     * Vacío en las copias hechas antes de que existiera: restaurar una de esas deja el tema
     * como estaba en vez de romperse, que es lo que tiene que pasar.
     */
    val ajustes: Map<String, String> = emptyMap()
) {
    /**
     * Cuántos días completados hay dentro, para poder avisar antes de sobrescribir. La frase
     * la monta la pantalla, que es la que sabe en qué idioma se pinta.
     */
    fun diasCompletados(): Int {
        val dias = progreso.entries
            .filter { it.key.startsWith("progress_json") }
            .sumOf { entrada ->
                runCatching {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString<ProgressState>(entrada.value)
                        .completedDays.size
                }.getOrDefault(0)
            }
        return dias
    }

    companion object {
        const val FORMATO_ACTUAL = 1
        const val APP = "building-my-future"
    }
}

/** Lee y escribe la copia. Las dos operaciones son explícitas: nunca pasa nada solo. */
object Backup {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Nombre sugerido del archivo: la fecha delante, para que se ordenen solos. */
    fun suggestedName(now: Long): String {
        val fecha = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return "building-my-future-$fecha.json"
    }

    suspend fun create(context: Context, version: String, now: Long): String {
        val repo = ProgressRepository(context)
        return json.encodeToString(
            BackupFile(
                creado = now,
                version = version,
                progreso = repo.exportAll(),
                planes = PlanStore.exportAll(context),
                ajustes = AppTheme.export() + AppLanguage.export(context)
            )
        )
    }

    /**
     * Qué salió mal al leer una copia. Lleva el texto como recurso y no como frase: aquí no
     * hay Context, y la pantalla lo resuelve en el idioma de la app.
     */
    sealed interface Result {
        data class Ok(val file: BackupFile) : Result
        data class Error(@StringRes val message: Int) : Result
    }

    /**
     * Comprueba el contenido ANTES de tocar nada: restaurar sobrescribe lo que hay, así que
     * abrir el archivo equivocado no puede costarte el progreso.
     */
    fun parse(raw: String): Result {
        val file = runCatching { json.decodeFromString<BackupFile>(raw) }.getOrNull()
            ?: return Result.Error(R.string.ini_backup_err_not_backup)
        if (file.app != BackupFile.APP) {
            return Result.Error(R.string.ini_backup_err_other_app)
        }
        if (file.formato > BackupFile.FORMATO_ACTUAL) {
            return Result.Error(R.string.ini_backup_err_newer)
        }
        if (file.progreso.isEmpty() && file.planes.isEmpty()) {
            return Result.Error(R.string.ini_backup_err_empty)
        }
        return Result.Ok(file)
    }

    /** Restaura la copia encima de lo que haya. El llamante ya ha avisado y confirmado. */
    suspend fun restore(context: Context, file: BackupFile) {
        PlanStore.importAll(context, file.planes)
        ProgressRepository(context).importAll(file.progreso)
        if (file.ajustes.isNotEmpty()) {
            AppTheme.import(context, file.ajustes)
            AppLanguage.import(context, file.ajustes)
        }
        // El plan activo puede haber cambiado: la ventana que mira toda la app se reabre.
        PlanStore.load(context)
    }
}
