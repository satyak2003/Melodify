package com.melodify.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DownloadErrorDialog(
    errorDetails: String,
    onRetry: () -> Unit,
    onDownloadNormal: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ouch, that download did not work \uD83D\uDE22", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "We couldn't download the High-Fidelity FLAC version of this song. It might not be available on Soulseek, or the connection timed out.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { showDetails = !showDetails },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (showDetails) "Hide Error Details" else "Show Error Details")
                }
                
                AnimatedVisibility(visible = showDetails) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = errorDetails,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownloadNormal) {
                Text("Download Normal Quality")
            }
        },
        dismissButton = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    )
}
