package com.gridsync.mobile.ui.screens.login

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.components.AuthHeroBackground
import com.gridsync.mobile.ui.components.BrandLogo
import com.gridsync.mobile.ui.components.BrandLogoVariant
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val authRepository = (LocalContext.current.applicationContext as? GridSyncApp)?.authRepository

    fun submit() {
        error = null
        if (email.isBlank() || password.isBlank()) {
            error = "Email and password are required"
            return
        }
        val repository = authRepository
        if (repository == null) {
            error = "App is not ready. Restart and try again."
            return
        }
        loading = true
        scope.launch {
            try {
                val session = repository.login(email, password)
                if (!session.role.equals("Prosumer", ignoreCase = true)) {
                    repository.logoutLocal()
                    error = "This app is for Prosumer accounts only. Your role is ${session.role}."
                    loading = false
                    return@launch
                }
                loading = false
                onLoginSuccess()
            } catch (e: Exception) {
                loading = false
                error = e.message ?: "Sign in failed"
            }
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
            SignInCard(
                email = email,
                onEmailChange = {
                    email = it
                    error = null
                },
                password = password,
                onPasswordChange = {
                    password = it
                    error = null
                },
                showPassword = showPassword,
                onToggleShowPassword = { showPassword = !showPassword },
                error = error,
                loading = loading,
                onForgotPassword = onForgotPassword,
                onRegister = onRegister,
                onSubmit = ::submit,
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
            text = "Power trading, synchronized for a cleaner grid.",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = SourceSerifFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Sign in to manage microgrid nodes, energy slots, and prosumer reservations.",
            color = Grid100.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SignInCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    error: String?,
    loading: Boolean,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onSubmit: () -> Unit,
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
            text = "Welcome back",
            color = Grid900,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use your GridSync credentials to continue.",
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

        Spacer(modifier = Modifier.height(20.dp))

        FieldLabel("Email")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text("you@gridsync.local", color = Slate600.copy(alpha = 0.7f))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors,
        )

        Spacer(modifier = Modifier.height(16.dp))

        FieldLabel("Password")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text("Enter your password", color = Slate600.copy(alpha = 0.7f))
            },
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon = {
                TextButton(onClick = onToggleShowPassword) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = "Forgot password?",
                color = Grid700,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.clickable(onClick = onForgotPassword),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.size(8.dp))
                Text("Signing in…", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("Sign in", style = MaterialTheme.typography.labelLarge)
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
                text = "New prosumer? ",
                color = Slate600,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Register here",
                color = Grid700,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.clickable(onClick = onRegister),
            )
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
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    GridSyncMobileTheme {
        LoginScreen()
    }
}
