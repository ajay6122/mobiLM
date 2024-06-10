package com.example.mobilm

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aj.recon.ocr.OCRResultListener
import com.aj.recon.ocr.OCRrecon
import com.example.mobilm.databinding.ActivityMainBinding
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var textToChange: TextView
    private  var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
//    private var textVal = findViewById<TextView>(R.id.reconText)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        textToChange = findViewById(R.id.recognizedText)
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissions()
        }

        viewBinding.imageCaptureButton.setOnClickListener{
            Log.d(TAG, "Capture button clicked")
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val captureButton: Button = findViewById(R.id.image_capture_button)

    }


    private fun startCamera() {
        val cameraProviderFeature = ProcessCameraProvider.getInstance(this)

        cameraProviderFeature.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFeature.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            try {
                val imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture
                )

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, preview
                )

                // Verify if the ImageCapture use case is not null
                if (imageCapture != null) {
                    this.imageCapture = imageCapture
                    Log.d(TAG, "ImageCapture initialized successfully")
                } else {
                    Log.e(TAG, "ImageCapture is null")
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Use Case Binding Failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture is null")
            return
        }
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageCapturedCallback(){
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    // super.onCaptureSuccess(imageProxy)
                    val bitmap = imageProxy.toBitmap()
                    imageProxy.close()
                    Log.d(TAG, "Bitmap captured successfully: $bitmap")
                    // Do OCR Here
                    Log.d(TAG, "Bitmap toString(): ${bitmap.toString()}")
                    OCRrecon().getImage(bitmap, object : OCRResultListener {
                        override fun onOCRSuccess(text: String) {
                            // Update UI or perform further processing with the recognized text
                            textToChange.text = text
                            Log.d(TAG, "OCR Success: $text")
                        }

                        override fun onOCRFailure(exception: Exception) {
                            // Handle OCR failure
                            Log.e(TAG, "OCR Failure", exception)
                        }
                    })
                }

                override fun onError(exception: ImageCaptureException) {
                    // super.onError(exception)
                    Log.e(TAG, "Photo Capture Failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun requestPermissions(){
        activityResultLauncher.launch((REQUIRED_PERMISSIONS))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
        var permissionGranted = true
        permissions.entries.forEach{
            if (it.key in REQUIRED_PERMISSIONS && it.value == false){
                permissionGranted = false
            }
        }
        if (!permissionGranted) {
            Toast.makeText(baseContext,
                "Permission Request Denied",
                Toast.LENGTH_SHORT).show()
        } else {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "TextRecon"
        private const val FILENAME_FORMAT = "yyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
