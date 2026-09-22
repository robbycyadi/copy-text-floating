package com.copytext.floating

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.copytext.floating.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectionManager: MediaProjectionManager

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissions() }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Simpan izin MediaProjection ke service
            ProjectionHolder.resultCode = result.resultCode
            ProjectionHolder.data = result.data
            Toast.makeText(this, "Izin screenshot OK! Sekarang aktifkan bubble.", Toast.LENGTH_SHORT).show()
            checkPermissions()
        } else {
            Toast.makeText(this, "Izin screenshot ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectionManager = getSystemService(MediaProjectionManager::class.java)

        binding.btnOverlay.setOnClickListener { requestOverlayPermission() }
        binding.btnProjection.setOnClickListener { requestProjectionPermission() }
        binding.btnToggle.setOnClickListener { toggleService() }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Izin overlay sudah aktif", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestProjectionPermission() {
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun checkPermissions() {
        val overlayOk = Settings.canDrawOverlays(this)
        val projectionOk = ProjectionHolder.data != null

        binding.btnOverlay.isEnabled = !overlayOk
        binding.btnOverlay.text = if (overlayOk) "✓ Izin Overlay OK" else "1. Beri Izin Overlay"
        binding.btnProjection.isEnabled = !projectionOk
        binding.btnProjection.text = if (projectionOk) "✓ Izin Screenshot OK" else "2. Beri Izin Screenshot"

        val serviceRunning = FloatingService.isRunning
        binding.btnToggle.text = if (serviceRunning) "Matikan Bubble" else "Aktifkan Bubble"
        binding.txtStatus.text = when {
            !overlayOk -> "Status: Butuh izin overlay"
            !projectionOk -> "Status: Butuh izin screenshot (tap tombol 2)"
            serviceRunning -> "Status: Bubble AKTIF — cek layar, ada tombol T COPY melayang"
            else -> "Status: Siap — tap Aktifkan Bubble"
        }
        binding.btnToggle.isEnabled = overlayOk
    }

    private fun toggleService() {
        if (FloatingService.isRunning) {
            stopService(Intent(this, FloatingService::class.java))
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Beri izin overlay dulu!", Toast.LENGTH_SHORT).show()
                return
            }
            if (ProjectionHolder.data == null) {
                Toast.makeText(this, "Beri izin screenshot dulu!", Toast.LENGTH_SHORT).show()
                requestProjectionPermission()
                return
            }
            startForegroundService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Bubble aktif! Minimalkan app ini.", Toast.LENGTH_LONG).show()
        }
        // delay cek status
        binding.root.postDelayed({ checkPermissions() }, 500)
    }
}

// Singleton untuk menyimpan izin MediaProjection (harus survive antar activity)
object ProjectionHolder {
    var resultCode: Int = 0
    var data: Intent? = null
}
