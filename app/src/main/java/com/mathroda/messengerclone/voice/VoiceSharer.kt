package com.mathroda.messengerclone.voice

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File

/**
 * Sends a recording to another app through Android's normal share mechanism.
 *
 * This is a file share, not a native Messenger voice note. The user still finishes the send in the
 * receiving app. Messenger exposes no public API for posting voice notes.
 */
object VoiceSharer {

    enum class Result { MESSENGER, CHOOSER, FAILED }

    private val MESSENGER_PACKAGES = listOf("com.facebook.orca", "com.facebook.mlite")
    private const val MIME_TYPE = "audio/mp4"

    /** Package name of an installed Messenger app, or null if none is installed. */
    fun installedMessenger(context: Context): String? =
        MESSENGER_PACKAGES.firstOrNull { isInstalled(context, it) }

    private fun isInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /** A content:// address for a clip, readable only through this app's own FileProvider. */
    @Throws(IllegalArgumentException::class)
    fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.voicefiles", file)

    fun share(context: Context, file: File, preferMessenger: Boolean): Result {
        val uri = try {
            uriFor(context, file)
        } catch (e: IllegalArgumentException) {
            return Result.FAILED
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Voice message", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val messenger = if (preferMessenger) installedMessenger(context) else null
        if (messenger != null) {
            try {
                context.startActivity(Intent(send).setPackage(messenger))
                return Result.MESSENGER
            } catch (e: ActivityNotFoundException) {
                // This Messenger version does not accept audio files; fall back to the chooser.
            } catch (e: SecurityException) {
                // Same fallback.
            }
        }

        return try {
            context.startActivity(Intent.createChooser(send, "Send voice message"))
            Result.CHOOSER
        } catch (e: ActivityNotFoundException) {
            Result.FAILED
        }
    }
}
