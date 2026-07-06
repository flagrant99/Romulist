package io.github.flagrant99.romulist

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import android.view.KeyEvent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackageList(
    onPackageSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val context = LocalContext.current
    var showSystemPackages by remember { mutableStateOf(false) }

    val allPackages = remember {
        try {
            context.packageManager.getInstalledPackages(0)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val packageNames = remember(showSystemPackages, allPackages) {
        allPackages.filter { packageInfo ->
            val isSystem = packageInfo.applicationInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
            showSystemPackages || !isSystem
        }.map { it.packageName }.sorted()
    }

    val toggleFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        toggleFocusRequester.requestFocus()
    }

    LazyColumn(modifier = modifier) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INSTALLED PACKAGES",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "Total: ${packageNames.size}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )

                var isToggleFocused by remember { mutableStateOf(false) }
                val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
                val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(toggleFocusRequester)
                        .onFocusChanged { isToggleFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    launchKey -> {
                                        showSystemPackages = !showSystemPackages
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
                        .background(if (isToggleFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            toggleFocusRequester.requestFocus()
                            showSystemPackages = !showSystemPackages
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "SHOW SYSTEM PACKAGES:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        color = if (isToggleFocused) Color.Green else Color.Green.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (showSystemPackages) "[ ON ]" else "[ OFF ]",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            shadow = if (isToggleFocused) Shadow(Color.Green.copy(alpha = 0.5f), blurRadius = 8f) else null
                        ),
                        color = if (showSystemPackages) Color.Red else Color.Green
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )
            }
        }

        items(packageNames) { packageName ->
            var isFocused by remember { mutableStateOf(false) }
            val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
            val launchKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B
            val focusRequester = if (packageName == packageNames.firstOrNull()) firstItemFocusRequester else remember { FocusRequester() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                launchKey -> {
                                    onPackageSelect(packageName)
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
                    .combinedClickable(
                        onClick = { focusRequester.requestFocus() },
                        onLongClick = { onPackageSelect(packageName) }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = if (isFocused) Shadow(Color.Green.copy(alpha = 0.5f), blurRadius = 8f) else null
                    ),
                    color = if (isFocused) Color.Green else Color.Green.copy(alpha = 0.8f)
                )
            }
        }
    }
}
