package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Patterns
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
    data class EmailNotVerified(val email: String, val message: String) : AuthState()
    data class RegistrationSuccess(val email: String, val message: String) : AuthState()
    data class VerificationEmailSent(val email: String, val message: String) : AuthState()
}

class EmailNotVerifiedException(val email: String, override val message: String) : Exception(message)

class AuthRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "gamevault_auth_prefs"
        private const val KEY_REMEMBER_ME = "pref_remember_me"
        private const val KEY_OFFLINE_USER_NAME = "pref_offline_user_name"
        private const val KEY_OFFLINE_USER_EMAIL = "pref_offline_user_email"
        private const val KEY_OFFLINE_USER_TAG = "pref_offline_user_tag"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _unverifiedEmail = MutableStateFlow<String?>(null)
    val unverifiedEmail: StateFlow<String?> = _unverifiedEmail.asStateFlow()

    private val _savedPasswordForResend = MutableStateFlow<String?>(null)

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
            null
        }
    }

    fun isFirebaseConfigured(): Boolean = firebaseAuth != null

    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, true)

    fun setRememberMeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    fun clearAuthState() {
        _authState.value = AuthState.Idle
    }

    /**
     * Attempts automatic login on application launch.
     * Checks if a Firebase user session exists and verifies email verification status.
     * Unverified users are signed out and prevented from accessing the main app.
     */
    suspend fun checkAutoLogin() = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        val rememberMe = isRememberMeEnabled()

        if (!rememberMe) {
            _authState.value = AuthState.Unauthenticated()
            _currentUser.value = null
            return@withContext
        }

        if (auth != null) {
            val user = auth.currentUser
            if (user != null) {
                try {
                    // Refresh token / verification status directly with Firebase servers
                    user.reload().awaitTask()
                    if (!user.isEmailVerified) {
                        Log.i(TAG, "User ${user.email} is not email verified. Signing out.")
                        auth.signOut()
                        _currentUser.value = null
                        _unverifiedEmail.value = user.email ?: ""
                        _authState.value = AuthState.EmailNotVerified(
                            email = user.email ?: "",
                            message = "Your email is not verified yet. Please check your inbox and verify your email before logging in."
                        )
                        return@withContext
                    }
                    val profile = loadOrCreateProfile(user)
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                    firestoreRepository.updateLastLogin(user.uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Auto login refresh failed, checking user: ${e.message}")
                    if (!user.isEmailVerified) {
                        auth.signOut()
                        _currentUser.value = null
                        _unverifiedEmail.value = user.email ?: ""
                        _authState.value = AuthState.EmailNotVerified(
                            email = user.email ?: "",
                            message = "Your email is not verified yet. Please check your inbox and verify your email before logging in."
                        )
                        return@withContext
                    }
                    val profile = UserProfile(
                        uid = user.uid,
                        fullName = user.displayName ?: "Vault Gamer",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString(),
                        isEmailVerified = user.isEmailVerified
                    )
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                }
                return@withContext
            }
        } else {
            // Check if user had an offline demo profile saved with remember me
            val offlineEmail = prefs.getString(KEY_OFFLINE_USER_EMAIL, null)
            if (offlineEmail != null) {
                val isVerified = prefs.getBoolean("pref_offline_verified_${offlineEmail}", true)
                if (!isVerified) {
                    _currentUser.value = null
                    _unverifiedEmail.value = offlineEmail
                    _authState.value = AuthState.EmailNotVerified(
                        email = offlineEmail,
                        message = "Your email is not verified yet. Please check your inbox and verify your email before logging in."
                    )
                    return@withContext
                }
                val offlineName = prefs.getString(KEY_OFFLINE_USER_NAME, "Vault Gamer") ?: "Vault Gamer"
                val offlineTag = prefs.getString(KEY_OFFLINE_USER_TAG, "VaultKeeper")
                val offlineProfile = UserProfile(
                    uid = "offline_local_user",
                    fullName = offlineName,
                    email = offlineEmail,
                    gamerTag = offlineTag,
                    isEmailVerified = true
                )
                _currentUser.value = offlineProfile
                _authState.value = AuthState.Authenticated(offlineProfile)
                return@withContext
            }
        }

        _authState.value = AuthState.Unauthenticated()
        _currentUser.value = null
    }

    suspend fun signIn(email: String, pass: String, rememberMe: Boolean): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        // Validation
        val validationError = validateCredentials(cleanEmail, cleanPass)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return@withContext Result.failure(IllegalArgumentException(validationError))
        }

        setRememberMeEnabled(rememberMe)

        val auth = firebaseAuth
        if (auth == null) {
            // Offline fallback when google-services.json is not configured
            val isVerified = prefs.getBoolean("pref_offline_verified_${cleanEmail}", true)
            if (!isVerified) {
                _currentUser.value = null
                _unverifiedEmail.value = cleanEmail
                _savedPasswordForResend.value = cleanPass
                val msg = "Your email address is not verified yet. Please check your inbox and verify your email before logging in."
                _authState.value = AuthState.EmailNotVerified(cleanEmail, msg)
                return@withContext Result.failure(EmailNotVerifiedException(cleanEmail, msg))
            }

            val profile = UserProfile(
                uid = "offline_user_${cleanEmail.hashCode()}",
                fullName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = cleanEmail,
                gamerTag = "Player1",
                isEmailVerified = true,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_OFFLINE_USER_EMAIL, cleanEmail)
                .putString(KEY_OFFLINE_USER_NAME, profile.fullName)
                .putString(KEY_OFFLINE_USER_TAG, profile.gamerTag)
                .apply()

            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            return@withContext Result.success(profile)
        }

        try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val user = result.user ?: throw IllegalStateException("Authentication succeeded but user is null")

            // Reload user from Firebase servers to get fresh email verification status
            user.reload().awaitTask()

            if (!user.isEmailVerified) {
                // Email is NOT verified: sign out, prevent navigation to home screen, show clear requirement message
                Log.w(TAG, "Sign in blocked: email $cleanEmail is not verified.")
                auth.signOut()
                _currentUser.value = null
                _unverifiedEmail.value = cleanEmail
                _savedPasswordForResend.value = cleanPass
                val msg = "Your email address is not verified yet. Please check your inbox and verify your email before logging in."
                _authState.value = AuthState.EmailNotVerified(cleanEmail, msg)
                return@withContext Result.failure(EmailNotVerifiedException(cleanEmail, msg))
            }

            // User is verified!
            val profile = loadOrCreateProfile(user)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            _unverifiedEmail.value = null
            _savedPasswordForResend.value = null
            firestoreRepository.updateLastLogin(user.uid)
            Result.success(profile)
        } catch (e: EmailNotVerifiedException) {
            Result.failure(e)
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            _authState.value = AuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        pass: String,
        confirmPass: String,
        gamerTag: String?,
        rememberMe: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanConfirm = confirmPass.trim()
        val cleanTag = gamerTag?.trim()

        if (cleanName.length < 2) {
            val msg = "Please enter your full name (at least 2 characters)."
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        val credError = validateCredentials(cleanEmail, cleanPass)
        if (credError != null) {
            _authState.value = AuthState.Error(credError)
            return@withContext Result.failure(IllegalArgumentException(credError))
        }

        if (cleanPass != cleanConfirm) {
            val msg = "Passwords do not match. Please re-enter your password."
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        setRememberMeEnabled(rememberMe)

        val auth = firebaseAuth
        if (auth == null) {
            // Offline fallback
            val profile = UserProfile(
                uid = "offline_user_${cleanEmail.hashCode()}",
                fullName = cleanName,
                email = cleanEmail,
                gamerTag = if (!cleanTag.isNullOrBlank()) cleanTag else "Player1",
                isEmailVerified = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_OFFLINE_USER_EMAIL, cleanEmail)
                .putString(KEY_OFFLINE_USER_NAME, cleanName)
                .putString(KEY_OFFLINE_USER_TAG, profile.gamerTag)
                .putBoolean("pref_offline_verified_${cleanEmail}", false)
                .apply()

            _currentUser.value = null
            _unverifiedEmail.value = cleanEmail
            _savedPasswordForResend.value = cleanPass
            val successMsg = "Account created! We've sent a verification link to $cleanEmail. Please check your inbox and verify your email before signing in."
            _authState.value = AuthState.RegistrationSuccess(cleanEmail, successMsg)
            return@withContext Result.success(profile)
        }

        try {
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val user = result.user ?: throw IllegalStateException("Account created but user is null")

            // Update display name in Firebase Auth
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).awaitTask()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update Firebase Auth profile: ${e.message}")
            }

            // Automatically send verification email using sendEmailVerification()
            var emailSent = false
            try {
                user.sendEmailVerification().awaitTask()
                emailSent = true
                Log.i(TAG, "sendEmailVerification() sent to $cleanEmail")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send initial verification email: ${e.message}")
            }

            // Create initial Firestore user document with isEmailVerified = false
            val initialProfile = UserProfile(
                uid = user.uid,
                fullName = cleanName,
                email = cleanEmail,
                gamerTag = cleanTag,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                isEmailVerified = false
            )
            try {
                firestoreRepository.saveUserProfile(initialProfile)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save initial Firestore profile: ${e.message}")
            }

            // Do not allow unverified users to access the main app: sign out immediately
            auth.signOut()
            _currentUser.value = null
            _unverifiedEmail.value = cleanEmail
            _savedPasswordForResend.value = cleanPass

            val successMsg = if (emailSent) {
                "Account created successfully! We've sent a verification email to $cleanEmail. Please check your inbox and verify your email before signing in."
            } else {
                "Account created! Please check your inbox and verify your email before signing in."
            }

            _authState.value = AuthState.RegistrationSuccess(cleanEmail, successMsg)
            Result.success(initialProfile)
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            _authState.value = AuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid email address to reset password."))
        }

        val auth = firebaseAuth
        if (auth == null) {
            return@withContext Result.success(Unit)
        }

        try {
            auth.sendPasswordResetEmail(cleanEmail).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    /**
     * Resends email verification to the user's email address.
     * Works whether the user is currently signed in or signed out.
     */
    suspend fun resendVerificationEmail(email: String? = null, pass: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val targetEmail = email?.trim()?.ifEmpty { null } ?: _unverifiedEmail.value
        val targetPass = pass?.trim()?.ifEmpty { null } ?: _savedPasswordForResend.value

        _authState.value = AuthState.Loading

        val auth = firebaseAuth
        if (auth == null) {
            val emailAddress = targetEmail ?: "demo@gamevault.io"
            // For offline testing, mark as verified so demo user can log in
            prefs.edit().putBoolean("pref_offline_verified_${emailAddress}", true).apply()
            val msg = "Verification email sent to $emailAddress! In offline mode, your account has been verified. You can now sign in."
            _authState.value = AuthState.VerificationEmailSent(emailAddress, msg)
            return@withContext Result.success(Unit)
        }

        if (targetEmail.isNullOrBlank()) {
            val msg = "Please provide your email address to resend the verification email."
            _authState.value = AuthState.Error(msg)
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        try {
            var user = auth.currentUser
            // If signed out, re-authenticate in background to get user handle
            if (user == null && !targetPass.isNullOrBlank()) {
                val signInResult = auth.signInWithEmailAndPassword(targetEmail, targetPass).awaitTask()
                user = signInResult.user
            }

            if (user != null) {
                user.sendEmailVerification().awaitTask()
                // Ensure unverified user remains signed out
                if (!user.isEmailVerified) {
                    auth.signOut()
                    _currentUser.value = null
                }
                _unverifiedEmail.value = targetEmail
                val msg = "Verification email sent to $targetEmail! Please check your inbox and spam folder."
                _authState.value = AuthState.VerificationEmailSent(targetEmail, msg)
                Result.success(Unit)
            } else {
                val msg = "Please enter your password above and click 'Resend Verification Email'."
                _authState.value = AuthState.Error(msg)
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            _authState.value = AuthState.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> = resendVerificationEmail()

    suspend fun reloadUserVerification(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        if (auth == null) {
            val curr = _currentUser.value
            return@withContext Result.success(curr)
        }

        val user = auth.currentUser ?: return@withContext Result.success(null)
        try {
            user.reload().awaitTask()
            val updated = loadOrCreateProfile(user)
            _currentUser.value = updated
            _authState.value = AuthState.Authenticated(updated)
            Result.success(updated)
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun updateProfile(fullName: String, gamerTag: String?, photoUrl: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        val curr = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not signed in"))

        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Name must be at least 2 characters"))
        }

        val updated = curr.copy(
            fullName = cleanName,
            gamerTag = gamerTag?.trim(),
            photoUrl = photoUrl?.trim()
        )

        val auth = firebaseAuth
        if (auth != null) {
            auth.currentUser?.let { fbUser ->
                try {
                    val updateReq = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .apply {
                            if (!photoUrl.isNullOrBlank()) {
                                setPhotoUri(android.net.Uri.parse(photoUrl.trim()))
                            }
                        }
                        .build()
                    fbUser.updateProfile(updateReq).awaitTask()
                } catch (e: Exception) {
                    Log.w(TAG, "Update Auth profile failed: ${e.message}")
                }
            }
            firestoreRepository.saveUserProfile(updated)
        } else {
            prefs.edit()
                .putString(KEY_OFFLINE_USER_NAME, cleanName)
                .putString(KEY_OFFLINE_USER_TAG, updated.gamerTag)
                .apply()
        }

        _currentUser.value = updated
        _authState.value = AuthState.Authenticated(updated)
        Result.success(updated)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Error signing out from FirebaseAuth: ${e.message}")
            }
        }
        prefs.edit()
            .remove(KEY_OFFLINE_USER_EMAIL)
            .remove(KEY_OFFLINE_USER_NAME)
            .remove(KEY_OFFLINE_USER_TAG)
            .apply()

        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated()
    }

    private suspend fun loadOrCreateProfile(user: FirebaseUser): UserProfile {
        val firestoreProfile = firestoreRepository.getUserProfile(
            uid = user.uid,
            email = user.email.orEmpty(),
            isEmailVerified = user.isEmailVerified
        ).getOrNull()

        if (firestoreProfile != null) {
            return firestoreProfile.copy(isEmailVerified = user.isEmailVerified)
        }

        // Profile doesn't exist yet in Firestore, create it
        val newProfile = UserProfile(
            uid = user.uid,
            fullName = user.displayName ?: user.email?.substringBefore("@").orEmpty().replaceFirstChar { it.uppercase() },
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis(),
            isEmailVerified = user.isEmailVerified
        )
        firestoreRepository.saveUserProfile(newProfile)
        return newProfile
    }

    private fun validateCredentials(email: String, pass: String): String? {
        if (email.isEmpty()) return "Email address cannot be empty."
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email address (e.g. name@example.com)."
        if (pass.isEmpty()) return "Password cannot be empty."
        if (pass.length < 6) return "Password must be at least 6 characters long."
        return null
    }

    private fun mapFirebaseAuthException(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> {
                "No account found with this email address. Please check your spelling or sign up."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Incorrect password or invalid email format. Please check your credentials."
            }
            is FirebaseAuthUserCollisionException -> {
                "An account already exists with this email address. Please sign in instead."
            }
            is FirebaseAuthWeakPasswordException -> {
                "Your password is too weak. Please use at least 6 characters including letters and numbers."
            }
            is FirebaseNetworkException -> {
                "Network connection failed. Please check your internet connection and try again."
            }
            is FirebaseTooManyRequestsException -> {
                "Too many unsuccessful attempts. For your security, please try again in a few minutes."
            }
            else -> {
                e.localizedMessage ?: "An unexpected error occurred during authentication. Please try again."
            }
        }
    }
}
