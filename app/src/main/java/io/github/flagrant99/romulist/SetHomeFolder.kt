package io.github.flagrant99.romulist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun SetHomeFolder(
    currentFolder: File?,
    homePath: String?,
    onSetHome: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Home Folder",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Display current path
        Text(
            text = "Current Path:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = currentFolder?.absolutePath ?: "Root (Drive List)",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Set Button
        if (currentFolder != null) {
            Text(
                text = "[ SET AS HOME ]",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Yellow,
                modifier = Modifier
                    .clickable { onSetHome(currentFolder.absolutePath) }
                    .padding(12.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color.DarkGray)

        // Display Saved Home Folder
        Text(
            text = "Saved Home Folder:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = homePath ?: "None",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = if (homePath != null) Color.Green else Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (homePath != null) {
            Text(
                text = "[ CLEAR HOME FOLDER ]",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Red,
                modifier = Modifier
                    .clickable { onSetHome(null) }
                    .padding(12.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color.DarkGray)

    }
}
