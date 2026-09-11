package com.example.lqwatch.reader.document

data class Chapter(val title: String, val paragraphs: List<String>) {
    val fullText: String
        get() = paragraphs.joinToString("\n\n")
}

enum class DocumentType { TXT, EPUB, PDF }

object DocumentTypeClassifier {
    fun fromName(name: String): DocumentType? = when (name.substringAfterLast('.', "").lowercase()) {
        "txt" -> DocumentType.TXT
        "epub" -> DocumentType.EPUB
        "pdf" -> DocumentType.PDF
        else -> null
    }
}
