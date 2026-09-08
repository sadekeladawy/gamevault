package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.AuthState
import com.example.ui.components.AuthTab
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberPurpleVariant
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuthScreen(
    currentUser: UserProfile?,
    authState: AuthState,
    isFirebaseConfigured: Boolean,
    isCloudSyncing: Boolean,
    lastSyncTimestamp: Long?,
    totalLocalGames: Int,
    initialTab: AuthTab = AuthTab.SIGN_IN,
    unverifiedEmail: String? = null,
    isResendingEmail: Boolean = false,
    onSignIn: (email: String, pass: String, rememberMe: Boolean) -> Unit,
    onSignUp: (fullName: String, email: String, pass: String, confirmPass: String, gamerTag: String?, rememberMe: Boolean) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onSendVerificationEmail: () -> Unit,
    onResendVerificationEmail: (email: String, pass: String?) -> Unit = { _, _ -> },
    onRefreshVerification: () -> Unit,
    onUpdateProfile: (fullName: String, gamerTag: String?, photoUrl: String?) -> Unit,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember(initialTab) { mutableStateOf(initialTab) }

    // If registration succeeds, switch to Sign In tab so user can see verification notice & sign in
    androidx.compose.runtime.LaunchedEffect(authState) {
        if (authState is AuthState.RegistrationSuccess || authState is AuthState.EmailNotVerified) {
            currentTab = AuthTab.SIGN_IN
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("auth_screen")
    ) {
        if (currentUser != null) {
            // User is logged in -> Render Gamer Commander Profile & Cloud Sync Hub
            UserProfileScreenView(
                user = currentUser,
                isFirebaseConfigured = isFirebaseConfigured,
                isCloudSyncing = isCloudSyncing,
                lastSyncTimestamp = lastSyncTimestamp,
                totalLocalGames = totalLocalGames,
                onSendVerificationEmail = onSendVerificationEmail,
                onRefreshVerification = onRefreshVerification,
                onUpdateProfile = onUpdateProfile,
                onSyncToCloud = onSyncToCloud,
                onRestoreFromCloud = onRestoreFromCloud,
                onSignOut = onSignOut,
                onNavigateToLibrary = onNavigateToLibrary
            )
        } else {
            // User is not logged in -> Tabbed Sign In / Register / Reset flow
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Segmented Bar for switching between Sign In, Register, Forgot Password
                TabRow(
                    selectedTabIndex = when (currentTab) {
                        AuthTab.SIGN_IN -> 0
                        AuthTab.CREATE_ACCOUNT -> 1
                        AuthTab.FORGOT_PASSWORD -> 2
                    },
                    containerColor = DarkSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        val index = when (currentTab) {
                            AuthTab.SIGN_IN -> 0
                            AuthTab.CREATE_ACCOUNT -> 1
                            AuthTab.FORGOT_PASSWORD -> 2
                        }
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("auth_screen_tabs")
                ) {
                    Tab(
                        selected = currentTab == AuthTab.SIGN_IN,
                        onClick = { currentTab = AuthTab.SIGN_IN },
                        text = {
                            Text(
                                text = "Sign In",
                                fontWeight = if (currentTab == AuthTab.SIGN_IN) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == AuthTab.SIGN_IN) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("auth_tab_sign_in")
                    )
                    Tab(
                        selected = currentTab == AuthTab.CREATE_ACCOUNT,
                        onClick = { currentTab = AuthTab.CREATE_ACCOUNT },
                        text = {
                            Text(
                                text = "Register",
                                fontWeight = if (currentTab == AuthTab.CREATE_ACCOUNT) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == AuthTab.CREATE_ACCOUNT) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("auth_tab_register")
                    )
                    Tab(
                        selected = currentTab == AuthTab.FORGOT_PASSWORD,
                        onClick = { currentTab = AuthTab.FORGOT_PASSWORD },
                        text = {
                            Text(
                                text = "Reset",
                                fontWeight = if (currentTab == AuthTab.FORGOT_PASSWORD) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == AuthTab.FORGOT_PASSWORD) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("auth_tab_reset")
                    )
                }

                // Body content with smooth transition
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "AuthScreenContent"
                ) { tab ->
                    when (tab) {
                        AuthTab.SIGN_IN -> {
                            SignInScreen(
                                authState = authState,
                                isFirebaseConfigured = isFirebaseConfigured,
                                unverifiedEmail = unverifiedEmail,
                                isResendingEmail = isResendingEmail,
                                onSignIn = onSignIn,
                                onResendVerification = onResendVerificationEmail,
                                onNavigateToSignUp = { currentTab = AuthTab.CREATE_ACCOUNT },
                                onNavigateToForgotPassword = { currentTab = AuthTab.FORGOT_PASSWORD }
                            )
                        }

                        AuthTab.CREATE_ACCOUNT -> {
                            RegisterScreen(
                                authState = authState,
                                isFirebaseConfigured = isFirebaseConfigured,
                                onSignUp = onSignUp,
                                onNavigateToSignIn = { currentTab = AuthTab.SIGN_IN }
                            )
                        }

                        AuthTab.FORGOT_PASSWORD -> {
                            ForgotPasswordScreenView(
                                authState = authState,
                                onForgotPassword = onForgotPassword,
                                onBackToSignIn = { currentTab = AuthTab.SIGN_IN }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreenView(
    authState: AuthState,
    onForgotPassword: (email: String) -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var successSent by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Loading
    val serverError = (authState as? AuthState.Error)?.message

    fun performReset() {
        localError = null
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            localError = "Please enter your registered email address."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            localError = "Please enter a valid email address."
            return
        }
        focusManager.clearFocus()
        onForgotPassword(cleanEmail)
        successSent = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("forgot_password_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(CyberPurple.copy(alpha = 0.4f), DarkSurface)
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(CyberPurple, NeonCyan)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "Reset Password",
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Reset Your Password",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Enter your registered email address to receive password reset instructions.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Success banner if sent
        if (successSent) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(AccentEmerald, AccentEmerald))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Password reset instructions dispatched to your email. Check your inbox and spam folder.",
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        val displayedError = localError ?: serverError
        if (!displayedError.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(AccentRose, AccentRose))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = AccentRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayedError,
                            color = AccentRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(DarkCardBorder, DarkCardBorder.copy(alpha = 0.6f)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = "REGISTERED EMAIL",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localError = null
                            },
                            placeholder = { Text("commander@gamevault.io", color = TextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (email.isNotEmpty()) NeonCyan else TextMuted
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { performReset() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )
                    }

                    Button(
                        onClick = { performReset() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("forgot_password_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sending Link...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Send Reset Link", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onBackToSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("forgot_password_back_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to Sign In", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileScreenView(
    user: UserProfile,
    isFirebaseConfigured: Boolean,
    isCloudSyncing: Boolean,
    lastSyncTimestamp: Long?,
    totalLocalGames: Int,
    onSendVerificationEmail: () -> Unit,
    onRefreshVerification: () -> Unit,
    onUpdateProfile: (fullName: String, gamerTag: String?, photoUrl: String?) -> Unit,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editFullName by remember(user) { mutableStateOf(user.fullName) }
    var editGamerTag by remember(user) { mutableStateOf(user.gamerTag.orEmpty()) }
    var editPhotoUrl by remember(user) { mutableStateOf(user.photoUrl.orEmpty()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Profile Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyberPurple.copy(alpha = 0.7f), NeonCyan.copy(alpha = 0.7f))),
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(DarkBg)
                            .border(2.5.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = user.fullName.firstOrNull()?.uppercase() ?: "G"
                            Text(
                                text = initial,
                                color = NeonCyan,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user.fullName.ifEmpty { "Gamer Commander" },
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (!user.gamerTag.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberPurple.copy(alpha = 0.22f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (user.gamerTag.startsWith("@")) user.gamerTag else "@${user.gamerTag}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = user.email,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditingProfile = !isEditingProfile },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(DarkCardBorder, NeonCyan.copy(alpha = 0.5f)))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEditingProfile) "Cancel Edit" else "Edit Profile", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onNavigateToLibrary,
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Inline Profile Editor
        if (isEditingProfile) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), CyberPurple.copy(alpha = 0.5f)))
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "EDIT IDENTITY",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = editGamerTag,
                            onValueChange = { editGamerTag = it },
                            label = { Text("Gamer Tag (e.g. Commander#101)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPurple,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = editPhotoUrl,
                            onValueChange = { editPhotoUrl = it },
                            label = { Text("Avatar Image URL (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                onUpdateProfile(
                                    editFullName.trim().ifEmpty { user.fullName },
                                    editGamerTag.trim().ifEmpty { null },
                                    editPhotoUrl.trim().ifEmpty { null }
                                )
                                isEditingProfile = false
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Email Verification Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user.isEmailVerified) AccentEmerald.copy(alpha = 0.08f) else AccentAmber.copy(alpha = 0.08f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (user.isEmailVerified) AccentEmerald.copy(alpha = 0.5f) else AccentAmber.copy(alpha = 0.5f),
                            if (user.isEmailVerified) AccentEmerald.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (user.isEmailVerified) Icons.Default.Verified else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (user.isEmailVerified) AccentEmerald else AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (user.isEmailVerified) "Email Verified" else "Email Unverified",
                                color = if (user.isEmailVerified) AccentEmerald else AccentAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(onClick = onRefreshVerification, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Status",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = if (user.isEmailVerified) {
                            "Your account is verified and ready for cloud backup and multiplayer features."
                        } else {
                            "Please verify your email address to ensure seamless cloud backup and vault recovery."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    if (!user.isEmailVerified) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onSendVerificationEmail,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = DarkBg),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resend Verification Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Cloud Vault Synchronization Card (Firestore)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(DarkCardBorder, DarkCardBorder.copy(alpha = 0.5f)))
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Vault Sync",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Backup your local game library ($totalLocalGames games) to Cloud Firestore or restore your collection onto this device.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    if (lastSyncTimestamp != null && lastSyncTimestamp > 0) {
                        Text(
                            text = "Last synced: ${dateFormat.format(Date(lastSyncTimestamp))}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncToCloud,
                            enabled = !isCloudSyncing,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onRestoreFromCloud,
                            enabled = !isCloudSyncing,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(DarkCardBorder, NeonCyan.copy(alpha = 0.5f)))
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Account Metadata & Sign Out
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(DarkCardBorder, DarkCardBorder.copy(alpha = 0.4f)))
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ACCOUNT DETAILS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Player UID", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = user.uid.take(12) + "...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (user.createdAt > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Created", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = dateFormat.format(Date(user.createdAt)),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Divider(color = DarkCardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("profile_sign_out_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(AccentRose.copy(alpha = 0.5f), AccentRose.copy(alpha = 0.5f)))
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out of GameVault", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
