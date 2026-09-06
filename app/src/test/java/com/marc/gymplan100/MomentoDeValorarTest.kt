package com.marc.gymplan100

import com.marc.gymplan100.review.MomentoDeValorar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cuándo se pide la valoración.
 *
 * Lo que se prueba aquí no es una cuenta, es una promesa: que la app no pregunta antes de que
 * la hayas usado y que, si preguntó una vez, no vuelve a la carga. Si esto falla, el fallo se
 * ve en la ficha de Play y ya no hay quien lo arregle.
 */
class MomentoDeValorarTest {

    private val ahora = 1_800_000_000_000L // un instante cualquiera, en milisegundos
    private val unDia = 24L * 60 * 60 * 1000

    @Test
    fun `sin entrenos suficientes no se pregunta`() {
        assertFalse(MomentoDeValorar.toca(entrenos = 0, ultimaPeticion = 0L, ahora = ahora))
        assertFalse(MomentoDeValorar.toca(entrenos = 2, ultimaPeticion = 0L, ahora = ahora))
    }

    @Test
    fun `al tercer entreno se pregunta por primera vez`() {
        assertTrue(MomentoDeValorar.toca(entrenos = 3, ultimaPeticion = 0L, ahora = ahora))
    }

    @Test
    fun `recien preguntado no se insiste`() {
        val ayer = ahora - unDia
        assertFalse(MomentoDeValorar.toca(entrenos = 40, ultimaPeticion = ayer, ahora = ahora))
    }

    @Test
    fun `pasados los meses de espera se puede volver a preguntar`() {
        val hace = ahora - MomentoDeValorar.DIAS_ENTRE_PETICIONES * unDia
        assertTrue(MomentoDeValorar.toca(entrenos = 40, ultimaPeticion = hace, ahora = ahora))
        // Un día antes todavía no.
        assertFalse(
            MomentoDeValorar.toca(entrenos = 40, ultimaPeticion = hace + unDia, ahora = ahora)
        )
    }

    @Test
    fun `un reloj movido hacia atras no reabre la pregunta`() {
        // La última petición queda "en el futuro" al cambiar la fecha del móvil a mano. Eso no
        // puede leerse como "ha pasado mucho tiempo": sería preguntar dos veces seguidas.
        val enElFuturo = ahora + 30 * unDia
        assertFalse(
            MomentoDeValorar.toca(entrenos = 40, ultimaPeticion = enElFuturo, ahora = ahora)
        )
    }
}
