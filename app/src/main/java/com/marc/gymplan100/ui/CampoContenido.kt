package com.marc.gymplan100.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marc.gymplan100.data.Contenido

/**
 * Un campo donde se escribe el nombre de un ejercicio que se guarda con su clave en
 * castellano pero se enseña en el idioma de la app.
 *
 * Lo que se ve es lo que se teclea, letra a letra: si el campo enseñara `t(clave)` a cada
 * pulsación, al escribir «Side plank » el espacio desaparecería (la clave es «Plancha
 * lateral» y su traducción no lleva espacio) y quien teclee en castellano con la app en
 * inglés vería «Plancha» convertirse en «Plank» a media palabra. Solo cuando el valor cambia
 * desde fuera (se elige de una lista) se vuelve a pintar su traducción.
 *
 * Devuelve el texto a enseñar y la función que recibe lo tecleado y avisa con la clave.
 */
@Composable
fun campoDeContenido(clave: String, onClave: (String) -> Unit): Pair<String, (String) -> Unit> {
    val propio = remember { arrayOf(clave) }
    var tecleado by remember { mutableStateOf(Contenido.t(clave)) }
    val mostrado = if (clave == propio[0]) tecleado else Contenido.t(clave)
    return mostrado to { nuevo: String ->
        val k = Contenido.clave(nuevo)
        propio[0] = k
        tecleado = nuevo
        onClave(k)
    }
}
