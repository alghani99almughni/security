package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CameraQrScannerScreen
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.components.VehiclePlateCameraScreen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityScreen(
  tenant: Tenant?,
  tickets: List<Ticket>,
  onGeneratePass: (visitorName: String, mobile: String, purpose: String, destination: String, vehicleNumber: String, bypassReception: Boolean, vehicleSnapshotUri: String?) -> Unit,
  onFastScan: (ticketId: String, guardName: String) -> Unit,
  onClosePassAtExit: (ticketId: String, guardName: String) -> Unit,
  onApprovePass: ((ticketId: String, approverName: String, isApproved: Boolean) -> Unit)? = null
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: Walk-in Gate Pass, 1: Fast QR Scan, 2: Inside & Exit Close
  var showPwaDialog by remember { mutableStateOf(false) }
  var showPlateCamera by remember { mutableStateOf(false) }
  var showQrScanner by remember { mutableStateOf(false) }
  var snapshotPhotoUri by remember { mutableStateOf<String?>(null) }
  var passToVerifyAndApprove by remember { mutableStateOf<Ticket?>(null) }

  // Walk-in Form Fields
  var visitorName by remember { mutableStateOf("") }
  var visitorMobile by remember { mutableStateOf("") }
  var purpose by remember { mutableStateOf("") }
  var destination by remember { mutableStateOf(if (tenant?.type == TenantType.APARTMENT) "Flat 402 - Mr. Sharma" else "Grade 5A - Alex Doe") }
  var vehicleNumber by remember { mutableStateOf("KA-05-MN-8841") }
  var snapshotTaken by remember { mutableStateOf(false) }
  var snapshotTimestamp by remember { mutableStateOf("") }
  var bypassReception by remember { mutableStateOf(!(tenant?.hasReception ?: true)) }

  // Update bypass based on tenant
  LaunchedEffect(tenant) {
    bypassReception = !(tenant?.hasReception ?: true)
  }

  // Active full-screen CameraX Plate Preview viewfinder
  if (showPlateCamera) {
    VehicleScannerScreen(
      onVehicleScanned = { bitmap, photoUri, timestamp, plateNumber, base64 ->
        snapshotPhotoUri = photoUri
        vehicleNumber = plateNumber
        snapshotTaken = true
        snapshotTimestamp = timestamp
        showPlateCamera = false
      },
      onDismiss = { showPlateCamera = false }
    )
    return
  }

  // Active full-screen CameraX QR Scanner viewfinder
  if (showQrScanner) {
    QrScannerScreen(
      tickets = tickets,
      onValidateToken = { token, onResult ->
        val clean = token.trim()
        val ticket = tickets.find { it.qrToken == clean || it.ticketId.equals(clean, ignoreCase = true) || clean.contains(it.ticketId) }
        if (ticket == null) {
          onResult(QrScanResultState.FAILURE, "Unregistered or invalid QR pass token: $clean", null)
        } else {
          when (ticket.status) {
            TicketStatus.APPROVED, TicketStatus.INSIDE_PREMISES -> {
              onResult(QrScanResultState.SUCCESS, "Entry Cleared! Pass is active & authorized by ${ticket.approverName ?: "Authority"}", ticket)
            }
            TicketStatus.PENDING_APPROVAL -> {
              onResult(QrScanResultState.PENDING, "Pending Resident / Authority Sign-Off (${ticket.targetDestination})", ticket)
            }
            TicketStatus.PENDING_RECEPTION -> {
              onResult(QrScanResultState.PENDING, "Pending Reception Badge Clearance", ticket)
            }
            TicketStatus.REJECTED -> {
              onResult(QrScanResultState.FAILURE, "Access Denied: Rejected by ${ticket.approverName ?: "Authority"}", ticket)
            }
            TicketStatus.CLOSED -> {
              onResult(QrScanResultState.FAILURE, "Expired: Gate pass already checked-out at exit.", ticket)
            }
          }
        }
      },
      onAccessGranted = { ticketId ->
        onFastScan(ticketId, "Officer Vikram S (Main Gate)")
        showQrScanner = false
      },
      onManualApprove = { ticketId, approverName ->
        onApprovePass?.invoke(ticketId, approverName, true)
      },
      onDismiss = { showQrScanner = false }
    )
    return
  }

  val activeGatePasses = tickets.filter { it.status != TicketStatus.CLOSED }
  val closedPasses = tickets.filter { it.status == TicketStatus.CLOSED }
  val preRegisteredApproved = tickets.filter { it.status == TicketStatus.APPROVED }
  val insidePremises = tickets.filter { it.status == TicketStatus.INSIDE_PREMISES || it.status == TicketStatus.APPROVED }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Gate Header Banner
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Rose600),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Main Security Gate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                  text = "Facility: ${tenant?.name ?: "Main Entry"} • Post #1",
                  color = Slate400,
                  fontSize = 12.sp
                )
              }
            }

            // Visitor PWA QR Download button
            IconButton(
              onClick = { showPwaDialog = true },
              modifier = Modifier.testTag("visitor_pwa_qr_button")
            ) {
              Icon(Icons.Default.QrCode2, contentDescription = "Visitor PWA QR", tint = Blue100)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (tenant?.hasReception == true) Color(0xFF7C3AED).copy(alpha = 0.3f) else Emerald600.copy(alpha = 0.3f)
            ) {
              Text(
                text = if (tenant?.hasReception == true) "Reception Desk: ENABLED" else "Reception: BYPASSED (Direct to Approver/Flat)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Text("Gate Clock: 10:15 AM", color = Slate400, fontSize = 11.sp)
          }
        }
      }
    }

    // Tab Bar
    item {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Slate100,
        contentColor = Blue600,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("1. Generate Gate Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("2. Fast QR Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("3. Exit & Close (${insidePremises.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
      }
    }

    // TAB 0: GENERATE GATE PASS (WALK-IN)
    if (selectedTab == 0) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "ON-THE-SPOT GATE PASS GENERATION",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = Slate900,
                letterSpacing = 1.sp
              )
              Surface(color = Amber100, shape = RoundedCornerShape(4.dp)) {
                Text("Walk-in Visitor", color = Amber600, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
              }
            }
            Text(
              text = "Use this when visitor/parent does NOT have the app. Captures vehicle snapshot & forwards to Approver.",
              fontSize = 11.sp,
              color = Slate600
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visitor Name & Mobile
            OutlinedTextField(
              value = visitorName,
              onValueChange = { visitorName = it },
              label = { Text("Visitor / Parent Name *") },
              placeholder = { Text("e.g. Ramesh Kumar / Courier Delivery") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("visitor_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = visitorMobile,
              onValueChange = { visitorMobile = it },
              label = { Text("Mobile Number *") },
              placeholder = { Text("e.g. 9876543210") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("visitor_mobile_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = purpose,
              onValueChange = { purpose = it },
              label = { Text("Purpose of the Visit *") },
              placeholder = { Text("e.g. Package delivery, Guest visit, Early pickup, Maintenance") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("visit_purpose_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = destination,
              onValueChange = { destination = it },
              label = { Text("Destination / Flat / Student / Bay *") },
              placeholder = { Text("e.g. Flat 402 - Mr. Sharma, or Class 5A Alex Doe") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("destination_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // VEHICLE NUMBER PLATE TRACKING & SNAPSHOT
            Text(
              text = "VEHICLE NUMBER PLATE TRACKING (ANPR)",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = Slate700
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
              value = vehicleNumber,
              onValueChange = { vehicleNumber = it.uppercase() },
              label = { Text("Vehicle License Plate") },
              placeholder = { Text("e.g. KA-05-MN-8841 or ON-FOOT") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("vehicle_plate_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Simulated Camera Snapshot Box
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate900)
                .padding(12.dp)
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Emerald400Variant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gate ANPR Camera Stream", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                  if (snapshotTaken) {
                    Surface(color = Emerald600, shape = RoundedCornerShape(4.dp)) {
                      Text("SNAPSHOT SAVED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Viewfinder Display
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .border(1.dp, if (snapshotTaken) Emerald500 else Slate600, RoundedCornerShape(8.dp))
                    .background(Slate800),
                  contentAlignment = Alignment.Center
                ) {
                  if (snapshotTaken) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Surface(
                        color = Amber500,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = " $vehicleNumber ",
                          fontWeight = FontWeight.Black,
                          color = Slate900,
                          fontSize = 16.sp,
                          letterSpacing = 2.sp,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text("Captured: $snapshotTimestamp", color = Slate400, fontSize = 10.sp)
                    }
                  } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Slate400, modifier = Modifier.size(28.dp))
                      Text("Ready to capture vehicle license snapshot", color = Slate400, fontSize = 11.sp)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { showPlateCamera = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f).testTag("open_plate_camerax_button")
                  ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (snapshotTaken) "Re-Scan Plate (CameraX)" else "CameraX Plate Preview", fontSize = 12.sp)
                  }

                  OutlinedButton(
                    onClick = {
                      snapshotTaken = true
                      snapshotTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.8f).testTag("take_snapshot_button")
                  ) {
                    Text("Quick Snap", fontSize = 11.sp)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RECEPTION ROUTING TOGGLE
            Card(
              colors = CardDefaults.cardColors(containerColor = Slate100),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (bypassReception) "Direct Routing to Approver" else "Route via Reception Checkpoint",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Slate900
                  )
                  Text(
                    text = if (bypassReception) "Apartments / Gated communities: auto-forwards to Flat Owner / Resident directly." else "Schools / Hospitals: forwards to Reception desk first.",
                    fontSize = 10.sp,
                    color = Slate600
                  )
                }
                Switch(
                  checked = !bypassReception,
                  onCheckedChange = { routeToReception -> bypassReception = !routeToReception }
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                if (visitorName.isNotBlank() && purpose.isNotBlank()) {
                  onGeneratePass(visitorName, visitorMobile, purpose, destination, vehicleNumber, bypassReception, snapshotPhotoUri)
                  visitorName = ""
                  visitorMobile = ""
                  purpose = ""
                  snapshotTaken = false
                  snapshotPhotoUri = null
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Blue600),
              modifier = Modifier.fillMaxWidth().testTag("submit_walkin_gate_pass_button"),
              enabled = visitorName.isNotBlank() && purpose.isNotBlank()
            ) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generate Gate Pass & Forward to ${if (bypassReception) "Approver (Flat Owner)" else "Reception"}")
            }
          }
        }
      }
    }

    // TAB 1: FAST QR SCAN (FOR APP USERS)
    if (selectedTab == 1) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Slate900),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("FAST QR CHECK-IN (FOR APP USERS)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Visitor / Parent who already have the app simply flash their approved QR Pass", color = Slate400, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(14.dp))
            Box(
              modifier = Modifier
                .size(130.dp)
                .border(2.dp, Blue500, RoundedCornerShape(12.dp))
                .background(Slate800),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Blue500, modifier = Modifier.size(56.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = { showQrScanner = true },
              colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("launch_camera_qr_scanner_button")
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Launch CameraX QR Scanner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }

      item {
        val passesForEntry = tickets.filter { it.status == TicketStatus.APPROVED || it.status == TicketStatus.PENDING_APPROVAL }
        Text("PASSES READY FOR ENTRY SCAN OR APPROVAL (${passesForEntry.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
      }

      val passesForEntry = tickets.filter { it.status == TicketStatus.APPROVED || it.status == TicketStatus.PENDING_APPROVAL }
      items(passesForEntry) { pass ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (pass.status == TicketStatus.APPROVED) Emerald300 else Amber300),
          modifier = Modifier.fillMaxWidth().testTag("fast_scan_item_${pass.ticketId.lowercase()}")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(if (pass.status == TicketStatus.APPROVED) Emerald100 else Amber100),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = if (pass.status == TicketStatus.APPROVED) Emerald600 else Amber700
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = if (pass.status == TicketStatus.APPROVED) Emerald100 else Amber100,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = if (pass.status == TicketStatus.APPROVED) "APPROVED" else "PENDING",
                    color = if (pass.status == TicketStatus.APPROVED) Emerald700 else Amber800,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
              Text("Destination: ${pass.targetDestination} • Plate: ${pass.vehicleNumber}", fontSize = 11.sp, color = Slate600)
              Text("Token: ${pass.qrToken ?: "VALID"}", fontSize = 10.sp, color = Slate400)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { showQrScanner = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp).testTag("scan_actual_qr_${pass.ticketId.lowercase()}")
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { passToVerifyAndApprove = pass },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp).testTag("verify_approve_${pass.ticketId.lowercase()}")
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // TAB 2: INSIDE PREMISES & EXIT GATE CLOSE
    if (selectedTab == 2) {
      item {
        Text("VISITORS CURRENTLY INSIDE PREMISES (${insidePremises.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
      }

      if (insidePremises.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              Text("No active visitors currently on premises", color = Slate400, fontSize = 12.sp)
            }
          }
        }
      }

      items(insidePremises) { pass ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Destination: ${pass.targetDestination}", fontSize = 12.sp, color = Slate600)
              }
              StatusBadge(status = pass.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Vehicle: ${pass.vehicleNumber} • Admitted: ${pass.gateEntryTime}", fontSize = 11.sp, color = Slate500)

            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onClosePassAtExit(pass.ticketId, "Gate Security Officer") },
              colors = ButtonDefaults.buttonColors(containerColor = Slate900),
              modifier = Modifier.fillMaxWidth().testTag("close_pass_${pass.ticketId.lowercase()}")
            ) {
              Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Verify Exit & Close Gate Pass")
            }
          }
        }
      }
    }
  }

  // Dialog: Scan QR to Download Visitor PWA App
  if (showPwaDialog) {
    AlertDialog(
      onDismissRequest = { showPwaDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Blue600)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Visitor Instant App Pass", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Visitors can scan this QR code on their phone to open the instant PWA app pass without visiting the app store!",
            fontSize = 12.sp,
            color = Slate600
          )
          Spacer(modifier = Modifier.height(14.dp))

          QrCodeCanvas(
            token = tenant?.pwaAppDownloadUrl ?: "https://smartcheck.app",
            modifier = Modifier.size(160.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Tenant Invite: ${tenant?.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Slate800
          )
          Text(
            text = "Authorized by: ${tenant?.adminEmail}",
            fontSize = 11.sp,
            color = Slate500
          )
        }
      },
      confirmButton = {
        Button(onClick = { showPwaDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // DIALOG: VERIFY & APPROVE PASS
  passToVerifyAndApprove?.let { pass ->
    AlertDialog(
      onDismissRequest = { passToVerifyAndApprove = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald600, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Gate Pass Clearance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Review visitor pass before granting entry:", fontSize = 12.sp, color = Slate600)
          Surface(
            color = Slate100,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Visitor: ${pass.visitorName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
              Text("Destination: ${pass.targetDestination}", fontSize = 12.sp, color = Blue600, fontWeight = FontWeight.SemiBold)
              Text("Purpose: ${pass.purpose}", fontSize = 11.sp, color = Slate700)
              Text("Vehicle Plate: ${pass.vehicleNumber.ifBlank { "None / Pedestrian" }}", fontSize = 11.sp, color = Emerald700, fontWeight = FontWeight.Bold)
              Text("Phone: ${pass.visitorMobile}", fontSize = 11.sp, color = Slate600)
              Text("Token: ${pass.qrToken ?: pass.ticketId}", fontSize = 10.sp, color = Slate400)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onApprovePass?.invoke(pass.ticketId, "Officer Vikram S (Gate)", true)
            onFastScan(pass.ticketId, "Officer Vikram S")
            passToVerifyAndApprove = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
          modifier = Modifier.testTag("dialog_approve_admit_button")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Approve & Admit", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(onClick = { passToVerifyAndApprove = null }) {
            Text("Cancel")
          }
          TextButton(
            onClick = {
              onApprovePass?.invoke(pass.ticketId, "Officer Vikram S (Gate)", false)
              passToVerifyAndApprove = null
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Rose600),
            modifier = Modifier.testTag("dialog_deny_pass_button")
          ) {
            Text("Deny", fontWeight = FontWeight.Bold)
          }
        }
      }
    )
  }
}
