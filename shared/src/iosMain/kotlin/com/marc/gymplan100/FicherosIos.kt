package com.marc.gymplan100

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

/**
 * El selector de ficheros del iPhone (la app Archivos), para importar planes y para guardar y
 * restaurar la copia de seguridad: lo que en Android hacen OpenDocument y CreateDocument.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
object FicherosIos {

    // UIKit guarda el delegado como referencia débil: hay que sujetarlo mientras está abierto.
    private var delegado: Delegado? = null

    /** Abre Archivos; [alLeer] recibe el texto del fichero elegido (null si no se pudo leer). */
    fun abrir(alLeer: (String?) -> Unit) {
        // Todos los tipos, como en Android: hay .json y .md que el sistema no etiqueta bien.
        val selector = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
        presentar(selector) { url ->
            alLeer(url?.let { NSString.stringWithContentsOfURL(it, NSUTF8StringEncoding, null) })
        }
    }

    /** Escribe [texto] en un fichero temporal [nombre] y deja elegir dónde guardarlo. */
    fun guardar(nombre: String, texto: String, alTerminar: (Boolean) -> Unit) {
        val temporal = NSURL.fileURLWithPath(NSTemporaryDirectory() + nombre)
        val escrito = NSString.create(string = texto).writeToURL(temporal, true, NSUTF8StringEncoding, null)
        if (!escrito) return alTerminar(false)
        val selector = UIDocumentPickerViewController(forExportingURLs = listOf(temporal), asCopy = true)
        presentar(selector) { url -> if (url != null) alTerminar(true) }
    }

    private fun presentar(selector: UIDocumentPickerViewController, alElegir: (NSURL?) -> Unit) {
        val d = Delegado { url ->
            delegado = null
            alElegir(url)
        }
        delegado = d
        selector.delegate = d
        arriba()?.presentViewController(selector, animated = true, completion = null)
    }

    /** El controlador que se ve ahora, para abrir el selector encima. */
    private fun arriba(): UIViewController? {
        @Suppress("DEPRECATION")
        var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (vc?.presentedViewController != null) vc = vc.presentedViewController
        return vc
    }

    /** null si se cancela. */
    private class Delegado(val alElegir: (NSURL?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            alElegir(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            alElegir(null)
        }
    }
}
