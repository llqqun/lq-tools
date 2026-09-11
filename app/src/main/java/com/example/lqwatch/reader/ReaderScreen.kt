package com.example.lqwatch.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lqwatch.reader.document.Chapter
import com.example.lqwatch.reader.document.DocumentType
import com.example.lqwatch.reader.document.DocumentTypeClassifier
import com.example.lqwatch.reader.document.EpubParser
import com.example.lqwatch.reader.document.PdfLoader
import com.example.lqwatch.reader.document.TextBlocks
import com.example.lqwatch.reader.document.TxtParser
import com.example.lqwatch.reader.pagination.TextPager
import com.example.lqwatch.reader.text.TextSimplifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String) : UiState
    data class TextDoc(val title: String, val chapters: List<Chapter>) : UiState {
        val fullText: String
            get() = chapters.joinToString("\n\n") { it.fullText }
    }
    data class PdfDoc(val title: String, val pageCount: Int) : UiState
}

private sealed interface TextItem {
    data class Header(val title: String) : TextItem
    data class Para(val text: String) : TextItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(uri: Uri, displayName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ReaderSettingsRepository(context) }
    var settings by remember { mutableStateOf(ReaderSettings()) }
    LaunchedEffect(Unit) {
        repository.settings.collect { settings = it }
    }

    var state by remember(uri) { mutableStateOf<UiState>(UiState.Loading) }
    val pdfLoader = remember { PdfLoader(context) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        state = UiState.Loading
        withContext(Dispatchers.IO) {
            try {
                val type = DocumentTypeClassifier.fromName(displayName)
                    ?: throw IllegalArgumentException("不支持的文件格式：$displayName")
                state = when (type) {
                    DocumentType.TXT -> {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("无法读取文件")
                        val simplified = TextSimplifier.toSimplified(TxtParser.parse(bytes))
                        UiState.TextDoc(displayName, listOf(Chapter("正文", TextBlocks.toParagraphs(simplified))))
                    }
                    DocumentType.EPUB -> {
                        val chapters = EpubParser.parse(context, uri).map { ch ->
                            ch.copy(paragraphs = TextBlocks.toParagraphs(TextSimplifier.toSimplified(ch.fullText)))
                        }
                        UiState.TextDoc(displayName, chapters)
                    }
                    DocumentType.PDF -> {
                        val renderer = pdfLoader.open(uri)
                        pdfRenderer = renderer
                        UiState.PdfDoc(displayName, renderer.pageCount)
                    }
                }
            } catch (e: Exception) {
                state = UiState.Error(e.message ?: "打开失败")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { pdfLoader.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "阅读设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(settings.bgPreset.background)
        ) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is UiState.Error -> ErrorView(
                    message = s.message,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
                is UiState.TextDoc -> TextReaderContent(
                    doc = s,
                    settings = settings,
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.PdfDoc -> PdfReaderContent(
                    doc = s,
                    renderer = pdfRenderer,
                    settings = settings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
            onChange = { new ->
                settings = new
                scope.launch { repository.update { new } }
            }
        )
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onBack) {
            Text("返回")
        }
    }
}

@Composable
private fun TextReaderContent(
    doc: UiState.TextDoc,
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = TextStyle(
        color = settings.bgPreset.foreground,
        fontSize = settings.fontSizeSp.sp,
        lineHeight = (settings.fontSizeSp * settings.lineHeightRatio).sp
    )

    when (settings.mode) {
        ReadingMode.SCROLL -> {
            val items = remember(doc) {
                buildList {
                    doc.chapters.forEach { ch ->
                        add(TextItem.Header(ch.title))
                        ch.paragraphs.forEach { add(TextItem.Para(it)) }
                    }
                }
            }
            LazyColumn(
                modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items) { _, item ->
                    when (item) {
                        is TextItem.Header -> Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = settings.bgPreset.foreground,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        is TextItem.Para -> Text(
                            text = item.text,
                            style = textStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        ReadingMode.PAGED -> {
            BoxWithConstraints(modifier = modifier) {
                val pageWidth = with(density) { maxWidth.toPx().toInt() }
                val pageHeight = with(density) { maxHeight.toPx().toInt() }
                val hPad = with(density) { 20.dp.toPx().toInt() }
                val vPad = with(density) { 12.dp.toPx().toInt() }
                val lineHeightPx = with(density) {
                    (settings.fontSizeSp * settings.lineHeightRatio).sp.toPx().toInt()
                }
                val pages = remember(doc, settings, pageWidth, pageHeight) {
                    TextPager.paginate(
                        text = doc.fullText,
                        measurer = measurer,
                        style = textStyle,
                        pageWidthPx = pageWidth,
                        pageHeightPx = pageHeight,
                        horizontalPaddingPx = hPad,
                        verticalPaddingPx = vPad,
                        lineHeightPx = lineHeightPx
                    )
                }
                if (pages.isEmpty()) {
                    Text(
                        text = "无内容",
                        color = settings.bgPreset.foreground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { pages.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val range = pages[page]
                        Text(
                            text = doc.fullText.substring(range.first, range.last + 1),
                            style = textStyle,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReaderContent(
    doc: UiState.PdfDoc,
    renderer: PdfRenderer?,
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val targetWidth = with(density) { maxWidth.toPx().toInt() }
        val targetHeight = with(density) { maxHeight.toPx().toInt() }
        val cache = remember { LruCache<Int, Bitmap>(16) }

        when (settings.mode) {
            ReadingMode.SCROLL -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(settings.bgPreset.background),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(doc.pageCount) { index ->
                    PdfPage(renderer, index, targetWidth, targetHeight, cache, settings)
                }
            }
            ReadingMode.PAGED -> {
                val pagerState = rememberPagerState(pageCount = { doc.pageCount })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(settings.bgPreset.background)
                ) { index ->
                    PdfPage(renderer, index, targetWidth, targetHeight, cache, settings)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRenderer?,
    index: Int,
    targetWidth: Int,
    targetHeight: Int,
    cache: LruCache<Int, Bitmap>,
    settings: ReaderSettings
) {
    var bitmap by remember(index, renderer) { mutableStateOf(cache.get(index)) }
    LaunchedEffect(index, renderer) {
        if (renderer == null) return@LaunchedEffect
        bitmap = cache.get(index) ?: withContext(Dispatchers.Default) {
            PdfLoader.renderPage(renderer, index, targetWidth, targetHeight)
                .also { cache.put(index, it) }
        }
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(24.dp))
        }
    } else {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "第 ${index + 1} 页",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
