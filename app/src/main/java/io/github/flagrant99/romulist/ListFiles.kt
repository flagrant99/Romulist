package io.github.flagrant99.romulist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import io.github.flagrant99.romulist.ui.theme.Black80
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun ListFiles(
    currentPath: File,
    favoritePath: String?,
    selectedFile: File?,
    listState: LazyListState = rememberLazyListState(),
    onPathChange: (File) -> Unit,
    onFileSelect: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refreshToggle by remember { mutableStateOf(false) }

    var lastPath by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(currentPath) {
        if (lastPath != currentPath.absolutePath) {
            listState.scrollToItem(0)
            lastPath = currentPath.absolutePath
        }
    }

    // Copy romulist.xml from assets if it matches the structure under favorite path
    LaunchedEffect(currentPath, favoritePath) {
        if (favoritePath == null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val favFile = File(favoritePath)
            val isInsideFavorite = currentPath.absolutePath == favFile.absolutePath ||
                    currentPath.absolutePath.startsWith(favFile.absolutePath + File.separator)

            if (isInsideFavorite) {
                val localConfig = File(currentPath, "romulist.xml")
                if (!localConfig.exists()) {
                    var relative = currentPath.absolutePath.removePrefix(favFile.absolutePath)
                    if (relative.startsWith(File.separator)) {
                        relative = relative.substring(1)
                    }

                    // Skip if we are at the favorite root; configs are in system sub-directories
                    if (relative.isEmpty()) return@withContext

                    // Normalize path for assets (always use forward slashes)
                    val assetSubPath = relative.replace(File.separator, "/")
                    val assetPath = "ROMs/$assetSubPath/romulist.xml"

                    try {
                        context.assets.open(assetPath).use { input ->
                            localConfig.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            refreshToggle = !refreshToggle
                        }
                    } catch (_: Exception) {
                        // No matching config in assets, ignore
                    }
                }
            }
        }
    }

    // Recursive search for the nearest romulist.xml upward to implement persistence
    val configResult = remember(currentPath, favoritePath, refreshToggle) {
        var dir: File? = currentPath
        val favFile = favoritePath?.let { File(it) }
        var foundConfig: EmulatorNavigator.RomulistConfig? = null
        var configSource: String? = null

        while (dir != null) {
            val configFile = File(dir, "romulist.xml")
            if (configFile.exists()) {
                foundConfig = EmulatorNavigator.parseConfig(configFile)
                if (foundConfig != null) {
                    configSource = configFile.absolutePath
                    break
                }
            }
            // Stop searching if we reach the favorite path root to avoid leaking config outside the intended tree
            if (favFile != null && dir.absolutePath == favFile.absolutePath) break
            dir = dir.parentFile
        }
        foundConfig to configSource
    }

    val romulistConfig = configResult.first

    val system = romulistConfig?.systemConfig
    val preferredIntent = system?.mainIntent

    val allowedExtensions = remember(romulistConfig) {
        romulistConfig?.systemConfig?.extensions?.map { it.lowercase() }?.toSet() ?: emptySet()
    }

    val nameExclusions = remember(romulistConfig) {
        romulistConfig?.nameExclusions ?: emptyList()
    }

    var files by remember(currentPath, allowedExtensions, nameExclusions) { mutableStateOf<List<File>>(emptyList()) }
    var isScanning by remember(currentPath, allowedExtensions, nameExclusions) { mutableStateOf(true) }

    val firstItemFocusRequester = remember { FocusRequester() }
    val selectedItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentPath) {
        // No-op here, handled by lastPath check above
    }

    LaunchedEffect(currentPath, allowedExtensions, nameExclusions) {
        isScanning = true
        withContext(Dispatchers.IO) {
            val result = currentPath.listFiles()
                ?.filter { it.name != "romulist.xml" && !it.name.startsWith(".") }
                ?.filter { file ->
                    // Filter by name exclusions (usually for folders as requested)
                    if (nameExclusions.any { it.equals(file.name, ignoreCase = true) }) return@filter false

                    // Show all directories for speed; only filter files by extension if a config is active
                    file.isDirectory || allowedExtensions.isEmpty() || file.extension.lowercase() in allowedExtensions
                }
                ?.sortedWith(
                    compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
                ) ?: emptyList()
            files = result
            isScanning = false
        }
    }

    LaunchedEffect(files, isScanning, selectedFile) {
        if (!isScanning && files.isNotEmpty()) {
            val index = files.indexOfFirst { it.absolutePath == selectedFile?.absolutePath }
            if (index != -1) {
                listState.scrollToItem(index)
                // We need to wait for the item to be composed to request focus if it's not the first item
                // or use a different mechanism. For now, let's try to focus it if it's visible.
                selectedItemFocusRequester.requestFocus()
            } else {
                firstItemFocusRequester.requestFocus()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = if (isLandscape) Modifier.weight(1f) else Modifier.fillMaxSize()) {
            Text(
                text = "Path: ${currentPath.absolutePath}",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            if (romulistConfig?.systemConfig != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .background(Black80)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${romulistConfig.systemConfig.name} : ${romulistConfig.systemConfig.mainIntent?.name ?: ""}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(Color.Green.copy(alpha = 0.5f), blurRadius = 8f)
                        ),
                        color = Color.Green,
                    )
                }
            }

            if (isScanning) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Green)
                }
            } else if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No files or folders found",
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val showIcons = files.any { it.isDirectory }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    itemsIndexed(files) { index, file ->
                        val isItemSelected = file.absolutePath == selectedFile?.absolutePath
                        FileRow(
                            name = file.name,
                            isDirectory = file.isDirectory,
                            onBack = onBack,
                            isSelected = isItemSelected,
                            showIcon = showIcons,
                            focusRequester = when {
                                isItemSelected -> selectedItemFocusRequester
                                index == 0 -> firstItemFocusRequester
                                else -> remember { FocusRequester() }
                            },
                            onFocus = {
                                if (!file.isDirectory) {
                                    onFileSelect(file)
                                }
                            },
                            onLongClick = {
                                if (file.isDirectory) {
                                    onPathChange(file)
                                } else {
                                    onFileSelect(file)
                                    // Launch game only if a favorite folder is set and file is underneath it
                                    favoritePath?.let { fav ->
                                        if (file.absolutePath.startsWith(fav)) {
                                            EmulatorNavigator.launchGame(
                                                context = context,
                                                filePath = file.absolutePath,
                                                config = romulistConfig,
                                                preferredIntent = preferredIntent
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            if (file.isDirectory) {
                                onPathChange(file)
                            } else {
                                onFileSelect(file)
                            }
                        }
                    }
                }
            }
        }

        if (isLandscape && romulistConfig?.systemConfig != null) {
            VerticalDivider(color = Color.DarkGray, thickness = 1.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ListMediaSection(
                    system = romulistConfig.systemConfig,
                    configSource = configResult.second,
                    selectedFile = selectedFile
                )
            }
        }
    }
}

