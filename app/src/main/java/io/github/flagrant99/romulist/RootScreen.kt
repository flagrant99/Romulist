package io.github.flagrant99.romulist

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import java.io.File

@Composable
fun RootScreen(
    volumes: List<StorageVolume>,
    storageManager: StorageManager?,
    storageStatsManager: StorageStatsManager?,
    context: Context,
    onVolumeClick: (File) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstVolumeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(volumes) {
        if (volumes.isNotEmpty()) {
            firstVolumeFocusRequester.requestFocus()
        }
    }

    LazyColumn(modifier = modifier)
    {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                val osVersion = android.os.Build.VERSION.RELEASE
                val is64Bit = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
                val arch = if (is64Bit) "64-bit" else "32-bit"

                Text(
                    text = "SYSTEM INFO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.5f), blurRadius = 8f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "OS: Android $osVersion",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
                Text(
                    text = "Architecture: $arch",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Green.copy(alpha = 0.3f)
                )
            }
        }

        itemsIndexed(volumes) { index, volume ->
            var isFocused by remember { mutableStateOf(false) }
            val uuid = try
            {
                if (volume.isPrimary)
                {
                    StorageManager.UUID_DEFAULT
                }
                else
                {
                    volume.directory?.let { storageManager?.getUuidForPath(it) }
                        ?: StorageManager.UUID_DEFAULT
                }
            } catch (e: Exception)
            {
                StorageManager.UUID_DEFAULT
            }

            var totalStr = "Unknown"
            var freeStr = "Unknown"
            try
            {
                storageStatsManager?.let { manager ->
                    val totalBytes = manager.getTotalBytes(uuid)
                    val freeBytes = manager.getFreeBytes(uuid)
                    totalStr = Formatter.formatFileSize(context, totalBytes)
                    freeStr = Formatter.formatFileSize(context, freeBytes)
                }
            } catch (e: Exception)
            {
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(if (index == 0) firstVolumeFocusRequester else remember { FocusRequester() })
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_BUTTON_B -> {
                                    onVolumeClick(volume.directory ?: File("/"))
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_CENTER -> {
                                    onVolumeClick(volume.directory ?: File("/"))
                                    true
                                }
                                KeyEvent.KEYCODE_ENTER -> {
                                    onVolumeClick(volume.directory ?: File("/"))
                                    true
                                }
                                KeyEvent.KEYCODE_BUTTON_A -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        onVolumeClick(volume.directory ?: File("/"))
                    }
                    .padding(16.dp)
            )
            {
                Text(
                    text = volume.getDescription(context),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(
                            color = Color.Green.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    ),
                    color = Color.Green
                )
                Text(
                    text = "Total: $totalStr | Free: $freeStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                    ),
                    color = Color.Green
                )
            }
        }
    }
}
