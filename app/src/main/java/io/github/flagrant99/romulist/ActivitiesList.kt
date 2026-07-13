package io.github.flagrant99.romulist

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

@Composable
fun ActivitiesList(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    swapAB: Boolean = false
) {
    val context = LocalContext.current
    val packageInfo = remember(packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    val discoveryMode = remember(packageInfo, packageName) {
        val activities = packageInfo?.activities
        if (activities != null && activities.isNotEmpty()) "Inventory" else "Intent Discovery"
    }

    val activityNames = remember(packageInfo, packageName) {
        val names = mutableSetOf<String>()

        // Strategy 1: Full Inventory (via PackageInfo)
        val activities = packageInfo?.activities
        if (activities != null) {
            for (activity in activities) {
                names.add(activity.name)
            }
        }

        // Strategy 2: Intent Discovery (Fallback for Android 13/14 redaction)
        if (names.isEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setPackage(packageName)
                }

                val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.queryIntentActivities(intent, 0)
                }

                resolved.forEach { names.add(it.activityInfo.name) }
            } catch (_: Exception) {
            }
        }

        names.toList().sorted()
    }

    val appName = remember(packageInfo) {
        try {
            val pm = context.packageManager
            packageInfo?.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    val appVersion = remember(packageInfo) {
        packageInfo?.let { 
            val version = it.versionName ?: "Unknown"
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()
            "$version ($code)"
        } ?: "Unknown"
    }

    val targetSdk = remember(packageInfo) {
        packageInfo?.applicationInfo?.targetSdkVersion?.toString() ?: "Unknown"
    }

    val targetAndroidVersion = remember(packageInfo) {
        val sdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0
        when (sdk) {
            36 -> "16"
            35 -> "15"
            34 -> "14"
            33 -> "13"
            32 -> "12L"
            31 -> "12"
            30 -> "11"
            29 -> "10"
            28 -> "9"
            27 -> "8.1"
            26 -> "8.0"
            25 -> "7.1"
            24 -> "7.0"
            else -> if (sdk > 0) "API $sdk" else "Unknown"
        }
    }

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(packageName) {
        if (activityNames.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVITIES",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        shadow = Shadow(Color.Green.copy(alpha = 0.7f), blurRadius = 16f)
                    ),
                    color = Color.Green
                )
                Text(
                    text = "Package: $packageName",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "App: $appName",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "Version: $appVersion",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "Target SDK: $targetSdk",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "Target Android: $targetAndroidVersion",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                Text(
                    text = "Discovery Mode: $discoveryMode",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = if (discoveryMode == "Inventory") Color.Green.copy(alpha = 0.7f) else Color.Yellow.copy(alpha = 0.7f)
                )
                Text(
                    text = "Total: ${activityNames.size}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Green.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = Color.Green.copy(alpha = 0.5f)
                )
            }
        }

        if (activityNames.isEmpty()) {
            item {
                Text(
                    text = "No activities found for this package.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
        } else {
            itemsIndexed(activityNames) { index, activityName ->
                var isFocused by remember { mutableStateOf(false) }
                val backKey = if (swapAB) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    backKey -> {
                                        onBack()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .background(if (isFocused) Color.Green.copy(alpha = 0.2f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = activityName,
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
}
