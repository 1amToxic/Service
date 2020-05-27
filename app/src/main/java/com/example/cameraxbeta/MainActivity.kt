package com.example.cameraxbeta

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.solver.widgets.Analyzer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
typealias LumaListener = (luma: Double) -> Unit
class MainActivity : AppCompatActivity() {
    var preview : Preview? = null
    lateinit var fabCapture : FloatingActionButton
    lateinit var imageCapture: ImageCapture
    lateinit var imageAnalyzer: ImageAnalysis
    lateinit var camera : Camera
    lateinit var outputDirectory : File
    lateinit var cameraExecutor : ExecutorService
    var LENS_FACING_BACK : Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        fabCapture.setOnClickListener{
            takePhoto()
        }
        fab_back.setOnClickListener{
            LENS_FACING_BACK = !LENS_FACING_BACK
            startCamera()
        }
        outputDirectory = getOutputDirectoryz()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()
            // Capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera
            var cameraSelector : CameraSelector
            if(LENS_FACING_BACK) {
                 cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
            }
            else{
                cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
            }
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,imageCapture,imageAnalyzer)

                preview?.setSurfaceProvider(preview_cam.createSurfaceProvider(camera.cameraInfo))
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))


    }
    private fun takePhoto(){
        val imageCapture = imageCapture

        // Create timestamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    image_pre.setImageURI(savedUri)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }
    private fun allPermissionsGranted()  = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun getOutputDirectoryz(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(allPermissionsGranted()){
            startCamera()
        }
        else{
            Toast.makeText(this,"Permission was not granted by user",Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun init(){
        fabCapture = findViewById(R.id.fab_capture)
    }
    companion object{
        private const val TAG = "CameraXBeta"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        listener(luma)

        image.close()
    }
}