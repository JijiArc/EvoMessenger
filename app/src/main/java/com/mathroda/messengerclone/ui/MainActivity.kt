package com.mathroda.messengerclone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mathroda.messengerclone.R
import com.mathroda.messengerclone.scanner.ScannerActivity
import com.mathroda.messengerclone.ui.theme.PurpleTheme
import com.mathroda.messengerclone.voice.VoiceChangerViewModel
import com.mathroda.messengerclone.voice.VoiceSharer
import com.mathroda.messengerclone.web.MessengerWebViewModel

/**
 * The only screen. It hosts Facebook's own Messages website in a WebView (see [MessengerScreen]),
 * plus the app's voice changer and link scanner.
 */
class MainActivity : ComponentActivity() {

    private val webViewModel by viewModels<MessengerWebViewModel>()
    private val voiceViewModel by viewModels<VoiceChangerViewModel>()

    // Must be registered before the activity starts, so it lives here and not in a composable.
    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                voiceViewModel.startRecording()
            } else {
                val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.RECORD_AUDIO
                )
                voiceViewModel.onPermissionDenied(permanent = !canAskAgain)
            }
        }

    /** Asks for the microphone only now, when the user actually taps record in the voice changer. */
    private fun startVoiceRecording() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            voiceViewModel.startRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        } catch (e: Exception) {
            voiceViewModel.showMessage("Open Settings > Apps > this app > Permissions and allow the microphone.")
        }
    }

    /** Returns true if a share screen or Messenger was opened. */
    private fun shareVoice(preferMessenger: Boolean): Boolean {
        val file = voiceViewModel.output ?: return false
        val result = VoiceSharer.share(this, file, preferMessenger)
        if (result == VoiceSharer.Result.FAILED) {
            voiceViewModel.showMessage("No app on this phone can receive the audio file.")
            return false
        }
        return true
    }

    /** Opens the official Messenger app, for when the web page cannot offer messaging. */
    private fun openMessengerApp() {
        val pkg = VoiceSharer.installedMessenger(this)
        val intent = pkg?.let { packageManager.getLaunchIntentForPackage(it) }
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Android 10 silences background recording, so stop cleanly and keep what was captured.
        voiceViewModel.onAppBackgrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PurpleTheme {
                MessengerScreen(
                    webViewModel = webViewModel,
                    voiceViewModel = voiceViewModel,
                    messengerAppInstalled = VoiceSharer.installedMessenger(this) != null,
                    onRecordClick = ::startVoiceRecording,
                    onOpenSettings = ::openAppSettings,
                    onShareVoice = ::shareVoice,
                    onOpenScanner = { startActivity(ScannerActivity.createIntent(this)) },
                    onOpenMessengerApp = ::openMessengerApp
                )
            }
        }
    }
}
