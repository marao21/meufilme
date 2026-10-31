package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadRecord
import com.example.model.DownloadStatus
import com.example.model.VideoMedia
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentPink
import com.example.ui.theme.TvCardBg
import com.example.ui.theme.TvCardHover
import com.example.ui.theme.TvDeepSurface
import com.example.ui.theme.TvFocusHighlight
import com.example.ui.theme.TvPrimaryPurple
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.tvFocusable

@Composable
fun TvDownloadsScreen(
    downloads: List<DownloadRecord>,
    onPlayLocalVideo: (VideoMedia) -> Unit,
    onDelete: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = TvDeepSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Biblioteca de Vídeos Baixados",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Assista a seus doramas salvos diretamente na TV Box sem precisar de internet.",
                            fontSize = 13.sp,
                            color = TvTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvCardBg)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${downloads.size} vídeo(s)", color = TvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (downloads.isEmpty()) {
            item {
                Surface(
                    color = TvCardBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = TvTextSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nenhum download realizado ainda",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ao encontrar um drama no Modo Web ou no Extrator Nativo, selecione 'Baixar' para guardar aqui.",
                            fontSize = 13.sp,
                            color = TvTextSecondary
                        )
                    }
                }
            }
        } else {
            items(downloads) { record ->
                val cardShape = RoundedCornerShape(14.dp)
                Surface(
                    color = TvCardBg,
                    shape = cardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (record.status == DownloadStatus.COMPLETED) Color(0xFF0F766E)
                                            else TvPrimaryPurple.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.status == DownloadStatus.COMPLETED) Icons.Default.PlayArrow else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val statusColor = when (record.status) {
                                            DownloadStatus.COMPLETED -> Color(0xFF10B981)
                                            DownloadStatus.DOWNLOADING -> TvAccentCyan
                                            DownloadStatus.FAILED -> TvAccentPink
                                            DownloadStatus.IDLE -> TvTextSecondary
                                        }

                                        val statusLabel = when (record.status) {
                                            DownloadStatus.COMPLETED -> "CONCLUÍDO"
                                            DownloadStatus.DOWNLOADING -> "BAIXANDO (${record.progress}%)"
                                            DownloadStatus.FAILED -> "FALHOU"
                                            DownloadStatus.IDLE -> "AGUARDANDO"
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (record.fileSizeText.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(record.fileSizeText, color = TvTextSecondary, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = record.title,
                                        color = TvTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Action buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val btnShape = RoundedCornerShape(8.dp)

                                if (record.status == DownloadStatus.COMPLETED && record.localUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(btnShape)
                                            .background(TvPrimaryPurple)
                                            .tvFocusable(shape = btnShape) {
                                                val localVideo = VideoMedia(
                                                    url = record.localUri,
                                                    title = record.title,
                                                    format = record.format,
                                                    isHls = record.format.contains("HLS") || record.localUri.endsWith(".ts")
                                                )
                                                onPlayLocalVideo(localVideo)
                                            }
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Assistir Offline", tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Assistir Offline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Delete button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(btnShape)
                                        .background(TvCardHover)
                                        .tvFocusable(shape = btnShape) { onDelete(record) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TvAccentPink, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Progress Bar if actively downloading
                        if (record.status == DownloadStatus.DOWNLOADING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (record.progress.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = TvAccentCyan,
                                trackColor = TvDeepSurface
                            )
                        }

                        if (record.error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Erro: ${record.error}", color = TvAccentPink, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
