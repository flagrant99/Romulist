package io.github.flagrant99.romulist

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import java.io.File

@Composable
fun Settings(
    currentFolder: File?,
    selectedFile: File?,
    favoritePath: String?,
) {
    val context = LocalContext.current

    var configSource by remember { mutableStateOf<String?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (romulistConfig?.systemConfig != null) {
                val system = romulistConfig.systemConfig

                Text(
                    text = "System: ${system.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Green,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                if (selectedFile != null) {
                    Text(
                        text = "Selected: ${selectedFile.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Cyan,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Text(
                        text = "No file selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = "Source: ${configSource ?: "Internal"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val allIntents = listOfNotNull(system.mainIntent) + system.altIntents
                
                allIntents.forEach { intent ->
                    val isMainIntent = intent == system.mainIntent
                    val isEnabled = selectedFile != null
                    
                    val displayName = intent.name.ifBlank { "LAUNCH" }.uppercase()
                    
                    Text(
                        text = if (isMainIntent) "[ $displayName ]" else displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            !isEnabled -> Color.DarkGray
                            isMainIntent -> Color.Cyan
                            else -> Color.LightGray
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                EmulatorNavigator.launchGame(
                                    context = context,
                                    filePath = selectedFile?.absolutePath ?: "",
                                    config = romulistConfig,
                                    preferredIntent = intent
                                )
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
}
