package com.example.airconapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConnectionErrorDialog(
    onDismiss: () -> Unit,
    onSwitchToMock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection Error") },
        text = { Text("Could not connect to the server. Would you like to switch to Mock Mode?") },
        confirmButton = {
            Button(onClick = onSwitchToMock) {
                Text("Switch to Mock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}