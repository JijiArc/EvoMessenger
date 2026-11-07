package com.mathroda.messengerclone

import android.app.Application
import com.getstream.sdk.chat.utils.DateFormatter
import com.mathroda.messengerclone.data.UserCredentialsRepository
import com.mathroda.messengerclone.utils.MessengerHelper
import io.getstream.chat.android.client.utils.internal.toggle.ToggleService
import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.chat.android.state.BuildConfig

class MessengerApp: Application() {

    override fun onCreate() {
        super.onCreate()
        credentialsRepository = UserCredentialsRepository(this)
        dateFormatter = DateFormatter.from(this)

        initializeToggleService()

        // No API key is bundled with the app. The chat client is only built once the user has
        // signed in with their own Stream project key (see UserLoginActivity).
        credentialsRepository.loadApiKey()?.let { savedKey ->
            MessengerHelper.initializeSdk(this, savedKey)
        }
    }

    @OptIn(InternalStreamChatApi::class)
    private fun initializeToggleService() {
        ToggleService.init(applicationContext, mapOf(ToggleService.TOGGLE_KEY_SOCKET_REFACTOR to  BuildConfig.DEBUG))
    }
    companion object {
        lateinit var credentialsRepository: UserCredentialsRepository
           private set

        lateinit var dateFormatter: DateFormatter
           private set
    }
}
