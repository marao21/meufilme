package com.example

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import com.example.ui.CursorVirtual
import com.example.update.AppUpdater
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var containerLayout: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var cursor: CursorVirtual
    private var cursorLigado = false
    private var passoDoCursor = 18f
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    /**
     * Numa TV Box nao ha dedo nem mouse: quem rola a pagina e o D-pad, e o site
     * tem um modo proprio para isso. No celular esse modo atrapalharia.
     */
    private val isTv: Boolean by lazy {
        val modo = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        modo?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    private val targetUrl: String by lazy { AppConfig.homeUrl(isTv) }

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

        cursor = CursorVirtual(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        containerLayout.addView(webView)
        containerLayout.addView(progressBar)
        containerLayout.addView(cursor)
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

        // Dica unica por abertura: dentro do video, os controles sao do
        // navegador e o D-pad nao os alcanca. Sem este aviso, o cursor fica
        // escondido e ninguem descobre que ele existe.
        containerLayout.postDelayed({
            if (!isFinishing) {
                Toast.makeText(
                    this,
                    "No video, segure o botao central para ligar o cursor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 2500)

        procurarAtualizacao()
    }

    /**
     * Ao abrir, pergunta se ha APK mais novo publicado. Havendo, oferece a
     * troca; nao havendo — ou sem rede —, nada aparece. E o que faz a TV Box
     * se atualizar sozinha, sem ninguem ter de ir ao GitHub pelo navegador.
     */
    private fun procurarAtualizacao() {
        lifecycleScope.launch {
            val nova = AppUpdater.procurarAtualizacao() ?: return@launch
            if (isFinishing || isDestroyed) return@launch
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Tem versao nova do app")
                .setMessage("Baixar e instalar agora? O filme que estiver aberto continua depois.")
                .setPositiveButton("Atualizar") { _, _ ->
                    AppUpdater.baixarEInstalar(this@MainActivity, nova.url)
                }
                .setNegativeButton("Agora nao", null)
                .show()
        }
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
        settings.userAgentString = AppConfig.WEBVIEW_USER_AGENT

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

            /**
             * Captura do link do servidor — o equivalente nativo da extensão.
             *
             * O endereço do arquivo de vídeo só nasce quando o player o pede,
             * e ele é pedido de dentro de uma moldura de outro site. Nenhuma
             * varredura nossa alcança isso: só quem vê a requisição passar.
             * No navegador quem vê é a extensão; aqui é esta função, por onde
             * passa cada pedido que a WebView faz.
             *
             * Devolve null de propósito: não interferimos no pedido, apenas
             * anotamos o endereço de passagem.
             */
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()
                if (url != null && pareceVideo(url)) entregarLinkAoSite(url)
                return null
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

    /**
     * Cursor de controle remoto.
     *
     * Dentro do player de video nao ha botao que o D-pad alcance: o play, a
     * barra de tempo e a tela cheia sao desenhados pelo site e so respondem a
     * toque. Da pagina nao da para mandar um clique para la, porque o player
     * vem de outro endereco e o navegador proibe. Aqui no app o caminho e
     * outro: o toque e despachado pelo Android para dentro da WebView, e
     * evento nativo atravessa essa barreira.
     *
     * Liga e desliga no botao Menu do controle. Com ele ligado, as setas
     * movem a seta na tela, o centro vira um toque naquele ponto e o Voltar
     * sai do modo cursor antes de sair da tela.
     */
    /** Endereços já entregues, para o mesmo link não chegar duas vezes. */
    private val linksJaVistos = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * É um arquivo de vídeo? Olhamos o caminho, não a query: a assinatura e a
     * validade vêm depois do "?" e mudam a cada pedido. Os pedaços (.ts) ficam
     * de fora — quem interessa é a lista que os costura.
     */
    private fun pareceVideo(url: String): Boolean {
        val caminho = url.substringBefore("?").lowercase()
        if (caminho.endsWith(".ts")) return false
        return caminho.endsWith(".m3u8") ||
            caminho.endsWith(".mp4") ||
            caminho.endsWith(".mkv") ||
            caminho.endsWith(".webm") ||
            caminho.endsWith(".mpd")
    }

    /**
     * Entrega o link à nossa página, na mesma forma que a extensão usa no
     * computador. Assim tudo o que o site já faz com o link capturado — medir
     * a duração, aplicar o mínimo de 15 minutos, oferecer o nosso player —
     * vale igual aqui, sem repetir nada.
     */
    private fun entregarLinkAoSite(url: String) {
        if (!linksJaVistos.add(url)) return
        val ehHls = url.substringBefore("?").lowercase().endsWith(".m3u8")
        val script =
            "window.postMessage({ source: 'baixar-video-drama', video: { url: " +
                org.json.JSONObject.quote(url) +
                ", title: document.title || 'Link capturado no app', isHls: " +
                ehHls +
                " } }, location.origin);"
        runOnUiThread {
            if (!isFinishing) webView.evaluateJavascript(script, null)
        }
    }

    private fun alternarCursor(ligar: Boolean) {
        cursorLigado = ligar
        cursor.visibility = if (ligar) View.VISIBLE else View.GONE
        if (ligar) {
            cursor.centralizar()
            Toast.makeText(
                this,
                "Cursor ligado: setas movem, centro clica, Voltar desliga",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Cursor desligado", Toast.LENGTH_SHORT).show()
        }
        passoDoCursor = 18f
    }

    /** Despacha um toque de verdade na posicao da seta. */
    private fun tocarOndeEstaOCursor() {
        val agora = android.os.SystemClock.uptimeMillis()
        val x = cursor.cursorX
        val y = cursor.cursorY
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { acao ->
            val evento = MotionEvent.obtain(agora, agora + if (acao == MotionEvent.ACTION_UP) 60 else 0, acao, x, y, 0)
            webView.dispatchTouchEvent(evento)
            evento.recycle()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            if (event.action == KeyEvent.ACTION_UP) passoDoCursor = 18f
            return super.dispatchKeyEvent(event)
        }

        // Muitos controles de TV Box nao tem a tecla Menu. O segurar do botao
        // central existe em todos, e e o que liga e desliga o cursor.
        val centro = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER
        if (event.keyCode == KeyEvent.KEYCODE_MENU ||
            (centro && event.repeatCount == 1)
        ) {
            alternarCursor(!cursorLigado)
            return true
        }

        if (!cursorLigado) return super.dispatchKeyEvent(event)

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> cursor.mover(-passoDoCursor, 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> cursor.mover(passoDoCursor, 0f)
            KeyEvent.KEYCODE_DPAD_UP -> cursor.mover(0f, -passoDoCursor)
            KeyEvent.KEYCODE_DPAD_DOWN -> cursor.mover(0f, passoDoCursor)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                tocarOndeEstaOCursor()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                alternarCursor(false)
                return true
            }
            else -> return super.dispatchKeyEvent(event)
        }

        // Segurando a seta, a seta anda cada vez mais rapido: atravessar a
        // tela de 18 em 18 pixels seria uma viagem.
        passoDoCursor = (passoDoCursor * 1.18f).coerceAtMost(90f)
        return true
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
