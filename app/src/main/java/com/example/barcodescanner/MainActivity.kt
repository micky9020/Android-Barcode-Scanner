package com.example.barcodescanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var analyzer: BarcodeAnalyzer? = null
    private var camera: Camera? = null
    private var torchOn = false

    private var current: ScanResult? = null
    private var lastValue: String? = null
    private var lastTime = 0L
    private val history = mutableListOf<ScanResult>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else showPermissionDenied()
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { scanFromImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCopy.setOnClickListener { copy() }
        binding.btnShare.setOnClickListener { share() }
        binding.btnOpen.setOnClickListener { openUrl() }
        binding.btnTorch.setOnClickListener { toggleTorch() }
        binding.btnHistory.setOnClickListener { showHistory() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnGrant.setOnClickListener { permissionLauncher.launch(Manifest.permission.CAMERA) }

        showResult(null)

        if (hasCameraPermission()) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        binding.permissionPanel.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------- Cámara

    private fun startCamera() {
        binding.permissionPanel.visibility = View.GONE
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            analyzer = BarcodeAnalyzer { onScanned(it) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer!!) }

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
                binding.btnTorch.isEnabled = camera?.cameraInfo?.hasFlashUnit() == true
            } catch (e: Exception) {
                Toast.makeText(this, R.string.camera_error, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        camera?.cameraControl?.enableTorch(torchOn)
        binding.btnTorch.setText(if (torchOn) R.string.torch_off else R.string.torch_on)
    }

    // ------------------------------------------------------------- Resultados

    private fun onScanned(result: ScanResult) {
        val now = System.currentTimeMillis()
        // Evita repetir el mismo código mientras sigue frente a la cámara
        if (result.value == lastValue && now - lastTime < 2000) {
            lastTime = now
            return
        }
        lastValue = result.value
        lastTime = now

        history.add(0, result)
        showResult(result)
        vibrate()
    }

    private fun showResult(result: ScanResult?) {
        current = result
        val has = result != null
        binding.tvResult.text = result?.value ?: getString(R.string.point_camera)
        binding.tvFormat.text = result?.let { "${it.format} · ${it.timeLabel}" }.orEmpty()
        binding.tvFormat.visibility = if (has) View.VISIBLE else View.GONE
        binding.btnCopy.isEnabled = has
        binding.btnShare.isEnabled = has
        binding.btnOpen.visibility = if (result?.isUrl == true) View.VISIBLE else View.GONE
    }

    private fun scanFromImage(uri: Uri) {
        val image = try {
            InputImage.fromFilePath(this, uri)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.image_error, Toast.LENGTH_SHORT).show()
            return
        }
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { list ->
                val barcode = list.firstOrNull { !it.rawValue.isNullOrEmpty() }
                if (barcode == null) {
                    Toast.makeText(this, R.string.no_code_found, Toast.LENGTH_SHORT).show()
                } else {
                    val r = ScanResult.from(barcode)
                    history.add(0, r)
                    showResult(r)
                    vibrate()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.image_error, Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { scanner.close() }
    }

    // ---------------------------------------------------------------- Acciones

    private fun copy() {
        val value = current?.value ?: return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("barcode", value))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun share() {
        val value = current?.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun openUrl() {
        val value = current?.value ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, R.string.history_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val items = history.map { "${it.timeLabel}  [${it.format}]\n${it.value}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.history)
            .setItems(items) { _, which -> showResult(history[which]) }
            .setNegativeButton(R.string.close, null)
            .setNeutralButton(R.string.clear) { _, _ -> history.clear() }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        analyzer?.close()
        cameraExecutor.shutdown()
    }
}
