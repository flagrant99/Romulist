package io.github.flagrant99.romulist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun FolderDetail(
    folder: File,
    onBack: () -> Unit
) {
    var subfolders by remember(folder) { mutableStateOf<List<Pair<File, Long>>>(emptyList()) }
    var isLoading by remember(folder) { mutableStateOf(true) }

    LaunchedEffect(folder) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val list = folder.listFiles()?.filter { it.isDirectory }?.map { subfolder ->
                subfolder to calculateRecursiveSize(subfolder)
            }?.sortedBy { it.first.name.lowercase() } ?: emptyList()
            subfolders = list
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "FOLDER: ${folder.name}",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
            color = Color.Green,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(color = Color.Green.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 16.dp))

        if (isLoading) {
            Text(
                text = "Calculating sizes...",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.Gray
            )
        } else if (subfolders.isEmpty()) {
            Text(
                text = "No subfolders found",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.Gray
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(subfolders) { (subfolder, size) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = subfolder.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Green,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatSize(size),
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = Color.LightGray
                        )
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

private fun calculateRecursiveSize(file: File): Long {
    if (file.isFile) return file.length()
    var totalSize: Long = 0
    val children = file.listFiles()
    if (children != null) {
        for (child in children) {
            totalSize += calculateRecursiveSize(child)
        }
    }
    return totalSize
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return if (gb >= 1.0) {
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        String.format(Locale.US, "%.2f MB", mb)
    }
}
