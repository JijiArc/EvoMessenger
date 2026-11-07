package com.mathroda.messengerclone.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.mathroda.messengerclone.common.MessengerCloneCommonHeader
import com.mathroda.messengerclone.data.UserCredentials
import com.mathroda.messengerclone.ui.channels.ChannelsActivity
import com.mathroda.messengerclone.ui.theme.BottomSelected
import com.mathroda.messengerclone.ui.theme.resultDanger
import com.mathroda.messengerclone.utils.MessengerHelper
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.compose.ui.theme.ChatTheme

private val API_KEY_REGEX = Regex("^[A-Za-z0-9]{6,64}$")
private val USER_ID_REGEX = Regex("^[A-Za-z0-9@_-]{1,64}$")

/**
 * Keeps the sign-in attempt alive across rotation so a slow connection cannot leave the screen stuck.
 */
class LoginViewModel : ViewModel() {
    var connecting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var connected by mutableStateOf(false)
        private set

    fun connect(
        context: Context,
        apiKey: String,
        userId: String,
        displayName: String,
        token: String
    ) {
        if (connecting) return
        error = null
        connecting = true
        try {
            MessengerHelper.ensureSdk(context, apiKey)

            // A blank token uses a development token. That only works when authentication checks
            // are disabled for the Stream app; otherwise paste a token your own server generated.
            val resolvedToken = token.ifBlank { ChatClient.instance().devToken(userId) }

            val user = User().apply {
                id = userId
                name = displayName.ifBlank { userId }
            }

            MessengerHelper.connectUser(
                UserCredentials(apiKey = apiKey, user = user, token = resolvedToken),
                onSuccess = {
                    connecting = false
                    connected = true
                },
                onError = { chatError ->
                    connecting = false
                    val hint = if (token.isBlank()) {
                        " You left the token empty, so the app used a development token. Turn off " +
                            "authentication checks for your Stream app, or paste a token from your server."
                    } else {
                        ""
                    }
                    error = (chatError.message ?: "Could not connect.") + hint
                }
            )
        } catch (e: Exception) {
            connecting = false
            error = "Could not start the chat client: ${e.message ?: "unknown error"}"
        }
    }

    fun clearError() {
        error = null
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
class UserLoginActivity : ComponentActivity() {

    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                UserLoginScreen(
                    viewModel = loginViewModel,
                    onBackPressed = { finish() },
                    onConnected = ::openChannels
                )
            }
        }
    }

    @Composable
    private fun UserLoginScreen(
        viewModel: LoginViewModel,
        onBackPressed: () -> Unit,
        onConnected: () -> Unit
    ) {
        var apiKey by rememberSaveable { mutableStateOf("") }
        var userId by rememberSaveable { mutableStateOf("") }
        var displayName by rememberSaveable { mutableStateOf("") }
        // The token is deliberately not saved in instance state.
        var token by remember { mutableStateOf("") }

        val apiKeyValid = API_KEY_REGEX.matches(apiKey.trim())
        val userIdValid = USER_ID_REGEX.matches(userId.trim())
        val canConnect = apiKeyValid && userIdValid && !viewModel.connecting

        LaunchedEffect(viewModel.connected) {
            if (viewModel.connected) onConnected()
        }

        val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = BottomSelected,
            focusedLabelColor = BottomSelected,
            cursorColor = BottomSelected
        )

        Scaffold(
            topBar = {
                MessengerCloneCommonHeader(
                    title = "Connect to your chat",
                    onBackPressed = onBackPressed
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Purple Messenger Lite uses its own chat service (Stream), not your " +
                        "Facebook inbox. Create a free Stream app, then enter its API key and a " +
                        "user ID below. Nothing is bundled with this app.",
                    fontSize = 14.sp,
                    color = ChatTheme.colors.textHighEmphasis
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = apiKey,
                    onValueChange = { apiKey = it; viewModel.clearError() },
                    label = { Text("Stream API key") },
                    singleLine = true,
                    isError = apiKey.isNotEmpty() && !apiKeyValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = userId,
                    onValueChange = { userId = it; viewModel.clearError() },
                    label = { Text("User ID (letters, numbers, @ _ -)") },
                    singleLine = true,
                    isError = userId.isNotEmpty() && !userIdValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = token,
                    onValueChange = { token = it; viewModel.clearError() },
                    label = { Text("User token (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors
                )

                Text(
                    text = "The token is stored encrypted on this device. Leave it empty only if " +
                        "you turned off authentication checks for your Stream app.",
                    fontSize = 12.sp,
                    color = ChatTheme.colors.textLowEmphasis
                )

                viewModel.error?.let { message ->
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = resultDanger()
                    )
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    enabled = canConnect,
                    onClick = {
                        viewModel.connect(
                            context = applicationContext,
                            apiKey = apiKey.trim(),
                            userId = userId.trim(),
                            displayName = displayName.trim(),
                            token = token.trim()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = BottomSelected,
                        contentColor = Color.White
                    )
                ) {
                    if (viewModel.connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Connecting…")
                    } else {
                        Text("Connect")
                    }
                }
            }
        }
    }

    private fun openChannels() {
        startActivity(ChannelsActivity.createIntent(this))
        finish()
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, UserLoginActivity::class.java)
        }
    }
}
