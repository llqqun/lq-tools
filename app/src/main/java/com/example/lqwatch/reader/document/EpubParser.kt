package com.example.lqwatch.reader.document

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * EPUB 解析：epub 本质是 zip，按标准流程解包
 * container.xml -> OPF(manifest/spine) -> 章节 XHTML -> 纯文本。
 * 纯手写解析，零第三方依赖。
 */
object EpubParser {

    fun parse(context: Context, uri: Uri): List<Chapter> {
        val entries = context.contentResolver.openInputStream(uri)?.use { readZipTextEntries(it) }
            ?: throw IllegalStateException("无法读取文件")

        val container = entries["META-INF/container.xml"]
            ?: throw IllegalArgumentException("无效的 EPUB：缺少 container.xml")
        val opfPath = Regex("""full-path\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
            .find(container)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("无效的 EPUB：未找到 OPF 路径")
        val opf = entries[opfPath]
            ?: throw IllegalArgumentException("无效的 EPUB：找不到 OPF 文件")

        val opfDir = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

        val manifest = HashMap<String, String>()
        for (tag in Regex("""<item\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(opf)) {
            val attrs = attrs(tag.value)
            val id = attrs["id"] ?: continue
            val href = attrs["href"] ?: continue
            manifest[id] = URLDecoder.decode(href, "UTF-8")
        }

        val spine = Regex("""<itemref\b[^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(opf)
            .mapNotNull { attrs(it.value)["idref"] }
            .toList()

        val chapters = mutableListOf<Chapter>()
        var idx = 0
        for (idref in spine) {
            val href = manifest[idref] ?: continue
            val raw = entries[opfDir + href] ?: continue
            val text = htmlToText(raw)
            if (text.isBlank()) continue
            idx++
            chapters.add(Chapter("第 $idx 章", TextBlocks.toParagraphs(text)))
        }
        if (chapters.isEmpty()) throw IllegalArgumentException("EPUB 中没有可读章节")
        return chapters
    }

    private fun attrs(tag: String): Map<String, String> {
        val result = HashMap<String, String>()
        Regex("""([\w:.-]+)\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE).findAll(tag).forEach { m ->
            result[m.groupValues[1]] = m.groupValues[2]
        }
        return result
    }

    private fun readZipTextEntries(input: InputStream): Map<String, String> {
        val map = HashMap<String, String>()
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && isRelevant(entry.name)) {
                    val bytes = zip.readBytes()
                    map[entry.name] = decodeXmlText(bytes)
                }
                entry = zip.nextEntry
            }
        }
        return map
    }

    private fun isRelevant(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".xml") ||
            lower.endsWith(".opf") ||
            lower.endsWith(".xhtml") ||
            lower.endsWith(".html") ||
            lower.endsWith(".htm")
    }

    private fun decodeXmlText(bytes: ByteArray): String {
        val head = String(bytes, 0, minOf(bytes.size, 512), Charsets.ISO_8859_1)
        val enc = Regex("""encoding\s*=\s*["']([A-Za-z0-9_\-]+)["']""", RegexOption.IGNORE_CASE)
            .find(head)?.groupValues?.get(1)
        val text = if (enc != null) {
            try {
                String(bytes, Charset.forName(enc))
            } catch (_: Exception) {
                String(bytes, Charsets.UTF_8)
            }
        } else {
            String(bytes, Charsets.UTF_8)
        }
        // 去除 UTF-8 BOM 残留字符
        return text.replace("\uFEFF", "")
    }

    private fun htmlToText(html: String): String {
        var s = html
        // 移除 script / style
        s = Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(s, "")
        s = Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(s, "")
        // 块级标签转换行
        s = Regex("""<(br|/p|/div|/h[1-6]|/li|/tr)\b[^>]*>""", RegexOption.IGNORE_CASE).replace(s) { "\n" }
        s = Regex("""<p\b[^>]*>""", RegexOption.IGNORE_CASE).replace(s, "\n\n")
        // 去除剩余标签
        s = s.replace(Regex("""<[^>]+>"""), "")
        // 反转义实体
        s = s.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
        s = Regex("""&#(\d+);""").replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.let { code ->
                runCatching { code.toChar().toString() }.getOrNull()
            } ?: m.value
        }
        // 折叠多余空行
        s = s.replace(Regex("""[ \t]+\n"""), "\n")
        s = s.replace(Regex("""\n{3,}"""), "\n\n")
        return s.trim()
    }
}
