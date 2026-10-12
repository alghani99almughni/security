package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
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
import com.example.model.Ticket
import com.example.ui.theme.*
import com.example.util.FeedbackHelper
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun CameraQrScannerScreen(
  title: String = "QR CODE SCANNER CHECKPOINT",
  subtitle: String = "Align Gate Pass QR within viewfinder",
  tickets: List<Ticket> = emptyList(),
  onQrScanned: (tokenOrTicketId: String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val feedbackHelper = remember { FeedbackHelper(context) }

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

  var isFlashOn by remember { mutableStateOf(false) }
  var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
  var hasDetected by remember { mutableStateOf(false) }
  var manualInputText by remember { mutableStateOf("") }
  var showManualInput by remember { mutableStateOf(false) }

  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
    }
  }

  // Scanning laser line animation
  val infiniteTransition = rememberInfiniteTransition(label = "laser")
  val scanLineProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_progress"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    if (hasCameraPermission) {
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

              val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

              val multiFormatReader = MultiFormatReader().apply {
                val hints = mapOf(
                  DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                  DecodeHintType.TRY_HARDER to true
                )
                setHints(hints)
              }

              analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                if (hasDetected) {
                  imageProxy.close()
                  return@setAnalyzer
                }
                try {
                  val plane = imageProxy.planes[0]
                  val buffer = plane.buffer
                  val bytes = ByteArray(buffer.remaining())
                  buffer.get(bytes)
                  val width = imageProxy.width
                  val height = imageProxy.height

                  val source = PlanarYUVLuminanceSource(
                    bytes, width, height, 0, 0, width, height, false
                  )
                  val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                  val result = multiFormatReader.decodeWithState(binaryBitmap)
                  result?.text?.let { qrText ->
                    hasDetected = true
                    ContextCompat.getMainExecutor(ctx).execute {
                      feedbackHelper.triggerScanSuccessFeedback()
                      onQrScanned(qrText)
                    }
                  }
                } catch (_: Exception) {
                  // Frame did not contain a valid QR code
                } finally {
                  multiFormatReader.reset()
                  imageProxy.close()
                }
              }

              val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
              cameraProvider.unbindAll()
              val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analyzer
              )
              cameraControl = cam.cameraControl
            } catch (e: Exception) {
              Log.e("CameraQrScanner", "Camera binding failed", e)
            }
          }, ContextCompat.getMainExecutor(ctx))
          previewView
        },
        modifier = Modifier.fillMaxSize()
      )

      // QR Reticle Overlay
      Canvas(modifier = Modifier.fillMaxSize()) {
        val reticleSize = size.width * 0.72f
        val left = (size.width - reticleSize) / 2f
        val top = (size.height - reticleSize) / 2f - 30.dp.toPx()

        // Semi-transparent darkened background around reticle
        drawRect(
          color = Color.Black.copy(alpha = 0.5f),
          topLeft = Offset.Zero,
          size = size
        )

        // Clear square for QR reticle
        drawRoundRect(
          color = Color.Transparent,
          topLeft = Offset(left, top),
          size = Size(reticleSize, reticleSize),
          cornerRadius = CornerRadius(24f, 24f)
        )

        // Bounding box border in Emerald Green
        drawRoundRect(
          color = Color(0xFF10B981),
          topLeft = Offset(left, top),
          size = Size(reticleSize, reticleSize),
          cornerRadius = CornerRadius(24f, 24f),
          style = Stroke(width = 3.dp.toPx())
        )

        // High-contrast corner brackets (Amber Yellow)
        val bracketLength = 40f
        val bracketStroke = 8f
        val amberColor = Color(0xFFF59E0B)

        // Top Left
        drawLine(amberColor, Offset(left, top), Offset(left + bracketLength, top), bracketStroke)
        drawLine(amberColor, Offset(left, top), Offset(left, top + bracketLength), bracketStroke)
        // Top Right
        drawLine(amberColor, Offset(left + reticleSize, top), Offset(left + reticleSize - bracketLength, top), bracketStroke)
        drawLine(amberColor, Offset(left + reticleSize, top), Offset(left + reticleSize, top + bracketLength), bracketStroke)
        // Bottom Left
        drawLine(amberColor, Offset(left, top + reticleSize), Offset(left + bracketLength, top + reticleSize), bracketStroke)
        drawLine(amberColor, Offset(left, top + reticleSize), Offset(left, top + reticleSize - bracketLength), bracketStroke)
        // Bottom Right
        drawLine(amberColor, Offset(left + reticleSize, top + reticleSize), Offset(left + reticleSize - bracketLength, top + reticleSize), bracketStroke)
        drawLine(amberColor, Offset(left + reticleSize, top + reticleSize), Offset(left + reticleSize, top + reticleSize - bracketLength), bracketStroke)

        // Laser scan line
        val scanY = top + (reticleSize * scanLineProgress)
        drawLine(
          color = Color(0xFFEF4444),
          start = Offset(left + 10f, scanY),
          end = Offset(left + reticleSize - 10f, scanY),
          strokeWidth = 3.dp.toPx()
        )
      }
    } else {
      // Permission request UI
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Emerald400, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Camera Permission Required",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Enable camera to scan visitor Gate Pass QR codes in real-time.",
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

    // Top Header Navigation
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
        Icon(Icons.Default.Close, contentDescription = "Close Scanner", tint = Color.White)
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
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
            text = title,
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

    // Centered instructions
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .offset(y = (-140).dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Emerald600.copy(alpha = 0.9f)
      ) {
        Text(
          text = subtitle.uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }

    // Bottom Quick Pickers / Simulation Chips (so tests and reviews on emulator are instant!)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(Color.Black.copy(alpha = 0.85f))
        .padding(horizontal = 16.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Scan with Camera or Pick Active Pass for Instant Testing",
        color = Slate300,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(10.dp))

      // Active Tickets Chips for 1-tap fast scanning simulation
      if (tickets.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          tickets.take(2).forEach { t ->
            AssistChip(
              onClick = {
                feedbackHelper.triggerScanSuccessFeedback()
                onQrScanned(t.qrToken ?: t.ticketId)
              },
              label = {
                Text(
                  text = "${t.ticketId}: ${t.visitorName.take(14)}",
                  fontSize = 11.sp,
                  color = Color.White
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.QrCode,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(14.dp)
                )
              },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = Slate800
              ),
              modifier = Modifier.weight(1f).testTag("quick_qr_scan_chip_${t.ticketId}")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { showManualInput = !showManualInput },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
          Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Type ID", fontSize = 12.sp)
        }

        Button(
          onClick = {
            feedbackHelper.triggerScanSuccessFeedback()
            onQrScanned("SMARTCHECK-QR-DEMO-APPROVED")
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
          modifier = Modifier.testTag("simulate_valid_qr_button")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Simulate Valid QR", fontSize = 12.sp)
        }
      }

      if (showManualInput) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = manualInputText,
            onValueChange = { manualInputText = it },
            placeholder = { Text("e.g. GP-4021 or QR Token", color = Slate400, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = Slate600
            )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (manualInputText.isNotBlank()) {
                feedbackHelper.triggerScanSuccessFeedback()
                onQrScanned(manualInputText.trim())
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Amber500)
          ) {
            Text("Validate", color = Slate900, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
