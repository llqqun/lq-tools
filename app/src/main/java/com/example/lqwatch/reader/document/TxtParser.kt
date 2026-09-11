package com.example.lqwatch.reader.document

import com.example.lqwatch.reader.text.TextDecoder

object TxtParser {

    /** 解码原始字节为字符串（去除 BOM 残留字符） */
    fun parse(bytes: ByteArray): String =
        TextDecoder.decode(bytes).replace("\uFEFF", "")
}

object TextBlocks {

    /** 按空行切分为段落，段落内保留换行 */
    fun toParagraphs(text: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        for (line in text.lines()) {
            if (line.isBlank()) {
                if (sb.isNotBlank()) {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
            } else {
                sb.append(line.trim()).append('\n')
            }
        }
        if (sb.isNotBlank()) result.add(sb.toString().trim())
        return result
    }
}
