package com.marcm.actualizador

/** Datos mínimos de una versión disponible, derivados del manifiesto. */
data class InfoActualizacion(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val sha256: String,
    val notas: String,
) {
    companion object {
        fun de(m: ManifiestoRemoto) = InfoActualizacion(
            versionCode = m.versionCode,
            versionName = m.versionName,
            url = m.url,
            sha256 = m.sha256,
            notas = m.notas,
        )
    }
}

/** Modo de comprobación: el automático calla los errores; el manual informa. */
enum class Modo { AUTOMATICO, MANUAL }

enum class TipoError { SIN_RED, HTTP, MANIFIESTO, DESCARGA, HASH, INSTALACION }

/** Estado observable por la UI (banner, progreso, mensajes). */
sealed interface EstadoActualizacion {
    data object Inactivo : EstadoActualizacion
    data object Comprobando : EstadoActualizacion
    data object AlDia : EstadoActualizacion
    data class Disponible(val info: InfoActualizacion) : EstadoActualizacion
    data class Descargando(val porcentaje: Int) : EstadoActualizacion
    data object Verificando : EstadoActualizacion
    data object PidiendoPermiso : EstadoActualizacion
    data object Instalando : EstadoActualizacion
    data class Error(val tipo: TipoError, val mensaje: String? = null) : EstadoActualizacion
}

internal fun tipoDe(e: ManifiestoError): TipoError = when (e) {
    is ManifiestoError.SinRed -> TipoError.SIN_RED
    is ManifiestoError.HttpNoOk -> TipoError.HTTP
    is ManifiestoError.Ilegible -> TipoError.MANIFIESTO
}

/**
 * Estado resultante de un fallo al obtener el manifiesto. Regla clave: en
 * AUTOMATICO cualquier error muere en silencio (Inactivo); en MANUAL informa.
 */
internal fun estadoTrasError(modo: Modo, e: ManifiestoError): EstadoActualizacion =
    if (modo == Modo.MANUAL) EstadoActualizacion.Error(tipoDe(e)) else EstadoActualizacion.Inactivo

/**
 * Estado resultante de un manifiesto obtenido con éxito. Hay novedad → Disponible
 * en ambos modos (el banner no es intrusivo). Sin novedad → AlDia solo en MANUAL
 * ("estás al día"); en AUTOMATICO no se molesta al usuario (Inactivo).
 */
internal fun estadoTrasManifiesto(
    modo: Modo,
    m: ManifiestoRemoto,
    versionCodeActual: Int,
): EstadoActualizacion =
    if (ComparadorVersion.hayNovedad(m.versionCode, versionCodeActual)) {
        EstadoActualizacion.Disponible(InfoActualizacion.de(m))
    } else if (modo == Modo.MANUAL) {
        EstadoActualizacion.AlDia
    } else {
        EstadoActualizacion.Inactivo
    }
