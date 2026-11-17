package com.marc.gymplan100.data

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Un texto de interfaz que se decide en la lógica pero se escribe en la pantalla.
 *
 * La lógica pura (el recomendador, la progresión) no sabe en qué idioma se ve la app:
 * devuelve el recurso y sus argumentos, y la pantalla lo resuelve con [texto]. Los
 * argumentos pueden ser a su vez otras frases, que se resuelven antes. Los tests comprueban
 * qué recursos salen ([ids]) en vez del texto, que depende del idioma.
 */
sealed interface Frase {

    /** Un `<string>` con sus argumentos. */
    data class Res(val id: StringResource, val args: List<Any> = emptyList()) : Frase

    /** Un `<plurals>`: [count] elige la forma; los argumentos van aparte (normalmente, el número). */
    data class Plural(
        val id: PluralStringResource,
        val count: Int,
        val args: List<Any> = listOf(count)
    ) : Frase

    /** Varias frases seguidas, con un separador neutro (una coma vale igual en los dos idiomas). */
    data class Lista(val partes: List<Frase>, val separador: String = ", ") : Frase

    /** Todos los recursos que la forman, anidados incluidos: para los tests. */
    fun ids(): List<Any> = when (this) {
        is Res -> listOf<Any>(id) + args.filterIsInstance<Frase>().flatMap { it.ids() }
        is Plural -> listOf<Any>(id) + args.filterIsInstance<Frase>().flatMap { it.ids() }
        is Lista -> partes.flatMap { it.ids() }
    }
}

/** Atajo para [Frase.Res]. */
fun frase(id: StringResource, vararg args: Any): Frase = Frase.Res(id, args.toList())

/** Atajo para [Frase.Plural] cuyo único argumento es el propio número. */
fun frasePlural(id: PluralStringResource, count: Int, vararg args: Any): Frase =
    Frase.Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

/** La frase escrita en el idioma con el que se está pintando. */
@Composable
fun Frase.texto(): String = when (this) {
    is Frase.Res -> stringResource(id, *args.map { argTexto(it) }.toTypedArray())
    is Frase.Plural -> pluralStringResource(id, count, *args.map { argTexto(it) }.toTypedArray())
    is Frase.Lista -> partes.map { it.texto() }.joinToString(separador)
}

@Composable
private fun argTexto(a: Any): Any = if (a is Frase) a.texto() else a
