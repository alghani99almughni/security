package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.util.CameraManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VehicleScannerScreen(
  onVehicleScanned: (bitmap: Bitmap, photoUri: String, timestamp: String, plateNumber: String, base64Payload: String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  val cameraManager = remember { CameraManager(context) }
  var isTorchOn by remember { mutableStateOf(false) }
  var isCapturing by remember { mutableStateOf(false) }
  var capturedData by remember { mutableStateOf<CameraManager.CapturedVehicleData?>(null) }
  var confirmedPlateNumber by remember { mutableStateOf("") }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

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

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: android.net.Uri? ->
    uri?.let {
      try {
        val inputStream = context.contentResolver.openInputStream(it)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        if (bitmap != null) {
          isCapturing = true
          coroutineScope.launch(Dispatchers.Default) {
            val ocr = com.example.util.PlateOcrProcessor.recognizePlateFromBitmap(bitmap)
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            withContext(Dispatchers.Main) {
              isCapturing = false
              capturedData = CameraManager.CapturedVehicleData(
                bitmap = bitmap,
                fileUri = it.toString(),
                timestamp = timestamp,
                base64Payload = base64,
                estimatedPlate = ocr.detectedPlate,
                ocrMethod = ocr.method,
                ocrConfidence = ocr.confidence
              )
              confirmedPlateNumber = ocr.detectedPlate
            }
          }
        }
      } catch (e: Exception) {
        isCapturing = false
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      cameraManager.release()
    }
  }

  Scaffold(
    containerColor = Color.Black
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (!hasCameraPermission) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Emerald400,
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Camera access is needed to capture vehicle plate snapshots and verify arrivals in real time.",
            color = Slate300,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Grant Camera Permission")
          }
          Spacer(modifier = Modifier.height(12.dp))
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = Slate400)
          }
        }
      } else {
        // CameraX Live Preview View
        AndroidView(
          factory = { ctx ->
            PreviewView(ctx).apply {
              previewViewRef = this
              scaleType = PreviewView.ScaleType.FILL_CENTER
            }
          },
          update = { pView ->
            previewViewRef = pView
          },
          modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(hasCameraPermission, previewViewRef) {
          if (hasCameraPermission && previewViewRef != null) {
            cameraManager.getCameraProvider()
            cameraManager.bindCamera(
              lifecycleOwner = lifecycleOwner,
              previewView = previewViewRef!!
            )
          }
        }

        // Live Optical Overlay Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
          val canvasWidth = size.width
          val canvasHeight = size.height
          val reticleWidth = canvasWidth * 0.85f
          val reticleHeight = reticleWidth * 0.45f
          val left = (canvasWidth - reticleWidth) / 2f
          val top = (canvasHeight - reticleHeight) / 2f - 40.dp.toPx()

          // Darkened bounding vignette
          drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(left, top),
            size = Size(reticleWidth, reticleHeight),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
          )
        }

        // Top Header Controls
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .align(Alignment.TopCenter),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.6f))
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF16A34A))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("CAMERAX LIVE VIEW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Torch toggle
            IconButton(
              onClick = {
                isTorchOn = !isTorchOn
                cameraManager.setTorch(isTorchOn)
              },
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isTorchOn) Amber500 else Color.Black.copy(alpha = 0.6f))
            ) {
              Icon(
                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flashlight",
                tint = if (isTorchOn) Slate900 else Color.White
              )
            }

            // Lens toggle
            IconButton(
              onClick = {
                previewViewRef?.let {
                  cameraManager.toggleCamera(lifecycleOwner, it)
                }
              },
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
            ) {
              Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
            }
          }
        }

        // Bottom Capture Controls or Review Card
        if (capturedData == null) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.Black.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 20.dp)
            ) {
              Text(
                text = "Align vehicle license plate inside the green frame",
                color = Slate200,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
              )
            }

            // Capture and Photo Picker Row
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              // Pick Image from Gallery for OCR
              IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.6f))
                  .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick Image for OCR", tint = Color.White)
              }

              // Big Capture Button with CameraX
              Box(
                modifier = Modifier
                  .size(80.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.2f))
                  .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Button(
                  onClick = {
                    isCapturing = true
                    cameraManager.captureVehicleImage(
                      onSuccess = { data ->
                        isCapturing = false
                        capturedData = data
                        confirmedPlateNumber = data.estimatedPlate
                      },
                      onError = {
                        isCapturing = false
                      }
                    )
                  },
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                  modifier = Modifier
                    .size(64.dp)
                    .testTag("vehicle_capture_button")
                ) {
                  if (isCapturing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                  } else {
                    Icon(Icons.Default.Camera, contentDescription = "Capture Plate", tint = Color.White, modifier = Modifier.size(30.dp))
                  }
                }
              }

              // OCR Engine indicator icon
              Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
                modifier = Modifier.size(48.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.DocumentScanner, contentDescription = "OCR Active", tint = Emerald400, modifier = Modifier.size(22.dp))
                }
              }
            }
          }
        } else {
          // Captured Image Preview & API Upload Preparation Card
          val data = capturedData!!
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500),
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(16.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Vehicle Plate Captured", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Slate800
                ) {
                  Text(
                    text = data.timestamp,
                    color = Slate300,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(modifier = Modifier.fillMaxWidth()) {
                // Bitmap Preview thumbnail
                Image(
                  bitmap = data.bitmap.asImageBitmap(),
                  contentDescription = "Captured Plate",
                  modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  OutlinedTextField(
                    value = confirmedPlateNumber,
                    onValueChange = { confirmedPlateNumber = it.uppercase() },
                    label = { Text("Plate Number", fontSize = 11.sp, color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White,
                      focusedBorderColor = Emerald400,
                      unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("confirmed_plate_input")
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      color = Emerald500.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "OCR: ${data.ocrMethod}",
                        fontSize = 9.sp,
                        color = Emerald400,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "${(data.ocrConfidence * 100).toInt()}% match",
                      fontSize = 10.sp,
                      color = Slate400
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Payload: ${data.base64Payload.take(16)}... (Ready)",
                    fontSize = 9.sp,
                    color = Slate500
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = { capturedData = null },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Retake", fontSize = 12.sp)
                }

                Button(
                  onClick = {
                    onVehicleScanned(
                      data.bitmap,
                      data.fileUri,
                      data.timestamp,
                      confirmedPlateNumber.ifBlank { data.estimatedPlate },
                      data.base64Payload
                    )
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1.6f)
                    .testTag("confirm_vehicle_log_button")
                ) {
                  Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Save & Log Entry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
