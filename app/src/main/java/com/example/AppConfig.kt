package com.example

/**
 * Configuração central do app. A URL do sistema web e o user agent do WebView
 * ficam aqui para não haver cópias espalhadas pelo código.
 */
object AppConfig {

    /** Base do sistema web (sem barra no final). */
    const val WEB_BASE_URL = "https://baixar-video-drama-web-uy33.vercel.app"

    /** Página inicial do sistema web. */
    const val WEB_HOME_URL = "$WEB_BASE_URL/"

    /**
     * User agent de Chrome em desktop Linux. Alguns players entregam um caminho
     * de reprodução diferente conforme o user agent.
     */
    const val USER_AGENT_CHROME_DESKTOP =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"

    /**
     * User agent de Chrome em Android. Trocar [WEBVIEW_USER_AGENT] para este
     * valor é o teste a fazer quando o player continuar preto: o site pode
     * servir um player que o WebView da TV não consegue executar no caminho
     * de desktop.
     */
    const val USER_AGENT_CHROME_ANDROID =
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    /** User agent realmente usado pelo WebView — mude só aqui. */
    const val WEBVIEW_USER_AGENT = USER_AGENT_CHROME_DESKTOP
}
