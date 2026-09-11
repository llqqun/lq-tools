package com.example.lqwatch.reader

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.readerSettingsDataStore by preferencesDataStore(name = "reader_settings")

data class BgPreset(val name: String, val background: Color, val foreground: Color)

object BgPresets {
    val list = listOf(
        BgPreset("白色", Color(0xFFFFFFFF), Color(0xFF1F1F1F)),
        BgPreset("米黄", Color(0xFFF7F1E3), Color(0xFF3E3A34)),
        BgPreset("护眼绿", Color(0xFFCDE7D0), Color(0xFF2E3B2E)),
        BgPreset("深色", Color(0xFF121212), Color(0xFFC8C8C8))
    )
}

data class ReaderSettings(
    val bgIndex: Int = 0,
    val fontSizeSp: Float = 17f,
    val lineHeightRatio: Float = 1.5f,
    val mode: ReadingMode = ReadingMode.SCROLL
) {
    val bgPreset: BgPreset
        get() = BgPresets.list[bgIndex.coerceIn(0, BgPresets.list.lastIndex)]
}

private object Keys {
    val BG = intPreferencesKey("bg_index")
    val FONT = floatPreferencesKey("font_size")
    val LINE_HEIGHT = floatPreferencesKey("line_height_ratio")
    val MODE = stringPreferencesKey("mode")
}

class ReaderSettingsRepository(private val context: Context) {

    val settings: Flow<ReaderSettings> = context.readerSettingsDataStore.data.map { prefs ->
        ReaderSettings(
            bgIndex = prefs[Keys.BG] ?: 0,
            fontSizeSp = prefs[Keys.FONT] ?: 17f,
            lineHeightRatio = prefs[Keys.LINE_HEIGHT] ?: 1.5f,
            mode = runCatching {
                ReadingMode.valueOf(prefs[Keys.MODE] ?: "")
            }.getOrDefault(ReadingMode.SCROLL)
        )
    }

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        context.readerSettingsDataStore.edit { prefs ->
            val current = ReaderSettings(
                bgIndex = prefs[Keys.BG] ?: 0,
                fontSizeSp = prefs[Keys.FONT] ?: 17f,
                lineHeightRatio = prefs[Keys.LINE_HEIGHT] ?: 1.5f,
                mode = runCatching {
                    ReadingMode.valueOf(prefs[Keys.MODE] ?: "")
                }.getOrDefault(ReadingMode.SCROLL)
            )
            val next = transform(current)
            prefs[Keys.BG] = next.bgIndex
            prefs[Keys.FONT] = next.fontSizeSp
            prefs[Keys.LINE_HEIGHT] = next.lineHeightRatio
            prefs[Keys.MODE] = next.mode.name
        }
    }
}
