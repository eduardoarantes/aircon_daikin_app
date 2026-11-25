package com.example.airconapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.airconapp.ui.theme.AirconColors

/** Shown once the app has searched for the aircon a few times without reaching it. */
@Composable
fun ConnectionErrorDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AirconColors.Surface,
        title = { Text("Can't connect to the aircon", color = AirconColors.TextPrimary) },
        text = {
            Text(
                "The aircon didn't answer after a few tries. Check that this phone is on the same " +
                    "Wi-Fi as the aircon, or find it or enter its address in Settings.",
                color = AirconColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings", color = AirconColors.Cool, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep trying", color = AirconColors.TextPrimary) }
        }
    )
}
