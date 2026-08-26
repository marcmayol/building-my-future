package com.marc.gymplan100.data

/**
 * "Este ejercicio mío es como este del catálogo".
 *
 * Un plan que te traes puede llamar a las cosas como quiera: «Press banca plano con barra» no
 * está en el catálogo aunque el movimiento sí. Hasta ahora ese ejercicio se quedaba sin
 * ilustración, sin ficha y —desde que existe el mapa— sin contar para los músculos: la app
 * decía «no lo conozco» y ahí acababa la conversación.
 *
 * En vez de pedirte que rellenes músculos y técnica a mano —que no lo haría nadie—, basta con
 * decir a cuál se parece: hereda dibujo, ficha, músculos y sitio en el mapa de una vez.
 *
 * Vive como ventana global, igual que [PlanData], porque lo consultan sitios que no tienen de
 * dónde sacar el progreso: el catálogo de imágenes, las fichas y los objetivos musculares son
 * objetos puros a los que no se les puede pasar el estado por parámetro sin arrastrarlo por
 * media app.
 */
object ExerciseAliases {

    @Volatile
    private var map: Map<String, String> = emptyMap()

    /** Refresca los alias guardados. Lo llama el ViewModel cuando cambia el progreso. */
    fun set(values: Map<String, String>) {
        map = values
    }

    /** El nombre con el que buscar en el catálogo: el alias si lo hay, y si no el suyo. */
    fun resolve(name: String): String = map[name]?.takeIf { it.isNotBlank() } ?: name

    /** El alias guardado para [name], o null si no tiene. */
    fun aliasOf(name: String): String? = map[name]?.takeIf { it.isNotBlank() }

    val all: Map<String, String> get() = map
}
