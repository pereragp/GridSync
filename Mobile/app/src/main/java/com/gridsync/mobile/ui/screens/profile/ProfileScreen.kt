package com.gridsync.mobile.ui.screens.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.gridsync.mobile.GridSyncApp
import com.gridsync.mobile.ui.components.AUTH_HERO_IMAGE_URL
import com.gridsync.mobile.ui.data.MockProsumerProfile
import com.gridsync.mobile.ui.data.MockUserRepository
import com.gridsync.mobile.ui.theme.ErrorRed200
import com.gridsync.mobile.ui.theme.ErrorRed50
import com.gridsync.mobile.ui.theme.ErrorRed800
import com.gridsync.mobile.ui.theme.Grid100
import com.gridsync.mobile.ui.theme.Grid50
import com.gridsync.mobile.ui.theme.Grid600
import com.gridsync.mobile.ui.theme.Grid700
import com.gridsync.mobile.ui.theme.Grid800
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
fun ProfileScreen(
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val session = (LocalContext.current.applicationContext as? GridSyncApp)?.sessionStore?.getSession()
    val initialProfile = remember(session?.userId, session?.role) {
        val mock = MockUserRepository.current()
        if (session == null) {
            mock
        } else {
            mock.copy(
                id = session.userId.ifBlank { mock.id },
                fullName = session.fullName.ifBlank { mock.fullName },
                email = session.email.ifBlank { mock.email },
                nic = session.nic?.ifBlank { mock.nic } ?: mock.nic,
                status = session.status.ifBlank { mock.status },
                role = when {
                    session.role.equals("GridOperator", ignoreCase = true) -> "Grid Operator"
                    session.role.isNotBlank() -> session.role
                    else -> mock.role
                },
            )
        }
    }
    var profile by remember { mutableStateOf(initialProfile) }
    var fullName by remember { mutableStateOf(profile.fullName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var address by remember { mutableStateOf(profile.address) }

    var contactError by remember { mutableStateOf<String?>(null) }
    var contactSuccess by remember { mutableStateOf<String?>(null) }
    var contactLoading by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var pwError by remember { mutableStateOf<String?>(null) }
    var pwSuccess by remember { mutableStateOf<String?>(null) }
    var pwLoading by remember { mutableStateOf(false) }

    var deactError by remember { mutableStateOf<String?>(null) }
    var deactSuccess by remember { mutableStateOf<String?>(null) }
    var deactLoading by remember { mutableStateOf(false) }
    var confirmDeact by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Grid600,
        unfocusedBorderColor = Slate300,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledBorderColor = Slate300,
        disabledContainerColor = Color(0xFFF8FAFC),
        disabledTextColor = Slate600,
        cursorColor = Grid700,
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
    )

    fun saveContact() {
        contactError = null
        contactSuccess = null
        when {
            fullName.isBlank() -> contactError = "Full name is required"
            phone.isBlank() -> contactError = "Phone is required"
            else -> {
                contactLoading = true
                scope.launch {
                    delay(700)
                    profile = MockUserRepository.updateContact(fullName, phone, address)
                    fullName = profile.fullName
                    phone = profile.phone
                    address = profile.address
                    contactLoading = false
                    contactSuccess = "Profile saved. Your contact details are up to date."
                }
            }
        }
    }

    fun savePassword() {
        pwError = null
        pwSuccess = null
        when {
            newPassword != confirmPassword -> pwError = "New password and confirmation do not match."
            else -> {
                pwLoading = true
                scope.launch {
                    delay(700)
                    try {
                        MockUserRepository.changePassword(currentPassword, newPassword)
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        pwSuccess = "Password updated. Use the new password next time you sign in."
                    } catch (ex: Exception) {
                        pwError = ex.message ?: "Change failed"
                    } finally {
                        pwLoading = false
                    }
                }
            }
        }
    }

    fun requestDeactivation() {
        deactError = null
        deactSuccess = null
        deactLoading = true
        scope.launch {
            delay(800)
            try {
                profile = MockUserRepository.requestDeactivation()
                confirmDeact = false
                deactSuccess = "Deactivation requested. A Backoffice officer will complete the process."
            } catch (ex: Exception) {
                deactError = ex.message ?: "Request failed"
            } finally {
                deactLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grid50)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            ProfileHero(
                profile = profile,
                showBack = showBack,
                onBack = onBack,
                onSignOut = onSignOut,
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SnapshotCard(profile = profile)

                if (profile.deactivationRequested) {
                    Banner(
                        text = "Deactivation requested. Your request is on file with Backoffice.",
                        error = false,
                    )
                }

                // Contact details
                SectionCard {
                    SectionHeader(
                        title = "Contact details",
                        subtitle = "Name, phone, and address used across GridSync.",
                    )
                    if (contactError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = contactError!!, error = true)
                    }
                    if (contactSuccess != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = contactSuccess!!, error = false)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    FieldLabel("Full name")
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            contactError = null
                            contactSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("Phone")
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            contactError = null
                            contactSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = profile.email,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                    )
                    Text(
                        text = "Email cannot be changed here. Contact Backoffice if needed.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("Address (optional)")
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            contactError = null
                            contactSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = ::saveContact,
                        enabled = !contactLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Grid700,
                            contentColor = Color.White,
                            disabledContainerColor = Grid700.copy(alpha = 0.45f),
                        ),
                    ) {
                        if (contactLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving…", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Text("Save changes", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Security
                SectionCard {
                    SectionHeader(
                        title = "Security",
                        subtitle = "Change your password using your current credentials.",
                    )
                    if (pwError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = pwError!!, error = true)
                    }
                    if (pwSuccess != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = pwSuccess!!, error = false)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    FieldLabel("Current password")
                    PasswordField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            pwError = null
                            pwSuccess = null
                        },
                        show = showCurrent,
                        onToggle = { showCurrent = !showCurrent },
                        colors = fieldColors,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("New password")
                    PasswordField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            pwError = null
                            pwSuccess = null
                        },
                        show = showNew,
                        onToggle = { showNew = !showNew },
                        colors = fieldColors,
                    )
                    Text(
                        text = "At least 8 characters with one letter and one number.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel("Confirm new password")
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            pwError = null
                            pwSuccess = null
                        },
                        show = false,
                        onToggle = {},
                        colors = fieldColors,
                        showToggle = false,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = ::savePassword,
                        enabled = !pwLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Grid700,
                            contentColor = Color.White,
                            disabledContainerColor = Grid700.copy(alpha = 0.45f),
                        ),
                    ) {
                        if (pwLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating…", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Text("Update password", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Deactivation
                SectionCard {
                    SectionHeader(
                        title = "Account deactivation",
                        subtitle = "Request to close your prosumer account. Backoffice must approve.",
                    )
                    if (deactError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = deactError!!, error = true)
                    }
                    if (deactSuccess != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(text = deactSuccess!!, error = false)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "You will lose booking access after Backoffice completes deactivation. This cannot be undone from the app.",
                        color = Slate600,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (profile.deactivationRequested) {
                        Text(
                            text = "Request already submitted",
                            color = Grid700,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else if (!confirmDeact) {
                        OutlinedButton(
                            onClick = { confirmDeact = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ErrorRed200),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed800),
                        ) {
                            Text("Request deactivation", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorRed50)
                                .border(1.dp, ErrorRed200, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Confirm request?",
                                color = ErrorRed800,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "A Backoffice officer will review and deactivate your account.",
                                color = ErrorRed800.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { confirmDeact = false },
                                    enabled = !deactLoading,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Slate300),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                                ) {
                                    Text("Keep account", style = MaterialTheme.typography.labelLarge)
                                }
                                Button(
                                    onClick = ::requestDeactivation,
                                    enabled = !deactLoading,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ErrorRed800,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    if (deactLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Confirm", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileHero(
    profile: MockProsumerProfile,
    showBack: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (showBack) 210.dp else 190.dp)
    ) {
        AsyncImage(
            model = AUTH_HERO_IMAGE_URL,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Grid900.copy(alpha = 0.82f),
                            Grid800.copy(alpha = 0.65f),
                            Grid50,
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBack) {
                    Text(
                        text = "← Back",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBack)
                            .padding(vertical = 4.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onSignOut)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Sign out",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Sign out",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Column {
                Text(
                    text = "PROSUMER",
                    color = Grid100.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your profile",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = SourceSerifFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manage contact details, password, and account status.",
                    color = Grid100.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(profile.status)
                    Pill(profile.email)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SnapshotCard(profile: MockProsumerProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Grid100),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(profile.fullName),
                    color = Grid800,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = profile.fullName,
                    color = Grid900,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = "NIC ${profile.nic}",
                    color = Slate600,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(12.dp))
        InfoRow("Role", profile.role)
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow("Status", profile.status)
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow("Email", profile.email)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Grid100, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = { content() },
    )
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Text(
        text = title,
        color = Grid900,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = subtitle,
        color = Slate600,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    show: Boolean,
    onToggle: () -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    showToggle: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        trailingIcon = if (showToggle) {
            {
                TextButton(onClick = onToggle) {
                    Text(
                        text = if (show) "Hide" else "Show",
                        color = Grid700,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(8.dp),
        colors = colors,
    )
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Slate600, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = Slate900,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun Banner(text: String, error: Boolean) {
    Text(
        text = text,
        color = if (error) ErrorRed800 else Grid700,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (error) ErrorRed50 else Grid50)
            .border(1.dp, if (error) ErrorRed200 else Grid100, RoundedCornerShape(10.dp))
            .padding(12.dp),
    )
}

@Composable
private fun Pill(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
    )
}

private fun initials(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfilePreview() {
    GridSyncMobileTheme {
        ProfileScreen()
    }
}
