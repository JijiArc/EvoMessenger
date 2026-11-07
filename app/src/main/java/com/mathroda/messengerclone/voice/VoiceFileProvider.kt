package com.mathroda.messengerclone.voice

import androidx.core.content.FileProvider

/**
 * Hands out read-only links to recordings in cacheDir/voice (see res/xml/voice_file_paths.xml).
 * It is a separate subclass so it never clashes with a FileProvider declared by a library.
 */
class VoiceFileProvider : FileProvider()
