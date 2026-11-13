package com.marc.gymplan100

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/** Vuelca los textos de contenido a build/ para traducirlos. No comprueba nada. */
class ContenidoVolcadoTest {
    @Test
    fun volcar() {
        val dir = File("build/contenido").apply { mkdirs() }
        val j = Json { prettyPrint = true }
        File(dir, "nombres.json").writeText(j.encodeToString(ContenidoFuentes.nombres().sorted()))
        File(dir, "textos.json").writeText(j.encodeToString(ContenidoFuentes.textos().sorted()))
        File(dir, "esquemas.json").writeText(j.encodeToString(ContenidoFuentes.esquemas().sorted()))
    }
}
