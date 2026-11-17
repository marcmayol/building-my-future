package com.marc.gymplan100.data

/**
 * Los ficheros de datos que lleva la app (catálogo, fichas, planes de serie, traducciones):
 * en Android salen de `assets/`, en el iPhone del bundle. Cada plataforma pone su [lector]
 * al arrancar; la lógica común solo pide rutas como «planes/index.json».
 */
object Datos {
    var lector: (ruta: String) -> String = { error("Datos.lector sin configurar") }

    fun leer(ruta: String): String = lector(ruta)
}
