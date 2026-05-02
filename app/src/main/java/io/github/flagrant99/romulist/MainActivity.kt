package io.github.flagrant99.romulist

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import io.github.flagrant99.romulist.ui.theme.RomulistTheme
import io.github.flagrant99.romulist.ui.theme.Purple80
import java.io.File

class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RomulistTheme {
                RomulistApp()
            }
        }
    }
}


//*******************************************************************************
//MAIN COMPOSABLE
//*******************************************************************************
@PreviewScreenSizes
@Composable
fun RomulistApp()
{
    RomulistTheme {
        val context = LocalContext.current
        val isPreview = LocalInspectionMode.current

        val storageManager = remember {
            if (isPreview) null else context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        }
        val storageStatsManager = remember {
            if (isPreview) null else context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        }
        val volumes = remember(storageManager) {
            storageManager?.storageVolumes ?: emptyList()
        }

        // Persistence Setup
        val sharedPrefs = remember {
            context.getSharedPreferences(
                "RomulistPrefs",
                Context.MODE_PRIVATE
            )
        }

        var currentScreen by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        var selectedFolder by remember { mutableStateOf<File?>(null) }

        // Loaded from SharedPreferences for persistence across reboots
        var favoriteFolder by remember {
            mutableStateOf(sharedPrefs.getString("favorite_folder", null))
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    AppDestinations.entries.forEach { destination ->

                        // 1. Determine if this item is selected
                        val isSelected = if (destination == AppDestinations.BACK) false
                        else currentScreen == destination

                        NavigationBarItem(
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color.Green,
                                unselectedIconColor = Color.Green,
                                unselectedTextColor = Color.Green,
                                indicatorColor = Color.Green
                            ),
                            onClick = {
                                if (destination == AppDestinations.BACK) {
                                    val parent = selectedFolder?.parentFile

                                    when {
                                        // 1. If we are in a subfolder, go to parent
                                        parent != null && selectedFolder?.absolutePath != volumes.find { it.directory?.absolutePath == selectedFolder?.absolutePath }?.directory?.absolutePath -> {
                                            selectedFolder = parent
                                        }
                                        // 2. If we are at the root of a drive, go back to Drive List
                                        selectedFolder != null -> {
                                            selectedFolder = null
                                        }
                                        // 3. Otherwise, just make sure we are on the Home tab
                                        else -> {
                                            currentScreen = AppDestinations.HOME
                                        }
                                    }
                                } else {
                                    // Navigate to favorite folder if HOME is clicked and favorite is set
                                    if (destination == AppDestinations.HOME) {
                                        selectedFolder = favoriteFolder?.let { File(it) }
                                    }

                                    currentScreen = destination
                                }
                            },
                            label = { Text(destination.label) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = destination.icon),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        )
        { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    AppDestinations.HOME -> {
                        // Toggle between the Drive List and File List
                        if (selectedFolder != null) {
                            ListFiles(
                                currentPath = selectedFolder!!,
                                favoritePath = favoriteFolder,
                                onPathChange = { selectedFolder = it }
                            )
                        } else {
                            RootScreen(
                                volumes = volumes,
                                storageStatsManager = storageStatsManager,
                                context = context,
                                onVolumeClick = { folder -> selectedFolder = folder }
                            )
                        }
                    }

                    AppDestinations.FAVORITE -> MakeFavorite(
                        currentFolder = selectedFolder,
                        favoritePath = favoriteFolder,
                        onSetFavorite = { path ->
                            favoriteFolder = path
                            sharedPrefs.edit().putString("favorite_folder", path).apply()
                        }
                    )

                    AppDestinations.BACK -> { /* Handled in onClick above */ }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int
)
{
    BACK("Back", R.drawable.baseline_arrow_back_24),
    HOME("Home", R.drawable.ic_home),
    FAVORITE("Favorite", R.drawable.ic_favorite);
}

//*******************************************************************************************
//ROOT SCREEN
//*******************************************************************************************
@Composable
fun RootScreen(
    volumes: List<StorageVolume>,
    storageStatsManager: StorageStatsManager?,
    context: Context,
    onVolumeClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier)
    {
        items(volumes) { volume ->
            val uuid = try
            {
                val uuidStr = volume.uuid
                if (uuidStr != null && uuidStr.length == 36)
                {
                    java.util.UUID.fromString(uuidStr)
                }
                else
                {
                    StorageManager.UUID_DEFAULT
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

//*******************************************************************************************
//List Files
//*******************************************************************************************
@Composable
fun ListFiles(
    currentPath: File,
    favoritePath: String?,
    onPathChange: (File) -> Unit
) {
    val context = LocalContext.current
    val files = remember(currentPath) {
        currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
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

@Composable
fun FileRow(name: String, isDirectory: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (isDirectory) Color.Yellow else Color.White
        )
        Spacer(Modifier.width(12.dp))
        Text(text = name, color = if (isDirectory) Purple80 else Color.Green)
    }
}

//*******************************************************************************************
//MAKE FAVORITE SCREEN
//*******************************************************************************************
@Composable
fun MakeFavorite(
    currentFolder: File?,
    favoritePath: String?,
    onSetFavorite: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Favorite Folder",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Display current path
        Text(
            text = "Current Path:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = currentFolder?.absolutePath ?: "Root (Drive List)",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Set Button
        if (currentFolder != null) {
            Text(
                text = "[ SET AS FAVORITE ]",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Yellow,
                modifier = Modifier
                    .clickable { onSetFavorite(currentFolder.absolutePath) }
                    .padding(12.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color.DarkGray)

        // Display Saved Favorite
        Text(
            text = "Saved Favorite:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = favoritePath ?: "None",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = if (favoritePath != null) Color.Green else Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favoritePath != null) {
            Text(
                text = "[ CLEAR FAVORITE ]",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Red,
                modifier = Modifier
                    .clickable { onSetFavorite(null) }
                    .padding(12.dp)
            )
        }
    }
}