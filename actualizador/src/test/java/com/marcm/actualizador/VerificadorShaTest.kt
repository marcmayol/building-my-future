package com.marcm.actualizador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class VerificadorShaTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun sha256Hex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in MessageDigest.getInstance("SHA-256").digest(bytes)) {
            sb.append("%02x".format(b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    @Test
    fun `calcula el sha256 conocido`() {
        val f = temp.newFile("a.bin").apply { writeBytes("hola".toByteArray()) }
        assertEquals(sha256Hex("hola".toByteArray()), VerificadorSha.calcular(f))
    }

    @Test
    fun `hash correcto conserva el archivo`() {
        val bytes = "apk falso".toByteArray()
        val f = temp.newFile("app.apk").apply { writeBytes(bytes) }

        assertTrue(VerificadorSha.verificarOBorrar(f, sha256Hex(bytes)))
        assertTrue("el archivo verificado debe conservarse", f.exists())
    }

    @Test
    fun `hash incorrecto borra el archivo y no se instala`() {
        val f = temp.newFile("app.apk").apply { writeBytes("apk manipulado".toByteArray()) }
        val esperadoFalso = "b".repeat(64)

        assertFalse(VerificadorSha.verificarOBorrar(f, esperadoFalso))
        assertFalse("un hash incorrecto debe borrar el APK", f.exists())
    }
}
