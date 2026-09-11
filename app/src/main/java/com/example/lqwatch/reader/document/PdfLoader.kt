package com.example.lqwatch.reader.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException

/**
 * PDF 加载：使用系统内置 PdfRenderer 逐页渲染为位图。
 * 注意：PdfRenderer 必须在同一线程使用，退出时调用 [close]。
 */
class PdfLoader(private val context: Context) {

    private var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
        private set

    fun open(uri: Uri): PdfRenderer {
        close()
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("无法打开 PDF 文件")
        val r = PdfRenderer(descriptor)
        pfd = descriptor
        renderer = r
        return r
    }

    fun close() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        renderer = null
        pfd = null
    }

    companion object {
        fun renderPage(renderer: PdfRenderer, index: Int, maxWidthPx: Int, maxHeightPx: Int): Bitmap {
            require(index in 0 until renderer.pageCount) { "页码越界" }
            renderer.openPage(index).use { page ->
                val scale = minOf(
                    maxWidthPx / page.width.toFloat(),
                    maxHeightPx / page.height.toFloat(),
                    3.5f
                ).coerceAtLeast(0.4f)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bmp
            }
        }
    }
}
