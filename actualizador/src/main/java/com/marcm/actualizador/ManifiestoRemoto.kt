package com.marcm.actualizador

import org.json.JSONObject

/**
 * Representación del manifiesto remoto (updates.json) que la app publica en
 * GitHub Pages. La comparación de versiones se hace SIEMPRE por [versionCode]
 * (entero), nunca por [versionName].
 */
data class ManifiestoRemoto(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val sha256: String,
    val notas: String,
    val checkHoras: Int,
)

/** Errores tipados de la obtención del manifiesto. */
sealed class ManifiestoError(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa) {
    /** No hay red / DNS / socket caído. */
    class SinRed(causa: Throwable) : ManifiestoError("Sin conexión", causa)

    /** El servidor respondió con un código distinto de 2xx. */
    class HttpNoOk(val codigo: Int) : ManifiestoError("HTTP $codigo")

    /** El cuerpo no es un manifiesto válido (JSON roto o campos inválidos). */
    class Ilegible(causa: Throwable? = null) : ManifiestoError("Manifiesto ilegible", causa)
}

/** Parseo estricto del JSON del manifiesto. Lanza [ManifiestoError.Ilegible] si algo no cuadra. */
object ParserManifiesto {

    fun parsear(bytes: ByteArray): ManifiestoRemoto {
        val json = try {
            JSONObject(bytes.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            throw ManifiestoError.Ilegible(e)
        }
        val versionCode = json.optIntOrNull("versionCode")
            ?: throw ManifiestoError.Ilegible()
        val url = json.optString("url").trim()
        val sha256 = json.optString("sha256").trim().lowercase()
        if (url.isBlank()) throw ManifiestoError.Ilegible()
        if (!esSha256(sha256)) throw ManifiestoError.Ilegible()
        return ManifiestoRemoto(
            versionCode = versionCode,
            versionName = json.optString("versionName").trim(),
            url = url,
            sha256 = sha256,
            notas = json.optString("notas"),
            checkHoras = json.optInt("check_horas", 24).coerceAtLeast(1),
        )
    }

    /** optInt de org.json devuelve 0 si falta; queremos distinguir "ausente". */
    private fun JSONObject.optIntOrNull(clave: String): Int? =
        if (has(clave) && !isNull(clave)) optInt(clave, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        else null

    private fun esSha256(s: String): Boolean =
        s.length == 64 && s.all { it in "0123456789abcdef" }
}
