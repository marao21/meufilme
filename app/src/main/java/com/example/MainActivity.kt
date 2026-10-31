package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var containerLayout: FrameLayout
    private lateinit var progressBar: ProgressBar
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val targetUrl = AppConfig.WEB_HOME_URL

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while playing dramas on TV Box
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Root layout
        containerLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF0F172A.toInt())
        }

        // Webview setup
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            setBackgroundColor(0xFF0F172A.toInt())
        }

        // Slim top progress bar for TV loading feedback
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                8
            )
            isIndeterminate = false
            max = 100
            visibility = View.VISIBLE
        }

        containerLayout.addView(webView)
        containerLayout.addView(progressBar)
        setContentView(containerLayout)

        configureWebViewSettings()
        setupClients()
        setupDownloadHandling()

        // Handle Back button for TV Box remote
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    // Exit fullscreen video
                    hideCustomView()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            webView.loadUrl(targetUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }

        webView.requestFocus()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebViewSettings() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Standard desktop/modern Chrome TV User Agent so streaming sites don't serve restricted or degraded players
        settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    private fun setupClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                // Inject blob download bridge so site's "Baixar HLS completo" works natively on TV
                injectBlobDownloadInterception(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val urlStr = uri.toString()

                // Keep all web navigation within the WebView
                if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                    return false
                }

                // Handle intent schemes if any external player is invoked
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Grant media protected DRM permissions automatically for drama streams
                request?.grant(request.resources)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE

                val fullscreenParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                containerLayout.addView(view, fullscreenParams)

                // Hide system UI for true TV immersive video
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }
    }

    private fun hideCustomView() {
        if (customView == null) return

        containerLayout.removeView(customView)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        webView.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        webView.requestFocus()
    }

    private fun setupDownloadHandling() {
        // Javascript bridge for blob downloads (e.g. HLS .ts files assembled in browser)
        webView.addJavascriptInterface(TvDownloadBridge(this), "AndroidDownloadBridge")

        // Standard HTTP downloads listener (e.g. "Baixar MP4" from /api/proxy)
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    setTitle(guessedName)
                    setDescription("Baixando drama para TV Box...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedName)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                Toast.makeText(this, "Iniciando download para a pasta Downloads!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao iniciar download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun injectBlobDownloadInterception(view: WebView?) {
        val script = """
            (function() {
                if (window.__blob_interceptor_ready) return;
                window.__blob_interceptor_ready = true;

                const origClick = HTMLAnchorElement.prototype.click;
                HTMLAnchorElement.prototype.click = function() {
                    const href = this.href || '';
                    if (href.startsWith('blob:')) {
                        const filename = this.download || 'video_drama.ts';
                        fetch(href)
                            .then(function(res) { return res.blob(); })
                            .then(function(blob) {
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    const base64Data = reader.result.split(',')[1];
                                    if (window.AndroidDownloadBridge && window.AndroidDownloadBridge.saveBlobFile) {
                                        window.AndroidDownloadBridge.saveBlobFile(base64Data, filename);
                                    }
                                };
                                reader.readAsDataURL(blob);
                            })
                            .catch(function(err) {
                                console.error('Blob capture error', err);
                            });
                    }
                    return origClick.apply(this, arguments);
                };
            })();
        """.trimIndent()
        view?.evaluateJavascript(script, null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV remote directional navigation support
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Focus element click
                return super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (customView == null && !webView.canScrollVertically(-1)) {
                    // Let TV navigation proceed
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (customView == null && !webView.canScrollVertically(1)) {
                    // Let TV navigation proceed
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        if (customView != null) {
            hideCustomView()
        }
        webView.destroy()
        super.onDestroy()
    }

    // JavaScript Bridge for saving files generated by the site's HLS downloader
    class TvDownloadBridge(private val activity: Activity) {
        @JavascriptInterface
        fun saveBlobFile(base64Data: String, fileName: String) {
            activity.runOnUiThread {
                try {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                    val file = File(downloadsDir, sanitized)
                    val fos = FileOutputStream(file)
                    fos.write(bytes)
                    fos.flush()
                    fos.close()

                    Toast.makeText(
                        activity,
                        "Vídeo salvo com sucesso na pasta Downloads:\n${file.name}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        "Erro ao salvar vídeo: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
