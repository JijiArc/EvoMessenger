package com.mathroda.messengerclone.scanner

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathroda.messengerclone.ui.theme.PurpleTheme
import com.mathroda.messengerclone.ui.theme.resultDanger
import com.mathroda.messengerclone.ui.theme.resultOk
import com.mathroda.messengerclone.ui.theme.resultWarning

/**
 * Checks a link against VirusTotal using the user's own API key.
 *
 * The scanned link itself is never opened by this screen. The only pages it can open are
 * VirusTotal's own report, sign-up and website pages, and only when the user taps for them.
 */
class ScannerActivity : ComponentActivity() {

    private val viewModel by viewModels<ScannerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PurpleTheme {
                ScannerScreen()
            }
        }
    }

    @Composable
    private fun ScannerScreen() {
        var keyInput by remember { mutableStateOf("") } // not saved in instance state on purpose
        val state = viewModel.state

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Link scanner") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PrivacyNote()

                    when (viewModel.hasKey) {
                        true -> KeySaved()
                        false -> KeyForm(
                            keyInput = keyInput,
                            onKeyChange = { keyInput = it },
                            onSave = {
                                viewModel.saveKey(keyInput)
                                keyInput = ""
                            }
                        )
                        null -> Unit
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = viewModel.input,
                        onValueChange = viewModel::onInputChange,
                        label = { Text("Link to scan") },
                        singleLine = true,
                        isError = viewModel.inputError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { viewModel.scan() })
                    )
                    viewModel.inputError?.let {
                        Text(it, color = resultDanger(), fontSize = 14.sp)
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = state !is ScanState.Scanning,
                        onClick = { viewModel.scan() }
                    ) {
                        Text("Scan link")
                    }

                    when (state) {
                        is ScanState.Idle -> NotScannedYet()
                        is ScanState.Scanning -> Scanning(state)
                        is ScanState.Done -> Result(state)
                        is ScanState.Failed -> Failure(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun PrivacyNote() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.07f))
                .padding(12.dp)
        ) {
            Text(
                text = "Scanning sends the link to VirusTotal, a Google service, which may share " +
                    "submitted links with security companies. Do not scan links that contain " +
                    "private information, such as password-reset or login links. This app never " +
                    "opens the link for you.",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground
            )
        }
    }

    @Composable
    private fun KeySaved() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "VirusTotal key saved (stored encrypted on this phone).",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground
            )
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { viewModel.removeKey() }
            ) {
                Text("Remove key")
            }
        }
    }

    @Composable
    private fun KeyForm(
        keyInput: String,
        onKeyChange: (String) -> Unit,
        onSave: () -> Unit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "To scan links, add your own free VirusTotal API key. It is stored " +
                    "encrypted on this phone and is only sent to VirusTotal.",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = keyInput,
                onValueChange = onKeyChange,
                label = { Text("VirusTotal API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() })
            )
            viewModel.keyMessage?.let {
                Text(it, color = resultDanger(), fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = keyInput.isNotBlank(),
                    onClick = onSave
                ) {
                    Text("Save key")
                }
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { openWebPage(VirusTotalClient.API_KEY_PAGE) }
                ) {
                    Text("Get a free key")
                }
            }
        }
    }

    @Composable
    private fun NotScannedYet() {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Not scanned yet. A link that has not been scanned is not necessarily safe.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
    }

    @Composable
    private fun Scanning(state: ScanState.Scanning) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Text(
                text = state.step,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onBackground
            )
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { viewModel.cancel() }
            ) {
                Text("Cancel")
            }
        }
    }

    @Composable
    private fun Result(done: ScanState.Done) {
        val color = when (done.verdict) {
            Verdict.DANGEROUS -> resultDanger()
            Verdict.SUSPICIOUS -> resultWarning()
            Verdict.NO_DETECTIONS -> resultOk()
            Verdict.UNKNOWN -> MaterialTheme.colors.onBackground
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.07f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = ScanVerdicts.title(done.verdict),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = ScanVerdicts.explanation(done.verdict, done.stats),
                fontSize = 14.sp,
                color = MaterialTheme.colors.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Count("Malicious", done.stats.malicious)
                Count("Suspicious", done.stats.suspicious)
                Count("Harmless", done.stats.harmless)
                Count("Undetected", done.stats.undetected)
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                onClick = { openWebPage(done.reportUrl) }
            ) {
                Text("View full report on VirusTotal")
            }
        }
    }

    @Composable
    private fun Count(label: String, value: Int) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun Failure(failed: ScanState.Failed) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = failed.message,
                fontSize = 14.sp,
                color = resultDanger()
            )
            Text(
                text = "The link has not been checked, so do not treat it as safe.",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { if (failed.canResume) viewModel.resume() else viewModel.scan() }
                ) {
                    Text(if (failed.canResume) "Check again" else "Try again")
                }
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = { openVirusTotalWebsite() }
                ) {
                    Text("Use the website")
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }

    /** Fallback when the API cannot be used: copy the link and open VirusTotal's own scan page. */
    private fun openVirusTotalWebsite() {
        val text = viewModel.input.trim()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Link to scan", text))
            Toast.makeText(this, "Link copied. Paste it into the VirusTotal box.", Toast.LENGTH_LONG).show()
        }
        openWebPage(VirusTotalClient.WEBSITE_FALLBACK)
    }

    /** Only ever called with VirusTotal addresses, never with the link being scanned. */
    private fun openWebPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No web browser found on this phone.", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, ScannerActivity::class.java)
    }
}
