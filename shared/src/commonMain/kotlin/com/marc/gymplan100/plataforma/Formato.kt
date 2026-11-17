package com.marc.gymplan100.plataforma

import androidx.compose.runtime.Composable
import com.marc.gymplan100.data.PlatformContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/** Separador decimal del idioma en uso («,» en español, «.» en inglés). */
expect fun separadorDecimal(): Char

/** Separador de miles del idioma en uso. */
expect fun separadorMiles(): Char

/** Fecha con un patrón de Unicode («d MMM yyyy») en el idioma en uso. */
expect fun formatearFecha(millis: Long, patron: String): String

/** El contexto de la plataforma dentro de una pantalla (en Android, LocalContext). */
@Composable
expect fun platformContext(): PlatformContext

/**
 * Lo que hacía `"…".format(…)` de Java, que no existe en iOS: %d, %s, %0Nd, %Nd, %,d y
 * %.Nf, con el separador decimal y de miles del idioma en uso, como String.format.
 */
fun String.fmt(vararg args: Any?): String {
    val re = Regex("""%(\d+\$)?([0,]*)(\d+)?(?:\.(\d+))?([dsf%])""")
    var i = 0
    return re.replace(this) { m ->
        val (pos, flags, ancho, decimales, tipo) = m.destructured
        if (tipo == "%") return@replace "%"
        val arg = if (pos.isNotEmpty()) args[pos.dropLast(1).toInt() - 1] else args[i++]
        val texto = when (tipo) {
            "d" -> {
                val n = (arg as Number).toLong()
                if (',' in flags) conMiles(n) else n.toString()
            }
            "f" -> decimal((arg as Number).toDouble(), decimales.ifEmpty { "6" }.toInt())
            else -> arg.toString()
        }
        val w = ancho.ifEmpty { "0" }.toInt()
        if ('0' in flags && tipo != "s") {
            if (texto.startsWith("-")) "-" + texto.drop(1).padStart(w - 1, '0') else texto.padStart(w, '0')
        } else texto.padStart(w)
    }
}

private fun conMiles(n: Long): String {
    val s = abs(n).toString().reversed().chunked(3).joinToString(separadorMiles().toString()).reversed()
    return if (n < 0) "-$s" else s
}

/** Redondea hacia arriba en el .5, como String.format. */
private fun decimal(v: Double, decimales: Int): String {
    val f = 10.0.pow(decimales)
    val r = floor(abs(v) * f + 0.5).toLong()
    val entero = r / f.toLong()
    val resto = (r % f.toLong()).toString().padStart(decimales, '0')
    val signo = if (v < 0 && r != 0L) "-" else ""
    return if (decimales == 0) "$signo$entero" else "$signo$entero${separadorDecimal()}$resto"
}

/** Ancho de la pantalla en dp (LocalConfiguration.screenWidthDp en Android). */
@Composable
expect fun anchoPantallaDp(): Int

/** Atrás del sistema (botón o gesto de Android; en iOS, el gesto de volver). */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) =
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)

/**
 * Un texto con huecos para rellenar fuera de la composición (en un onClick): «Fase %1$s».
 * Se rellena después con [fmt]. Sustituye a context.getString(id, args) de Android.
 */
@Composable
fun plantillaDe(res: org.jetbrains.compose.resources.StringResource): String =
    org.jetbrains.compose.resources.stringResource(res, "%1\$s", "%2\$s", "%3\$s", "%4\$s")

/** Ordena un mapa por sus claves, como toSortedMap() de Java. */
fun <K : Comparable<K>, V> Map<K, V>.ordenadoPorClave(): Map<K, V> =
    entries.sortedBy { it.key }.associate { it.key to it.value }

/** Como URLEncoder.encode(texto, "UTF-8"): espacios como «+» y el resto en %XX. */
fun codificarUrl(texto: String): String = buildString {
    for (b in texto.encodeToByteArray()) {
        val c = (b.toInt() and 0xFF)
        val ch = c.toChar()
        when {
            ch.isLetterOrDigit() && c < 128 -> append(ch)
            ch == '.' || ch == '-' || ch == '*' || ch == '_' -> append(ch)
            ch == ' ' -> append('+')
            else -> append('%').append(c.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
