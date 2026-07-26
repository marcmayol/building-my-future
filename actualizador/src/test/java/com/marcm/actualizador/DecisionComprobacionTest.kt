package com.marcm.actualizador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la regla de tolerancia a fallos y de novedad a nivel de decisión pura:
 * AUTOMATICO calla, MANUAL informa; hay novedad solo por versionCode mayor.
 */
class DecisionComprobacionTest {

    private fun manifiesto(versionCode: Int) = ManifiestoRemoto(
        versionCode = versionCode,
        versionName = "x",
        url = "https://e/app.apk",
        sha256 = "a".repeat(64),
        notas = "n",
        checkHoras = 24,
    )

    // --- Errores ---

    @Test
    fun `error en automatico muere en silencio (Inactivo)`() {
        val estado = estadoTrasError(Modo.AUTOMATICO, ManifiestoError.HttpNoOk(500))
        assertEquals(EstadoActualizacion.Inactivo, estado)
    }

    @Test
    fun `mismo error en manual informa con su tipo`() {
        assertEquals(
            EstadoActualizacion.Error(TipoError.HTTP),
            estadoTrasError(Modo.MANUAL, ManifiestoError.HttpNoOk(500)),
        )
        assertEquals(
            EstadoActualizacion.Error(TipoError.SIN_RED),
            estadoTrasError(Modo.MANUAL, ManifiestoError.SinRed(java.io.IOException())),
        )
        assertEquals(
            EstadoActualizacion.Error(TipoError.MANIFIESTO),
            estadoTrasError(Modo.MANUAL, ManifiestoError.Ilegible()),
        )
    }

    // --- Manifiesto obtenido ---

    @Test
    fun `version mayor es Disponible en ambos modos`() {
        val auto = estadoTrasManifiesto(Modo.AUTOMATICO, manifiesto(10), versionCodeActual = 4)
        val manual = estadoTrasManifiesto(Modo.MANUAL, manifiesto(10), versionCodeActual = 4)
        assertTrue(auto is EstadoActualizacion.Disponible)
        assertTrue(manual is EstadoActualizacion.Disponible)
        assertEquals(10, (manual as EstadoActualizacion.Disponible).info.versionCode)
    }

    @Test
    fun `sin novedad en manual dice AlDia, en automatico calla`() {
        assertEquals(
            EstadoActualizacion.AlDia,
            estadoTrasManifiesto(Modo.MANUAL, manifiesto(4), versionCodeActual = 4),
        )
        assertEquals(
            EstadoActualizacion.Inactivo,
            estadoTrasManifiesto(Modo.AUTOMATICO, manifiesto(4), versionCodeActual = 4),
        )
    }
}
