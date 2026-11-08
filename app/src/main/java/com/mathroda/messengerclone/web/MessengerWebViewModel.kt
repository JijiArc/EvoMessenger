package com.mathroda.messengerclone.web

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebError(val kind: Kind, val httpStatus: Int = 0, val url: String? = null) {
    enum class Kind { NETWORK, SSL, HTTP, RENDERER, UNAVAILABLE }
}

data class WebUiState(
    val loading: Boolean = true,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val currentUrl: String? = null,
    val pageTitle: String? = null,
    val error: WebError? = null
)

/**
 * UI state for the embedded Facebook page. It only tracks loading, errors and display preferences.
 * It never sees page content, cookies or credentials.
 */
class MessengerWebViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = WebPrefs(application)

    private val _state = MutableStateFlow(WebUiState())
    val state: StateFlow<WebUiState> = _state.asStateFlow()

    private val _viewMode = MutableStateFlow(prefs.viewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _desktopSite = MutableStateFlow(prefs.desktopSite)
    val desktopSite: StateFlow<Boolean> = _desktopSite.asStateFlow()

    fun onPageStarted(url: String?) {
        // A new load clears the previous error. A failing load sets it again right after.
        _state.update { it.copy(loading = true, progress = 0, currentUrl = url, pageTitle = null, error = null) }
    }

    fun onProgress(progress: Int) {
        _state.update { it.copy(progress = progress, loading = progress < 100) }
    }

    fun onPageFinished(url: String?, canGoBack: Boolean) {
        _state.update { it.copy(loading = false, progress = 100, currentUrl = url ?: it.currentUrl, canGoBack = canGoBack) }
    }

    fun onCanGoBack(canGoBack: Boolean) {
        _state.update { it.copy(canGoBack = canGoBack) }
    }

    /**
     * Facebook shows "this content isn't available" style pages for things its mobile site cannot do.
     * The title is page metadata only; the app never reads page content.
     */
    fun onTitle(title: String?, url: String?) {
        _state.update { it.copy(pageTitle = title) }
        if (WebConfig.looksUnavailable(title) && WebConfig.isAllowedHost(url?.let { runCatching { java.net.URI(it).host }.getOrNull() })) {
            onError(WebError(WebError.Kind.UNAVAILABLE, url = url))
        }
    }

    fun onError(error: WebError) {
        _state.update {
            // A certificate failure is the real cause; do not replace it with a generic network error.
            if (it.error?.kind == WebError.Kind.SSL && error.kind == WebError.Kind.NETWORK) it
            else it.copy(error = error, loading = false)
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun setViewMode(mode: ViewMode) {
        prefs.viewMode = mode
        _viewMode.value = mode
    }

    fun setDesktopSite(enabled: Boolean) {
        prefs.desktopSite = enabled
        _desktopSite.value = enabled
    }
}
