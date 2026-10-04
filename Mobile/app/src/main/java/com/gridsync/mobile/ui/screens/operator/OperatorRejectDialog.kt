package com.gridsync.mobile.ui.screens.operator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate900

@Composable
fun RejectReasonDialog(
    reservationCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reject booking",
                color = Grid900,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "Provide a reason for rejecting $reservationCode.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        localError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = {
                        Text("e.g. Station capacity unavailable", color = Slate600.copy(alpha = 0.7f))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Grid600,
                        unfocusedBorderColor = Slate300,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Grid700,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                    ),
                )
                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError.orEmpty(),
                        color = ErrorRed800,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = reason.trim()
                    if (trimmed.isBlank()) {
                        localError = "Rejection reason is required"
                        return@Button
                    }
                    onConfirm(trimmed)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed800,
                    contentColor = Color.White,
                ),
            ) {
                Text("Reject", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Grid700)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
    )
}
