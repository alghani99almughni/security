package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.Tenant
import com.example.model.Ticket
import com.example.model.TicketStatus
import com.example.ui.components.CameraQrScannerScreen
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ReceptionScreen(
  tenant: Tenant?,
  tickets: List<Ticket>,
  onConfirmAtReception: (ticketId: String, operatorName: String) -> Unit,
  onToggleReception: (Boolean) -> Unit
) {
  val hasReception = tenant?.hasReception ?: true
  val receptionQueue = tickets.filter { it.status == TicketStatus.PENDING_RECEPTION }
  val passedReception = tickets.filter { it.status != TicketStatus.PENDING_RECEPTION }
  var showQrScanner by remember { mutableStateOf(false) }

  if (showQrScanner) {
    QrScannerScreen(
      tickets = tickets,
      onValidateToken = { token, onResult ->
        val clean = token.trim()
        val ticket = tickets.find { it.qrToken == clean || it.ticketId.equals(clean, ignoreCase = true) || clean.contains(it.ticketId) }
        if (ticket == null) {
          onResult(QrScanResultState.FAILURE, "Pass not found. Unregistered QR pass token: $clean", null)
        } else {
          when (ticket.status) {
            TicketStatus.PENDING_RECEPTION -> {
              onResult(QrScanResultState.SUCCESS, "Visitor Arrived at Reception! Identity verified.", ticket)
            }
            TicketStatus.PENDING_APPROVAL -> {
              onResult(QrScanResultState.PENDING, "Already cleared reception; waiting for Approver sign-off.", ticket)
            }
            TicketStatus.APPROVED, TicketStatus.INSIDE_PREMISES -> {
              onResult(QrScanResultState.SUCCESS, "Pass Approved & Cleared for ${ticket.targetDestination}", ticket)
            }
            TicketStatus.REJECTED -> {
              onResult(QrScanResultState.FAILURE, "Access Denied / Pass was rejected.", ticket)
            }
            TicketStatus.CLOSED -> {
              onResult(QrScanResultState.FAILURE, "Expired / Pass closed.", ticket)
            }
          }
        }
      },
      onAccessGranted = { ticketId ->
        onConfirmAtReception(ticketId, "Desk Operator Rajan K")
        showQrScanner = false
      },
      onDismiss = { showQrScanner = false }
    )
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Reception Header
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
                  .background(Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Desk, contentDescription = null, tint = Color.White)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Reception Desk Checkpoint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Desk Operator: Rajan K", color = Slate400, fontSize = 12.sp)
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (hasReception) Emerald600 else Slate700
            ) {
              Text(
                text = if (hasReception) "ACTIVE" else "BYPASSED",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Tenant: ${tenant?.name ?: "Facility"} (${tenant?.type?.displayName})",
              color = Slate300,
              fontSize = 11.sp
            )
            TextButton(
              onClick = { onToggleReception(!hasReception) },
              colors = ButtonDefaults.textButtonColors(contentColor = Blue100)
            ) {
              Text(if (hasReception) "Disable Reception" else "Enable Reception", fontSize = 11.sp)
            }
          }
        }
      }
    }

    if (!hasReception) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Amber100.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Amber600)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reception is Disabled for this Tenant", fontWeight = FontWeight.Bold, color = Amber600, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "In residential apartments, gated communities, or commercial parking, there is no intermediate reception desk. Passes generated at the gate automatically forward directly to the Approver (e.g. Flat Owner, Resident, Bay Manager).",
              fontSize = 12.sp,
              color = Slate700
            )
          }
        }
      }
    } else {
      // Scanner Viewfinder Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Slate900),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("RECEPTION QR OPTICAL SCANNER", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Surface(shape = RoundedCornerShape(4.dp), color = Emerald600) {
                Text("SCANNER READY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
              modifier = Modifier
                .size(120.dp)
                .border(2.dp, Color(0xFF7C3AED), RoundedCornerShape(12.dp))
                .background(Slate800),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(50.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Scan visitor pass or confirm identity at reception", color = Slate400, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { showQrScanner = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("reception_launch_camera_scanner_button")
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Launch Camera QR Scanner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }

      item {
        Text(
          text = "AWAITING RECEPTION CLEARANCE (${receptionQueue.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Slate700,
          letterSpacing = 1.sp
        )
      }

      if (receptionQueue.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("No visitors waiting at reception desk", fontSize = 13.sp, color = Slate600)
            }
          }
        }
      }

      items(receptionQueue) { pass ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(16.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth().testTag("reception_queue_item_${pass.ticketId.lowercase()}")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                Text("Destination: ${pass.targetDestination} • Mobile: ${pass.visitorMobile}", fontSize = 11.sp, color = Slate600)
              }
              StatusBadge(status = pass.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Purpose: \"${pass.purpose}\"", fontSize = 12.sp, color = Slate700)
            if (pass.vehicleNumber.isNotBlank()) {
              Text("Vehicle: ${pass.vehicleNumber} • Gate Arrival: ${pass.gateEntryTime}", fontSize = 11.sp, color = Slate500)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { onConfirmAtReception(pass.ticketId, "Rajan K (Reception)") },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              modifier = Modifier.fillMaxWidth().testTag("confirm_reception_${pass.ticketId.lowercase()}")
            ) {
              Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Confirm Identity & Route to Approver")
            }
          }
        }
      }
    }

    if (passedReception.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text("ROUTED TO APPROVER / CLOSED PASSES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate600)
      }

      items(passedReception) { pass ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Slate50),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text("Pass #${pass.ticketId} • Dest: ${pass.targetDestination}", fontSize = 11.sp, color = Slate600)
            }
            StatusBadge(status = pass.status)
          }
        }
      }
    }
  }
}
