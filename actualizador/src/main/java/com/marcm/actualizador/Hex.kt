package com.marcm.actualizador

private const val HEX = "0123456789abcdef"

/** Bytes a hexadecimal en minúsculas (para comparar SHA-256 con el manifiesto). */
internal fun ByteArray.aHexMinuscula(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX[v ushr 4])
        sb.append(HEX[v and 0x0F])
    }
    return sb.toString()
}
