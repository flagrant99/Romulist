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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
fun FileDetail(
    currentFolder: File?,
    selectedFile: File?,
    favoritePath: String?,
    swapAB: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val firstIntentFocusRequester = remember { FocusRequester() }

    val folderConfig = remember(selectedFile ?: currentFolder, favoritePath) {
        var dir: File? = selectedFile ?: currentFolder
        val favFile = favoritePath?.let { File(it) }
        var foundConfig: EmulatorNavigator.RomulistConfig? = null

        while (dir != null) {
            val configFile = File(dir, "romulist.xml")
            if (configFile.exists()) {
                foundConfig = EmulatorNavigator.parseConfig(configFile)
                if (foundConfig != null) break
            }
            if (favFile != null && dir.absolutePath == favFile.absolutePath) break
            dir = dir.parentFile
        }
        foundConfig
    }

    val romulistConfig = remember(selectedFile, folderConfig) {
        if (selectedFile?.name?.lowercase()?.endsWith(".rax") == true) {
            val raxConfig = EmulatorNavigator.parseConfig(selectedFile)
            if (raxConfig?.systemConfig != null) {
                return@remember raxConfig.copy(
                    systemConfig = raxConfig.systemConfig.copy(
                        media = folderConfig?.systemConfig?.media
                    )
                )
            }
        }
        folderConfig
    }

    val displayConfigSource = remember(romulistConfig?.configSource, favoritePath) {
        val source = romulistConfig?.configSource ?: return@remember "Internal"
        if (favoritePath != null && source.startsWith(favoritePath)) {
            source.substring(favoritePath.length).trimStart(File.separatorChar)
                .ifEmpty { "romulist.xml" }
        } else {
            source
        }
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
                            swapAB,
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
                        DetailMediaSectionLandscape(system, folderConfig?.configSource, selectedFile)
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
                        swapAB,
                        onBack
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Green.copy(alpha = 0.3f)
                    )

                    DetailMediaSectionVertical(system, folderConfig?.configSource, selectedFile)
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
    swapAB: Boolean,
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

        val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
        val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(if (index == 0) firstIntentFocusRequester else remember { FocusRequester() })
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            launchKey,
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
                            backKey -> {
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
    DetailMediaContent(system, configSource, selectedFile)
}

@Composable
internal fun DetailMediaSectionLandscape(
    system: EmulatorNavigator.FolderConfig,
    configSource: String?,
    selectedFile: File?
) {
    DetailMediaContent(system, configSource, selectedFile)
}

@Composable
private fun DetailMediaContent(
    system: EmulatorNavigator.FolderConfig,
    configSource: String?,
    selectedFile: File?
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                mediaPlayerRef.value?.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                mediaPlayerRef.value?.start()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val boxartFile = remember(system.media?.cover, configSource, selectedFile) {
        system.media?.cover?.resolveMediaFile(configSource, selectedFile, listOf(".png", ".jpg", ".PNG", ".JPG"))
    }
    val boxartBitmap = remember(boxartFile) {
        try {
            boxartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val mixartFile = remember(system.media?.mixart, configSource, selectedFile) {
        system.media?.mixart?.resolveMediaFile(configSource, selectedFile, listOf(".png", ".jpg", ".PNG", ".JPG"))
    }
    val mixartBitmap = remember(mixartFile) {
        try {
            mixartFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val screenFile = remember(system.media?.screen, configSource, selectedFile) {
        system.media?.screen?.resolveMediaFile(configSource, selectedFile, listOf(".png", ".jpg", ".PNG", ".JPG"))
    }
    val screenBitmap = remember(screenFile) {
        try {
            screenFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val marqueeBitmap = remember(system.media?.marquee, configSource, selectedFile) {
        val file = system.media?.marquee?.resolveMediaFile(configSource, selectedFile, listOf(".png", ".jpg", ".PNG", ".JPG"))
        try {
            file?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }

    val videoFile = remember(system.media?.video, configSource, selectedFile) {
        system.media?.video?.resolveMediaFile(configSource, selectedFile, listOf(".mp4", ".MP4"))
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

        if (screenBitmap != null) {
            Image(
                bitmap = screenBitmap,
                contentDescription = "Screenshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            val textureView = this
                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                var mediaPlayer: MediaPlayer? = null
                                override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(ctx, Uri.fromFile(videoFile))
                                        setSurface(android.view.Surface(st))
                                        isLooping = true
                                        setOnVideoSizeChangedListener { _, width, height ->
                                            if (width > 0 && height > 0) {
                                                val matrix = android.graphics.Matrix()
                                                val vw = textureView.width.toFloat()
                                                val vh = textureView.height.toFloat()
                                                val videoRatio = width.toFloat() / height
                                                val viewRatio = vw / vh
                                                if (videoRatio > viewRatio) {
                                                    matrix.setScale(1f, viewRatio / videoRatio, vw / 2, vh / 2)
                                                } else {
                                                    matrix.setScale(videoRatio / viewRatio, 1f, vw / 2, vh / 2)
                                                }
                                                textureView.setTransform(matrix)
                                            }
                                        }
                                        setOnPreparedListener { start() }
                                        prepareAsync()
                                    }
                                    mediaPlayerRef.value = mediaPlayer
                                }
                                override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    mediaPlayerRef.value = null
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
    }
}
