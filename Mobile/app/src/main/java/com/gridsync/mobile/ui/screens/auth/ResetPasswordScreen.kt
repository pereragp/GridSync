package com.gridsync.mobile.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridsync.mobile.ui.components.AuthHeroBackground
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid900
import com.gridsync.mobile.ui.theme.GridSyncMobileTheme
import com.gridsync.mobile.ui.theme.OutfitFontFamily
import com.gridsync.mobile.ui.theme.Slate300
import com.gridsync.mobile.ui.theme.Slate600
import com.gridsync.mobile.ui.theme.Slate700
import com.gridsync.mobile.ui.theme.Slate900
import com.gridsync.mobile.ui.theme.SourceSerifFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    initialEmail: String = "",
    onBackToLogin: () -> Unit = {},
) {
    var email by remember { mutableStateOf(initialEmail) }
    var resetToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        when {
            email.isBlank() || !email.contains("@") -> error = "Enter a valid email"
            resetToken.isBlank() -> error = "Reset token is required"
            newPassword.length < 8 ||
                !newPassword.any { it.isLetter() } ||
                !newPassword.any { it.isDigit() } ->
                error = "Password needs 8+ characters with a letter and a number"
            else -> {
                loading = true
                // Frontend-only stub — wire to POST /api/auth/reset-password later
                scope.launch {
                    delay(900)
                    loading = false
                    success = true
                }
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Grid600,
        unfocusedBorderColor = Slate300,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = Grid700,
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid900)
    ) {
        AuthHeroBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
        ) {
            BrandLogo(variant = BrandLogoVariant.Auth)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Choose a new password.",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = SourceSerifFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Paste the token from your email (or the API fallback), then set a new password.",
                color = Grid100.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "RESET PASSWORD",
                    color = Grid600,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set a new password",
                    color = Grid900,
                    style = MaterialTheme.typography.headlineMedium,
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = error!!,
                        color = ErrorRed800,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorRed50)
                            .border(1.dp, ErrorRed200, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                if (success) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Grid50)
                            .border(1.dp, Color(0xFFC6E6D2), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Password reset",
                            color = Grid900,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You can sign in with your new password now.",
                            color = Grid700,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onBackToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Grid700,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Back to sign in", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("Reset token")
                    OutlinedTextField(
                        value = resetToken,
                        onValueChange = {
                            resetToken = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text("Paste token from email", color = Slate600.copy(alpha = 0.7f))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("New password")
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = if (showPassword) "Hide" else "Show",
                                    color = Grid700,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                    )
                    Text(
                        text = "At least 8 characters with one letter and one number.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = ::submit,
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Grid700,
                            contentColor = Color.White,
                            disabledContainerColor = Grid700.copy(alpha = 0.6f),
                        ),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving…", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Text("Reset password", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Back to sign in",
                        color = Grid700,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.clickable(onClick = onBackToLogin),
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Slate700,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ResetPasswordPreview() {
    GridSyncMobileTheme {
        ResetPasswordScreen(initialEmail = "ayesha.perera@example.com")
    }
}
