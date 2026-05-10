package io.github.flagrant99.romulist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import android.view.KeyEvent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
fun Detail(
    currentFolder: File?,
    selectedFile: File?,
    favoritePath: String?,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val firstIntentFocusRequester = remember { FocusRequester() }

    var configSource by remember { mutableStateOf<String?>(null) }
    val displayConfigSource = remember(configSource, favoritePath) {
        val source = configSource ?: return@remember "Internal"
        if (favoritePath != null && source.startsWith(favoritePath)) {
            source.substring(favoritePath.length).trimStart(File.separatorChar)
                .ifEmpty { "romulist.xml" }
        } else {
            source
        }
    }

    val romulistConfig = remember(selectedFile ?: currentFolder, favoritePath) {
        var dir: File? = selectedFile ?: currentFolder
        val favFile = favoritePath?.let { File(it) }
        var foundConfig: EmulatorNavigator.RomulistConfig? = null

        while (dir != null) {
            val configFile = File(dir, "romulist.xml")
            if (configFile.exists()) {
                configSource = configFile.absolutePath
                foundConfig = EmulatorNavigator.parseConfig(configFile)
                if (foundConfig != null) break
            }
            if (favFile != null && dir.absolutePath == favFile.absolutePath) break
            dir = dir.parentFile
        }
        foundConfig
    }

    LaunchedEffect(romulistConfig) {
        if (romulistConfig?.systemConfig != null) {
            firstIntentFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (romulistConfig?.systemConfig != null) {
            val system = romulistConfig.systemConfig

            if (selectedFile != null) {
                Text(
                    text = "File: ${selectedFile.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green
                )
            } else {
                Text(
                    text = "No file selected",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Red.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "System: ${system.name}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.Green
            )

            val mediaScreenPath = remember(system.media?.screen, configSource) {
                val screenRel = system.media?.screen ?: return@remember null
                val configPath = configSource ?: return@remember null
                File(File(configPath).parentFile, screenRel).absolutePath
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


            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Green.copy(alpha = 0.3f)
            )

            Text(
                text = "INTENTS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    shadow = Shadow(Color.Green.copy(alpha = 0.5f), blurRadius = 8f)
                ),
                color = Color.Green
            )

            Text(
                text = "Source: $displayConfigSource",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                ),
                color = Color.Green,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val allIntents = listOfNotNull(system.mainIntent) + system.altIntents

            allIntents.forEachIndexed { index, intent ->
                var isFocused by remember { mutableStateOf(false) }
                val isMainIntent = intent == system.mainIntent
                val isEnabled = selectedFile != null

                val displayName = intent.name.ifBlank { "LAUNCH" }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(if (index == 0) firstIntentFocusRequester else remember { FocusRequester() })
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_BUTTON_B,
                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                    KeyEvent.KEYCODE_ENTER -> {
                                        if (isEnabled) {
                                            EmulatorNavigator.launchGame(
                                                context = context,
                                                filePath = selectedFile?.absolutePath ?: "",
                                                config = romulistConfig,
                                                preferredIntent = intent
                                            )
                                        }
                                        true
                                    }
                                    KeyEvent.KEYCODE_BUTTON_A -> {
                                        onBack()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(enabled = isEnabled) {
                            EmulatorNavigator.launchGame(
                                context = context,
                                filePath = selectedFile?.absolutePath ?: "",
                                config = romulistConfig,
                                preferredIntent = intent
                            )
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isMainIntent) "[ $displayName ]" else displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = Shadow(
                                color = Color.Green.copy(alpha = 0.5f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        ),
                        color = if (isEnabled) Color.Green else Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
