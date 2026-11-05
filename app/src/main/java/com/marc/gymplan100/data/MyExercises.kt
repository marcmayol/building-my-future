package com.marc.gymplan100.data

/**
 * Los ejercicios que te has inventado tú y los que haces con tu peso.
 *
 * En el entreno libre se puede apuntar cualquier cosa, también lo que no está en el catálogo
 * ni en el plan («Remo en TRX», «Fondos en banco de casa»). Hasta ahora ese nombre se usaba
 * una vez y se perdía: el buscador solo mira el catálogo, así que la siguiente vez había que
 * escribirlo otra vez, y si no llevaba kilos ni siquiera quedaba en «Mis pesos». Ahora se
 * guarda en el móvil y sale en las sugerencias y en el buscador como uno más; solo le falta
 * lo que no se puede inventar, el dibujo y el mapa de músculos.
 *
 * [bodyweight] son los ejercicios marcados «peso corporal»: no se les piden kilos, solo
 * repeticiones. Vale también para los del catálogo, porque unas sentadillas pueden ir con
 * barra o sin nada y eso solo lo sabe quien las hace.
 *
 * Se guardan en el perfil, una sola lista para todo el móvil, pero **solo se ofrecen con un
 * plan propio activo** (uno creado o importado por ti): decisión de Marc (5-nov-2026). En los
 * planes de serie se pueden seguir apuntando, y quedan guardados para cuando estés en uno tuyo.
 *
 * Vive como ventana global, igual que [ExerciseAliases]: el buscador de ejercicios es un
 * componente suelto y no tiene de dónde sacar el perfil.
 */
object MyExercises {

    @Volatile
    private var propios: List<String> = emptyList()

    @Volatile
    private var conTuPeso: Set<String> = emptySet()

    /** Lo llama el ViewModel cuando cambia el progreso. */
    fun set(names: List<String>, bodyweight: Set<String>) {
        propios = names
        conTuPeso = bodyweight
    }

    /** ¿Se ofrecen ahora? Solo con un plan propio activo. */
    val offered: Boolean get() = !PlanData.active.builtin

    /** Los tuyos, del último que usaste al primero; vacía si el plan activo es de serie. */
    val names: List<String> get() = if (offered) propios else emptyList()

    /** Todos los guardados, sin mirar el plan activo: para el editor, que siempre es de uno tuyo. */
    val all: List<String> get() = propios

    fun isBodyweight(name: String): Boolean = name.trim() in conTuPeso

    /** Los tuyos que casan con lo escrito, con la misma búsqueda por palabras que el catálogo. */
    fun search(text: String, limit: Int = 8, from: List<String> = names): List<String> {
        val q = ExerciseCatalog.tokens(text)
        if (q.isEmpty()) return from.take(limit)
        return from.filter { nombre ->
            val t = ExerciseCatalog.tokens(nombre)
            q.all { p -> t.any { it.startsWith(p) } }
        }.take(limit)
    }

    /**
     * Apunta como tuyos los nombres que la app no conoce ([isKnown] dice cuáles conoce: el
     * catálogo y el plan). Los ya guardados suben al principio, para que lo que usas ahora
     * salga antes que lo de hace meses. Sin cambios devuelve el mismo estado.
     */
    fun remember(state: UserProfile, names: List<String>, isKnown: (String) -> Boolean): UserProfile {
        val nuevos = names.map { it.trim() }
            .filter { it.isNotEmpty() && !isKnown(it) }
            .distinctBy { it.lowercase() }
        if (nuevos.isEmpty()) return state
        val resto = state.customExercises.filter { viejo -> nuevos.none { it.equals(viejo, ignoreCase = true) } }
        val lista = (nuevos + resto).take(MAX)
        return if (lista == state.customExercises) state else state.copy(customExercises = lista)
    }

    /** Lo quita de los tuyos (y de «peso corporal»). */
    fun forget(state: UserProfile, name: String): UserProfile = state.copy(
        customExercises = state.customExercises.filterNot { it.equals(name.trim(), ignoreCase = true) },
        bodyweightExercises = state.bodyweightExercises - name.trim()
    )

    /** Marca o desmarca [name] como ejercicio de peso corporal. */
    fun setBodyweight(state: UserProfile, name: String, on: Boolean): UserProfile {
        val nombre = name.trim()
        if (nombre.isEmpty()) return state
        return state.copy(
            bodyweightExercises = if (on) state.bodyweightExercises + nombre else state.bodyweightExercises - nombre
        )
    }

    /** Con 200 ya no es una lista de ejercicios, es un cajón: los más viejos se caen. */
    const val MAX = 200
}
