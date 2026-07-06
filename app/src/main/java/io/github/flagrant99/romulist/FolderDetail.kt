package io.github.flagrant99.romulist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class SortColumn {
    NAME, SIZE
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

@Composable
fun FolderDetail(
    folder: File,
    onBack: () -> Unit
) {
    var subfolders by remember(folder) { mutableStateOf<List<Pair<File, Long>>>(emptyList()) }
    var isLoading by remember(folder) { mutableStateOf(true) }
    var sortColumn by remember { mutableStateOf(SortColumn.NAME) }
    var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }

    LaunchedEffect(folder) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val list = folder.listFiles()?.filter { it.isDirectory }?.map { subfolder ->
                subfolder to calculateRecursiveSize(subfolder)
            } ?: emptyList()
            subfolders = list
        }
        isLoading = false
    }

    val sortedSubfolders = remember(subfolders, sortColumn, sortOrder) {
        val comparator = when (sortColumn) {
            SortColumn.NAME -> compareBy<Pair<File, Long>> { it.first.name.lowercase() }
            SortColumn.SIZE -> compareBy<Pair<File, Long>> { it.second }
        }
        if (sortOrder == SortOrder.ASCENDING) {
            subfolders.sortedWith(comparator)
        } else {
            subfolders.sortedWith(comparator.reversed())
        }
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
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                val nameIndicator = if (sortColumn == SortColumn.NAME) {
                    if (sortOrder == SortOrder.ASCENDING) " ▲" else " ▼"
                } else ""
                Text(
                    text = "Name$nameIndicator",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (sortColumn == SortColumn.NAME) Color.Green else Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (sortColumn == SortColumn.NAME) {
                                sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            } else {
                                sortColumn = SortColumn.NAME
                                sortOrder = SortOrder.ASCENDING
                            }
                        }
                )

                Spacer(modifier = Modifier.width(16.dp))

                val sizeIndicator = if (sortColumn == SortColumn.SIZE) {
                    if (sortOrder == SortOrder.ASCENDING) " ▲" else " ▼"
                } else ""
                Text(
                    text = "Size$sizeIndicator",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (sortColumn == SortColumn.SIZE) Color.Green else Color.Gray,
                    modifier = Modifier.clickable {
                        if (sortColumn == SortColumn.SIZE) {
                            sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                        } else {
                            sortColumn = SortColumn.SIZE
                            sortOrder = SortOrder.ASCENDING
                        }
                    }
                )
            }
            HorizontalDivider(color = Color.Green.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedSubfolders) { (subfolder, size) ->
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
