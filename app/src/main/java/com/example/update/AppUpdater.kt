package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.AppConfig
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Atualização do próprio app.
 *
 * O APK mais novo mora sempre no mesmo endereço: a release `apk-latest` do
 * repositório. Em vez de obrigar a pessoa a ir até o GitHub pelo navegador —
 * coisa penosa num controle de TV Box —, o app pergunta ao GitHub se o arquivo
 * de lá é mais recente que a build instalada e, sendo, baixa e chama o
 * instalador do Android.
 *
 * A comparação é por data: a build carimba [BuildConfig.BUILD_TIME] e a release
 * informa quando o APK foi enviado. Mais novo lá que aqui quer dizer que há
 * versão nova.
 */
object AppUpdater {

    /** Folga para não confundir o próprio arquivo que gerou esta build. */
    private const val MARGEM_MS = 5 * 60 * 1000L

    data class Disponivel(val url: String, val publicadoEm: Long)

    /**
     * Pergunta ao GitHub se há APK mais novo. Devolve null quando já estamos na
     * versão mais recente, quando não há rede ou quando a resposta não veio no
     * formato esperado — em nenhum desses casos vale incomodar a pessoa.
     */
    suspend fun procurarAtualizacao(): Disponivel? = withContext(Dispatchers.IO) {
        try {
            val conexao = (URL(AppConfig.RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "meufilme-app")
            }
            if (conexao.responseCode !in 200..299) return@withContext null

            val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
            val assets = JSONObject(corpo).optJSONArray("assets") ?: return@withContext null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") != AppConfig.APK_ASSET_NAME) continue

                val url = asset.optString("browser_download_url").ifBlank { return@withContext null }
                val publicado = paraMillis(asset.optString("updated_at")) ?: return@withContext null
                return@withContext if (publicado > BuildConfig.BUILD_TIME + MARGEM_MS) {
                    Disponivel(url, publicado)
                } else {
                    null
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Baixa o APK e, terminando, abre o instalador do Android. */
    fun baixarEInstalar(context: Context, url: String) {
        val destino = File(context.getExternalFilesDir(null), AppConfig.APK_ASSET_NAME)
        if (destino.exists()) destino.delete()

        val pedido = DownloadManager.Request(Uri.parse(url))
            .setTitle("Atualização do app")
            .setDescription("Baixando a versão nova…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destino))

        val gerenciador = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = gerenciador.enqueue(pedido)

        val receptor = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
                context.unregisterReceiver(this)
                instalar(context, destino)
            }
        }

        val filtro = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receptor, filtro, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receptor, filtro)
        }

        Toast.makeText(context, "Baixando a atualização…", Toast.LENGTH_SHORT).show()
    }

    private fun instalar(context: Context, arquivo: File) {
        if (!arquivo.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não deu para abrir o instalador.", Toast.LENGTH_LONG).show()
        }
    }

    /** Converte a data ISO do GitHub (sempre em UTC) para milissegundos. */
    private fun paraMillis(iso: String): Long? {
        if (iso.isBlank()) return null
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formato.timeZone = TimeZone.getTimeZone("UTC")
            formato.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }
}
