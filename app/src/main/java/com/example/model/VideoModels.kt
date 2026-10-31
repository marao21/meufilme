package com.example.model

enum class TvNavTab(val title: String) {
    WEB("Modo Web Vercel"),
    EXTRACTOR("Extrator Nativo"),
    DOWNLOADS("Meus Downloads"),
    SETTINGS("Opções")
}

data class VideoMedia(
    val url: String,
    val title: String,
    val format: String = "MP4",
    val isHls: Boolean = false,
    val durationSeconds: Long? = null,
    val isLive: Boolean = false,
    val sourcePageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val durationFormatted: String
        get() {
            if (isLive) return "AO VIVO"
            if (durationSeconds == null || durationSeconds <= 0) return "Duração desc."
            val h = durationSeconds / 3600
            val m = (durationSeconds % 3600) / 60
            val s = durationSeconds % 60
            return if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }
}

data class DramaShortcut(
    val name: String,
    val url: String,
    val category: String,
    val description: String,
    val iconEmoji: String
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadRecord(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val format: String,
    val downloadId: Long = -1L,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val localUri: String? = null,
    val fileSizeText: String = "",
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppSettings(
    val minDurationMinutes: Int = 15,
    val showUnknownDuration: Boolean = true,
    val protectedMode: Boolean = true,
    val homeUrl: String = "https://baixar-video-drama-web-uy33.vercel.app/"
)
