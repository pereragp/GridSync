package com.gridsync.mobile.ui.screens.register

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
import com.gridsync.mobile.ui.theme.Grid500
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
fun RegisterScreen(
    onBackToLogin: () -> Unit = {},
) {
    var nic by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        when {
            nic.isBlank() -> error = "NIC is required"
            fullName.isBlank() -> error = "Full name is required"
            email.isBlank() -> error = "Email is required"
            phone.isBlank() -> error = "Phone is required"
            password.length < 8 -> error = "Password must be at least 8 characters"
            !password.any { it.isLetter() } || !password.any { it.isDigit() } ->
                error = "Password needs at least one letter and one number"
            else -> {
                loading = true
                // Frontend-only stub — wire to API later
                scope.launch {
                    delay(900)
                    loading = false
                    success = true
                }
            }
        }
    }

    fun resetForm() {
        nic = ""
        fullName = ""
        email = ""
        phone = ""
        address = ""
        password = ""
        showPassword = false
        error = null
        success = false
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RegisterBrandColumn()
            RegisterCard(
                nic = nic,
                onNicChange = {
                    nic = it
                    error = null
                },
                fullName = fullName,
                onFullNameChange = {
                    fullName = it
                    error = null
                },
                email = email,
                onEmailChange = {
                    email = it
                    error = null
                },
                phone = phone,
                onPhoneChange = {
                    phone = it
                    error = null
                },
                address = address,
                onAddressChange = {
                    address = it
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
                success = success,
                loading = loading,
                onSubmit = ::submit,
                onBackToLogin = onBackToLogin,
                onRegisterAnother = ::resetForm,
            )
        }
    }
}

@Composable
private fun RegisterBrandColumn() {
    Column(modifier = Modifier.fillMaxWidth()) {
        BrandLogo(variant = BrandLogoVariant.Auth)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Join GridSync as a solar prosumer.",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = SourceSerifFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Register with your NIC to reserve Charging and Drop-off slots once Backoffice activates your account.",
            color = Grid100.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        BenefitRow("NIC is your primary account key")
        Spacer(modifier = Modifier.height(8.dp))
        BenefitRow("Status starts as Pending after signup")
        Spacer(modifier = Modifier.height(8.dp))
        BenefitRow("Book energy slots after approval")
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Grid500.copy(alpha = 0.35f))
                .border(1.dp, Grid100.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                color = Grid100,
                style = MaterialTheme.typography.labelMedium,
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
private fun RegisterCard(
    nic: String,
    onNicChange: (String) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    error: String?,
    success: Boolean,
    loading: Boolean,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    onRegisterAnother: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PROSUMER SIGNUP",
                    color = Grid600,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Create your account",
                    color = Grid900,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Takes a minute. Approval is required before you can book.",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(
                onClick = onBackToLogin,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate300),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
            ) {
                Text("Sign in", style = MaterialTheme.typography.labelMedium)
            }
        }

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
                    .border(1.dp, Grid100, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "You are registered",
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Registered successfully. Your account is Pending until a Backoffice officer approves it.",
                    color = Grid700,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBackToLogin,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Grid700,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Go to sign in", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onRegisterAnother,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Slate300),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                    ) {
                        Text("Register another", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))

            FieldLabel("National Identity Card (NIC)")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nic,
                onValueChange = onNicChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("e.g. 199012345678", color = Slate600.copy(alpha = 0.7f))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )
            Text(
                text = "Used as your unique prosumer key across GridSync.",
                color = Slate600,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Full name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Your full name", color = Slate600.copy(alpha = 0.7f))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Email")
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
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Phone")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("07xxxxxxxx", color = Slate600.copy(alpha = 0.7f))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Address (optional)")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Street, city", color = Slate600.copy(alpha = 0.7f))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel("Password")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Min 8 characters, letter + number", color = Slate600.copy(alpha = 0.7f))
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
            Text(
                text = "At least 8 characters with one letter and one number.",
                color = Slate600,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))
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
                    Text("Creating account…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Create prosumer account", style = MaterialTheme.typography.labelLarge)
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
                    text = "Already registered? ",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Sign in",
                    color = Grid700,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.clickable(onClick = onBackToLogin),
                )
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
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    GridSyncMobileTheme {
        RegisterScreen()
    }
}
