package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * CameraManager utility for CameraX lifecycle binding, preview management,
 * flash control, image capture, bitmap transformation, and data preparation for API upload.
 */
class CameraManager(private val context: Context) {

  private var cameraProvider: ProcessCameraProvider? = null
  private var imageCapture: ImageCapture? = null
  private var camera: Camera? = null
  private var preview: Preview? = null

  var isTorchOn: Boolean = false
    private set

  var currentLensFacing: Int = CameraSelector.LENS_FACING_BACK
    private set

  /**
   * Suspended initialization of the ProcessCameraProvider.
   */
  suspend fun getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
      val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
      cameraProviderFuture.addListener(
        {
          try {
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            continuation.resume(provider)
          } catch (e: Exception) {
            Log.e("CameraManager", "Failed to obtain ProcessCameraProvider", e)
            continuation.resumeWithException(e)
          }
        },
        ContextCompat.getMainExecutor(context)
      )
    }

  /**
   * Binds live preview and image capture use cases to the provided LifecycleOwner and PreviewView.
   */
  fun bindCamera(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    onError: (Exception) -> Unit = {}
  ) {
    val provider = cameraProvider ?: return
    try {
      provider.unbindAll()

      currentLensFacing = lensFacing
      val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

      preview = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()
        .also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }

      imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

      camera = provider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageCapture
      )
    } catch (e: Exception) {
      Log.e("CameraManager", "Binding camera use cases failed", e)
      onError(e)
    }
  }

  /**
   * Toggles the torch / flashlight state.
   */
  fun setTorch(enable: Boolean) {
    camera?.let {
      if (it.cameraInfo.hasFlashUnit()) {
        it.cameraControl.enableTorch(enable)
        isTorchOn = enable
      }
    }
  }

  /**
   * Toggle between back and front camera.
   */
  fun toggleCamera(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onError: (Exception) -> Unit = {}
  ) {
    val newLens = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }
    bindCamera(lifecycleOwner, previewView, newLens, onError)
  }

  /**
   * Result model containing captured bitmap, local URI, timestamp, base64 payload ready for API upload.
   */
  data class CapturedVehicleData(
    val bitmap: Bitmap,
    val fileUri: String,
    val timestamp: String,
    val base64Payload: String,
    val estimatedPlate: String,
    val ocrMethod: String = "OCR_SCAN",
    val ocrConfidence: Float = 0.95f
  )

  /**
   * Captures an image, stores bitmap, timestamps it, and prepares data for API upload.
   */
  fun captureVehicleImage(
    executor: Executor = ContextCompat.getMainExecutor(context),
    onSuccess: (CapturedVehicleData) -> Unit,
    onError: (ImageCaptureException) -> Unit
  ) {
    val capture = imageCapture ?: run {
      onError(ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED, "ImageCapture not initialized", null))
      return
    }

    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val filename = "VEHICLE_${System.currentTimeMillis()}.jpg"
    val photoFile = File(context.cacheDir, filename)

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
      outputOptions,
      executor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          try {
            // Load bitmap and rotate correctly
            val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            val matrix = Matrix()
            // Orient if needed
            val rotatedBitmap = Bitmap.createBitmap(
              rawBitmap,
              0,
              0,
              rawBitmap.width,
              rawBitmap.height,
              matrix,
              true
            )

            // Prepare base64 for API upload (scaled down for low latency transfer)
            val scaledWidth = 800
            val scaledHeight = (rotatedBitmap.height * (800f / rotatedBitmap.width)).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, scaledWidth, scaledHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            // Perform OCR Recognition using PlateOcrProcessor
            CoroutineScope(Dispatchers.Default).launch {
              val ocrResult = PlateOcrProcessor.recognizePlateFromBitmap(scaledBitmap)
              val detectedPlate = ocrResult.detectedPlate

              withContext(Dispatchers.Main) {
                onSuccess(
                  CapturedVehicleData(
                    bitmap = rotatedBitmap,
                    fileUri = Uri.fromFile(photoFile).toString(),
                    timestamp = timestamp,
                    base64Payload = base64Data,
                    estimatedPlate = detectedPlate,
                    ocrMethod = ocrResult.method,
                    ocrConfidence = ocrResult.confidence
                  )
                )
              }
            }
          } catch (e: Exception) {
            Log.e("CameraManager", "Error processing captured vehicle photo", e)
            onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Bitmap processing failed: ${e.message}", e))
          }
        }

        override fun onError(exception: ImageCaptureException) {
          Log.e("CameraManager", "Photo capture failed", exception)
          onError(exception)
        }
      }
    )
  }

  fun release() {
    cameraProvider?.unbindAll()
  }
}
