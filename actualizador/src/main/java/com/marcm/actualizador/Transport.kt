package com.marcm.actualizador

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta HTTP cruda: código y cuerpo en bytes. */
class RespuestaHttp(val codigo: Int, val cuerpo: ByteArray)

/**
 * Abstracción mínima de red para poder inyectar dobles en los tests (sin OkHttp
 * ni MockWebServer). Debe lanzar [IOException] ante cualquier fallo de red/DNS/socket.
 */
fun interface Transport {
    @Throws(IOException::class)
    fun get(url: String): RespuestaHttp
}

/** Implementación real con HttpURLConnection nativo (cero dependencias de red). */
class HttpUrlConnectionTransport(
    private val timeoutMs: Int = 15_000,
) : Transport {
    @Throws(IOException::class)
    override fun get(url: String): RespuestaHttp {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            val codigo = conn.responseCode
            val stream = if (codigo in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val cuerpo = stream?.use { it.readBytes() } ?: ByteArray(0)
            return RespuestaHttp(codigo, cuerpo)
        } finally {
            conn.disconnect()
        }
    }
}
