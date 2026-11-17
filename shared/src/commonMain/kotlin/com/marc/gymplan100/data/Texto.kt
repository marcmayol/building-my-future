package com.marc.gymplan100.data

import kotlin.math.floor

/**
 * Ayudas de texto y de tiempo que en Android salían de Java (java.text.Normalizer,
 * String.format, System.currentTimeMillis) y que en el iPhone no existen.
 */

private val sinTilde: Map<Char, Char> = buildMap {
    val con = "áàâäãåéèêëíìîïóòôöõúùûüñçýÿÁÀÂÄÃÅÉÈÊËÍÌÎÏÓÒÔÖÕÚÙÛÜÑÇÝ"
    val sin = "aaaaaaeeeeiiiiooooouuuuncyyAAAAAAEEEEIIIIOOOOOUUUUNCY"
    con.forEachIndexed { i, c -> put(c, sin[i]) }
}

/** «Elevación» → «Elevacion»: lo que hacía Normalizer NFD + quitar las marcas. */
fun quitarTildes(texto: String): String {
    val sb = StringBuilder(texto.length)
    for (c in texto) {
        if (c in '̀'..'ͯ') continue // marca suelta (texto ya descompuesto)
        sb.append(sinTilde[c] ?: c)
    }
    return sb.toString()
}

/** Milisegundos desde 1970, como System.currentTimeMillis(). */
fun ahoraMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

/** 12.5 → "12.5", 12.25 → "12.3": un decimal redondeando hacia arriba, como "%.1f". */
fun unDecimal(v: Double): String {
    val decimas = floor(kotlin.math.abs(v) * 10 + 0.5).toLong()
    val signo = if (v < 0 && decimas != 0L) "-" else ""
    return "$signo${decimas / 10}.${decimas % 10}"
}
