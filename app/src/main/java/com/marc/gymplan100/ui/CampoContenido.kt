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
 * inglés vería «Plancha» convertirse en «Plank» a media palabra. Cuando el valor cambia desde
 * fuera, o se elige de una lista ([CampoContenido.elegir]), se vuelve a pintar su traducción.
 */
class CampoContenido internal constructor(
    /** Lo que se enseña en el campo. */
    val texto: String,
    /** Para `onValueChange`: recibe lo tecleado y avisa con la clave. */
    val escribir: (String) -> Unit,
    /** Para cuando se elige un nombre de una lista: avisa con su clave y la pinta traducida. */
    val elegir: (clave: String) -> Unit
)

@Composable
fun campoDeContenido(clave: String, onClave: (String) -> Unit): CampoContenido {
    val propio = remember { arrayOf(clave) }
    var tecleado by remember { mutableStateOf(Contenido.t(clave)) }
    val mostrado = if (clave == propio[0]) tecleado else Contenido.t(clave)
    return CampoContenido(
        texto = mostrado,
        escribir = { nuevo ->
            // Si lo escrito sigue siendo la traducción de la clave que ya había, se queda esa:
            // dos claves pueden compartir traducción y tocar el campo no debe cambiar de una
            // a otra (ni de historial).
            val actual = propio[0]
            val k = if (actual.isNotBlank() && Contenido.t(actual).equals(nuevo.trim(), ignoreCase = true)) actual
            else Contenido.clave(nuevo)
            propio[0] = k
            tecleado = nuevo
            onClave(k)
        },
        elegir = { k ->
            propio[0] = k
            tecleado = Contenido.t(k)
            onClave(k)
        }
    )
}
