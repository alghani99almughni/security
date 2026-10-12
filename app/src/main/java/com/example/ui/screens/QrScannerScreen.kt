package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.Ticket
import com.example.model.TicketStatus
import com.example.ui.theme.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

enum class QrScanResultState {
  IDLE,
  VALIDATING,
  SUCCESS,  // Green - Valid & Approved Pass
  FAILURE,  // Red - Invalid, Rejected, or Expired Pass
  PENDING   // Yellow - Pending Approver Sign-off or Reception Check
}

data class QrValidationOutcome(
  val state: QrScanResultState,
  val message: String,
  val ticket: Ticket? = null,
  val token: String = ""
)

@Composable
fun QrScannerScreen(
  tickets: List<Ticket>,
  onValidateToken: (token: String, onResult: (QrScanResultState, String, Ticket?) -> Unit) -> Unit,
  onAccessGranted: (ticketId: String) -> Unit,
  onManualApprove: ((ticketId: String, approverName: String) -> Unit)? = null,
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

  var isTorchOn by remember { mutableStateOf(false) }
  var cameraControlRef by remember { mutableStateOf<CameraControl?>(null) }
  var validationOutcome by remember { mutableStateOf<QrValidationOutcome?>(null) }
  var isScanningPaused by remember { mutableStateOf(false) }

  // ZXing MultiFormatReader
  val multiFormatReader = remember {
    MultiFormatReader().apply {
      val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128),
        DecodeHintType.TRY_HARDER to true
      )
      setHints(hints)
    }
  }

  val redColor = Color(0xFFDC2626)
  val yellowColor = Color(0xFFEAB308)
  val greenColor = Color(0xFF16A34A)

  val triggerValidation: (String) -> Unit = { rawToken ->
    if (!isScanningPaused && validationOutcome == null) {
      isScanningPaused = true
      validationOutcome = QrValidationOutcome(
        state = QrScanResultState.VALIDATING,
        message = "Validating QR token against backend...",
        token = rawToken
      )

      onValidateToken(rawToken) { state, msg, ticket ->
        validationOutcome = QrValidationOutcome(
          state = state,
          message = msg,
          ticket = ticket,
          token = rawToken
        )
      }
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
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Emerald400,
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text("Camera Permission Needed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Text("Allow camera access to scan visitor QR passes in real time.", color = Slate400, fontSize = 13.sp, textAlign = TextAlign.Center)
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
          ) {
            Text("Grant Permission")
          }
          Spacer(modifier = Modifier.height(12.dp))
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = Slate400)
          }
        }
      } else {
        // CameraX Live Preview with ZXing ImageAnalysis
        AndroidView(
          factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
              scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val cameraExecutor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener(
              {
                try {
                  val cameraProvider = cameraProviderFuture.get()
                  cameraProvider.unbindAll()

                  val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                  }

                  val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                  imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!isScanningPaused) {
                      val buffer = imageProxy.planes[0].buffer
                      val bytes = ByteArray(buffer.remaining())
                      buffer.get(bytes)

                      val width = imageProxy.width
                      val height = imageProxy.height

                      val source = PlanarYUVLuminanceSource(
                        bytes, width, height,
                        0, 0, width, height,
                        false
                      )
                      val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                      try {
                        val result = multiFormatReader.decodeWithState(binaryBitmap)
                        val text = result.text
                        if (!text.isNullOrBlank()) {
                          ContextCompat.getMainExecutor(ctx).execute {
                            triggerValidation(text)
                          }
                        }
                      } catch (_: NotFoundException) {
                        // QR not in frame
                      } catch (e: Exception) {
                        Log.e("QrScannerScreen", "ZXing decode error: ${e.message}")
                      } finally {
                        multiFormatReader.reset()
                      }
                    }
                    imageProxy.close()
                  }

                  val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                  val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                  )
                  cameraControlRef = camera.cameraControl
                } catch (e: Exception) {
                  Log.e("QrScannerScreen", "Camera bind failed", e)
                }
              },
              ContextCompat.getMainExecutor(ctx)
            )

            previewView
          },
          modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Reticle Overlay
        val activeFrameColor = when (validationOutcome?.state) {
          QrScanResultState.SUCCESS -> greenColor
          QrScanResultState.FAILURE -> redColor
          QrScanResultState.PENDING -> yellowColor
          QrScanResultState.VALIDATING -> Blue500
          else -> Emerald400
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
          val canvasWidth = size.width
          val canvasHeight = size.height
          val boxSize = canvasWidth * 0.72f
          val left = (canvasWidth - boxSize) / 2f
          val top = (canvasHeight - boxSize) / 2f - 40.dp.toPx()

          drawRoundRect(
            color = activeFrameColor,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
          )
        }

        // Top Navigation & Flashlight
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
            border = androidx.compose.foundation.BorderStroke(1.dp, activeFrameColor)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(activeFrameColor)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("ZXING QR SCANNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
          }

          IconButton(
            onClick = {
              isTorchOn = !isTorchOn
              cameraControlRef?.enableTorch(isTorchOn)
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
        }

        // Quick-pick test chips for emulator / instant testing
        if (validationOutcome == null) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(bottom = 24.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.Black.copy(alpha = 0.75f),
              modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
            ) {
              Text(
                text = "Point camera at visitor QR pass or tap a sample below:",
                color = Slate300,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }

            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(tickets.take(5)) { t ->
                val chipColor = when (t.status) {
                  TicketStatus.APPROVED, TicketStatus.INSIDE_PREMISES -> greenColor
                  TicketStatus.PENDING_APPROVAL, TicketStatus.PENDING_RECEPTION -> yellowColor
                  TicketStatus.REJECTED, TicketStatus.CLOSED -> redColor
                }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Slate900.copy(alpha = 0.9f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, chipColor),
                  modifier = Modifier
                    .clickable { triggerValidation(t.qrToken ?: t.ticketId) }
                    .testTag("test_qr_chip_${t.ticketId.lowercase()}")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(chipColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "${t.visitorName} (#${t.ticketId.take(4)})",
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }
        }

        // Scanned Result State Modal Dialog / Bottom Sheet
        if (validationOutcome != null) {
          val outcome = validationOutcome!!
          val cardBg = Slate900
          val stateColor = when (outcome.state) {
            QrScanResultState.SUCCESS -> greenColor
            QrScanResultState.FAILURE -> redColor
            QrScanResultState.PENDING -> yellowColor
            QrScanResultState.VALIDATING -> Blue500
            else -> Slate400
          }

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(2.dp, stateColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(16.dp)
              .testTag("qr_result_card")
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              // Result Icon
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(stateColor.copy(alpha = 0.2f))
                  .border(2.dp, stateColor, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                when (outcome.state) {
                  QrScanResultState.VALIDATING -> {
                    CircularProgressIndicator(color = Blue500, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                  }
                  QrScanResultState.SUCCESS -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = greenColor, modifier = Modifier.size(34.dp))
                  }
                  QrScanResultState.FAILURE -> {
                    Icon(Icons.Default.Cancel, contentDescription = "Invalid", tint = redColor, modifier = Modifier.size(34.dp))
                  }
                  QrScanResultState.PENDING -> {
                    Icon(Icons.Default.HourglassTop, contentDescription = "Pending", tint = yellowColor, modifier = Modifier.size(34.dp))
                  }
                  else -> {}
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = when (outcome.state) {
                  QrScanResultState.SUCCESS -> "ENTRY CLEARED (GREEN)"
                  QrScanResultState.FAILURE -> "ACCESS DENIED (RED)"
                  QrScanResultState.PENDING -> "PENDING APPROVAL (YELLOW)"
                  QrScanResultState.VALIDATING -> "VALIDATING PASS..."
                  else -> "RESULT"
                },
                color = stateColor,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
              )

              Text(
                text = outcome.message,
                color = Slate300,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )

              // Ticket Details if present
              if (outcome.ticket != null) {
                val t = outcome.ticket
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Slate800,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(t.visitorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text("#${t.ticketId}", color = Slate400, fontSize = 12.sp)
                    }
                    Text("Destination: ${t.targetDestination} • ${t.purpose}", color = Slate300, fontSize = 11.sp)
                    if (t.vehicleNumber.isNotBlank()) {
                      Text("Vehicle: ${t.vehicleNumber}", color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    validationOutcome = null
                    isScanningPaused = false
                  },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Scan Again", fontSize = 12.sp)
                }

                if (outcome.state == QrScanResultState.SUCCESS && outcome.ticket != null) {
                  Button(
                    onClick = {
                      onAccessGranted(outcome.ticket.ticketId)
                      validationOutcome = null
                      isScanningPaused = false
                      onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f).testTag("grant_access_confirm_button")
                  ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Entry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                } else if (outcome.state == QrScanResultState.PENDING && outcome.ticket != null) {
                  Button(
                    onClick = {
                      val tid = outcome.ticket.ticketId
                      onManualApprove?.invoke(tid, "Gate Officer (QR Verified)")
                      onAccessGranted(tid)
                      validationOutcome = null
                      isScanningPaused = false
                      onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f).testTag("approve_and_admit_scanned_button")
                  ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve & Admit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                } else {
                  Button(
                    onClick = {
                      validationOutcome = null
                      isScanningPaused = false
                      onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Done", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
