package com.example.lqwatch.reader.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * 文本解码：优先按 BOM 判定，其次严格校验 UTF-8，
 * 失败则回退 GB18030（GBK/GB2312 的超集，覆盖常见中文编码），保证不乱码。
 */
object TextDecoder {

    fun decode(bytes: ByteArray): String {
        // UTF-8 BOM
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        // UTF-32 LE BOM
        if (bytes.size >= 4 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() &&
            bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()
        ) {
            return String(bytes, 4, bytes.size - 4, Charset.forName("UTF-32LE"))
        }
        // UTF-32 BE BOM
        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
            bytes[2] == 0xFE.toByte() && bytes[3] == 0xFF.toByte()
        ) {
            return String(bytes, 4, bytes.size - 4, Charset.forName("UTF-32BE"))
        }
        // UTF-16 LE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        // UTF-16 BE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        // 无 BOM：严格按 UTF-8 解码，出现非法字节则说明不是 UTF-8
        val decoder = Charset.forName("UTF-8").newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            // 中文文本常见 GBK/GB2312，GB18030 是其超集，可无损解码
            String(bytes, Charset.forName("GB18030"))
        }
    }
}
