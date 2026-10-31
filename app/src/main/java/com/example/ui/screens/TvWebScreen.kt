package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.AppConfig
import com.example.BuildConfig
import com.example.model.VideoMedia
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvCardBg
import com.example.ui.theme.TvDeepSurface
import com.example.ui.theme.TvFocusHighlight
import com.example.ui.theme.TvPrimaryPurple
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.tvFocusable

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWebScreen(
    currentUrl: String,
    detectedVideos: List<VideoMedia>,
    onVideoDetected: (String, String?) -> Unit,
    onOpenDetectedList: () -> Unit,
    onPlayVideo: (VideoMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("Baixar Vídeo Drama") }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showHud by remember { mutableStateOf(true) }

    // Intercept back button for WebView navigation
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Remote Navigation HUD
            AnimatedVisibility(visible = showHud) {
                Surface(
                    color = TvDeepSurface,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Back button
                            TvNavButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                label = "Voltar",
                                onClick = { webViewInstance?.goBack() }
                            )

                            // Forward button
                            TvNavButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                label = "Avançar",
                                onClick = { webViewInstance?.goForward() }
                            )

                            // Reload button
                            TvNavButton(
                                icon = Icons.Default.Refresh,
                                label = "Recarregar",
                                onClick = { webViewInstance?.reload() }
                            )

                            // Home button (Vercel app)
                            TvNavButton(
                                icon = Icons.Default.Home,
                                label = "Início Vercel",
                                onClick = {
                                    val home = AppConfig.WEB_HOME_URL
                                    inputUrl = home
                                    webViewInstance?.loadUrl(home)
                                }
                            )

                            // URL input field
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TvTextPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TvFocusHighlight,
                                    unfocusedBorderColor = TvCardBg,
                                    focusedContainerColor = TvCardBg,
                                    unfocusedContainerColor = TvCardBg
                                ),
                                placeholder = {
                                    Text("Cole o endereço do site, ou um link .mp4 / .m3u8", fontSize = 12.sp, color = TvTextSecondary)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        var target = inputUrl.trim()
                                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                            target = "https://$target"
                                        }
                                        webViewInstance?.loadUrl(target)
                                    }
                                )
                            )

                            // Go button
                            val goShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(goShape)
                                    .background(TvPrimaryPurple)
                                    .tvFocusable(shape = goShape) {
                                        var target = inputUrl.trim()
                                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                            target = "https://$target"
                                        }
                                        webViewInstance?.loadUrl(target)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = "Ir", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // TV D-Pad Scroll Up
                            TvNavButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                label = "Cima",
                                onClick = { webViewInstance?.scrollBy(0, -300) }
                            )

                            // TV D-Pad Scroll Down
                            TvNavButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                label = "Baixo",
                                onClick = { webViewInstance?.scrollBy(0, 300) }
                            )

                            // Zoom -
                            TvNavButton(
                                icon = Icons.Default.Remove,
                                label = "Zoom -",
                                onClick = {
                                    webViewInstance?.zoomOut()
                                }
                            )

                            // Zoom +
                            TvNavButton(
                                icon = Icons.Default.Add,
                                label = "Zoom +",
                                onClick = {
                                    webViewInstance?.zoomIn()
                                }
                            )

                            // Detected videos pill button
                            if (detectedVideos.isNotEmpty()) {
                                val pillShape = RoundedCornerShape(8.dp)
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = TvAccentCyan, contentColor = Color.Black) {
                                            Text(detectedVideos.size.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(pillShape)
                                            .background(Color(0xFF0F766E))
                                            .tvFocusable(shape = pillShape, onClick = onOpenDetectedList)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.VideoLibrary,
                                                contentDescription = "Vídeos",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Vídeos (${detectedVideos.size})",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WebView Container
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        // Container do WebView: o fullscreen do HTML5 entra aqui por cima,
                        // senão o player que abre em tela cheia mostra superfície preta.
                        val container = FrameLayout(ctx)
                        var fullscreenView: View? = null
                        var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

                        // Permite inspecionar a aba por chrome://inspect e ler o console real.
                        if (BuildConfig.DEBUG) {
                            WebView.setWebContentsDebuggingEnabled(true)
                        }

                        val webView = WebView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                // User agent definido em um único lugar (AppConfig): alguns players
                                // entregam caminhos de reprodução diferentes conforme o user agent.
                                userAgentString = AppConfig.WEBVIEW_USER_AGENT
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { inputUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    view?.title?.let { pageTitle = it }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString()
                                    if (reqUrl != null) {
                                        val lower = reqUrl.lowercase()
                                        // Video stream pattern sniffer
                                        if (lower.contains(".mp4") ||
                                            lower.contains(".m3u8") ||
                                            lower.contains(".webm") ||
                                            lower.contains(".mkv") ||
                                            lower.contains("/master.m3u8") ||
                                            lower.contains("/playlist.m3u8") ||
                                            lower.contains("video/mp4")
                                        ) {
                                            // Discard tiny chunks or ads below threshold if needed, but intercept candidates
                                            if (!lower.contains("ad_") && !lower.contains("doubleclick")) {
                                                view?.post {
                                                    onVideoDetected(reqUrl, view.title)
                                                }
                                            }
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onLoadResource(view: WebView?, url: String?) {
                                    super.onLoadResource(view, url)
                                    if (url != null) {
                                        val lower = url.lowercase()
                                        if ((lower.endsWith(".mp4") || lower.contains(".mp4?") ||
                                             lower.endsWith(".m3u8") || lower.contains(".m3u8?")) &&
                                            !lower.contains("googlesyndication") && !lower.contains("adservice")
                                        ) {
                                            view?.post {
                                                onVideoDetected(url, view.title)
                                            }
                                        }
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { pageTitle = it }
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    // Players HLS pedem Encrypted Media Extensions; sem a concessão
                                    // o WebView nega e o vídeo nunca começa (retângulo preto).
                                    val req = request ?: return
                                    val protectedMedia = req.resources.filter {
                                        it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                                    }
                                    if (protectedMedia.isNotEmpty()) {
                                        req.grant(protectedMedia.toTypedArray())
                                    } else {
                                        req.deny()
                                    }
                                }

                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?
                                ) {
                                    if (fullscreenView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    fullscreenView = view
                                    fullscreenCallback = callback
                                    this@apply.visibility = View.GONE
                                    if (view != null) {
                                        container.addView(
                                            view,
                                            FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        )
                                    }
                                }

                                override fun onHideCustomView() {
                                    val view = fullscreenView ?: return
                                    container.removeView(view)
                                    fullscreenView = null
                                    this@apply.visibility = View.VISIBLE
                                    fullscreenCallback?.onCustomViewHidden()
                                    fullscreenCallback = null
                                }
                            }

                            loadUrl(currentUrl)
                        }

                        container.addView(
                            webView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        webViewInstance = webView
                        container
                    },
                    update = {
                        val view = webViewInstance
                        if (view != null && view.url != currentUrl && currentUrl.isNotBlank()) {
                            view.loadUrl(currentUrl)
                        }
                    }
                )

                // Loading bar
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvDeepSurface.copy(alpha = 0.9f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TvFocusHighlight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Carregando página...", fontSize = 12.sp, color = TvTextPrimary)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}

@Composable
private fun TvNavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(TvCardBg)
            .tvFocusable(shape = shape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TvTextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
