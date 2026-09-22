package com.copytext.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class FloatingService : Service() {

    companion object {
        var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var resultView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var resultParams: WindowManager.LayoutParams
    private var isMenuVisible = false
    private var isResultVisible = false

    // For screenshot
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())

        initScreenMetrics()
        initMediaProjection()
        showBubble()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { windowManager.removeView(bubbleView) } catch (_: Exception) {}
        try { if (isResultVisible) windowManager.removeView(resultView) } catch (_: Exception) {}
        releaseProjection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("floating", "Copy Text Floating", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "floating")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Copy Text Floating aktif")
            .setContentText("Tap bubble T COPY untuk scan teks dari gambar")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()
    }

    private fun initScreenMetrics() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    private fun initMediaProjection() {
        val data = ProjectionHolder.data ?: return
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(ProjectionHolder.resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { releaseProjection() }
        }, Handler(Looper.getMainLooper()))
    }

    private fun releaseProjection() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
    }

    private fun showBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_bubble, null)
        val bubble = bubbleView.findViewById<View>(R.id.bubbleView)
        val menu = bubbleView.findViewById<View>(R.id.bubbleMenu)

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        windowManager.addView(bubbleView, bubbleParams)

        // Drag logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isDragging = true
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView.findViewById<View>(R.id.btnScanFull).setOnClickListener {
            toggleMenu()
            captureAndRecognize(fullScreen = true)
        }
        bubbleView.findViewById<View>(R.id.btnScanArea).setOnClickListener {
            toggleMenu()
            // Buka activity transparan untuk pilih area
            val intent = Intent(this, CropActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        bubbleView.findViewById<View>(R.id.btnCloseBubble).setOnClickListener {
            stopSelf()
        }
    }

    private fun toggleMenu() {
        val menu = bubbleView.findViewById<View>(R.id.bubbleMenu)
        isMenuVisible = !isMenuVisible
        menu.visibility = if (isMenuVisible) View.VISIBLE else View.GONE
        // Update layout agar menu bisa tampil (wrap_content perlu refresh)
        try { windowManager.updateViewLayout(bubbleView, bubbleParams) } catch (_: Exception) {}
    }

    // Dipanggil dari CropActivity via broadcast/intent
    fun captureAndRecognize(fullScreen: Boolean, cropRect: android.graphics.Rect? = null) {
        if (mediaProjection == null) {
            initMediaProjection()
            if (mediaProjection == null) {
                Toast.makeText(this, "Izin screenshot belum ada, buka app dulu", Toast.LENGTH_LONG).show()
                return
            }
        }

        showResultLoading()

        // Setup ImageReader
        imageReader?.close()
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        // Tunggu 300ms biar surface siap
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader!!.acquireLatestImage()
                if (image == null) {
                    // retry once
                    Handler(Looper.getMainLooper()).postDelayed({
                        val img2 = imageReader!!.acquireLatestImage()
                        if (img2 != null) processImage(img2, cropRect)
                        else {
                            Toast.makeText(this, "Gagal capture layar", Toast.LENGTH_SHORT).show()
                            hideResult()
                        }
                    }, 400)
                } else {
                    processImage(image, cropRect)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error capture: ${e.message}", Toast.LENGTH_SHORT).show()
                hideResult()
            }
        }, 350)
    }

    private fun processImage(image: android.media.Image, cropRect: android.graphics.Rect?) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth
        val bitmapWidth = screenWidth + rowPadding / pixelStride

        val bitmap = Bitmap.createBitmap(bitmapWidth, screenHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        // Release virtual display segera setelah capture
        virtualDisplay?.release()
        virtualDisplay = null

        // Crop kalau ada
        val finalBitmap = if (cropRect != null) {
            // cropRect dalam koordinat layar (0..screenWidth)
            val safeRect = android.graphics.Rect(
                cropRect.left.coerceIn(0, bitmap.width),
                cropRect.top.coerceIn(0, bitmap.height),
                cropRect.right.coerceIn(0, bitmap.width),
                cropRect.bottom.coerceIn(0, bitmap.height)
            )
            if (safeRect.width() > 20 && safeRect.height() > 20) {
                Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
            } else bitmap
        } else {
            // potong padding jika ada
            if (bitmapWidth != screenWidth) Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            else bitmap
        }

        runOcr(finalBitmap)
    }

    private fun runOcr(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                if (text.isEmpty()) {
                    updateResultText("Tidak ada teks terdeteksi. Coba pilih area yang lebih jelas.")
                } else {
                    // Auto copy
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("OCR Text", text))
                    updateResultText(text)
                    Toast.makeText(this, "✓ Teks tercopy!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                updateResultText("Gagal OCR: ${e.message}")
            }
    }

    // ---- Result overlay ----
    private fun showResultLoading() {
        if (isResultVisible) {
            updateResultText("Mengenali teks…")
            return
        }
        resultView = LayoutInflater.from(this).inflate(R.layout.layout_result, null)
        resultParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        resultView.findViewById<View>(R.id.btnCloseResult).setOnClickListener { hideResult() }
        resultView.findViewById<View>(R.id.btnCopy).setOnClickListener {
            val txt = resultView.findViewById<android.widget.TextView>(R.id.txtResult).text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OCR", txt))
            Toast.makeText(this, "Tercopy!", Toast.LENGTH_SHORT).show()
        }

        windowManager.addView(resultView, resultParams)
        isResultVisible = true
        updateResultText("Mengenali teks…")
    }

    private fun updateResultText(text: String) {
        if (!isResultVisible) return
        resultView.findViewById<android.widget.TextView>(R.id.txtResult).text = text
    }

    private fun hideResult() {
        if (!isResultVisible) return
        try { windowManager.removeView(resultView) } catch (_: Exception) {}
        isResultVisible = false
    }

    // Untuk dipanggil CropActivity
    companion object {
        var instance: FloatingService? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        // handle intent dari CropActivity
        if (intent?.hasExtra("crop_rect") == true) {
            val rect = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra("crop_rect", android.graphics.Rect::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("crop_rect") as? android.graphics.Rect
            }
            captureAndRecognize(fullScreen = false, cropRect = rect)
        }
        return START_STICKY
    }
}
