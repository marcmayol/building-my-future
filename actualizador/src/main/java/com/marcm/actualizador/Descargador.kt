package com.marcm.actualizador

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Conexión de descarga abstracta, para poder inyectar un servidor local en tests. */
interface Conexion : Closeable {
    val codigo: Int
    val longitud: Long   // -1 si el servidor no la anuncia
    val flujo: InputStream
}

fun interface AbridorConexion {
    @Throws(IOException::class)
    fun abrir(url: String): Conexion
}

/** Abridor real con HttpURLConnection nativo (sigue redirecciones dentro de https). */
class AbridorConexionHttp(private val timeoutMs: Int = 20_000) : AbridorConexion {
    @Throws(IOException::class)
    override fun abrir(url: String): Conexion {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        val codigoResp = conn.responseCode
        return object : Conexion {
            override val codigo = codigoResp
            override val longitud = conn.contentLengthLong
            override val flujo: InputStream =
                if (codigoResp in 200..299) conn.inputStream
                else conn.errorStream ?: ByteArrayInputStream(ByteArray(0))

            override fun close() = conn.disconnect()
        }
    }
}

sealed interface ResultadoDescarga {
    /** Descarga completa; [sha256] es el hash calculado al vuelo (aún sin verificar). */
    data class Ok(val archivo: File, val sha256: String) : ResultadoDescarga
    data class Error(val causa: Throwable) : ResultadoDescarga
}

/**
 * Descarga un APK a almacenamiento privado, calculando su SHA-256 mientras baja y
 * emitiendo progreso 0..100. Nunca deja un fichero a medias como definitivo: ante
 * cualquier error borra el destino.
 */
class Descargador(
    private val abridor: AbridorConexion = AbridorConexionHttp(),
) {
    suspend fun descargar(
        url: String,
        destino: File,
        onProgreso: (Int) -> Unit = {},
    ): ResultadoDescarga = withContext(Dispatchers.IO) {
        try {
            abridor.abrir(url).use { con ->
                if (con.codigo !in 200..299) {
                    // Lo tratamos como error: el catch de abajo borra el destino.
                    throw IOException("HTTP ${con.codigo}")
                }
                destino.parentFile?.mkdirs()
                val digest = MessageDigest.getInstance("SHA-256")
                val total = con.longitud
                var leidos = 0L
                var ultimoPct = -1
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = con.flujo.read(buffer)
                        if (n < 0) break
                        salida.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        leidos += n
                        if (total > 0) {
                            val pct = ((leidos * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != ultimoPct) {
                                ultimoPct = pct
                                onProgreso(pct)
                            }
                        }
                    }
                }
                if (ultimoPct != 100) onProgreso(100)
                ResultadoDescarga.Ok(destino, digest.digest().aHexMinuscula())
            }
        } catch (e: Exception) {
            destino.delete()
            ResultadoDescarga.Error(e)
        }
    }
}
