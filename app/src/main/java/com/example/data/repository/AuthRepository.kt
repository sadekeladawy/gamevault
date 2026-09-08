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
}

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

    /**
     * Attempts automatic login on application launch.
     * Checks if a Firebase user session or persisted remember-me session exists.
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
                    // Refresh token / verification status
                    user.reload().awaitTask()
                    val profile = loadOrCreateProfile(user)
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                    firestoreRepository.updateLastLogin(user.uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Auto login refresh failed, using cached user: ${e.message}")
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
            val profile = loadOrCreateProfile(user)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            firestoreRepository.updateLastLogin(user.uid)
            Result.success(profile)
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
                .apply()

            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
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

            // Send initial verification email automatically
            try {
                user.sendEmailVerification().awaitTask()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send initial verification email: ${e.message}")
            }

            // Create initial Firestore user document
            val initialProfile = UserProfile(
                uid = user.uid,
                fullName = cleanName,
                email = cleanEmail,
                gamerTag = cleanTag,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                isEmailVerified = user.isEmailVerified
            )
            firestoreRepository.saveUserProfile(initialProfile)

            _currentUser.value = initialProfile
            _authState.value = AuthState.Authenticated(initialProfile)
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

    suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.success(Unit)
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No active user session found."))

        try {
            user.sendEmailVerification().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = mapFirebaseAuthException(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

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
