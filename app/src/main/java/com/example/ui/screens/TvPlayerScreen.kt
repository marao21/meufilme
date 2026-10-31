package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoMedia
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentPink
import com.example.ui.theme.TvCardBg
import com.example.ui.theme.TvDeepSurface
import com.example.ui.theme.TvFocusHighlight
import com.example.ui.theme.TvPrimaryPurple
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.tvFocusable
import kotlinx.coroutines.delay

@Composable
fun TvPlayerScreen(
    video: VideoMedia,
    onClose: () -> Unit,
    onDownload: (VideoMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Intercept back key to close player
    BackHandler {
        videoViewRef?.stopPlayback()
        onClose()
    }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    // Periodic time progress updater
    LaunchedEffect(videoViewRef, isPlaying) {
        while (true) {
            delay(500)
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPosMs = vv.currentPosition
                    val dur = vv.duration
                    if (dur > 0) totalDurationMs = dur
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video View Component
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    setOnPreparedListener { mp ->
                        isBuffering = false
                        mp.isLooping = false
                        totalDurationMs = duration
                        start()
                        isPlaying = true
                    }

                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            isBuffering = true
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            isBuffering = false
                        }
                        true
                    }

                    setOnErrorListener { _, what, extra ->
                        isBuffering = false
                        playbackError = "Não foi possível reproduzir este fluxo de vídeo (código $what-$extra)."
                        true
                    }

                    setOnCompletionListener {
                        isPlaying = false
                        showControls = true
                    }

                    setVideoURI(Uri.parse(video.url))
                    videoViewRef = this
                }
            }
        )

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDeepSurface.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TvAccentCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Carregando vídeo...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Error message if stream failed
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvDeepSurface.copy(alpha = 0.95f))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Erro de Reprodução", color = TvAccentPink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(playbackError ?: "", color = TvTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val retryShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .clip(retryShape)
                            .background(TvPrimaryPurple)
                            .tvFocusable(shape = retryShape) {
                                playbackError = null
                                isBuffering = true
                                videoViewRef?.setVideoURI(Uri.parse(video.url))
                                videoViewRef?.start()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Tentar Novamente", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // On-Screen TV Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Top Overlay Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val backShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(backShape)
                                .background(TvCardBg)
                                .tvFocusable(shape = backShape) {
                                    videoViewRef?.stopPlayback()
                                    onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
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
                                    Text(video.format, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Player TV Box", color = TvAccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Download button in player
                    val dlShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(dlShape)
                            .background(Color(0xFF0F766E))
                            .tvFocusable(shape = dlShape) { onDownload(video) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = "Baixar", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Baixar Vídeo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Center Play/Rewind/Forward Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Rewind 10s
                    val btnShape = CircleShape
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(btnShape)
                            .background(TvCardBg)
                            .tvFocusable(shape = btnShape) {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                    vv.seekTo(newPos)
                                    currentPosMs = newPos
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Voltar 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Play / Pause main button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(btnShape)
                            .background(TvPrimaryPurple)
                            .tvFocusable(shape = btnShape, scaleFocused = 1.08f) {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Fast Forward 10s
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(btnShape)
                            .background(TvCardBg)
                            .tvFocusable(shape = btnShape) {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                    vv.seekTo(newPos)
                                    currentPosMs = newPos
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Avançar 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Bottom Timeline Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    val progressRatio = if (totalDurationMs > 0) currentPosMs.toFloat() / totalDurationMs else 0f

                    Slider(
                        value = progressRatio.coerceIn(0f, 1f),
                        onValueChange = { ratio ->
                            if (totalDurationMs > 0) {
                                val targetMs = (ratio * totalDurationMs).toInt()
                                videoViewRef?.seekTo(targetMs)
                                currentPosMs = targetMs
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = TvFocusHighlight,
                            activeTrackColor = TvPrimaryPurple,
                            inactiveTrackColor = TvCardBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(currentPosMs), color = TvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (totalDurationMs > 0) formatMs(totalDurationMs) else "Ao Vivo / M3U8",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
