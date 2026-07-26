package com.marcm.actualizador

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparadorVersionTest {

    @Test
    fun `remoto mayor es novedad`() {
        assertTrue(ComparadorVersion.hayNovedad(remoto = 5, actual = 4))
    }

    @Test
    fun `remoto igual no es novedad`() {
        assertFalse(ComparadorVersion.hayNovedad(remoto = 4, actual = 4))
    }

    @Test
    fun `remoto menor no es novedad`() {
        assertFalse(ComparadorVersion.hayNovedad(remoto = 3, actual = 4))
    }
}
