package com.marcm.actualizador

import java.io.IOException

/**
 * Descarga y parsea el manifiesto remoto, traduciendo cualquier fallo a un
 * [ManifiestoError] tipado. No decide si hay novedad (eso es [ComparadorVersion])
 * ni calla errores: quien llama decide si informar (modo manual) o ignorar (auto).
 */
class ClienteManifiesto(
    private val transport: Transport,
    private val parser: (ByteArray) -> ManifiestoRemoto = ParserManifiesto::parsear,
) {
    @Throws(ManifiestoError::class)
    fun obtener(url: String): ManifiestoRemoto {
        val resp = try {
            transport.get(url)
        } catch (e: IOException) {
            throw ManifiestoError.SinRed(e)
        }
        if (resp.codigo !in 200..299) throw ManifiestoError.HttpNoOk(resp.codigo)
        return try {
            parser(resp.cuerpo)
        } catch (e: ManifiestoError) {
            throw e
        } catch (e: Exception) {
            throw ManifiestoError.Ilegible(e)
        }
    }
}
