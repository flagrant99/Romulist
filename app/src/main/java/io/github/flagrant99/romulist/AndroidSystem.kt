package io.github.flagrant99.romulist

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun AndroidSystem(
    onBack: () -> Unit,
    onDts: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val context = LocalContext.current

    LazyColumn(modifier = modifier) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ANDROID SYSTEM",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )
            }
        }

        item {
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
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
                    .clickable {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    }
                    .padding(16.dp)
            ) {
                Text(
                    text = "ANDROID SETTINGS",
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
                    text = "Launch Android system settings",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                    ),
                    color = Color.Green
                )
            }
        }

        item {
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            fun launchUsbSettings() {
                val actions = listOf(
                    "android.settings.USB_DETAILS_SETTINGS",
                    "android.settings.USB_SETTINGS",
                    "android.settings.DEVICE_CONNECTION_SETTINGS"
                )
                
                var success = false
                for (action in actions) {
                    try {
                        val intent = Intent(action)
                        context.startActivity(intent)
                        success = true
                        break
                    } catch (_: Exception) { }
                }

                if (!success) {
                    try {
                        // Try explicit component for stock Android
                        val intent = Intent()
                        intent.setClassName("com.android.settings", "com.android.settings.Settings\$UsbDetailsFragmentActivity")
                        context.startActivity(intent)
                        success = true
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            context.startActivity(intent)
                            success = true
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    launchUsbSettings()
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
                    .clickable { launchUsbSettings() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "USB SETTINGS",
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
                    text = "Shortcut to USB File Transfer preferences. (Ensure USB is connected)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.3f), blurRadius = 4f)
                    ),
                    color = Color.Green
                )
            }
        }

        item {
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    onDts()
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
                    .clickable { onDts() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "DTS",
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
                    text = "Control DTS audio effects",
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
