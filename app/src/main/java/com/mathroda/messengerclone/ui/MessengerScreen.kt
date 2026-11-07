package com.mathroda.messengerclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mathroda.messengerclone.R
import com.mathroda.messengerclone.ui.theme.BottomSelected
import com.mathroda.messengerclone.voice.VoiceChangerSheet
import com.mathroda.messengerclone.voice.VoiceChangerViewModel
import com.mathroda.messengerclone.web.ExternalLinks
import com.mathroda.messengerclone.web.MessengerWebView
import com.mathroda.messengerclone.web.MessengerWebViewModel
import com.mathroda.messengerclone.web.ViewMode
import com.mathroda.messengerclone.web.WebController

/**
 * Two viewing modes over the same embedded Facebook page:
 *  - SHELL: the app's purple top bar and bottom navigation around the page.
 *  - FULL_SCREEN: only the page; the menu sits behind a small handle on the left edge.
 *
 * Both modes keep the page in the same place in the composition, so switching modes does not
 * reload it or lose the conversation that is open.
 */
@Composable
fun MessengerScreen(
    webViewModel: MessengerWebViewModel,
    voiceViewModel: VoiceChangerViewModel,
    messengerAppInstalled: Boolean,
    onRecordClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareVoice: (preferMessenger: Boolean) -> Boolean,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { WebController() }
    val mode by webViewModel.viewMode.collectAsState()
    val desktop by webViewModel.desktopSite.collectAsState()

    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showVoice by rememberSaveable { mutableStateOf(false) }
    var showSignOut by rememberSaveable { mutableStateOf(false) }

    val isShell = mode == ViewMode.SHELL

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isShell) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.nav_messages)) },
                        actions = {
                            IconButton(onClick = { webViewModel.setDesktopSite(!desktop) }) {
                                Icon(
                                    imageVector = if (desktop) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows,
                                    contentDescription = stringResource(
                                        if (desktop) R.string.action_mobile_site else R.string.action_desktop_site
                                    )
                                )
                            }
                            IconButton(onClick = { controller.reload() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_reload))
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isShell) {
                    BottomNavigation(backgroundColor = MaterialTheme.colors.surface) {
                        BottomNavigationItem(
                            selected = true,
                            onClick = { controller.loadHome() },
                            icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_messages)) },
                            selectedContentColor = MaterialTheme.colors.primary,
                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        BottomNavigationItem(
                            selected = false,
                            onClick = { showVoice = true },
                            icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_voice)) },
                            selectedContentColor = MaterialTheme.colors.primary,
                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        BottomNavigationItem(
                            selected = false,
                            onClick = onOpenScanner,
                            icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_scanner)) },
                            selectedContentColor = MaterialTheme.colors.primary,
                            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        ) { innerPadding ->
            MessengerWebView(
                controller = controller,
                viewModel = webViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        if (!isShell) {
            MenuHandle(
                onClick = { showMenu = true },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }

    if (showMenu) {
        ActionsDialog(
            desktop = desktop,
            isShell = isShell,
            onDismiss = { showMenu = false },
            onReload = { showMenu = false; controller.reload() },
            onToggleDesktop = { showMenu = false; webViewModel.setDesktopSite(!desktop) },
            onToggleMode = {
                showMenu = false
                webViewModel.setViewMode(if (isShell) ViewMode.FULL_SCREEN else ViewMode.SHELL)
            },
            onOpenInChrome = { showMenu = false; ExternalLinks.openInCustomTab(context, controller.currentUrl()) },
            onVoice = { showMenu = false; showVoice = true },
            onScanner = { showMenu = false; onOpenScanner() },
            onSignOut = { showMenu = false; showSignOut = true }
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text(stringResource(R.string.sign_out_title)) },
            text = { Text(stringResource(R.string.sign_out_body)) },
            confirmButton = {
                TextButton(onClick = { showSignOut = false; controller.clearSession() }) {
                    Text(stringResource(R.string.sign_out_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showVoice) {
        VoiceChangerSheet(
            viewModel = voiceViewModel,
            messengerInstalled = messengerAppInstalled,
            onRecordClick = onRecordClick,
            onOpenSettings = onOpenSettings,
            onSend = {
                if (onShareVoice(true)) {
                    voiceViewModel.finishAfterShare()
                    showVoice = false
                }
            },
            onShareOther = {
                if (onShareVoice(false)) {
                    voiceViewModel.finishAfterShare()
                    showVoice = false
                }
            },
            onDismiss = {
                voiceViewModel.resetAll()
                showVoice = false
            }
        )
    }
}

@Composable
private fun ActionsDialog(
    desktop: Boolean,
    isShell: Boolean,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenInChrome: () -> Unit,
    onVoice: () -> Unit,
    onScanner: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ActionRow(Icons.Filled.Refresh, stringResource(R.string.action_reload), onReload)
                ActionRow(
                    if (desktop) Icons.Filled.PhoneAndroid else Icons.Filled.DesktopWindows,
                    stringResource(if (desktop) R.string.action_mobile_site else R.string.action_desktop_site),
                    onToggleDesktop
                )
                ActionRow(
                    if (isShell) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                    stringResource(if (isShell) R.string.action_full_screen else R.string.action_app_shell),
                    onToggleMode
                )
                ActionRow(Icons.Filled.OpenInBrowser, stringResource(R.string.action_open_in_chrome), onOpenInChrome)
                ActionRow(Icons.Filled.Mic, stringResource(R.string.action_voice_changer), onVoice)
                ActionRow(Icons.Filled.Security, stringResource(R.string.action_link_scanner), onScanner)
                ActionRow(Icons.Filled.ExitToApp, stringResource(R.string.action_sign_out), onSignOut)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colors.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.body1)
    }
}

/** Small grab handle on the left edge that opens the menu while the page is full screen. */
@Composable
private fun MenuHandle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.menu_handle_description)
    Box(
        modifier = modifier
            .size(width = 24.dp, height = 72.dp)
            .clickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 40.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(BottomSelected.copy(alpha = 0.7f))
        )
    }
}
