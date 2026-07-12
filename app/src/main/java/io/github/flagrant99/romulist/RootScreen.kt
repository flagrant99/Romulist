package io.github.flagrant99.romulist

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.StatFs
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
    storageStatsManager: StorageStatsManager?,
    context: Context,
    onVolumeClick: (File) -> Unit,
    onVolumeFocus: (File?) -> Unit,
    onListPackages: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
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
                val packageInfo = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
                val appVersion = packageInfo?.versionName ?: "Unknown"

                Text(
                    text = "ROMULIST",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "v$appVersion",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )

                val osVersion = android.os.Build.VERSION.RELEASE
                val is64Bit = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
                val arch = if (is64Bit) "64-bit" else "32-bit"
                val deviceName = "${android.os.Build.MODEL}"

                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                fun formatRam(bytes: Long): String {
                    return if (bytes >= 1024L * 1024 * 1024) {
                        "${(bytes + 512L * 1024 * 1024) / (1024 * 1024 * 1024)} GB"
                    } else {
                        "${bytes / (1024 * 1024)} MB"
                    }
                }

                val usableRamBytes = memoryInfo.totalMem
                val usableRamStr = formatRam(usableRamBytes)

                val physicalRamBytes = run {
                    val gb = usableRamBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val tiers = listOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64)
                    val matchedTier = tiers.firstOrNull { it >= gb } ?: gb.toInt()
                    matchedTier.toLong() * 1024 * 1024 * 1024
                }
                val physicalRamStr = formatRam(physicalRamBytes)

                val advertisedRamBytes = if (android.os.Build.VERSION.SDK_INT >= 34) {
                    memoryInfo.advertisedMem
                } else {
                    physicalRamBytes
                }
                val virtualRamStr = formatRam(advertisedRamBytes)

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
                Text(
                    text = "Device: $deviceName",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
                if (usableRamStr != physicalRamStr) {
                    Text(
                        text = "Usable RAM: $usableRamStr",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Green
                    )
                }
                Text(
                    text = "Physical RAM: $physicalRamStr",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
                if (virtualRamStr != physicalRamStr) {
                    Text(
                        text = "Virtual RAM: $virtualRamStr",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Green
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Green.copy(alpha = 0.3f)
                )
            }
        }

        itemsIndexed(volumes) { index, volume ->
            var isFocused by remember { mutableStateOf(false) }

            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            var totalStr = "Unknown"
            var freeStr = "Unknown"
            try
            {
                val dir = volume.directory
                if (dir != null)
                {
                    val stat = StatFs(dir.absolutePath)
                    val totalBytes = stat.blockCountLong * stat.blockSizeLong
                    val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
                    totalStr = Formatter.formatFileSize(context, totalBytes)
                    freeStr = Formatter.formatFileSize(context, freeBytes)
                }
                else if (volume.isPrimary)
                {
                    storageStatsManager?.let { manager ->
                        val totalBytes = manager.getTotalBytes(StorageManager.UUID_DEFAULT)
                        val freeBytes = manager.getFreeBytes(StorageManager.UUID_DEFAULT)
                        totalStr = Formatter.formatFileSize(context, totalBytes)
                        freeStr = Formatter.formatFileSize(context, freeBytes)
                    }
                }
            } catch (e: Exception)
            {
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(if (index == 0) firstVolumeFocusRequester else remember { FocusRequester() })
                    .onFocusChanged { 
                        isFocused = it.isFocused 
                        if (it.isFocused) {
                            val root = volume.directory ?: if (volume.isPrimary) android.os.Environment.getExternalStorageDirectory() else File("/")
                            onVolumeFocus(root)
                        }
                    }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    val root = volume.directory ?: if (volume.isPrimary) android.os.Environment.getExternalStorageDirectory() else File("/")
                                    onVolumeClick(root)
                                    true
                                }
                                backKey -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        val root = volume.directory ?: if (volume.isPrimary) android.os.Environment.getExternalStorageDirectory() else File("/")
                        onVolumeClick(root)
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

        item {
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { 
                        isFocused = it.isFocused 
                        if (it.isFocused) {
                            onVolumeFocus(null)
                        }
                    }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    onListPackages()
                                    true
                                }
                                backKey -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onListPackages() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "LIST PACKAGES",
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
                    text = "View all installed package names",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                    ),
                    color = Color.Green
                )
            }
        }

        item {
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { 
                        isFocused = it.isFocused 
                        if (it.isFocused) {
                            onVolumeFocus(null)
                        }
                    }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    onExit()
                                    true
                                }
                                backKey -> {
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onExit() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "EXIT",
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
                    text = "Close Application",
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
