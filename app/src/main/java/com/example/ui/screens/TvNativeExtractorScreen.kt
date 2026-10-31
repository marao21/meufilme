package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DramaShortcut
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
fun TvNativeExtractorScreen(
    inputUrl: String,
    isAnalyzing: Boolean,
    analyzedVideos: List<VideoMedia>,
    shortcuts: List<DramaShortcut>,
    errorMessage: String?,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPlayVideo: (VideoMedia) -> Unit,
    onDownloadVideo: (VideoMedia) -> Unit,
    onOpenShortcutInWeb: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search & Input Bar Section
        item {
            Surface(
                color = TvDeepSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Analisar Link de Drama ou Stream Direto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cole o link de uma página de dorama ou o endereço direto de um vídeo .mp4 / .m3u8",
                        fontSize = 13.sp,
                        color = TvTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = onUrlChange,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TvTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TvFocusHighlight,
                                unfocusedBorderColor = TvCardBg,
                                focusedContainerColor = TvCardBg,
                                unfocusedContainerColor = TvCardBg
                            ),
                            placeholder = {
                                Text("https://exemplo.com/drama/episodio-1 ou link .m3u8 / .mp4", fontSize = 13.sp, color = TvTextSecondary)
                            }
                        )

                        // Paste from clipboard button
                        val pasteShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(pasteShape)
                                .background(TvCardBg)
                                .tvFocusable(shape = pasteShape) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) {
                                            onUrlChange(text)
                                            Toast.makeText(context, "Link colado da área de transferência!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Colar", tint = TvAccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Colar", color = TvAccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Analyze button
                        val analyzeShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(analyzeShape)
                                .background(if (isAnalyzing) TvCardBg else TvPrimaryPurple)
                                .tvFocusable(shape = analyzeShape, onClick = onAnalyze)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analisando...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Analisar", tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Extrair Vídeo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚠️ $errorMessage",
                            color = TvAccentPink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Popular Drama Portals Shortcuts
        item {
            Column {
                Text(
                    text = "Portais de Drama e Minisséries Recomendados",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(shortcuts) { shortcut ->
                        val cardShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(100.dp)
                                .clip(cardShape)
                                .background(TvCardBg)
                                .tvFocusable(shape = cardShape) {
                                    onOpenShortcutInWeb(shortcut.url)
                                }
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(shortcut.iconEmoji, fontSize = 22.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(TvPrimaryPurple.copy(alpha = 0.3f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(shortcut.category, fontSize = 10.sp, color = TvAccentCyan, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column {
                                    Text(
                                        text = shortcut.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TvTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = shortcut.description,
                                        fontSize = 11.sp,
                                        color = TvTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Extracted & Detected Videos Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vídeos Encontrados (${analyzedVideos.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
            }
        }

        if (analyzedVideos.isEmpty()) {
            item {
                Surface(
                    color = TvCardBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = TvTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum link de vídeo extraído ainda",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Navegue no Modo Web Vercel para capturar streams automaticamente ou cole uma URL acima.",
                            fontSize = 13.sp,
                            color = TvTextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        val demoShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .clip(demoShape)
                                .background(TvCardHover)
                                .tvFocusable(shape = demoShape) {
                                    onUrlChange("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                                    onAnalyze()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Testar com vídeo de demonstração MP4", color = TvAccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(analyzedVideos) { video ->
                TvVideoCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onDownload = { onDownloadVideo(video) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Link do Vídeo", video.url))
                        Toast.makeText(context, "Link copiado!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun TvVideoCard(
    video: VideoMedia,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(14.dp)

    Surface(
        color = TvCardBg,
        shape = cardShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvPrimaryPurple.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = TvAccentCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (video.isHls) TvAccentPink else TvAccentCyan)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.format,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (video.durationSeconds != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvCardHover)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = video.durationFormatted,
                                    color = TvTextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.title,
                        color = TvTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = video.url,
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val btnShape = RoundedCornerShape(8.dp)

                // Play
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(btnShape)
                        .background(TvPrimaryPurple)
                        .tvFocusable(shape = btnShape, onClick = onPlay)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Assistir", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assistir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Download
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(btnShape)
                        .background(Color(0xFF0F766E))
                        .tvFocusable(shape = btnShape, onClick = onDownload)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = "Baixar", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Copy Link
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(btnShape)
                        .background(TvCardHover)
                        .tvFocusable(shape = btnShape, onClick = onCopy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Link", tint = TvTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
