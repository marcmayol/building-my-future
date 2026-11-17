package com.marc.gymplan100.plataforma

import androidx.compose.runtime.Composable
import com.marc.gymplan100.data.ActiveSession
import kotlinx.datetime.LocalDate

/**
 * Lo que la app compartida pide al sistema en el que corre.
 *
 * Android lo hace con lo de siempre (AlarmManager, Health Connect, el reloj Wear OS, el
 * selector de documentos…); iOS con sus equivalentes o, donde todavía no hay nada, con una
 * versión que no hace nada. Cada plataforma pone la suya en [plataforma] al arrancar.
 */
interface Plataforma {

    /** Versión visible de la app («2.23.0»). */
    val version: String

    /** Abre un enlace en el navegador. */
    fun abrirEnlace(url: String)

    /** Abre un vídeo de YouTube: en la app si la hay, si no en el navegador. */
    fun abrirYoutube(videoId: String)

    /** Un aviso corto que se va solo (Toast en Android). */
    fun avisoBreve(texto: String)

    /** Vuelve a pintar la app entera: tras cambiar de idioma. */
    fun reiniciarPantalla()

    // --- Aviso de fin de descanso -------------------------------------------------------

    fun programarDescanso(alMs: Long, tipo: Int, dia: Int)
    fun cancelarDescanso()
    fun sincronizarDescanso(sesion: ActiveSession?)

    /** ¿El sistema deja avisar a la hora exacta? (Android 12+ puede no dejar). */
    fun puedeAvisarExacto(): Boolean
    fun abrirAjustesAvisoExacto()

    /** ¿La app está exenta del ahorro de batería? Si no, los avisos pueden llegar tarde. */
    fun ignoraAhorroBateria(): Boolean
    fun abrirAjustesBateria()

    // --- Reloj ----------------------------------------------------------------------------

    fun publicarEnReloj(sesion: ActiveSession?, siguienteDia: Int)

    // --- Salud (Health Connect / HealthKit) --------------------------------------------------

    val salud: Salud

    // --- Recordatorio para entrenar ------------------------------------------------------

    val recordatorio: Recordatorio

    // --- Ficheros ---------------------------------------------------------------------------

    /**
     * Devuelve una función que abre el selector de ficheros y entrega el texto del elegido
     * (null si no se pudo leer; no se llama si el usuario cancela).
     */
    @Composable
    fun rememberAbrirTexto(tipos: List<String>, alLeer: (String?) -> Unit): () -> Unit

    /**
     * Devuelve una función que pide dónde guardar un fichero [nombre] y escribe en él el
     * texto que devuelve `contenido`. [alTerminar] recibe si se guardó.
     */
    @Composable
    fun rememberGuardarTexto(
        alTerminar: (Boolean) -> Unit
    ): (nombre: String, contenido: suspend () -> String) -> Unit

    /** Pide los permisos de salud; [alVolver] se llama al cerrar el diálogo del sistema. */
    @Composable
    fun rememberPedirPermisosSalud(alVolver: () -> Unit): () -> Unit

    // --- Trozos de pantalla que solo tiene una plataforma -----------------------------------

    /** Actualizaciones de la app (solo la versión de Android que no va por la tienda). */
    @Composable
    fun SeccionActualizaciones()

    @Composable
    fun BannerActualizacion()

    /** Se acaba de terminar un entreno: el momento de pedir que valoren la app, si toca. */
    fun trasEntrenar()

    /** «Valora la app» en Ajustes. */
    @Composable
    fun SeccionValoracion()
}

/** Health Connect en Android, HealthKit en iOS. */
interface Salud {
    val disponible: Boolean
    val necesitaActualizar: Boolean
    suspend fun tienePermisos(): Boolean
    suspend fun escribirEntreno(titulo: String, notas: String, inicioMs: Long, finMs: Long, kcal: Double?)
    suspend fun ultimoPesoKg(): Double?
}

/** Recordatorio de entrenar a una hora, ciertos días, con pausa opcional. */
interface Recordatorio {
    fun activo(): Boolean
    fun dias(): Set<Int>
    fun minutoDelDia(): Int
    fun pausaHasta(): LocalDate?
    fun guardar(activo: Boolean, dias: Set<Int>, minutoDelDia: Int)
    fun pausar(hasta: LocalDate?)
}

/** La de la plataforma en la que corre la app. Se pone al arrancar. */
lateinit var plataforma: Plataforma
