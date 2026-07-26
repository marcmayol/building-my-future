package com.marcm.actualizador

/**
 * Lo único que el módulo necesita saber de la app anfitriona: dónde está el
 * manifiesto y qué versionCode corre ahora mismo (BuildConfig.VERSION_CODE).
 */
data class ActualizadorConfig(
    val manifiestoUrl: String,
    val versionCodeActual: Int,
    val checkHorasPorDefecto: Int = 24,
)
