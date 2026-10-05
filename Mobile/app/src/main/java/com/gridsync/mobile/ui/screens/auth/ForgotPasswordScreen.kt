package com.gridsync.mobile.ui.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit = {},
    onContinueToReset: (email: String) -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        if (email.isBlank() || !email.contains("@")) {
            error = "Enter a valid account email"
            return
        }
        loading = true
        // Frontend-only stub — wire to POST /api/auth/forgot-password later
        scope.launch {
            delay(900)
            loading = false
            success = true
        }
    }

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
            BrandColumn()
            Spacer(modifier = Modifier.height(24.dp))
            RecoveryCard(
                email = email,
                onEmailChange = {
                    email = it
                    error = null
                },
                error = error,
                success = success,
                loading = loading,
                onSubmit = ::submit,
                onBackToLogin = onBackToLogin,
                onContinueToReset = { onContinueToReset(email.trim()) },
                onTryAgain = {
                    success = false
                    error = null
                },
            )
        }
    }
}

@Composable
private fun BrandColumn() {
    Column(modifier = Modifier.fillMaxWidth()) {
        BrandLogo(variant = BrandLogoVariant.Auth)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Regain access in a few steps.",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = SourceSerifFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Enter the email on your GridSync account. We will send a secure reset link so you can choose a new password.",
            color = Grid100.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        StepRow(1, "Enter your account email")
        Spacer(modifier = Modifier.height(8.dp))
        StepRow(2, "Open the reset link from your inbox")
        Spacer(modifier = Modifier.height(8.dp))
        StepRow(3, "Choose a new password and sign in")
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Grid100.copy(alpha = 0.2f))
                .border(1.dp, Grid100.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = Grid100,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Grid100.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RecoveryCard(
    email: String,
    onEmailChange: (String) -> Unit,
    error: String?,
    success: Boolean,
    loading: Boolean,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    onContinueToReset: () -> Unit,
    onTryAgain: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Grid600,
        unfocusedBorderColor = Slate300,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = Grid700,
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(24.dp)
    ) {
        Text(
            text = "ACCOUNT RECOVERY",
            color = Grid600,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Forgot password",
            color = Grid900,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "We will email a reset link when mail is configured on the API.",
            color = Slate600,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
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
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Grid50)
                    .border(1.dp, Color(0xFFC6E6D2), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Check your inbox",
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If an account exists for $email, a password reset link has been sent. (UI stub — API wiring comes next.)",
                    color = Grid700,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onContinueToReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Grid700,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Continue to reset", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onTryAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate300),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                ) {
                    Text("Use a different email", style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Email",
                color = Slate700,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("you@example.com", color = Slate600.copy(alpha = 0.7f))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onSubmit,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Grid700,
                    contentColor = Color.White,
                    disabledContainerColor = Grid700.copy(alpha = 0.6f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Send reset link", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Remembered it? ",
                color = Slate600,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Back to sign in",
                color = Grid700,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.clickable(onClick = onBackToLogin),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ForgotPasswordPreview() {
    GridSyncMobileTheme {
        ForgotPasswordScreen()
    }
}
