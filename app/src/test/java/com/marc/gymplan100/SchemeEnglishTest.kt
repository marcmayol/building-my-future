package com.marc.gymplan100

import com.marc.gymplan100.data.isBodyweightScheme
import com.marc.gymplan100.data.setCountFromScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Con la app en inglés, el editor propone escribir las series como "3 rounds". El motor
 * tiene que entenderlo igual que "3 vueltas", o la sesión pone 3 series y pide kilos.
 */
class SchemeEnglishTest {

    @Test
    fun rounds_cuentan_como_vueltas() {
        assertEquals(5, setCountFromScheme("5 rounds"))
        assertEquals(1, setCountFromScheme("1 round"))
        assertTrue(isBodyweightScheme("3 rounds"))
    }

    @Test
    fun sets_cuentan_como_series() {
        assertEquals(2, setCountFromScheme("2 sets"))
        assertFalse(isBodyweightScheme("2 sets"))
    }

    @Test
    fun el_castellano_sigue_igual() {
        assertEquals(3, setCountFromScheme("3 vueltas"))
        assertEquals(1, setCountFromScheme("1 serie"))
        assertEquals(4, setCountFromScheme("4 x 8-10"))
        assertTrue(isBodyweightScheme("3 vueltas"))
        assertFalse(isBodyweightScheme("3 x 12"))
    }
}
