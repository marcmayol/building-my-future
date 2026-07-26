package com.marcm.actualizador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Tests del ClienteManifiesto sin red real: se inyecta un [Transport] doble que
 * devuelve exactamente lo que cada escenario necesita.
 */
class ClienteManifiestoTest {

    private val sha = "a".repeat(64)

    private fun json(versionCode: Int, notas: String = "Novedades") = """
        {
          "versionCode": $versionCode,
          "versionName": "9.9",
          "url": "https://example.org/app.apk",
          "sha256": "$sha",
          "notas": "$notas",
          "check_horas": 12
        }
    """.trimIndent()

    private fun cliente(transport: Transport) = ClienteManifiesto(transport)

    @Test
    fun `version mayor devuelve manifiesto con novedad y notas`() {
        val m = cliente { RespuestaHttp(200, json(10).toByteArray()) }
            .obtener("http://test/updates.json")

        assertEquals(10, m.versionCode)
        assertEquals("Novedades", m.notas)
        assertEquals(12, m.checkHoras)
        assertTrue(ComparadorVersion.hayNovedad(m.versionCode, actual = 4))
    }

    @Test
    fun `version igual o menor no es novedad`() {
        val m = cliente { RespuestaHttp(200, json(4).toByteArray()) }
            .obtener("http://test/updates.json")

        assertFalse(ComparadorVersion.hayNovedad(m.versionCode, actual = 4))
    }

    @Test(expected = ManifiestoError.Ilegible::class)
    fun `json malformado lanza Ilegible`() {
        cliente { RespuestaHttp(200, "{esto no es json".toByteArray()) }
            .obtener("http://test/updates.json")
    }

    @Test(expected = ManifiestoError.Ilegible::class)
    fun `sha256 invalido lanza Ilegible`() {
        val roto = json(10).replace(sha, "1234")
        cliente { RespuestaHttp(200, roto.toByteArray()) }
            .obtener("http://test/updates.json")
    }

    @Test
    fun `http distinto de 200 lanza HttpNoOk con el codigo`() {
        try {
            cliente { RespuestaHttp(500, ByteArray(0)) }.obtener("http://test/updates.json")
            throw AssertionError("Debería haber lanzado HttpNoOk")
        } catch (e: ManifiestoError.HttpNoOk) {
            assertEquals(500, e.codigo)
        }
    }

    @Test(expected = ManifiestoError.SinRed::class)
    fun `fallo de red lanza SinRed`() {
        cliente { throw IOException("dns caido") }.obtener("http://test/updates.json")
    }
}
