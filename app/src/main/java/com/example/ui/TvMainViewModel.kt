package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DramaRepository
import com.example.download.TvDownloadManager
import com.example.model.AppSettings
import com.example.model.DownloadRecord
import com.example.model.TvNavTab
import com.example.model.VideoMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TvMainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DramaRepository()
    private val downloadManager = TvDownloadManager(application)

    private val _currentTab = MutableStateFlow(TvNavTab.WEB)
    val currentTab: StateFlow<TvNavTab> = _currentTab.asStateFlow()

    // Fullscreen TV Player state
    private val _activePlayerVideo = MutableStateFlow<VideoMedia?>(null)
    val activePlayerVideo: StateFlow<VideoMedia?> = _activePlayerVideo.asStateFlow()

    // Detected videos during web session
    private val _detectedVideos = MutableStateFlow<List<VideoMedia>>(emptyList())
    val detectedVideos: StateFlow<List<VideoMedia>> = _detectedVideos.asStateFlow()

    // Transient TV notification when new video stream is intercepted
    private val _latestDetectedPrompt = MutableStateFlow<VideoMedia?>(null)
    val latestDetectedPrompt: StateFlow<VideoMedia?> = _latestDetectedPrompt.asStateFlow()

    // Web URL state
    private val _currentWebUrl = MutableStateFlow("https://baixar-video-drama-web-uy33.vercel.app/")
    val currentWebUrl: StateFlow<String> = _currentWebUrl.asStateFlow()

    // Extrator Nativo state
    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzedVideos = MutableStateFlow<List<VideoMedia>>(emptyList())
    val analyzedVideos: StateFlow<List<VideoMedia>> = _analyzedVideos.asStateFlow()

    private val _extractorError = MutableStateFlow<String?>(null)
    val extractorError: StateFlow<String?> = _extractorError.asStateFlow()

    // Downloads list from download manager
    val downloads: StateFlow<List<DownloadRecord>> = downloadManager.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val shortcuts = repository.popularShortcuts

    fun setTab(tab: TvNavTab) {
        _currentTab.value = tab
    }

    fun setInputUrl(url: String) {
        _inputUrl.value = url
    }

    fun playVideo(video: VideoMedia) {
        _activePlayerVideo.value = video
    }

    fun closePlayer() {
        _activePlayerVideo.value = null
    }

    fun dismissDetectedPrompt() {
        _latestDetectedPrompt.value = null
    }

    fun loadUrlInWeb(url: String) {
        _currentWebUrl.value = url
        _currentTab.value = TvNavTab.WEB
    }

    fun onVideoDetected(url: String, rawTitle: String? = null) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        // Prevent duplicate spam
        if (_detectedVideos.value.any { it.url == trimmed }) return

        val isHls = trimmed.lowercase().contains(".m3u8")
        val isMp4 = trimmed.lowercase().contains(".mp4")
        val format = if (isHls) "M3U8 (HLS)" else if (isMp4) "MP4" else "STREAM"

        val title = rawTitle?.takeIf { it.isNotBlank() } ?: run {
            val filename = trimmed.substringAfterLast('/').substringBefore('?')
            if (filename.isNotBlank() && filename.length > 4) filename else "Vídeo Drama Interceptado"
        }

        val video = VideoMedia(
            url = trimmed,
            title = title,
            format = format,
            isHls = isHls,
            durationSeconds = null,
            sourcePageUrl = _currentWebUrl.value
        )

        _detectedVideos.value = listOf(video) + _detectedVideos.value
        _latestDetectedPrompt.value = video
    }

    fun analyzeUrl(urlToAnalyze: String = _inputUrl.value) {
        val trimmed = urlToAnalyze.trim()
        if (trimmed.isBlank()) return

        _isAnalyzing.value = true
        _extractorError.value = null

        viewModelScope.launch {
            val result = repository.probeVideoUrl(trimmed)
            _isAnalyzing.value = false
            result.onSuccess { video ->
                _analyzedVideos.value = listOf(video) + _analyzedVideos.value
                _extractorError.value = null
            }.onFailure { err ->
                _extractorError.value = err.localizedMessage ?: "Não foi possível extrair o vídeo deste link."
            }
        }
    }

    fun startDownload(video: VideoMedia) {
        downloadManager.startDownload(video)
        _latestDetectedPrompt.value = null
    }

    fun deleteDownload(record: DownloadRecord) {
        downloadManager.deleteDownload(record)
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }

    fun clearDetectedVideos() {
        _detectedVideos.value = emptyList()
        _latestDetectedPrompt.value = null
    }
}
