package com.example.lqwatch.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onChange: (ReaderSettings) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("阅读设置", style = MaterialTheme.typography.titleLarge)

            SettingRow("背景") {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BgPresets.list.forEachIndexed { i, preset ->
                        val selected = settings.bgIndex == i
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onChange(settings.copy(bgIndex = i)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(preset.background)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SettingRow("字号") {
                Stepper(
                    label = settings.fontSizeSp.toInt().toString(),
                    onMinus = { onChange(settings.copy(fontSizeSp = (settings.fontSizeSp - 1).coerceIn(12f, 32f))) },
                    onPlus = { onChange(settings.copy(fontSizeSp = (settings.fontSizeSp + 1).coerceIn(12f, 32f))) }
                )
            }

            SettingRow("行高") {
                Stepper(
                    label = "%.1f".format(settings.lineHeightRatio),
                    onMinus = { onChange(settings.copy(lineHeightRatio = (settings.lineHeightRatio - 0.1f).coerceIn(1.0f, 2.0f))) },
                    onPlus = { onChange(settings.copy(lineHeightRatio = (settings.lineHeightRatio + 0.1f).coerceIn(1.0f, 2.0f))) }
                )
            }

            SettingRow("阅读模式") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReadingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.mode == mode,
                            onClick = { onChange(settings.copy(mode = mode)) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "减少")
        }
        Text(
            label,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "增加")
        }
    }
}
