package com.example.nexus.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.nexus.ui.model.User

class AuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PROFILE_PICTURE = "user_profile_picture"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val TAG = "AuthManager"
    }

    // ✅ Use regular SharedPreferences to avoid encryption issues
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ✅ StateFlow để theo dõi authentication state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // ✅ Safe initialization
        try {
            checkSavedToken()
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization", e)
            _isLoggedIn.value = false
            _currentUser.value = null
        }
    }

    // ✅ Safe token checking
    private fun checkSavedToken() {
        try {
            val accessToken = getAccessToken()
            val userId = getUserId()
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

            if (isLoggedIn && !accessToken.isNullOrBlank() && userId != null) {
                _isLoggedIn.value = true

                // Khôi phục user info từ SharedPreferences
                val username = prefs.getString(KEY_USERNAME, null)
                val email = prefs.getString(KEY_USER_EMAIL, null)
                val profilePicture = prefs.getString(KEY_USER_PROFILE_PICTURE, null)
                val bio = prefs.getString(KEY_USER_BIO, null)

                if (username != null) {
                    _currentUser.value = User(
                        id = userId,
                        username = username,
                        email = email ?: "",
                        profilePicture = profilePicture,
                        bio = bio
                    )
                }
            } else {
                _isLoggedIn.value = false
                _currentUser.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking saved token", e)
            _isLoggedIn.value = false
            _currentUser.value = null
        }
    }

    // ✅ Backward compatible - saveTokens
    fun saveTokens(accessToken: String, refreshToken: String, userId: Long) {
        try {
            prefs.edit {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                putLong(KEY_USER_ID, userId)
                putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000)) // 24h
                putBoolean(KEY_IS_LOGGED_IN, true)
            }
            _isLoggedIn.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tokens", e)
        }
    }

    // ✅ Enhanced save with user info
    fun saveAuthData(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        user: User? = null,
        expiryTimeMillis: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
    ) {
        try {
            prefs.edit {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                putLong(KEY_USER_ID, userId)
                putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
                putBoolean(KEY_IS_LOGGED_IN, true)

                // Lưu user info nếu có
                user?.let {
                    putString(KEY_USERNAME, it.username)
                    putString(KEY_USER_EMAIL, it.email)
                    putString(KEY_USER_PROFILE_PICTURE, it.profilePicture)
                    putString(KEY_USER_BIO, it.bio)
                }
            }

            _isLoggedIn.value = true
            _currentUser.value = user
        } catch (e: Exception) {
            Log.e(TAG, "Error saving auth data", e)
        }
    }

    fun getAccessToken(): String? {
        return try {
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)

            // Kiểm tra token expiry
            if (token != null && isTokenExpired()) {
                logoutSync()
                null
            } else {
                token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token", e)
            null
        }
    }

    fun getRefreshToken(): String? {
        return try {
            prefs.getString(KEY_REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refresh token", e)
            null
        }
    }

    fun getUserId(): Long? {
        return try {
            val userId = prefs.getLong(KEY_USER_ID, -1L)
            if (userId != -1L) userId else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID", e)
            null
        }
    }

    // ✅ Token expiry check
    private fun isTokenExpired(): Boolean {
        return try {
            val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
            System.currentTimeMillis() > expiryTime
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token expiry", e)
            true // Assume expired if error
        }
    }

    // ✅ Login status check
    fun isUserLoggedIn(): Boolean {
        return _isLoggedIn.value && !getAccessToken().isNullOrBlank()
    }

    // ✅ Safe refresh token
    suspend fun refreshToken(): Boolean {
        return try {
            val refreshToken = getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                logoutSync() // Use sync version to avoid recursion
                return false
            }

            val response = RetrofitClient.apiService.refreshToken(RefreshTokenRequest(refreshToken))

            if (response.access_token.isNotBlank()) {
                // Lưu token mới
                prefs.edit {
                    putString(KEY_ACCESS_TOKEN, response.access_token)
                    putString(KEY_REFRESH_TOKEN, response.refresh_token)
                    putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000))
                }
                true
            } else {
                logoutSync()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            logoutSync()
            false
        }
    }

    // ✅ Login

    // ✅ Async logout
    suspend fun logout() {
        try {
            val token = getAccessToken()
            if (!token.isNullOrBlank()) {
                try {
                    RetrofitClient.apiService.logout()

                } catch (e: Exception) {
                    Log.w(TAG, "Logout API call failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        } finally {
            logoutSync()
        }
    }

    // ✅ Sync logout - SAFE
    fun logoutSync() {
        try {
            clearTokens()
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync logout", e)
            // Force clear state even if SharedPreferences fails
            _isLoggedIn.value = false
            _currentUser.value = null
        }
    }

    // ✅ Safe clear tokens
    fun clearTokens() {
        try {
            prefs.edit { clear() }
            Log.d(TAG, "Tokens cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing tokens", e)
        } finally {
            _isLoggedIn.value = false
            _currentUser.value = null
        }
    }

    // ✅ Update user info
    fun updateUserInfo(user: User) {
        try {
            prefs.edit {
                putString(KEY_USERNAME, user.username)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_PROFILE_PICTURE, user.profilePicture)
                putString(KEY_USER_BIO, user.bio)
            }
            _currentUser.value = user
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user info", e)
        }
    }

    // ✅ Get cached user
    fun getCachedUser(): User? {
        return try {
            val userId = getUserId() ?: return null
            val username = prefs.getString(KEY_USERNAME, null) ?: return null

            User(
                id = userId,
                username = username,
                email = prefs.getString(KEY_USER_EMAIL, null) ?: "",
                profilePicture = prefs.getString(KEY_USER_PROFILE_PICTURE, null),
                bio = prefs.getString(KEY_USER_BIO, null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached user", e)
            null
        }
    }
}
data class RefreshTokenRequest(
    val refresh_token: String
)

