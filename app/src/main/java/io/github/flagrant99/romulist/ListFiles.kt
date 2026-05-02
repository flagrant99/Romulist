package io.github.flagrant99.romulist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ListFiles(
    currentPath: File,
    favoritePath: String?,
    onPathChange: (File) -> Unit
) {
    val context = LocalContext.current
    val files = remember(currentPath) {
        currentPath.listFiles()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Path: ${currentPath.absolutePath}",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        if (files.isEmpty()) {
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
                                        filePath = file.absolutePath
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
