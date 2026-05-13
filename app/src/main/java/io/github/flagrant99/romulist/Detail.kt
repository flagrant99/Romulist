package io.github.flagrant99.romulist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.viewinterop.AndroidView
import android.view.TextureView
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

@Composable
fun Detail(
    currentFolder: File?,
    selectedFile: File?,
    favoritePath: String?,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        InfoSection(
                            system,
                            selectedFile,
                            displayConfigSource,
                            firstIntentFocusRequester,
                            context,
                            romulistConfig,
                            onBack
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        color = Color.Green.copy(alpha = 0.3f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DetailMediaSectionLandscape(system, configSource, selectedFile)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoSection(
                        system,
                        selectedFile,
                        displayConfigSource,
                        firstIntentFocusRequester,
                        context,
                        romulistConfig,
                        onBack
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Green.copy(alpha = 0.3f)
                    )

                    DetailMediaSectionVertical(system, configSource, selectedFile)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    system: EmulatorNavigator.FolderConfig,
    selectedFile: File?,
    displayConfigSource: String,
    firstIntentFocusRequester: FocusRequester,
    context: android.content.Context,
    romulistConfig: EmulatorNavigator.RomulistConfig?,
    onBack: () -> Unit
) {
    if (selectedFile != null) {
        Text(
            text = "${selectedFile.name}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Color.Green
        )
    } else {
        Text(
            text = "No file selected",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.Red.copy(alpha = 0.7f)
        )
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
        text = "System: ${system.name}",
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
                                        filePath = selectedFile.absolutePath ?: "",
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

@Composable
internal fun DetailMediaSectionVertical(
    system: EmulatorNavigator.FolderConfig,
    configSource: String?,
    selectedFile: File?
) {
    val mediaBoxartPath = remember(system.media?.cover, configSource) {
        system.media?.cover?.resolvePath(configSource)
    }
    val boxartFile = remember(mediaBoxartPath, selectedFile) {
        if (selectedFile == null || mediaBoxartPath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaBoxartPath)
        listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }
    }
    val boxartBitmap = remember(boxartFile) {
        try {
            boxartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val mediaMixartPath = remember(system.media?.mixart, configSource) {
        system.media?.mixart?.resolvePath(configSource)
    }
    val mixartFile = remember(mediaMixartPath, selectedFile) {
        if (selectedFile == null || mediaMixartPath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaMixartPath)
        listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }
    }
    val mixartBitmap = remember(mixartFile) {
        try {
            mixartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val mediaMarqueePath = remember(system.media?.marquee, configSource) {
        system.media?.marquee?.resolvePath(configSource)
    }
    val marqueeBitmap = remember(mediaMarqueePath, selectedFile) {
        if (selectedFile == null || mediaMarqueePath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaMarqueePath)
        val file = listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }

        try {
            file?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val mediaVideoPath = remember(system.media?.video, configSource) {
        system.media?.video?.resolvePath(configSource)
    }
    val videoFile = remember(mediaVideoPath, selectedFile) {
        if (selectedFile == null || mediaVideoPath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaVideoPath)
        listOf("$baseName.mp4", "$baseName.MP4")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (boxartBitmap != null || mixartBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (boxartBitmap != null) {
                        Image(
                            bitmap = boxartBitmap,
                            contentDescription = "Boxart",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (mixartBitmap != null) {
                        Image(
                            bitmap = mixartBitmap,
                            contentDescription = "Mixart",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        if (marqueeBitmap != null) {
            Image(
                bitmap = marqueeBitmap,
                contentDescription = "Marquee",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(vertical = 8.dp),
                contentScale = ContentScale.Fit
            )
        }

        if (videoFile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 8.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                var mediaPlayer: MediaPlayer? = null
                                override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(ctx, Uri.fromFile(videoFile))
                                        setSurface(android.view.Surface(st))
                                        isLooping = true
                                        setOnPreparedListener { start() }
                                        prepareAsync()
                                    }
                                }
                                override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    mediaPlayer?.release()
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun DetailMediaSectionLandscape(
    system: EmulatorNavigator.FolderConfig,
    configSource: String?,
    selectedFile: File?
) {
    val mediaBoxartPath = remember(system.media?.cover, configSource) {
        system.media?.cover?.resolvePath(configSource)
    }
    val boxartFile = remember(mediaBoxartPath, selectedFile) {
        if (selectedFile == null || mediaBoxartPath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaBoxartPath)
        listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }
    }
    val boxartBitmap = remember(boxartFile) {
        try {
            boxartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val mediaMixartPath = remember(system.media?.mixart, configSource) {
        system.media?.mixart?.resolvePath(configSource)
    }
    val mixartFile = remember(mediaMixartPath, selectedFile) {
        if (selectedFile == null || mediaMixartPath == null) return@remember null
        val baseName = selectedFile.nameWithoutExtension
        val dir = File(mediaMixartPath)
        listOf("$baseName.png", "$baseName.jpg", "$baseName.PNG", "$baseName.JPG")
            .map { File(dir, it) }
            .firstOrNull { it.exists() }
    }
    val mixartBitmap = remember(mixartFile) {
        try {
            mixartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (boxartBitmap != null || mixartBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (boxartBitmap != null) {
                        Image(
                            bitmap = boxartBitmap,
                            contentDescription = "Boxart",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (mixartBitmap != null) {
                        Image(
                            bitmap = mixartBitmap,
                            contentDescription = "Mixart",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

