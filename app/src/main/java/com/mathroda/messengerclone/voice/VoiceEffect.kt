package com.mathroda.messengerclone.voice

/** The voice styles offered in the voice sheet. NORMAL leaves the recording untouched. */
enum class VoiceEffect(val label: String, val description: String) {
    NORMAL("Normal", "Your own voice"),
    MONSTER("Monster", "Deep and growly"),
    GHOST("Ghost", "Hollow, with an echo"),
    ROBOT("Robot", "Metallic buzz"),
    ALIEN("Alien", "Higher, with a warble"),
    DIGITAL_MONSTER("Digital Monster", "Deep and glitchy")
}
