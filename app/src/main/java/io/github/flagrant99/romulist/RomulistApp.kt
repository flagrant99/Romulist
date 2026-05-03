package io.github.flagrant99.romulist

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent
import io.github.flagrant99.romulist.ui.theme.RomulistTheme
import java.io.File

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

        val handleBack = {
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
        }

        val handleHome = {
            selectedFolder = favoriteFolder?.let { File(it) }
            currentScreen = AppDestinations.HOME
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_BUTTON_A -> {
                                handleBack()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y -> {
                                handleHome()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
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
                                    handleBack()
                                } else if (destination == AppDestinations.HOME) {
                                    handleHome()
                                } else {
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
                                onPathChange = { selectedFolder = it },
                                onBack = handleBack
                            )
                        } else {
                            RootScreen(
                                volumes = volumes,
                                storageManager = storageManager,
                                storageStatsManager = storageStatsManager,
                                context = context,
                                onVolumeClick = { folder -> selectedFolder = folder },
                                onBack = handleBack
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
