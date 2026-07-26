package com.marcm.actualizador

/**
 * Comparación de versiones por versionCode entero. Regla única: hay novedad solo
 * si el versionCode remoto es ESTRICTAMENTE mayor que el actual. Nunca se comparan
 * strings ni versionName; igual o menor = sin novedad.
 */
object ComparadorVersion {
    fun hayNovedad(remoto: Int, actual: Int): Boolean = remoto > actual
}
