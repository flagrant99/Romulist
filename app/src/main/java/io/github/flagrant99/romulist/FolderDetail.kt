package io.github.flagrant99.romulist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class SortColumn {
    NAME, SIZE, USED
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

data class FolderStats(
    val file: File,
    val size: Long,
    val used: Long
)

@Composable
fun FolderDetail(
    folder: File,
    onBack: () -> Unit
) {
    var subfolders by remember(folder) { mutableStateOf<List<FolderStats>>(emptyList()) }
    var totalStats by remember(folder) { mutableStateOf<Pair<Long, Long>?>(null) }
    var isLoading by remember(folder) { mutableStateOf(true) }
    var sortColumn by remember { mutableStateOf(SortColumn.NAME) }
    var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }

    LaunchedEffect(folder) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val folderStats = calculateRecursiveStats(folder)
            totalStats = folderStats

            val list = folder.listFiles()?.filter { it.isDirectory }?.map { subfolder ->
                val stats = calculateRecursiveStats(subfolder)
                FolderStats(subfolder, stats.first, stats.second)
            } ?: emptyList()
            subfolders = list
        }
        isLoading = false
    }

    val sortedSubfolders = remember(subfolders, sortColumn, sortOrder) {
        val comparator = when (sortColumn) {
            SortColumn.NAME -> compareBy<FolderStats> { it.file.name.lowercase() }
            SortColumn.SIZE -> compareBy<FolderStats> { it.size }
            SortColumn.USED -> compareBy<FolderStats> { it.used }
        }
        if (sortOrder == SortOrder.ASCENDING) {
            subfolders.sortedWith(comparator)
        } else {
            subfolders.sortedWith(comparator.reversed())
        }
    }

    val totalSpace = remember(folder) { folder.totalSpace }
    val freeSpace = remember(folder) { folder.usableSpace }
    val usedSpace = totalSpace - freeSpace
    val volumePath = remember(folder, totalSpace) {
        var root = folder
        var parent = root.parentFile
        while (parent != null && parent.totalSpace == totalSpace && parent.totalSpace > 0) {
            root = parent
            parent = root.parentFile
        }
        root.absolutePath
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "VOLUME: $volumePath  SIZE: ${formatSize(totalSpace)}  USED: ${formatSize(usedSpace)}  FREE: ${formatSize(freeSpace)}",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "FOLDER: ${folder.name}",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
            color = Color.Green,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading) {
            HorizontalDivider(color = Color.Green.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 16.dp))
            Text(
                text = "Calculating sizes...",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.Gray
            )
        } else if (subfolders.isEmpty()) {
            HorizontalDivider(color = Color.Green.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 16.dp))
            Text(
                text = "No subfolders found",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.Gray
            )
        } else {
            // Totals Row
            totalStats?.let { stats ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTALS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.LightGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatSize(stats.first),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Green,
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = formatSize(stats.second),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Green,
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(color = Color.Green.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 16.dp))

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier
                        .width(100.dp)
                        .clickable {
                            if (sortColumn == SortColumn.SIZE) {
                                sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            } else {
                                sortColumn = SortColumn.SIZE
                                sortOrder = SortOrder.ASCENDING
                            }
                        },
                    textAlign = TextAlign.End
                )

                val usedIndicator = if (sortColumn == SortColumn.USED) {
                    if (sortOrder == SortOrder.ASCENDING) " ▲" else " ▼"
                } else ""
                Text(
                    text = "Used$usedIndicator",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (sortColumn == SortColumn.USED) Color.Green else Color.Gray,
                    modifier = Modifier
                        .width(100.dp)
                        .clickable {
                            if (sortColumn == SortColumn.USED) {
                                sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            } else {
                                sortColumn = SortColumn.USED
                                sortOrder = SortOrder.ASCENDING
                            }
                        },
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(color = Color.Green.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedSubfolders) { stats ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stats.file.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Green,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatSize(stats.size),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = Color.LightGray,
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = formatSize(stats.used),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray,
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

private fun calculateRecursiveStats(file: File): Pair<Long, Long> {
    if (file.isFile) {
        val size = file.length()
        val used = try {
            android.system.Os.lstat(file.absolutePath).st_blocks * 512
        } catch (_: Exception) {
            size
        }
        return Pair(size, used)
    }
    var totalSize: Long = 0
    var totalUsed: Long = 0
    val children = file.listFiles()
    if (children != null) {
        for (child in children) {
            val (s, u) = calculateRecursiveStats(child)
            totalSize += s
            totalUsed += u
        }
    }
    return Pair(totalSize, totalUsed)
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
