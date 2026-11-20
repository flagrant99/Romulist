package io.github.flagrant99.romulist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import java.io.File
import com.caverock.androidsvg.SVG
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import io.github.flagrant99.romulist.ui.theme.Pink40
import io.github.flagrant99.romulist.ui.theme.Purple80

@Composable
fun FileRow(
    name: String,
    isDirectory: Boolean,
    onBack: () -> Unit,
    isSelected: Boolean = false,
    showIcon: Boolean = true,
    isAtHome: Boolean = false,
    currentPath: File? = null,
    file: File? = null,
    swapAB: Boolean = false,
    useLargeFont: Boolean = false,
    shouldAutoCaptureFocus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocus: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(shouldAutoCaptureFocus) {
        if (shouldAutoCaptureFocus) {
            focusRequester.requestFocus()
        }
    }

    val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
    val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (it.isFocused) onFocus()
            }
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                if (keyCode == launchKey || keyCode == backKey || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyCode) {
                            launchKey -> {
                                onLongClick()
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                onClick()
                            }
                            backKey -> {
                                onBack()
                            }
                        }
                    }
                    // Consume both Down and Up to prevent combinedClickable from seeing the event
                    true
                } else false
            }
            .background(
                when {
                    isFocused -> Color.Green.copy(alpha = 0.2f)
                    isSelected -> Color.Cyan.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(
                onClick = {
                    focusRequester.requestFocus()
                    onClick()
                },
                onLongClick = {
                    focusRequester.requestFocus()
                    onLongClick()
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            val iconTint = when {
                isDirectory -> Color.Yellow
                isSelected -> Color.Cyan
                else -> Color.White
            }

            if (isAtHome && isDirectory) {
                val svgFile = remember(currentPath, file, name) {
                    if (currentPath != null) {
                        val targetName = file?.name ?: name
                        currentPath.listFiles()?.firstOrNull { 
                            it.isFile && it.extension.lowercase() == "svg" && it.nameWithoutExtension.equals(targetName, ignoreCase = true) 
                        }
                    } else null
                }

                val svgBitmap = remember(svgFile) {
                    if (svgFile != null && svgFile.exists()) {
                        try {
                            val svg = SVG.getFromInputStream(svgFile.inputStream())
                            val docW = if (svg.documentWidth > 0f) svg.documentWidth else 24f
                            val docH = if (svg.documentHeight > 0f) svg.documentHeight else 24f
                            
                            val scale = maxOf(128f / docW, 128f / docH)
                            val bitmap = Bitmap.createBitmap((docW * scale).toInt().coerceAtLeast(128), (docH * scale).toInt().coerceAtLeast(128), Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.scale(scale, scale)
                            svg.renderToCanvas(canvas)
                            bitmap
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                if (svgBitmap != null) {
                    Image(
                        bitmap = svgBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(iconTint)
                    )
                } else {
                    when (name)
                    {
                        "atari2600", "atari5200", "atari7800", "c64", "vectrex", "fbneo", "mame", "colecovision", "neogeo" -> Icon(
                            painter = painterResource(id = R.drawable.outline_joystick_24),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "gb", "gba", "gbc", "gamegear" -> Icon(
                            painter = painterResource(id = R.drawable.outline_gamepad_24),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "snes" -> Icon(
                            painter = painterResource(id = R.drawable.outline_gamepad_circle_right_24),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "nes" -> Icon(
                            painter = painterResource(id = R.drawable.outline_gamepad_right_24),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "3do", "pcengine", "pcenginecd", "mastersystem", "megadrive", "saturn", "sega32x", "segacd" -> Icon(
                            painter = painterResource(id = R.drawable.outline_videogame_asset_24 ),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "apps" -> Icon(
                            painter = painterResource(id = R.drawable.outline_apps_24),
                            contentDescription = null,
                            tint = iconTint
                        )
                        "ports" -> Icon(
                            painter = painterResource(id = R.drawable.outline_directions_boat_24),
                            contentDescription = null,
                            tint = iconTint
                        )

                        else ->
                        {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_stadia_controller_24),
                                contentDescription = null,
                                tint = iconTint
                            )
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = iconTint
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = name,
            style = (if (useLargeFont) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium).copy(
                fontFamily = if (isDirectory) FontFamily.Default else FontFamily.Default,
                shadow = if (isSelected) Shadow(
                    color = Color.Green.copy(alpha = 0.5f),
                    blurRadius = 8f
                ) else null
            ),
            color = when {
                isDirectory -> Purple80
                isSelected -> Color.Green
                else -> Color.Green
            }
        )
    }
}
