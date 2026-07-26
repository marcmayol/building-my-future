package com.marcm.actualizador

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/**
 * Descarga sin red real: se inyecta un [AbridorConexion] doble que sirve el cuerpo
 * desde memoria por un InputStream (se lee en bloques, igual que una descarga real),
 * anunciando o no la longitud. Cubre sha al vuelo, progreso, error HTTP y verificación.
 */
class DescargadorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val cuerpoApk = ByteArray(300_000) { (it % 256).toByte() }

    private fun sha256Hex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in MessageDigest.getInstance("SHA-256").digest(bytes)) {
            sb.append("%02x".format(b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun conexionDe(codigo: Int, cuerpo: ByteArray, anunciaLongitud: Boolean = true) =
        AbridorConexion {
            object : Conexion {
                override val codigo = codigo
                override val longitud = if (anunciaLongitud) cuerpo.size.toLong() else -1L
                override val flujo: InputStream = ByteArrayInputStream(cuerpo)
                override fun close() {}
            }
        }

    @Test
    fun `descarga completa calcula el sha correcto y llega al 100`() = runBlocking {
        val destino = temp.newFile("descarga.apk")
        var ultimoProgreso = 0

        val res = Descargador(conexionDe(200, cuerpoApk))
            .descargar("http://irrelevante/app.apk", destino) { ultimoProgreso = it }

        assertTrue(res is ResultadoDescarga.Ok)
        res as ResultadoDescarga.Ok
        assertEquals(sha256Hex(cuerpoApk), res.sha256)
        assertEquals(cuerpoApk.size.toLong(), destino.length())
        assertEquals(100, ultimoProgreso)

        assertTrue(VerificadorSha.verificarOBorrar(destino, res.sha256))
        assertTrue(destino.exists())
    }

    @Test
    fun `sin longitud anunciada igualmente descarga y avisa del 100 final`() = runBlocking {
        val destino = temp.newFile("sinlong.apk")
        var ultimoProgreso = -1

        val res = Descargador(conexionDe(200, cuerpoApk, anunciaLongitud = false))
            .descargar("http://irrelevante/app.apk", destino) { ultimoProgreso = it }

        assertTrue(res is ResultadoDescarga.Ok)
        assertEquals(100, ultimoProgreso)
    }

    @Test
    fun `descarga con sha manipulado en el manifiesto borra el APK`() = runBlocking {
        val destino = temp.newFile("descarga2.apk")
        val res = Descargador(conexionDe(200, cuerpoApk)).descargar("http://x/app.apk", destino)
        assertTrue(res is ResultadoDescarga.Ok)

        assertFalse(VerificadorSha.verificarOBorrar(destino, "c".repeat(64)))
        assertFalse("APK con hash incorrecto debe borrarse", destino.exists())
    }

    @Test
    fun `codigo http de error devuelve Error y no deja archivo`() = runBlocking {
        val destino = temp.newFile("descarga3.apk")
        val res = Descargador(conexionDe(404, ByteArray(0))).descargar("http://x/nope.apk", destino)
        assertTrue(res is ResultadoDescarga.Error)
        assertFalse(destino.exists())
    }

    @Test
    fun `fallo de red durante la apertura devuelve Error`() = runBlocking {
        val abridor = AbridorConexion { throw IOException("sin red") }
        val destino = temp.newFile("descarga4.apk")
        val res = Descargador(abridor).descargar("http://x/app.apk", destino)
        assertTrue(res is ResultadoDescarga.Error)
        assertFalse(destino.exists())
    }
}
