package io.github.flagrant99.romulist

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
fun DTS(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val context = LocalContext.current
    var dtsState by remember { mutableStateOf(-1) }
    val focusRequester = remember { FocusRequester() }

    fun refreshDtsState() {
        try {
            dtsState = Settings.System.getInt(context.contentResolver, "dts_state")
        } catch (_: Exception) {
            dtsState = -1
        }
    }

    LaunchedEffect(Unit) {
        refreshDtsState()
        focusRequester.requestFocus()
    }

    fun setDtsState(state: Int) {
        if (Settings.System.canWrite(context)) {
            try {
                // Try writing as a String first, which can sometimes bypass stricter 'Int' type checks
                // for custom vendor keys on newer Android versions.
                val success = Settings.System.putString(context.contentResolver, "dts_state", state.toString())
                if (success) {
                    Toast.makeText(context, "DTS ${if (state == 1) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    refreshDtsState()
                } else {
                    // Fallback to putInt
                    val successInt = Settings.System.putInt(context.contentResolver, "dts_state", state)
                    if (successInt) {
                        Toast.makeText(context, "DTS ${if (state == 1) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                        refreshDtsState()
                    } else {
                        Toast.makeText(context, "Failed to change DTS state", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("secure", ignoreCase = true)) {
                    Toast.makeText(context, "OS blocked write. Use ADB command on this device version.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permission needed to write settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DTS CONTROL",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "Current State: ${if (dtsState == -1) "Unknown" else dtsState.toString()}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = if (dtsState == 0) Color.Green else Color.Red
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )

                Text(
                    text = "OS LIMITATION:",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Red
                )
                Text(
                    text = "Android 16+ blocks apps from changing this setting directly. If the buttons below fail, run this from your PC:",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "adb shell settings put system dts_state 0",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        background = Color.DarkGray
                    ),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Yellow
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
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
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey,
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER -> {
                                    setDtsState(0)
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
                    .clickable { setDtsState(0) }
                    .padding(16.dp)
            ) {
                Text(
                    text = "DISABLE DTS",
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
                    text = "Set dts_state to 0",
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
                                    setDtsState(1)
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
                    .clickable { setDtsState(1) }
                    .padding(16.dp)
            ) {
                Text(
                    text = "ENABLE DTS",
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
                    text = "Set dts_state to 1",
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
