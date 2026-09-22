package com.copytext.floating

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class CropActivity : AppCompatActivity() {

    private lateinit var overlay: CropOverlayView
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var isDragging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen translucent
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        overlay = CropOverlayView(this)
        setContentView(overlay)

        overlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    endX = startX
                    endY = startY
                    isDragging = true
                    overlay.setRect(startX, startY, endX, endY)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        endX = event.x
                        endY = event.y
                        overlay.setRect(startX, startY, endX, endY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    val left = minOf(startX, endX).toInt()
                    val top = minOf(startY, endY).toInt()
                    val right = maxOf(startX, endX).toInt()
                    val bottom = maxOf(startY, endY).toInt()

                    if (right - left > 50 && bottom - top > 50) {
                        // Kirim ke service
                        val rect = Rect(left, top, right, bottom)
                        val intent = Intent(this, FloatingService::class.java).apply {
                            putExtra("crop_rect", rect)
                        }
                        startService(intent)
                    }
                    finish()
                    // Hilangkan animasi
                    overridePendingTransition(0, 0)
                }
            }
            true
        }

        // Tap di luar untuk cancel
        overlay.setOnClickListener {
            // akan handle via touch, tapi kalau tidak drag -> cancel
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(0, 0)
    }
}

class CropOverlayView(context: android.content.Context) : View(context) {
    private var rect: RectF? = null
    private val dimPaint = Paint().apply { color = 0x88000000.toInt() }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun setRect(x1: Float, y1: Float, x2: Float, y2: Float) {
        rect = RectF(minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Paksa layer untuk CLEAR mode
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // Dim seluruh layar
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        rect?.let {
            // Bolongi area seleksi
            canvas.drawRect(it, clearPaint)
            // Border
            canvas.drawRect(it, borderPaint)
        } ?: run {
            // Hint kalau belum drag
            canvas.drawText("Drag untuk pilih area teks", width / 2f, height / 2f, textPaint)
            val small = Paint(textPaint).apply { textSize = 26f; alpha = 180 }
            canvas.drawText("Lepas untuk scan • Back untuk batal", width / 2f, height / 2f + 40f, small)
        }

        canvas.restoreToCount(save)
    }
}
