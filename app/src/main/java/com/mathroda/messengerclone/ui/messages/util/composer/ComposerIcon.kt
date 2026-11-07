package com.mathroda.messengerclone.ui.messages.util

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mathroda.messengerclone.R
import com.mathroda.messengerclone.ui.theme.BottomSelected

@Composable
fun ComposerIcon(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    painter: Painter,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    IconButton(
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .padding(8.dp),
        content = {
            Icon(
                painter = painter,
                contentDescription = contentDescription
                    ?: stringResource(id = R.string.stream_compose_attachments),
                tint = BottomSelected,
            )
        },
        onClick = onClick
    )
}