package com.marc.gymplan100

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Compose Multiplatform no convierte «%%» en «%»: en la portada salía «1 %%». En
 * composeResources el porcentaje va con un solo «%»; la copia para R.string de Android lo
 * vuelve a escapar al compilar (tarea textosCompartidos de app/build.gradle.kts).
 */
class TextosPorcentajeTest {

    private val fuente = File("../shared/src/commonMain/composeResources")

    @Test
    fun losTextosCompartidosNoLlevanDoblePorcentaje() {
        val ficheros = fuente.walkTopDown()
            .filter { it.isFile && it.name.startsWith("strings_") && it.extension == "xml" }
            .toList()
        assertTrue("No se encuentran los textos en ${fuente.absolutePath}", ficheros.isNotEmpty())
        val malos = ficheros.flatMap { f ->
            f.readLines().filter { "%%" in it }.map { "${f.parentFile?.name}/${f.name}: ${it.trim()}" }
        }
        assertTrue("Con «%%» Compose Multiplatform pinta los dos:\n" + malos.joinToString("\n"), malos.isEmpty())
    }

    @Test
    fun laCopiaParaAndroidEscapaElPorcentaje() {
        val generados = File("build/generated/res/textos")
        if (!generados.exists()) return
        val malos = generados.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .flatMap { f -> f.readLines().asSequence() }
            .filter { Regex("""%(?![0-9]+\$|%)""").containsMatchIn(it.replace("%%", "")) }
            .toList()
        assertTrue("String.format de Android falla con un «%» suelto:\n" + malos.joinToString("\n"), malos.isEmpty())
    }
}
