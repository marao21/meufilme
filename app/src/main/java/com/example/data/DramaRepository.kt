package com.example.data

import com.example.AppConfig
import com.example.model.DramaShortcut
import com.example.model.VideoMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class DramaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val popularShortcuts: List<DramaShortcut> = listOf(
        DramaShortcut(
            name = "Vercel Web App",
            url = AppConfig.WEB_HOME_URL,
            category = "Sistema Oficial",
            description = "Navegador com captura de link MP4 e M3U8 para dramas",
            iconEmoji = "🌐"
        ),
        DramaShortcut(
            name = "Doramas Online",
            url = "https://doramasgo.com",
            category = "Doramas Dublado/Legendado",
            description = "Catálogo completo de K-dramas, C-dramas e doramas populares",
            iconEmoji = "🎭"
        ),
        DramaShortcut(
            name = "Rakuten Viki",
            url = "https://www.viki.com",
            category = "Streaming Asiático",
            description = "Dramas coreanos, chineses e taiwaneses em alta definição",
            iconEmoji = "⭐"
        ),
        DramaShortcut(
            name = "DramaBox",
            url = "https://www.dramaboxdb.com",
            category = "Minisséries",
            description = "Episódios curtos de dramas e novelas verticais",
            iconEmoji = "📱"
        ),
        DramaShortcut(
            name = "ShortMax Dramas",
            url = "https://shortmax.com",
            category = "Minisséries",
            description = "Séries dramáticas e romances asiáticos rápidos",
            iconEmoji = "🔥"
        ),
        DramaShortcut(
            name = "AsianCrush",
            url = "https://www.asiancrush.com",
            category = "Cinema Asiático",
            description = "Filmes, séries e produções premiadas da Ásia",
            iconEmoji = "🎬"
        )
    )

    suspend fun probeVideoUrl(rawUrl: String): Result<VideoMedia> = withContext(Dispatchers.IO) {
        try {
            val trimmed = rawUrl.trim()
            val lower = trimmed.lowercase()
            val isLikelyHls = lower.contains(".m3u8")
            val isLikelyMp4 = lower.contains(".mp4")

            // 1. First attempt: call Vercel api/probe
            try {
                val encodedUrl = URLEncoder.encode(trimmed, "UTF-8")
                val probeApiUrl = "${AppConfig.WEB_BASE_URL}/api/probe?url=$encodedUrl"
                val request = Request.Builder()
                    .url(probeApiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val finalUrl = json.optString("url", trimmed)
                            val title = json.optString("title", extractTitleFromUrl(trimmed))
                            val format = json.optString("format", if (isLikelyHls) "M3U8 (HLS)" else "MP4")
                            val isHls = json.optBoolean("isHls", isLikelyHls)
                            val duration = if (json.has("durationSeconds") && !json.isNull("durationSeconds")) {
                                json.getLong("durationSeconds")
                            } else null
                            val live = json.optBoolean("live", false)

                            return@withContext Result.success(
                                VideoMedia(
                                    url = finalUrl,
                                    title = title,
                                    format = format,
                                    isHls = isHls,
                                    durationSeconds = duration,
                                    isLive = live,
                                    sourcePageUrl = trimmed
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // If Vercel API times out or fails, proceed to direct HTTP HEAD inspection below
            }

            // 2. Direct inspection via HEAD/GET request
            val directReq = Request.Builder()
                .url(trimmed)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .head()
                .build()

            var contentType = ""
            var contentLength = -1L
            try {
                client.newCall(directReq).execute().use { res ->
                    contentType = res.header("Content-Type", "") ?: ""
                    contentLength = res.header("Content-Length", "-1")?.toLongOrNull() ?: -1L
                }
            } catch (e: Exception) {
                // Ignore HEAD error
            }

            val isHls = isLikelyHls || contentType.contains("mpegurl", ignoreCase = true)
            val format = if (isHls) "M3U8 (HLS)" else if (isLikelyMp4 || contentType.contains("mp4", ignoreCase = true)) "MP4" else "VÍDEO"

            val video = VideoMedia(
                url = trimmed,
                title = extractTitleFromUrl(trimmed),
                format = format,
                isHls = isHls,
                durationSeconds = null,
                isLive = false,
                sourcePageUrl = trimmed
            )

            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val path = uri.path ?: ""
            val filename = path.substringAfterLast('/')
            if (filename.isNotBlank()) {
                filename.substringBefore('?')
            } else {
                uri.host ?: "Vídeo Drama"
            }
        } catch (e: Exception) {
            "Vídeo Drama Encontrado"
        }
    }
}
