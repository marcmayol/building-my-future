package com.marc.gymplan100.data

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Un texto de interfaz que se decide en la lógica pero se escribe en la pantalla.
 *
 * La lógica pura (el recomendador, la progresión) no tiene Context ni sabe en qué idioma se
 * ve la app: devuelve el recurso y sus argumentos, y la pantalla lo resuelve con [texto]. Los
 * argumentos pueden ser a su vez otras frases, que se resuelven antes. Los tests comprueban
 * qué recursos salen ([ids]) en vez del texto, que depende del idioma.
 */
sealed interface Frase {

    /** Un `<string>` con sus argumentos. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : Frase

    /** Un `<plurals>`: [count] elige la forma; los argumentos van aparte (normalmente, el número). */
    data class Plural(
        @PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = listOf(count)
    ) : Frase

    /** Varias frases seguidas, con un separador neutro (una coma vale igual en los dos idiomas). */
    data class Lista(val partes: List<Frase>, val separador: String = ", ") : Frase

    fun resolver(res: Resources): String = when (this) {
        is Res -> res.getString(id, *args.map { arg(res, it) }.toTypedArray())
        is Plural -> res.getQuantityString(id, count, *args.map { arg(res, it) }.toTypedArray())
        is Lista -> partes.joinToString(separador) { it.resolver(res) }
    }

    /** Todos los recursos que la forman, anidados incluidos: para los tests. */
    fun ids(): List<Int> = when (this) {
        is Res -> listOf(id) + args.filterIsInstance<Frase>().flatMap { it.ids() }
        is Plural -> listOf(id) + args.filterIsInstance<Frase>().flatMap { it.ids() }
        is Lista -> partes.flatMap { it.ids() }
    }

    private fun arg(res: Resources, a: Any): Any = if (a is Frase) a.resolver(res) else a
}

/** Atajo para [Frase.Res]. */
fun frase(@StringRes id: Int, vararg args: Any): Frase = Frase.Res(id, args.toList())

/** Atajo para [Frase.Plural] cuyo único argumento es el propio número. */
fun frasePlural(@PluralsRes id: Int, count: Int, vararg args: Any): Frase =
    Frase.Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

/** La frase escrita en el idioma con el que se está pintando. */
@Composable
@ReadOnlyComposable
fun Frase.texto(): String {
    LocalConfiguration.current // para que cambie con el idioma, como stringResource
    return resolver(LocalContext.current.resources)
}
