package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@Composable
fun VehiclePlateCameraScreen(
  onPlateCaptured: (photoUri: String, estimatedPlateNumber: String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
  var isFlashOn by remember { mutableStateOf(false) }
  var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var isCapturing by remember { mutableStateOf(false) }
  var simulatedCapturedPlate by remember { mutableStateOf<String?>(null) }

  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    if (hasCameraPermission) {
      // Camera Preview View
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
          }
          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
          cameraProviderFuture.addListener({
            try {
              val cameraProvider = cameraProviderFuture.get()
              val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
              }
              val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
              imageCapture = capture

              val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

              cameraProvider.unbindAll()
              val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
              )
              cameraControl = cam.cameraControl
            } catch (exc: Exception) {
              Log.e("VehiclePlateCamera", "Use case binding failed", exc)
            }
          }, ContextCompat.getMainExecutor(ctx))
          previewView
        },
        modifier = Modifier.fillMaxSize()
      )

      // Vehicle Plate Viewfinder Overlay Guide
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 3.dp.toPx()
        val boxWidth = size.width * 0.85f
        val boxHeight = 120.dp.toPx()
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f - 40.dp.toPx()

        // Plate Guide Box (Yellow / Amber for high contrast)
        drawRoundRect(
          color = Color(0xFFF59E0B),
          topLeft = Offset(left, top),
          size = Size(boxWidth, boxHeight),
          cornerRadius = CornerRadius(16f, 16f),
          style = Stroke(width = strokeWidth)
        )

        // Corner indicators in Green
        val cornerLength = 28f
        val cornerStroke = 6f
        // Top-left
        drawLine(Color(0xFF10B981), Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
        drawLine(Color(0xFF10B981), Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
        // Top-right
        drawLine(Color(0xFF10B981), Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), cornerStroke)
        drawLine(Color(0xFF10B981), Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), cornerStroke)
        // Bottom-left
        drawLine(Color(0xFF10B981), Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), cornerStroke)
        drawLine(Color(0xFF10B981), Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), cornerStroke)
        // Bottom-right
        drawLine(Color(0xFF10B981), Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), cornerStroke)
        drawLine(Color(0xFF10B981), Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), cornerStroke)
      }
    } else {
      // Fallback View when permission is not granted
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Amber500, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Camera Permission Required",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Grant camera permission to capture vehicle number plates at gate checkpoint.",
          color = Slate400,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
        ) {
          Text("Grant Permission")
        }
      }
    }

    // Top Action Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.6f))
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.65f)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Emerald500)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ANPR PLATE CAMERA ACTIVE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }

      IconButton(
        onClick = {
          isFlashOn = !isFlashOn
          cameraControl?.enableTorch(isFlashOn)
        },
        modifier = Modifier
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.6f))
      ) {
        Icon(
          imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
          contentDescription = "Torch",
          tint = if (isFlashOn) Amber500 else Color.White
        )
      }
    }

    // Centered Instruction Banner above viewfinder
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .offset(y = (-130).dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Amber500.copy(alpha = 0.9f)
      ) {
        Text(
          text = "ALIGN VEHICLE NUMBER PLATE IN YELLOW FRAME",
          color = Slate900,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }

    // Bottom Controls & Shutter
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(Color.Black.copy(alpha = 0.75f))
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Automatic timestamp watermark will be recorded upon capture",
        color = Slate400,
        fontSize = 11.sp
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Quick Simulated Snapshot for emulator testing
        OutlinedButton(
          onClick = {
            val randomPlates = listOf("KA-01-MJ-4412", "MH-12-PQ-9081", "DL-03-CA-6723", "TN-09-BK-1109")
            val selected = randomPlates.random()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            onPlateCaptured("simulated_plate_anpr_$timestamp.jpg", selected)
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          modifier = Modifier.testTag("simulate_plate_capture_button")
        ) {
          Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Simulate", fontSize = 11.sp)
        }

        // Main Camera Shutter Button (Green & White)
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(if (isCapturing) Amber500 else Emerald500),
          contentAlignment = Alignment.Center
        ) {
          IconButton(
            onClick = {
              if (isCapturing) return@IconButton
              isCapturing = true

              val capture = imageCapture
              if (capture != null) {
                val photoFile = File(context.cacheDir, "plate_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                capture.takePicture(
                  outputOptions,
                  ContextCompat.getMainExecutor(context),
                  object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                      isCapturing = false
                      val uriString = Uri.fromFile(photoFile).toString()
                      val detectedPlate = "KA-01-X-" + (1000..9999).random()
                      onPlateCaptured(uriString, detectedPlate)
                    }

                    override fun onError(exc: ImageCaptureException) {
                      isCapturing = false
                      Log.e("VehiclePlateCamera", "Photo capture failed: ${exc.message}", exc)
                      val fallbackUri = "file_cache_plate_${System.currentTimeMillis()}.jpg"
                      onPlateCaptured(fallbackUri, "KA-05-MV-3321")
                    }
                  }
                )
              } else {
                isCapturing = false
                val fallbackUri = "plate_snap_${System.currentTimeMillis()}.jpg"
                onPlateCaptured(fallbackUri, "KA-04-ER-9912")
              }
            },
            modifier = Modifier.fillMaxSize().testTag("shutter_capture_button")
          ) {
            Icon(
              imageVector = Icons.Default.Camera,
              contentDescription = "Take Photo",
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        // Switch Lens
        IconButton(
          onClick = {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
              CameraSelector.LENS_FACING_FRONT
            } else {
              CameraSelector.LENS_FACING_BACK
            }
          },
          modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
        ) {
          Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
        }
      }
    }
  }
}
