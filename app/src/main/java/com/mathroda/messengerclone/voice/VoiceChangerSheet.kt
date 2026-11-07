package com.mathroda.messengerclone.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mathroda.messengerclone.ui.theme.BottomSelected
import com.mathroda.messengerclone.ui.theme.BubbleGray
import com.mathroda.messengerclone.ui.theme.resultDanger
import kotlin.math.roundToInt

/**
 * Compact bottom sheet shown when the microphone icon in the composer is tapped.
 * Flow: choose a voice, record, preview, send.
 *
 * @param onRecordClick starts recording; the host checks the microphone permission first.
 * @param onSend hands the clip to Messenger (or the share sheet when Messenger is not installed).
 * @param onShareOther always opens the share sheet so another app can be picked.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VoiceChangerSheet(
    viewModel: VoiceChangerViewModel,
    messengerInstalled: Boolean,
    onRecordClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSend: () -> Unit,
    onShareOther: () -> Unit,
    onDismiss: () -> Unit
) {
    val phase = viewModel.phase
    val busy = phase == VoiceChangerViewModel.Phase.RECORDING ||
        phase == VoiceChangerViewModel.Phase.PROCESSING

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice message",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface
                        )
                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close voice message",
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    // Voice choices
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = viewModel.effect.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    // Strength
                    if (viewModel.effect != VoiceEffect.NORMAL) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Effect strength: ${(viewModel.strength * 100).roundToInt()}%",
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                            Slider(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                            Text(
                                text = "Lower strength keeps your voice easier to understand.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    StatusArea(viewModel)

                    if (viewModel.needsSettings) {
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            onClick = onOpenSettings
                        ) {
                            Text("Open app settings", color = BottomSelected)
                        }
                    }

                    Controls(
                        viewModel = viewModel,
                        onRecordClick = onRecordClick
                    )

                    if (phase == VoiceChangerViewModel.Phase.READY) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            onClick = onSend,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = BottomSelected,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (messengerInstalled) "Send via Messenger" else "Share voice message")
                        }
                        if (messengerInstalled) {
                            TextButton(
                                modifier = Modifier.heightIn(min = 48.dp),
                                onClick = onShareOther
                            ) {
                                Text("Share with another app", color = BottomSelected)
                            }
                        }
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "This sends an audio file. In Messenger you pick the " +
                                "conversation and tap send. It will not appear as a " +
                                "native Messenger voice note.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(Modifier.size(4.dp))
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
private fun StatusArea(viewModel: VoiceChangerViewModel) {
    val phase = viewModel.phase
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (phase) {
            VoiceChangerViewModel.Phase.IDLE -> {
                if (viewModel.message == null) {
                    StatusText("Pick a voice, then tap the microphone to record.")
                }
            }
            VoiceChangerViewModel.Phase.RECORDING -> {
                Text(
                    text = formatTime(viewModel.seconds),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                StatusText("Recording… tap stop when you are done.")
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 6.dp),
                    progress = (viewModel.level * 2f).coerceIn(0f, 1f),
                    color = BottomSelected,
                    backgroundColor = BubbleGray
                )
            }
            VoiceChangerViewModel.Phase.PROCESSING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = BottomSelected,
                    strokeWidth = 3.dp
                )
                StatusText("Applying the voice…")
            }
            VoiceChangerViewModel.Phase.READY -> {
                Text(
                    text = formatTime(viewModel.seconds),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                StatusText("Ready. Preview it, try another voice, or send it.")
            }
            VoiceChangerViewModel.Phase.ERROR -> Unit
        }

        viewModel.message?.let { text ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = if (phase == VoiceChangerViewModel.Phase.ERROR || viewModel.needsSettings) {
                    resultDanger()
                } else {
                    MaterialTheme.colors.onSurface
                }
            )
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    )
}

@Composable
private fun Controls(
    viewModel: VoiceChangerViewModel,
    onRecordClick: () -> Unit
) {
    when (viewModel.phase) {
        VoiceChangerViewModel.Phase.IDLE -> {
            RoundButton(
                icon = Icons.Filled.Mic,
                description = "Start recording",
                onClick = onRecordClick
            )
        }
        VoiceChangerViewModel.Phase.ERROR -> {
            RoundButton(
                icon = Icons.Filled.Mic,
                description = "Try recording again",
                onClick = {
                    viewModel.dismissError()
                    onRecordClick()
                }
            )
        }
        VoiceChangerViewModel.Phase.RECORDING -> {
            RoundButton(
                icon = Icons.Filled.Stop,
                description = "Stop recording",
                onClick = { viewModel.stopRecording() }
            )
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { viewModel.cancelRecording() }
            ) {
                Text("Cancel", color = resultDanger())
            }
        }
        VoiceChangerViewModel.Phase.PROCESSING -> Unit
        VoiceChangerViewModel.Phase.READY -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { viewModel.togglePlayback() }
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = BottomSelected
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.isPlaying) "Stop preview" else "Play preview",
                        color = BottomSelected
                    )
                }
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = onRecordClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = BottomSelected
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Record again", color = BottomSelected)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(BottomSelected)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(36.dp),
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
