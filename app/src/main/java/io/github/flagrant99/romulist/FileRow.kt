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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontFamily
import io.github.flagrant99.romulist.ui.theme.Purple80

@Composable
fun FileRow(
    name: String,
    isDirectory: Boolean,
    onBack: () -> Unit,
    isSelected: Boolean = false,
    showIcon: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocus: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (it.isFocused) onFocus()
            }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BUTTON_B -> {
                            onLongClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            onLongClick()
                            true
                        }
                        KeyEvent.KEYCODE_ENTER -> {
                            onLongClick()
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
            .background(
                when {
                    isFocused -> Color.Green.copy(alpha = 0.2f)
                    isSelected -> Color.Cyan.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = when {
                    isDirectory -> Color.Yellow
                    isSelected -> Color.Cyan
                    else -> Color.White
                }
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
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
