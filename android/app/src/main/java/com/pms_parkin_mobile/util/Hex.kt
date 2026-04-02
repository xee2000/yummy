package com.pms_parkin_mobile.util

object Hex {
    private val HEX_STRING_LOWER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    private val HEX_STRING_UPPER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    const val HEX_LOWER: Int = 0
    const val HEX_UPPER: Int = 1


    fun bytesToHex(bytes: ByteArray, start: Int, length: Int, hexType: Int): String {
        require(!(bytes == null || bytes.size <= start + length)) { "bytes cannot be null or invalid length" }

        var HEX = HEX_STRING_LOWER
        if (hexType == 1) {
            HEX = HEX_STRING_UPPER
        }

        val result = CharArray(2 * length)
        var j = 0
        for (i in start..<start + length) {
            result[j++] = HEX[(0xf0 and bytes[i].toInt()) ushr 4]
            result[j++] = HEX[(0x0f and bytes[i].toInt())]
        }
        return String(result)
    }
}
