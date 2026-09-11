package com.example.lqwatch.reader.pagination

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * 文本分页：按给定视口宽度/高度、字体与行高，把整段文本切成一页页的字符区间。
 * 通过 TextMeasurer 分块测量，得到每页实际容纳的字符数。
 */
object TextPager {

    fun paginate(
        text: String,
        measurer: TextMeasurer,
        style: TextStyle,
        pageWidthPx: Int,
        pageHeightPx: Int,
        horizontalPaddingPx: Int,
        verticalPaddingPx: Int,
        lineHeightPx: Int
    ): List<IntRange> {
        if (text.isBlank()) return emptyList()
        val contentWidth = (pageWidthPx - 2 * horizontalPaddingPx).coerceAtLeast(1)
        val contentHeight = (pageHeightPx - 2 * verticalPaddingPx).coerceAtLeast(1)
        val constraints = Constraints(maxWidth = contentWidth, maxHeight = contentHeight)
        val maxLines = (contentHeight / lineHeightPx.coerceAtLeast(1)).toInt().coerceAtLeast(1)

        val ranges = mutableListOf<IntRange>()
        var cursor = 0
        val chunkSize = 2000
        while (cursor < text.length) {
            val end = minOf(text.length, cursor + chunkSize)
            val layout: TextLayoutResult = measurer.measure(
                text = AnnotatedString(text.substring(cursor, end)),
                style = style,
                constraints = constraints,
                maxLines = maxLines,
                overflow = TextOverflow.Clip
            )
            var newCursor: Int
            if (layout.lineCount <= maxLines) {
                newCursor = end
            } else {
                val lineEnd = layout.getLineEnd(maxLines - 1, visibleEnd = true).coerceAtLeast(1)
                newCursor = (cursor + lineEnd).coerceAtMost(end)
            }
            if (newCursor <= cursor) {
                // 单行也放不下时至少推进一个字符，避免死循环
                newCursor = (cursor + 1).coerceAtMost(text.length)
            }
            ranges.add(cursor until newCursor)
            cursor = newCursor
        }
        return ranges
    }
}
