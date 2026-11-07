package com.mathroda.messengerclone.data

import android.content.Context
import android.content.SharedPreferences
import io.getstream.chat.android.client.models.User

/**
 * Stores the user's own Stream Chat credentials in encrypted storage.
 *
 * If encrypted storage is unavailable on a device, the credentials are kept in memory for the
 * current process only and the user simply logs in again next time.
 */
class UserCredentialsRepository(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences? by lazy {
        // The original project kept these values in plain preferences. Remove that file.
        appContext.deleteSharedPreferences(LEGACY_PREFS_USER_CREDENTIALS)
        SecurePrefs.open(appContext, PREFS_USER_CREDENTIALS)
    }

    @Volatile
    private var inMemory: UserCredentials? = null

    fun loadUserCredentials(): UserCredentials? {
        val p = prefs ?: return inMemory
        val apiKey = p.getString(KEY_API_KEY, null) ?: return null
        val userId = p.getString(KEY_USER_ID, null) ?: return null
        val token = p.getString(KEY_USER_TOKEN, null) ?: return null
        val userName = p.getString(KEY_USER_NAME, null).orEmpty().ifEmpty { userId }
        val userImage = p.getString(KEY_USER_IMAGE, null).orEmpty()

        return UserCredentials(
            apiKey = apiKey,
            user = User().apply {
                id = userId
                name = userName
                if (userImage.isNotEmpty()) {
                    image = userImage
                }
            },
            token = token
        )
    }

    fun loadApiKey(): String? {
        val p = prefs ?: return inMemory?.apiKey
        return p.getString(KEY_API_KEY, null)
    }

    fun saveUserCredentials(userCredentials: UserCredentials) {
        inMemory = userCredentials
        prefs?.edit()
            ?.putString(KEY_API_KEY, userCredentials.apiKey)
            ?.putString(KEY_USER_ID, userCredentials.user.id)
            ?.putString(KEY_USER_NAME, userCredentials.user.name)
            ?.putString(KEY_USER_IMAGE, userCredentials.user.image)
            ?.putString(KEY_USER_TOKEN, userCredentials.token)
            ?.apply()
    }

    fun clearCredentials() {
        inMemory = null
        prefs?.edit()
            ?.clear()
            ?.apply()
    }

    companion object {
        const val PREFS_USER_CREDENTIALS = "pml_secure_credentials"
        private const val LEGACY_PREFS_USER_CREDENTIALS = "user_credentials"

        private const val KEY_API_KEY = "api_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_IMAGE = "user_image"
        private const val KEY_USER_TOKEN = "user_token"
    }
}
