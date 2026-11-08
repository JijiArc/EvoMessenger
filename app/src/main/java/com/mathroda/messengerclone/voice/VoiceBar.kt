package com.mathroda.messengerclone.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathroda.messengerclone.ui.theme.BottomSelected
import com.mathroda.messengerclone.ui.theme.BubbleGray
import com.mathroda.messengerclone.ui.theme.resultDanger
import kotlin.math.roundToInt

/**
 * The voice changer as a strip docked under the chat, in the same screen as Facebook's conversation,
 * instead of a separate pop-up page. Flow: pick a voice, record, preview, then "Attach to chat".
 *
 * "Attach to chat" does not type anything into Facebook's page. It arms the clip, and the next time the
 * user taps Facebook's own attach (paperclip) button, the app offers the clip in that file picker.
 * Facebook then sends it as an audio file attachment, not as a native voice note.
 *
 * @param armed true while a clip is waiting to be offered in Facebook's file picker.
 * @param onRecordClick starts recording; the host checks the microphone permission first.
 * @param onToggleAttach arms or disarms the clip.
 * @param onShare opens Messenger's or Android's share screen for people who prefer that.
 */
@Composable
fun VoiceBar(
    viewModel: VoiceChangerViewModel,
    armed: Boolean,
    onRecordClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAttach: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = viewModel.phase
    val busy = phase == VoiceChangerViewModel.Phase.RECORDING ||
        phase == VoiceChangerViewModel.Phase.PROCESSING

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Voices, with the close button at the end of the same row.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceEffect.values().forEach { option ->
                        EffectPill(
                            label = option.label,
                            selected = option == viewModel.effect,
                            enabled = !busy,
                            onClick = { viewModel.selectEffect(option) }
                        )
                    }
                }
                IconButton(modifier = Modifier.size(48.dp), onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close voice bar",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            }

            if (viewModel.effect != VoiceEffect.NORMAL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.width(112.dp),
                        text = "Strength ${(viewModel.strength * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.onSurface
                    )
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Effect strength" },
                        value = viewModel.strength,
                        onValueChange = { viewModel.updateStrength(it) },
                        onValueChangeFinished = { viewModel.strengthChangeFinished() },
                        valueRange = AudioEffects.MIN_STRENGTH..AudioEffects.MAX_STRENGTH,
                        enabled = !busy,
                        colors = SliderDefaults.colors(
                            thumbColor = BottomSelected,
                            activeTrackColor = BottomSelected
                        )
                    )
                }
            }

            Controls(
                viewModel = viewModel,
                armed = armed,
                onRecordClick = onRecordClick,
                onToggleAttach = onToggleAttach,
                onShare = onShare
            )

            viewModel.message?.let { text ->
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    text = text,
                    fontSize = 13.sp,
                    color = if (phase == VoiceChangerViewModel.Phase.ERROR || viewModel.needsSettings) {
                        resultDanger()
                    } else {
                        MaterialTheme.colors.onSurface
                    }
                )
            }

            if (viewModel.needsSettings) {
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = onOpenSettings
                ) {
                    Text("Open app settings", color = BottomSelected)
                }
            }

            if (armed && phase == VoiceChangerViewModel.Phase.READY) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    text = "Now tap Facebook's paperclip or attach button in the chat. " +
                        "Your clip will be offered there. It sends as an audio file, not a voice note.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun Controls(
    viewModel: VoiceChangerViewModel,
    armed: Boolean,
    onRecordClick: () -> Unit,
    onToggleAttach: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (viewModel.phase) {
            VoiceChangerViewModel.Phase.IDLE -> {
                RoundButton(Icons.Filled.Mic, "Start recording", onRecordClick)
                if (viewModel.message == null) {
                    HintText("Pick a voice, then tap the microphone to record.")
                }
            }
            VoiceChangerViewModel.Phase.ERROR -> {
                RoundButton(Icons.Filled.Mic, "Try recording again") {
                    viewModel.dismissError()
                    onRecordClick()
                }
            }
            VoiceChangerViewModel.Phase.RECORDING -> {
                RoundButton(Icons.Filled.Stop, "Stop recording") { viewModel.stopRecording() }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTime(viewModel.seconds),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = (viewModel.level * 2f).coerceIn(0f, 1f),
                        color = BottomSelected,
                        backgroundColor = BubbleGray
                    )
                }
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { viewModel.cancelRecording() }
                ) {
                    Text("Cancel", color = resultDanger())
                }
            }
            VoiceChangerViewModel.Phase.PROCESSING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = BottomSelected,
                    strokeWidth = 3.dp
                )
                HintText("Applying the voice…")
            }
            VoiceChangerViewModel.Phase.READY -> {
                IconButton(modifier = Modifier.size(48.dp), onClick = { viewModel.togglePlayback() }) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (viewModel.isPlaying) "Stop preview" else "Play preview",
                        tint = BottomSelected
                    )
                }
                Text(
                    text = formatTime(viewModel.seconds),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                IconButton(modifier = Modifier.size(48.dp), onClick = onRecordClick) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Record again",
                        tint = BottomSelected
                    )
                }
                Spacer(Modifier.weight(1f))
                if (armed) {
                    OutlinedButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = onToggleAttach
                    ) {
                        Text("Armed (tap to cancel)", color = BottomSelected, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = onToggleAttach,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = BottomSelected,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Attach to chat", fontSize = 13.sp)
                    }
                }
                IconButton(modifier = Modifier.size(48.dp), onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share with another app",
                        tint = BottomSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) BottomSelected else BubbleGray
    val textColor = if (selected) Color.White else Color(0xFF1C1B1F)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    )
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(BottomSelected)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%d:%02d".format(minutes, secs)
}
