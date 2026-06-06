package io.github.flagrant99.romulist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    useNavRail: Boolean,
    swapAB: Boolean,
    onToggleNavRail: (Boolean) -> Unit,
    onToggleSwapAB: (Boolean) -> Unit,
    onSetHome: (String?) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Use Side Nav in Landscape",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = useNavRail,
                onCheckedChange = onToggleNavRail,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    checkedTrackColor = Color.DarkGray,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Black
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Swap South/East Buttons",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = swapAB,
                onCheckedChange = onToggleSwapAB,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    checkedTrackColor = Color.DarkGray,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Black
                )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

        Text(
            text = "Set Home Folder",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 16.dp)
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
