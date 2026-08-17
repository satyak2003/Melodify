package com.melodify.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melodify.shared.domain.download.DownloadQuality

@Composable
fun QualitySelectionDialog(
    onDismiss: () -> Unit,
    onQualitySelected: (DownloadQuality) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Download Quality", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "High-Fidelity (FLAC)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Requires Soulseek credentials in Settings. Lossless studio quality.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onQualitySelected(DownloadQuality.FLAC) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download in FLAC")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Normal Quality (M4A)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Downloads instantly from YouTube. High quality format.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { onQualitySelected(DownloadQuality.NORMAL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download in Normal Quality")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
