package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * Seta do "mouse de controle remoto".
 *
 * Dentro do player de vídeo não há botão que o D-pad alcance: o play, a barra
 * de tempo e o botão de tela cheia são desenhados pelo site e só respondem a
 * toque. E, como o player vem de outro endereço, a página não pode mandar um
 * clique para lá — o navegador proíbe um site de mexer no conteúdo de outro.
 *
 * No app esse muro não existe: aqui o clique é um toque de verdade, despachado
 * pelo Android para dentro da WebView. Então desenhamos uma seta por cima,
 * movida pelas setas do controle, e o centro vira um toque naquele ponto.
 */
class CursorVirtual(context: Context) : View(context) {

    /** Posição da seta, em pixels dentro da view. */
    var cursorX = 0f
        private set
    var cursorY = 0f
        private set

    private val miolo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 160
    }

    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    init {
        isFocusable = false
        isClickable = false
        visibility = GONE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cursorX == 0f && cursorY == 0f) centralizar()
    }

    fun centralizar() {
        cursorX = width / 2f
        cursorY = height / 2f
        invalidate()
    }

    /** Move a seta, sem deixá-la escapar da tela. */
    fun mover(dx: Float, dy: Float) {
        cursorX = (cursorX + dx).coerceIn(0f, width.toFloat())
        cursorY = (cursorY + dy).coerceIn(0f, height.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Um ponto claro com contorno escuro: aparece tanto sobre cena clara
        // quanto sobre cena escura, que é o caso de um filme.
        canvas.drawCircle(cursorX, cursorY, 11f, miolo)
        canvas.drawCircle(cursorX, cursorY, 11f, contorno)
        canvas.drawCircle(cursorX, cursorY, 20f, halo)
    }
}
