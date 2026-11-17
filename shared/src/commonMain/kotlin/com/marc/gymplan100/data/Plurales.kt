package com.marc.gymplan100.data

/**
 * Concordancia de número en los textos de la app.
 *
 * Con los planes de serie el uno casi nunca salía —cien días, cuatro fases—, así que muchos
 * textos se escribieron en plural fijo. Con planes propios sí sale: un día de un ejercicio,
 * una fase, una serie. "1 ejercicios" delata que nadie miró ese caso, y de paso hace dudar
 * del resto de lo que dice la pantalla.
 *
 * Uso: `contar(n, "ejercicio", "ejercicios")` → "1 ejercicio" / "5 ejercicios".
 */
fun contar(n: Int, singular: String, plural: String): String =
    if (n == 1) "1 $singular" else "$n $plural"

/**
 * Solo la palabra, para cuando el número ya está escrito aparte (una cifra grande, un
 * marcador "3 de 5"). En "0 de 1 días" el que manda es el total, no el primero.
 */
fun palabra(n: Int, singular: String, plural: String): String = if (n == 1) singular else plural
