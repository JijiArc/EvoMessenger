package com.mathroda.messengerclone.voice

import android.content.Context
import java.io.File

/** Temporary voice recordings live in cacheDir/voice and are removed when no longer needed. */
object VoiceFiles {

    private const val DIR_NAME = "voice"
    private const val MAX_AGE_MS = 60L * 60L * 1000L // one hour

    private fun dir(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun newOutputFile(context: Context): File =
        File(dir(context), "voice_${System.currentTimeMillis()}.m4a")

    fun delete(file: File?) {
        try {
            file?.delete()
        } catch (e: SecurityException) {
            // Nothing else to do; the system clears the cache folder if space runs low.
        }
    }

    /**
     * Removes recordings older than an hour. Recent files are kept because the app that received
     * a shared recording may still be reading it.
     */
    fun cleanupOld(context: Context) {
        val now = System.currentTimeMillis()
        dir(context).listFiles()?.forEach { file ->
            if (now - file.lastModified() > MAX_AGE_MS) delete(file)
        }
    }
}
