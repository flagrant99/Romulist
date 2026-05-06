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
    favoritePath: String?,
    onSetFavorite: (String?) -> Unit,
) {
    var showSetHome by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("RomulistPrefs", android.content.Context.MODE_PRIVATE) }

    val romulistConfig = remember(currentFolder, favoritePath) {
        var dir: File? = currentFolder
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
                favoritePath = favoritePath,
                onSetFavorite = onSetFavorite
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
                val prefKey = "preferred_intent_${system.name}"
                var preferredIntentName by remember(system.name) { 
                    mutableStateOf(sharedPrefs.getString(prefKey, system.mainIntent?.name)) 
                }

                Text(
                    text = "Emulator: ${system.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Green,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                val allIntents = listOfNotNull(system.mainIntent) + system.altIntents
                
                allIntents.forEach { intent ->
                    val isSelected = preferredIntentName == intent.name
                    Text(
                        text = if (isSelected) "[ ${intent.name.uppercase()} ]" else intent.name.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.Cyan else Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                preferredIntentName = intent.name
                                sharedPrefs.edit().putString(prefKey, intent.name).apply()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
