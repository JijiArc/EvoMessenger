package com.mathroda.messengerclone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Opens an encrypted SharedPreferences file (AES-256, keys held in the Android Keystore).
 *
 * Some devices lose Keystore keys after a restore or a system update, which makes the old file
 * unreadable. In that case the file is deleted once and recreated, which signs the user out
 * rather than crashing. If encryption is still unavailable, null is returned and callers keep
 * their data in memory only. Secrets are never written to plain storage and never logged.
 */
object SecurePrefs {

    fun open(context: Context, fileName: String): SharedPreferences? {
        repeat(2) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                return EncryptedSharedPreferences.create(
                    fileName,
                    masterKeyAlias,
                    context.applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // Unreadable or corrupted store: wipe it and try once more.
                context.applicationContext.deleteSharedPreferences(fileName)
            }
        }
        return null
    }
}
