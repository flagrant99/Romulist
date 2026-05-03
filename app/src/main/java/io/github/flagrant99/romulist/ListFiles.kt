package io.github.flagrant99.romulist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ListFiles(
    currentPath: File,
    favoritePath: String?,
    onPathChange: (File) -> Unit
) {
    val context = LocalContext.current

    // Recursive search for the nearest romulist.xml upward to implement persistence
    val configResult = remember(currentPath, favoritePath) {
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
    val activeConfigPath = configResult.second

    val allowedExtensions = remember(romulistConfig) {
        romulistConfig?.folders?.flatMap { it.extensions }?.map { it.lowercase() }?.toSet() ?: emptySet()
    }

    var files by remember(currentPath, allowedExtensions) { mutableStateOf<List<File>>(emptyList()) }
    var isScanning by remember(currentPath, allowedExtensions) { mutableStateOf(true) }

    LaunchedEffect(currentPath, allowedExtensions) {
        isScanning = true
        withContext(Dispatchers.IO) {
            val result = currentPath.listFiles()
                ?.filter { it.name != "romulist.xml" && !it.name.startsWith(".") }
                ?.filter { file ->
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Path: ${currentPath.absolutePath}",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        if (activeConfigPath != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "Config active from: $activeConfigPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green
                )
                if (allowedExtensions.isNotEmpty()) {
                    Text(
                        text = "Filtering by: ${allowedExtensions.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Cyan
                    )
                }
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files) { file ->
                    FileRow(
                        name = file.name,
                        isDirectory = file.isDirectory
                    ) {
                        if (file.isDirectory) {
                            onPathChange(file)
                        } else {
                            // NEW: Launch game only if a favorite folder is set and file is underneath it
                            favoritePath?.let { fav ->
                                if (file.absolutePath.startsWith(fav)) {
                                    EmulatorNavigator.launchGame(
                                        context = context,
                                        favoritePath = favoritePath,
                                        filePath = file.absolutePath,
                                        config = romulistConfig
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
