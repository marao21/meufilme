package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.model.DownloadRecord
import com.example.model.DownloadStatus
import com.example.model.VideoMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.UUID

class TvDownloadManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("tv_downloads_prefs", Context.MODE_PRIVATE)
    private val _downloads = MutableStateFlow<List<DownloadRecord>>(emptyList())
    val downloads: StateFlow<List<DownloadRecord>> = _downloads.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        loadPersistedDownloads()
    }

    private fun loadPersistedDownloads() {
        val rawJson = prefs.getString("saved_downloads", "[]") ?: "[]"
        try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<DownloadRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DownloadRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Vídeo"),
                        sourceUrl = obj.optString("sourceUrl", ""),
                        format = obj.optString("format", "MP4"),
                        downloadId = obj.optLong("downloadId", -1L),
                        progress = obj.optInt("progress", 0),
                        status = try {
                            DownloadStatus.valueOf(obj.optString("status", "IDLE"))
                        } catch (e: Exception) {
                            DownloadStatus.IDLE
                        },
                        localUri = obj.optString("localUri").takeIf { it.isNotEmpty() },
                        fileSizeText = obj.optString("fileSizeText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _downloads.value = list
        } catch (e: Exception) {
            _downloads.value = emptyList()
        }
    }

    private fun persistDownloads() {
        try {
            val arr = JSONArray()
            _downloads.value.forEach { rec ->
                val obj = JSONObject()
                obj.put("id", rec.id)
                obj.put("title", rec.title)
                obj.put("sourceUrl", rec.sourceUrl)
                obj.put("format", rec.format)
                obj.put("downloadId", rec.downloadId)
                obj.put("progress", rec.progress)
                obj.put("status", rec.status.name)
                obj.put("localUri", rec.localUri ?: "")
                obj.put("fileSizeText", rec.fileSizeText)
                obj.put("timestamp", rec.timestamp)
                arr.put(obj)
            }
            prefs.edit().putString("saved_downloads", arr.toString()).apply()
        } catch (e: Exception) {
            // Ignore persistence errors
        }
    }

    fun startDownload(video: VideoMedia) {
        val id = UUID.randomUUID().toString()
        val sanitizedTitle = video.title
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .take(60)
            .ifBlank { "Drama_Video" }
        val extension = if (video.isHls) "ts" else "mp4"
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis() % 10000}.$extension"

        val initialRecord = DownloadRecord(
            id = id,
            title = video.title,
            sourceUrl = video.url,
            format = video.format,
            progress = 0,
            status = DownloadStatus.DOWNLOADING,
            timestamp = System.currentTimeMillis()
        )

        _downloads.value = listOf(initialRecord) + _downloads.value
        persistDownloads()

        // Background download coroutine
        scope.launch {
            try {
                // Try system DownloadManager first for direct MP4, or fallback to file stream
                if (!video.isHls && (video.url.startsWith("http://") || video.url.startsWith("https://"))) {
                    trySystemDownloadManager(initialRecord, fileName)
                } else {
                    performDirectStreamDownload(initialRecord, fileName)
                }
            } catch (e: Exception) {
                updateRecord(id) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        error = e.localizedMessage ?: "Erro ao baixar"
                    )
                }
            }
        }
    }

    private suspend fun trySystemDownloadManager(record: DownloadRecord, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(record.sourceUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle(record.title)
                setDescription("Baixando drama para TV Box")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val downloadId = downloadManager.enqueue(request)

            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadDir, fileName)

            updateRecord(record.id) {
                it.copy(
                    downloadId = downloadId,
                    localUri = targetFile.absolutePath,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 15
                )
            }

            // Monitor download progress in background
            scope.launch {
                var finished = false
                while (!finished) {
                    kotlinx.coroutines.delay(1000)
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            finished = true
                            val sizeMb = if (targetFile.exists()) String.format("%.1f MB", targetFile.length() / (1024.0 * 1024.0)) else ""
                            updateRecord(record.id) {
                                it.copy(
                                    status = DownloadStatus.COMPLETED,
                                    progress = 100,
                                    localUri = targetFile.absolutePath,
                                    fileSizeText = sizeMb
                                )
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            finished = true
                            // Fallback to direct stream download if system download manager fails
                            performDirectStreamDownload(record, fileName)
                        } else if (bytesTotal > 0) {
                            val pct = ((bytesDownloaded.toDouble() / bytesTotal) * 100).toInt()
                            val sizeText = String.format("%.1f / %.1f MB", bytesDownloaded / (1024.0 * 1024.0), bytesTotal / (1024.0 * 1024.0))
                            updateRecord(record.id) {
                                it.copy(progress = pct, fileSizeText = sizeText)
                            }
                        }
                        cursor.close()
                    } else {
                        finished = true
                    }
                }
            }
        } catch (e: Exception) {
            // If system download manager fails (e.g. URI permissions), fallback to direct stream
            performDirectStreamDownload(record, fileName)
        }
    }

    private suspend fun performDirectStreamDownload(record: DownloadRecord, fileName: String) = withContext(Dispatchers.IO) {
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val targetFile = File(targetDir, fileName)

        try {
            val url = URL(record.sourceUrl)
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            val fileLength = connection.contentLength
            val input: InputStream = connection.getInputStream()
            val output = FileOutputStream(targetFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            var lastUpdate = System.currentTimeMillis()

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                val now = System.currentTimeMillis()
                if (now - lastUpdate > 600) {
                    lastUpdate = now
                    val pct = if (fileLength > 0) ((total * 100) / fileLength).toInt() else 50
                    val sizeText = String.format("%.1f MB", total / (1024.0 * 1024.0))
                    updateRecord(record.id) {
                        it.copy(progress = pct.coerceIn(0, 99), fileSizeText = sizeText)
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            val finalSizeMb = String.format("%.1f MB", targetFile.length() / (1024.0 * 1024.0))
            updateRecord(record.id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    localUri = targetFile.absolutePath,
                    fileSizeText = finalSizeMb
                )
            }
        } catch (e: Exception) {
            updateRecord(record.id) {
                it.copy(
                    status = DownloadStatus.FAILED,
                    error = e.localizedMessage ?: "Erro na conexão"
                )
            }
        }
    }

    private fun updateRecord(id: String, transform: (DownloadRecord) -> DownloadRecord) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) transform(it) else it
        }
        persistDownloads()
    }

    fun deleteDownload(record: DownloadRecord) {
        record.localUri?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // Ignore delete errors
            }
        }
        _downloads.value = _downloads.value.filter { it.id != record.id }
        persistDownloads()
    }
}
