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
import java.io.File

@Composable
fun Settings(
    currentFolder: File?,
    favoritePath: String?,
    onSetFavorite: (String?) -> Unit,
) {
    var showSetHome by rememberSaveable { mutableStateOf(false) }

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
        }
    }
}
