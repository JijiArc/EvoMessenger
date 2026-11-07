package com.mathroda.messengerclone.web

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebError(val kind: Kind, val httpStatus: Int = 0, val url: String? = null) {
    enum class Kind { NETWORK, SSL, HTTP, RENDERER }
}

data class WebUiState(
    val loading: Boolean = true,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val currentUrl: String? = null,
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
        _state.update { it.copy(loading = true, progress = 0, currentUrl = url, error = null) }
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
