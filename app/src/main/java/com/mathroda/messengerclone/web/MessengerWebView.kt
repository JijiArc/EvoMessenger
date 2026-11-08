package com.mathroda.messengerclone.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mathroda.messengerclone.R
import com.mathroda.messengerclone.ui.theme.BottomSelected
import com.mathroda.messengerclone.voice.VoiceSharer
import java.io.File

/**
 * Shows Facebook's own Messages website inside the app.
 *
 * Everything Messenger does (conversations, history, sending, receiving, reactions, read receipts,
 * search, voice messages, media) is done by Facebook's page. This composable only hosts the page,
 * keeps its cookies in the normal WebView store, and reports load state and errors.
 *
 * Never added on purpose: any JavaScript bridge, any cookie or token reading, any script injected
 * into the page, and any form or credential handling. Login happens on Facebook's own page.
 */
@Composable
fun MessengerWebView(
    controller: WebController,
    viewModel: MessengerWebViewModel,
    modifier: Modifier = Modifier,
    onVoiceClipAttached: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.state.collectAsState()
    val desktop by viewModel.desktopSite.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val fileChooser = remember { FileChooserHolder() }
    val permissions = remember { PermissionHolder() }
    val customView = remember { CustomViewHolder() }
    var voicePromptClip by remember { mutableStateOf<File?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileChooser.deliver(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val grantedSomething = permissions.resolve { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (!grantedSomething) {
                Toast.makeText(context, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
            }
        }

    val host = remember {
        Host(
            context = context,
            activity = activity,
            controller = controller,
            viewModel = viewModel,
            fileChooser = fileChooser,
            permissions = permissions,
            customView = customView,
            showVoicePrompt = { clip -> voicePromptClip = clip },
            launchFileChooser = { intent -> fileLauncher.launch(intent) },
            launchPermissions = { perms -> permissionLauncher.launch(perms) }
        )
    }

    BackHandler(enabled = customView.showing || uiState.canGoBack) {
        if (customView.showing) customView.hide(activity) else controller.goBack()
    }

    voicePromptClip?.let { clip ->
        AlertDialog(
            onDismissRequest = {
                voicePromptClip = null
                fileChooser.deliver(null)
            },
            title = { Text(stringResource(R.string.voice_attach_title)) },
            text = { Text(stringResource(R.string.voice_attach_body)) },
            confirmButton = {
                TextButton(onClick = {
                    voicePromptClip = null
                    val uri = try {
                        VoiceSharer.uriFor(context, clip)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    if (uri != null && clip.exists()) {
                        fileChooser.deliver(arrayOf(uri))
                        controller.voiceClip = null
                        onVoiceClipAttached()
                    } else {
                        fileChooser.deliver(null)
                        Toast.makeText(context, R.string.voice_attach_failed, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.voice_attach_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    voicePromptClip = null
                    val intent = fileChooser.pickerIntent
                    if (intent != null) {
                        try {
                            fileLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            fileChooser.deliver(null)
                        }
                    } else {
                        fileChooser.deliver(null)
                    }
                }) { Text(stringResource(R.string.voice_attach_other)) }
            }
        )
    }

    Box(modifier = modifier) {
        key(controller.generation) {
            val webView = remember { createWebView(host, viewModel.desktopSite.value) }

            DisposableEffect(webView) {
                controller.webView = webView
                onDispose {
                    if (controller.webView === webView) controller.webView = null
                    customView.hide(activity)
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.destroy()
                }
            }

            DisposableEffect(lifecycleOwner, webView) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> webView.onResume()
                        Lifecycle.Event.ON_PAUSE -> {
                            webView.onPause()
                            // Writes the WebView's own cookie jar to disk so the login survives a restart.
                            CookieManager.getInstance().flush()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(desktop) { controller.setDesktopMode(desktop) }

            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        }

        if (uiState.loading) {
            if (uiState.progress <= 5) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = BottomSelected
                )
            } else {
                LinearProgressIndicator(
                    progress = uiState.progress / 100f,
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = BottomSelected
                )
            }
        }

        val error = uiState.error
        if (error != null) {
            val retry = {
                viewModel.dismissError()
                if (error.kind == WebError.Kind.RENDERER) controller.recreate() else controller.reload()
            }
            val openInBrowser = {
                ExternalLinks.openInCustomTab(context, error.url ?: controller.currentUrl())
            }
            when (error.kind) {
                WebError.Kind.NETWORK -> ErrorOverlay(
                    title = stringResource(R.string.error_network_title),
                    body = stringResource(R.string.error_network_body),
                    onRetry = retry,
                    onOpenInBrowser = openInBrowser
                )
                WebError.Kind.SSL -> ErrorOverlay(
                    title = stringResource(R.string.error_ssl_title),
                    body = stringResource(R.string.error_ssl_body),
                    onRetry = retry,
                    onOpenInBrowser = openInBrowser
                )
                WebError.Kind.RENDERER -> ErrorOverlay(
                    title = stringResource(R.string.error_renderer_title),
                    body = stringResource(R.string.error_renderer_body),
                    onRetry = retry,
                    onOpenInBrowser = openInBrowser
                )
                WebError.Kind.UNAVAILABLE -> ErrorBanner(
                    text = stringResource(R.string.error_unavailable_banner),
                    onRetry = retry,
                    onOpenInBrowser = openInBrowser,
                    onDismiss = { viewModel.dismissError() },
                    onSwitchSite = {
                        viewModel.dismissError()
                        viewModel.setDesktopSite(!desktop)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                WebError.Kind.HTTP -> ErrorBanner(
                    text = stringResource(R.string.error_http_banner, error.httpStatus),
                    onRetry = retry,
                    onOpenInBrowser = openInBrowser,
                    onDismiss = { viewModel.dismissError() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// WebView construction
// ---------------------------------------------------------------------------------------------

private class Host(
    val context: Context,
    val activity: Activity?,
    val controller: WebController,
    val viewModel: MessengerWebViewModel,
    val fileChooser: FileChooserHolder,
    val permissions: PermissionHolder,
    val customView: CustomViewHolder,
    val showVoicePrompt: (File) -> Unit,
    val launchFileChooser: (Intent) -> Unit,
    val launchPermissions: (Array<String>) -> Unit
)

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(host: Host, desktop: Boolean): WebView {
    val webView = WebView(host.context)
    host.controller.defaultUserAgent = webView.settings.userAgentString.orEmpty()

    webView.settings.apply {
        // Facebook's site does not work without JavaScript and DOM storage.
        javaScriptEnabled = true
        domStorageEnabled = true
        // Nothing in the app needs the page to read local files or content providers.
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        setGeolocationEnabled(false)
        setSupportMultipleWindows(false)
        javaScriptCanOpenWindowsAutomatically = false
        useWideViewPort = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }
    host.controller.configureForMode(webView, desktop)

    // Facebook keeps its login in cookies. The WebView stores them itself; this app never reads them.
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    var lastBlockedToast = 0L

    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val target = request.url.toString()
            // With the desktop site on, Facebook's mobile hosts (m., touch., mbasic.) only show a "get the
            // app" page for messaging, so the same address is opened on www.facebook.com instead.
            if (host.viewModel.desktopSite.value) {
                WebConfig.toDesktopHost(target)?.let { view.loadUrl(it); return true }
            }
            return when (val nav = WebConfig.classify(target)) {
                WebConfig.Navigation.Allow -> false
                is WebConfig.Navigation.External -> {
                    ExternalLinks.openInCustomTab(view.context, nav.url)
                    true
                }
                is WebConfig.Navigation.ViewIntent -> {
                    ExternalLinks.view(view.context, nav.url)
                    true
                }
                WebConfig.Navigation.Block -> {
                    val now = System.currentTimeMillis()
                    if (now - lastBlockedToast > 3_000) {
                        lastBlockedToast = now
                        Toast.makeText(view.context, R.string.link_blocked_toast, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            host.viewModel.onPageStarted(url)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            host.viewModel.onPageFinished(url, view.canGoBack())
        }

        // Facebook is a single-page app, so most navigation shows up here rather than as page loads.
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            host.viewModel.onCanGoBack(view.canGoBack())
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                host.viewModel.onError(WebError(WebError.Kind.NETWORK, url = request.url.toString()))
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                host.viewModel.onError(
                    WebError(WebError.Kind.HTTP, errorResponse.statusCode, request.url.toString())
                )
            }
        }

        // Never continue past a certificate problem.
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            host.viewModel.onError(WebError(WebError.Kind.SSL, url = error.url))
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // Returning true keeps the app alive; the Retry button rebuilds the WebView.
            host.viewModel.onError(WebError(WebError.Kind.RENDERER))
            return true
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            host.viewModel.onProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            host.viewModel.onTitle(title, view.url)
        }

        // Photo, video and file pickers for attachments. The system picker does the work.
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            host.fileChooser.deliver(null) // cancel any picker that is still pending
            host.fileChooser.callback = filePathCallback

            // A voice clip the user chose to attach is offered when Facebook opens a picker that takes
            // audio. The photo and video buttons do not, so they open the normal picker.
            val clip = host.controller.voiceClip
            if (clip != null && clip.exists()) {
                if (WebConfig.acceptsAudio(fileChooserParams.acceptTypes)) {
                    host.fileChooser.pickerIntent = try {
                        fileChooserParams.createIntent()
                    } catch (e: Exception) {
                        null
                    }
                    host.showVoicePrompt(clip)
                    return true
                }
                Toast.makeText(host.context, R.string.voice_not_here_toast, Toast.LENGTH_LONG).show()
            }

            return try {
                val intent = fileChooserParams.createIntent()
                host.fileChooser.pickerIntent = intent
                host.launchFileChooser(intent)
                true
            } catch (e: ActivityNotFoundException) {
                host.fileChooser.deliver(null)
                false
            }
        }

        // Voice messages need the microphone; the camera is only asked for if Facebook asks.
        override fun onPermissionRequest(request: PermissionRequest) {
            if (!WebConfig.isAllowedHost(request.origin.host)) {
                request.deny()
                return
            }
            val wanted = LinkedHashMap<String, String>() // Android permission -> web resource
            for (resource in request.resources) {
                when (resource) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> wanted[Manifest.permission.RECORD_AUDIO] = resource
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> wanted[Manifest.permission.CAMERA] = resource
                }
            }
            if (wanted.isEmpty()) {
                request.deny()
                return
            }
            val missing = wanted.keys.filter {
                ContextCompat.checkSelfPermission(host.context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                request.grant(wanted.values.toTypedArray())
                return
            }
            host.permissions.cancel()
            host.permissions.request = request
            host.permissions.wanted = wanted
            host.launchPermissions(missing.toTypedArray())
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            if (host.permissions.request === request) host.permissions.request = null
        }

        // Full-screen video.
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            host.customView.show(host.activity, view, callback)
        }

        override fun onHideCustomView() {
            host.customView.hide(host.activity)
        }
    }

    // Saving files needs the login cookies, which this app deliberately never reads.
    webView.setDownloadListener { _, _, _, _, _ ->
        Toast.makeText(host.context, R.string.downloads_unsupported, Toast.LENGTH_LONG).show()
    }

    webView.loadUrl(WebConfig.MESSAGES_URL)
    return webView
}

// ---------------------------------------------------------------------------------------------
// Small holders for state that the WebChromeClient callbacks need
// ---------------------------------------------------------------------------------------------

private class FileChooserHolder {
    var callback: ValueCallback<Array<Uri>>? = null

    /** The normal system picker intent for the pending request, kept for "Choose another file". */
    var pickerIntent: Intent? = null

    /** Always answers the pending callback, with null when the user picked nothing. */
    fun deliver(uris: Array<Uri>?) {
        val cb = callback
        callback = null
        pickerIntent = null
        cb?.onReceiveValue(uris)
    }
}

private class PermissionHolder {
    var request: PermissionRequest? = null
    var wanted: Map<String, String> = emptyMap()

    fun cancel() {
        request?.deny()
        request = null
    }

    /** Grants the web resources whose Android permission is now held. Returns true if any was granted. */
    fun resolve(isGranted: (String) -> Boolean): Boolean {
        val pending = request ?: return false
        request = null
        val resources = wanted.filterKeys(isGranted).values
        return if (resources.isEmpty()) {
            pending.deny()
            false
        } else {
            pending.grant(resources.toTypedArray())
            true
        }
    }
}

private class CustomViewHolder {
    private var view: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    var showing by mutableStateOf(false)

    fun show(activity: Activity?, newView: View, newCallback: WebChromeClient.CustomViewCallback) {
        val decor = activity?.window?.decorView as? FrameLayout
        if (view != null || decor == null) {
            newCallback.onCustomViewHidden()
            return
        }
        newView.setBackgroundColor(AndroidColor.BLACK)
        decor.addView(
            newView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        view = newView
        callback = newCallback
        showing = true
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun hide(activity: Activity?) {
        val current = view ?: return
        (current.parent as? ViewGroup)?.removeView(current)
        view = null
        callback?.onCustomViewHidden()
        callback = null
        showing = false
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------------------------------------------------------------------------------------
// Error UI
// ---------------------------------------------------------------------------------------------

@Composable
private fun ErrorOverlay(
    title: String,
    body: String,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            // Stops taps from reaching the page that sits underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = body, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenInBrowser) { Text(stringResource(R.string.action_open_in_chrome)) }
    }
}

@Composable
private fun ErrorBanner(
    text: String,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSwitchSite: (() -> Unit)? = null
) {
    Surface(modifier = modifier.fillMaxWidth(), elevation = 6.dp, color = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = text, style = MaterialTheme.typography.body2)
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss)) }
                if (onSwitchSite != null) {
                    TextButton(onClick = onSwitchSite) { Text(stringResource(R.string.action_switch_site)) }
                }
                TextButton(onClick = onOpenInBrowser) { Text(stringResource(R.string.action_chrome_short)) }
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}
