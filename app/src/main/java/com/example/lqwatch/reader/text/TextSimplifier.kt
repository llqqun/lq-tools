package com.example.lqwatch.reader.text

import com.github.houbb.opencc4j.util.ZhConverterUtil

/**
 * 简体中文转换：一律将繁体转简体（opencc4j 本地词典，不联网）。
 * 英文及非汉字字符原样保留。
 */
object TextSimplifier {

    fun toSimplified(text: String): String {
        if (text.isBlank()) return text
        return runCatching { ZhConverterUtil.toSimple(text) }.getOrDefault(text)
    }
}
