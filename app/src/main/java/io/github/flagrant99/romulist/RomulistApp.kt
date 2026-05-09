package io.github.flagrant99.romulist

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import android.view.KeyEvent
import io.github.flagrant99.romulist.ui.theme.RomulistTheme
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@PreviewScreenSizes
@Composable
fun RomulistApp()
{
    RomulistTheme {
        val context = LocalContext.current
        val isPreview = LocalInspectionMode.current
        val haptics = LocalHapticFeedback.current

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
        val settings = remember { PersistentSettings(context) }

        var currentScreen by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        var selectedFolder by remember { mutableStateOf<File?>(null) }
        var selectedFile by remember { mutableStateOf<File?>(null) }

        // Loaded from PersistentSettings for persistence across reboots
        var favoriteFolder by remember {
            mutableStateOf(settings.favoriteFolder)
        }

        val handleHome = {
            if (favoriteFolder == null) {
                currentScreen = AppDestinations.SET_HOME
            } else {
                selectedFolder = File(favoriteFolder!!)
                selectedFile = null
                currentScreen = AppDestinations.HOME
            }
        }

        LaunchedEffect(Unit) {
            if (favoriteFolder != null) {
                handleHome()
            }
        }

        val handleBack = {
            val parent = selectedFolder?.parentFile

            when {
                // 1. If we are on a different tab, go back to Home first (keeping folder state)
                currentScreen != AppDestinations.HOME -> {
                    currentScreen = AppDestinations.HOME
                }
                // 2. If we are in a subfolder on Home, go to parent
                parent != null && selectedFolder?.absolutePath != volumes.find { it.directory?.absolutePath == selectedFolder?.absolutePath }?.directory?.absolutePath -> {
                    selectedFolder = parent
                    selectedFile = null
                }
                // 3. If we are at the root of a drive on Home, go back to Drive List
                selectedFolder != null -> {
                    selectedFolder = null
                    selectedFile = null
                }
                // 4. At the top of Home (RootScreen), disable back navigation
                else -> {
                    // Do nothing
                }
            }
        }

        val canGoBack = selectedFolder != null || currentScreen != AppDestinations.HOME

        BackHandler(enabled = true) {
            handleBack()
        }

        var isHomeLongPressActive by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_BUTTON_A -> {
                                if (canGoBack) {
                                    handleBack()
                                }
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
                    AppDestinations.entries.filter { it != AppDestinations.SET_HOME }.forEach { destination ->

                        // 1. Determine if this item is selected
                        val isSelected = if (destination == AppDestinations.BACK) false
                        else currentScreen == destination

                        val interactionSource = remember { MutableInteractionSource() }

                        NavigationBarItem(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            enabled = when (destination) {
                                AppDestinations.BACK -> canGoBack
                                AppDestinations.SETTINGS -> selectedFile != null
                                else -> true
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color.Green,
                                unselectedIconColor = Color.Green,
                                unselectedTextColor = Color.Green,
                                indicatorColor = Color.Green,
                                disabledIconColor = Color.Gray,
                                disabledTextColor = Color.Gray
                            ),
                            modifier = if (destination == AppDestinations.HOME) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        // Use Initial pass to see the event before NavigationBarItem consumes it
                                        awaitFirstDown(pass = PointerEventPass.Initial)
                                        isHomeLongPressActive = false
                                        
                                        val timeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                        }

                                        if (timeout == null) {
                                            // Long press detected
                                            isHomeLongPressActive = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentScreen = AppDestinations.SET_HOME
                                        }
                                    }
                                }
                            } else Modifier,
                            onClick = {
                                when (destination) {
                                    AppDestinations.BACK -> handleBack()
                                    AppDestinations.HOME -> {
                                        if (!isHomeLongPressActive) {
                                            handleHome()
                                        }
                                        isHomeLongPressActive = false
                                    }
                                    else -> currentScreen = destination
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
                                selectedFile = selectedFile,
                                onPathChange = { 
                                    selectedFolder = it 
                                    selectedFile = null
                                },
                                onFileSelect = { selectedFile = it },
                                onBack = handleBack
                            )
                        } else {
                            RootScreen(
                                volumes = volumes,
                                storageStatsManager = storageStatsManager,
                                context = context,
                                onVolumeClick = { folder -> selectedFolder = folder },
                                onBack = handleBack
                            )
                        }
                    }

                    AppDestinations.SETTINGS -> Settings(
                        currentFolder = selectedFolder,
                        selectedFile = selectedFile,
                        favoritePath = favoriteFolder,
                    )

                    AppDestinations.SET_HOME -> SetHomeFolder(
                        currentFolder = selectedFolder,
                        homePath = favoriteFolder,
                        onSetHome = { path ->
                            favoriteFolder = path
                            settings.favoriteFolder = path
                            if (path != null) {
                                currentScreen = AppDestinations.HOME
                                selectedFolder = File(path)
                            }
                        }
                    )

                    AppDestinations.BACK -> { /* Handled in onClick above */ }
                }
            }
        }
    }
}