@Composable
internal fun ListMediaSection(
    system: EmulatorNavigator.FolderConfig,
    configSource: String?,
    selectedFile: File?
) {
    val mediaMarqueePath = remember(system.media?.marquee, configSource) {
        system.media?.marquee?.resolvePath(configSource)
    }

    if (mediaMarqueePath != null) {
        val marqueeFile = remember(mediaMarqueePath, selectedFile) {
            if (selectedFile == null) return@remember null
            val baseName = selectedFile.nameWithoutExtension
            val dir = File(mediaMarqueePath)
            listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
                .map { File(dir, it) }
                .firstOrNull { it.exists() }
        }

        if (marqueeFile != null) {
            val bitmap = remember(marqueeFile) {
                try {
                    BitmapFactory.decodeFile(marqueeFile.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Marquee",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    val mediaScreenPath = remember(system.media?.screen, configSource) {
        system.media?.screen?.resolvePath(configSource)
    }

    if (mediaScreenPath != null) {
        val screenshotFile = remember(mediaScreenPath, selectedFile) {
            if (selectedFile == null) return@remember null
            val baseName = selectedFile.nameWithoutExtension
            val dir = File(mediaScreenPath)
            listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
                .map { File(dir, it) }
                .firstOrNull { it.exists() }
        }

        if (screenshotFile != null) {
            val bitmap = remember(screenshotFile) {
                try {
                    BitmapFactory.decodeFile(screenshotFile.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Screenshot",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
