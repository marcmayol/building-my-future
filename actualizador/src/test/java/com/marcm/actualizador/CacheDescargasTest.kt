package com.marcm.actualizador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CacheDescargasTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun crear(dir: File, vararg nombres: String) {
        nombres.forEach { File(dir, it).writeBytes(ByteArray(8)) }
    }

    @Test
    fun `conserva el apk y el part de la version en curso`() {
        val dir = temp.newFolder("actualizaciones")
        crear(dir, "7.apk", "7.apk.part")

        assertEquals(0, CacheDescargas.limpiar(dir, conservarVc = 7))
        assertTrue(File(dir, "7.apk").exists())
        assertTrue(File(dir, "7.apk.part").exists())
    }

    @Test
    fun `borra descargas a medias de versiones anteriores`() {
        val dir = temp.newFolder("actualizaciones")
        crear(dir, "5.apk.part", "6.apk", "7.apk")

        assertEquals(2, CacheDescargas.limpiar(dir, conservarVc = 7))
        assertFalse("el .part huérfano de la v5 debe desaparecer", File(dir, "5.apk.part").exists())
        assertFalse("el APK de la v6 ya no sirve", File(dir, "6.apk").exists())
        assertTrue(File(dir, "7.apk").exists())
    }

    @Test
    fun `sin version que conservar lo borra todo`() {
        val dir = temp.newFolder("actualizaciones")
        crear(dir, "5.apk.part", "6.apk")

        assertEquals(2, CacheDescargas.limpiar(dir, conservarVc = null))
        assertEquals(0, dir.listFiles()?.size)
    }

    @Test
    fun `directorio inexistente no revienta`() {
        val dir = File(temp.root, "no-existe")

        assertEquals(0, CacheDescargas.limpiar(dir, conservarVc = 7))
    }
}
