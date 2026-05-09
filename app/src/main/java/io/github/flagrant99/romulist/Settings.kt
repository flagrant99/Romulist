package io.github.flagrant99.romulist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import java.io.File

@Composable
fun Settings(
    currentFolder: File?,
    selectedFile: File?,
    favoritePath: String?,
    onSetHome: (String?) -> Unit,
) {
    var showSetHome by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = remember { PersistentSettings(context) }

    val romulistConfig = remember(selectedFile ?: currentFolder, favoritePath) {
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

    if (showSetHome) {
        BackHandler {
            showSetHome = false
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "< BACK TO SETTINGS",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier
                    .clickable { showSetHome = false }
                    .padding(16.dp)
            )
            SetHomeFolder(
                currentFolder = currentFolder,
                homePath = favoritePath,
                onSetHome = onSetHome
            )
        }
    } else {
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

            Text(
                text = "[ SET HOME FOLDER ]",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Yellow,
                modifier = Modifier
                    .clickable { showSetHome = true }
                    .padding(vertical = 8.dp)
            )

            if (romulistConfig?.systemConfig != null) {
                val system = romulistConfig.systemConfig
                var preferredIntentName by remember(system.name) { 
                    mutableStateOf(settings.getPreferredIntent(system.name, system.mainIntent?.name)) 
                }

                Text(
                    text = "Emulator: ${system.name}",
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
                        text = "No file selected in Home",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                val allIntents = listOfNotNull(system.mainIntent) + system.altIntents
                
                allIntents.forEach { intent ->
                    val isSelected = preferredIntentName == intent.name
                    val isEnabled = selectedFile != null
                    
                    Text(
                        text = if (isSelected) "[ ${intent.name.uppercase()} ]" else intent.name.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            !isEnabled -> Color.DarkGray
                            isSelected -> Color.Cyan
                            else -> Color.LightGray
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                preferredIntentName = intent.name
                                settings.setPreferredIntent(system.name, intent.name)
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
}
