package com.mathroda.messengerclone.utils

import android.content.Context
import com.mathroda.messengerclone.MessengerApp
import com.mathroda.messengerclone.data.UserCredentials
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.offline.plugin.configuration.Config
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory

object MessengerHelper {

    /** The API key the Stream client was built with, or null if it has not been built yet. */
    @Volatile
    private var initializedApiKey: String? = null

    val isSdkInitialized: Boolean
        get() = initializedApiKey != null

    fun initializeSdk(context: Context, apiKey: String) {

        val offlinePlugin = StreamOfflinePluginFactory(Config(
            userPresence = true,
            persistenceEnabled = true
        ), context )

        ChatClient.Builder(apiKey, context)
            .withPlugin(offlinePlugin)
            .build()

        initializedApiKey = apiKey
    }

    /** Builds the Stream client if it does not exist yet or was built with a different API key. */
    fun ensureSdk(context: Context, apiKey: String) {
        if (initializedApiKey != apiKey) {
            initializeSdk(context.applicationContext, apiKey)
        }
    }

    fun connectUser(
        userCredentials: UserCredentials,
        onSuccess: () -> Unit = {},
        onError: (ChatError) -> Unit = {}
    ) {
        ChatClient.instance().run {
            if (getCurrentUser() == null) {
                connectUser(userCredentials.user, userCredentials.token)
                    .enqueue() { result ->
                        if (result.isSuccess) {
                            MessengerApp.credentialsRepository.saveUserCredentials(userCredentials)
                            onSuccess()
                        } else {
                            onError(result.error())
                        }
                    }
            } else {
                onSuccess()
            }

        }
    }

    fun disconnectUser() {
        MessengerApp.credentialsRepository.clearCredentials()
        if (isSdkInitialized) {
            ChatClient.instance().disconnect()
        }
    }

    const val attach_giphy: String = "giphy"
}
