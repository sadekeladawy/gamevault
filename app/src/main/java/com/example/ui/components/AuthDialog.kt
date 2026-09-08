package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AuthState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberPurpleVariant
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AuthTab {
    SIGN_IN,
    CREATE_ACCOUNT,
    FORGOT_PASSWORD
}

@Composable
fun AuthDialog(
    authState: AuthState,
    isFirebaseConfigured: Boolean,
    initialTab: AuthTab = AuthTab.SIGN_IN,
    unverifiedEmail: String? = null,
    isResendingEmail: Boolean = false,
    onDismiss: () -> Unit,
    onSignIn: (email: String, pass: String, rememberMe: Boolean) -> Unit,
    onSignUp: (fullName: String, email: String, pass: String, confirmPass: String, gamerTag: String?, rememberMe: Boolean) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onResendVerification: (email: String, pass: String?) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    // Form inputs
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(unverifiedEmail ?: "") }
    var gamerTag by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var clientValidationError by remember { mutableStateOf<String?>(null) }
    var passwordResetSent by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isLoading = authState is AuthState.Loading

    LaunchedEffect(unverifiedEmail) {
        if (!unverifiedEmail.isNullOrBlank() && email.isBlank()) {
            email = unverifiedEmail
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.EmailNotVerified) {
            selectedTab = AuthTab.SIGN_IN
            if (authState.email.isNotBlank() && email.isBlank()) {
                email = authState.email
            }
        } else if (authState is AuthState.RegistrationSuccess) {
            selectedTab = AuthTab.SIGN_IN
            if (authState.email.isNotBlank() && email.isBlank()) {
                email = authState.email
            }
        }
    }

    // Reset errors on tab change
    LaunchedEffect(selectedTab) {
        clientValidationError = null
        passwordResetSent = false
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Brush.verticalGradient(listOf(CyberPurple, DarkCardBorder)), RoundedCornerShape(24.dp)),
            color = DarkCard,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(CyberPurple, NeonCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "GameVault Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GameVault ID",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFirebaseConfigured) "Cloud Sync & Profile" else "Offline Vault Profile",
                                color = if (isFirebaseConfigured) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        AuthTab.SIGN_IN to "Sign In",
                        AuthTab.CREATE_ACCOUNT to "Create Account",
                        AuthTab.FORGOT_PASSWORD to "Reset"
                    )

                    tabs.forEach { (tab, label) ->
                        val isSelected = selectedTab == tab
                        val backgroundModifier = if (isSelected) {
                            Modifier.background(Brush.horizontalGradient(listOf(CyberPurpleVariant, CyberPurple)))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(backgroundModifier)
                                .clickable(enabled = !isLoading) { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Email Verification Required Banner
                if (authState is AuthState.EmailNotVerified) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentAmber.copy(alpha = 0.12f))
                            .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Email Verification Required",
                                tint = AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Email Verification Required",
                                    color = AccentAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = authState.message,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val targetEmail = if (email.isNotBlank()) email.trim() else authState.email
                                val targetPass = if (password.isNotBlank()) password.trim() else null
                                onResendVerification(targetEmail, targetPass)
                            },
                            enabled = !isResendingEmail,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentAmber,
                                contentColor = DarkBg
                            )
                        ) {
                            if (isResendingEmail) {
                                CircularProgressIndicator(
                                    color = DarkBg,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resending...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resend Verification Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Registration Success (Verification email sent)
                if (authState is AuthState.RegistrationSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentEmerald.copy(alpha = 0.12f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verification Sent",
                                tint = AccentEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Created! Verification Email Sent",
                                    color = AccentEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = authState.message,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val targetEmail = if (email.isNotBlank()) email.trim() else authState.email
                                val targetPass = if (password.isNotBlank()) password.trim() else null
                                onResendVerification(targetEmail, targetPass)
                            },
                            enabled = !isResendingEmail,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isResendingEmail) {
                                CircularProgressIndicator(
                                    color = AccentEmerald,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sending...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Didn't receive email? Resend", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Verification Email Dispatched Notification
                if (authState is AuthState.VerificationEmailSent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = authState.message,
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Error Display (Server or Client)
                val errorMessage = clientValidationError ?: (authState as? AuthState.Error)?.message
                if (errorMessage != null && !passwordResetSent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentRose.copy(alpha = 0.12f))
                            .border(1.dp, AccentRose.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = AccentRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = AccentRose,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Password Reset Success Message
                if (passwordResetSent && selectedTab == AuthTab.FORGOT_PASSWORD) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentEmerald.copy(alpha = 0.15f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Password reset email sent! Please check your inbox and spam folder.",
                            color = AccentEmerald,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Form Fields Animated by Tab
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AuthFormAnimation"
                ) { tab ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (tab) {
                            AuthTab.SIGN_IN -> {
                                // Email Field
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it; clientValidationError = null },
                                    label = { Text("Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Password Field
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it; clientValidationError = null },
                                    label = { Text("Password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        onSignIn(email, password, rememberMe)
                                    }),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Remember Me & Forgot Password Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { if (!isLoading) rememberMe = !rememberMe }
                                    ) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it },
                                            enabled = !isLoading,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = CyberPurple,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Remember me",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = { selectedTab = AuthTab.FORGOT_PASSWORD },
                                        enabled = !isLoading
                                    ) {
                                        Text(
                                            text = "Forgot password?",
                                            color = NeonCyan,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Sign In Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (email.isBlank()) {
                                            clientValidationError = "Please enter your email address."
                                        } else if (password.isBlank()) {
                                            clientValidationError = "Please enter your password."
                                        } else {
                                            onSignIn(email, password, rememberMe)
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_signin_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Sign In to GameVault",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            AuthTab.CREATE_ACCOUNT -> {
                                // Full Name
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it; clientValidationError = null },
                                    label = { Text("Full Name") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Gamer Tag (Optional)
                                OutlinedTextField(
                                    value = gamerTag,
                                    onValueChange = { gamerTag = it },
                                    label = { Text("Gamer Tag / Alias (Optional)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_tag_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Email
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it; clientValidationError = null },
                                    label = { Text("Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_create_email_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Password
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it; clientValidationError = null },
                                    label = { Text("Password (min 6 characters)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_create_password_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Confirm Password
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it; clientValidationError = null },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Remember Me
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { if (!isLoading) rememberMe = !rememberMe }
                                ) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = { rememberMe = it },
                                        enabled = !isLoading,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CyberPurple,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Text(
                                        text = "Keep me signed in on this device",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Create Account Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (fullName.trim().length < 2) {
                                            clientValidationError = "Please enter your full name."
                                        } else if (email.isBlank()) {
                                            clientValidationError = "Please enter a valid email address."
                                        } else if (password.length < 6) {
                                            clientValidationError = "Password must be at least 6 characters."
                                        } else if (password != confirmPassword) {
                                            clientValidationError = "Passwords do not match."
                                        } else {
                                            onSignUp(fullName, email, password, confirmPassword, gamerTag, rememberMe)
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_signup_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Create Vault Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            AuthTab.FORGOT_PASSWORD -> {
                                Text(
                                    text = "Enter your registered email address and we'll send you instructions to securely reset your password.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it; clientValidationError = null; passwordResetSent = false },
                                    label = { Text("Email Address") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_reset_email_input"),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (email.isBlank()) {
                                            clientValidationError = "Please enter your email address."
                                        } else {
                                            onForgotPassword(email)
                                            passwordResetSent = true
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_reset_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Send Reset Link",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { selectedTab = AuthTab.SIGN_IN },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading
                                ) {
                                    Text(
                                        text = "Back to Sign In",
                                        color = NeonCyan,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guest Mode / Offline dismissal button
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Continue Offline / Browse as Guest",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                if (!isFirebaseConfigured) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg.copy(alpha = 0.6f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Note: Place google-services.json in the /app folder to connect with live Firebase servers.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberPurple,
    unfocusedBorderColor = DarkCardBorder,
    focusedLabelColor = CyberPurple,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkBg,
    unfocusedContainerColor = DarkBg
)
