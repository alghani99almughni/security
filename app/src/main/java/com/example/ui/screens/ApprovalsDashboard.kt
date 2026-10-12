package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Tenant
import com.example.model.TenantType
import com.example.model.Ticket
import com.example.model.TicketStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.FeedbackHelper

/**
 * ApprovalsDashboard screen displaying pending requests from the API.
 * Styled using the requested Red, Yellow, Green color palette with
 * vibration and sound feedback upon approval or rejection.
 */
@Composable
fun ApprovalsDashboard(
  tenant: Tenant?,
  tickets: List<Ticket>,
  onSignOff: (ticketId: String, approverName: String, isApproved: Boolean, remarks: String) -> Unit,
  onPullPendingApprovals: (onComplete: (Int) -> Unit) -> Unit = {}
) {
  val context = LocalContext.current
  val feedbackHelper = remember { FeedbackHelper(context) }

  var rejectTicketId by remember { mutableStateOf<String?>(null) }
  var rejectionReason by remember { mutableStateOf("") }
  var isPullingFromApi by remember { mutableStateOf(false) }
  var syncNotice by remember { mutableStateOf<String?>(null) }

  val defaultApproverTitle = when (tenant?.type) {
    TenantType.APARTMENT -> "Mr. R. Sharma (Flat 402 Owner)"
    TenantType.COMPANY -> "VP Operations / HR Head"
    TenantType.HOSPITAL -> "Medical Superintendent"
    TenantType.MALLS -> "Mall Operations Director"
    TenantType.SCHOOL -> "Dr. Sarah Vance (School Principal)"
    else -> "Authorized Facility Signatory"
  }

  var currentApproverName by remember { mutableStateOf(defaultApproverTitle) }

  LaunchedEffect(tenant) {
    currentApproverName = when (tenant?.type) {
      TenantType.APARTMENT -> "Mr. R. Sharma (Flat 402 Owner)"
      TenantType.COMPANY -> "VP Operations / HR Head"
      TenantType.HOSPITAL -> "Medical Superintendent"
      TenantType.MALLS -> "Mall Operations Director"
      TenantType.SCHOOL -> "Dr. Sarah Vance (School Principal)"
      else -> "Authorized Facility Signatory"
    }
  }

  val pendingTickets = tickets.filter { it.status == TicketStatus.PENDING_APPROVAL }
  val signedOffTickets = tickets.filter { it.status != TicketStatus.PENDING_APPROVAL && it.status != TicketStatus.PENDING_RECEPTION }

  // Requested Color Palette
  val redColor = Color(0xFFDC2626)     // Red: Denial / Rejection
  val yellowColor = Color(0xFFEAB308)  // Yellow: Pending Review / Hold
  val greenColor = Color(0xFF16A34A)   // Green: Approval / Clearance

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Top Approvals Dashboard Banner
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(yellowColor),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Slate900, modifier = Modifier.size(26.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Approvals Dashboard", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                  text = "Signatory: $currentApproverName",
                  color = Slate300,
                  fontSize = 12.sp
                )
              }
            }

            // Sync from API Button
            Button(
              onClick = {
                isPullingFromApi = true
                syncNotice = null
                onPullPendingApprovals { count ->
                  isPullingFromApi = false
                  syncNotice = if (count > 0) "Retrieved $count new approvals from API queue" else "API queue in sync (0 new requests)"
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Slate800),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("dashboard_sync_api_button")
            ) {
              if (isPullingFromApi) {
                CircularProgressIndicator(color = yellowColor, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = yellowColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pull API", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (syncNotice != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Emerald900.copy(alpha = 0.5f),
              border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(syncNotice!!, color = Emerald200, fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Slate800
          ) {
            Text(
              text = "Facility: ${tenant?.name ?: "All Premises"} • Role: Approver",
              color = Slate300,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    // Color Palette Indicator Bar (Red, Yellow, Green)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color.White)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(yellowColor))
          Spacer(modifier = Modifier.width(4.dp))
          Text("YELLOW: Pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenColor))
          Spacer(modifier = Modifier.width(4.dp))
          Text("GREEN: Approve", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(redColor))
          Spacer(modifier = Modifier.width(4.dp))
          Text("RED: Reject", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
        }
      }
    }

    // Pending Queue Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(yellowColor)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PENDING APPROVAL QUEUE (${pendingTickets.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate800,
            letterSpacing = 0.8.sp
          )
        }
      }
    }

    // Empty state
    if (pendingTickets.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = greenColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Queue Clear", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 16.sp)
            Text(
              "No pending requests awaiting your sign-off at this moment.",
              fontSize = 12.sp,
              color = Slate500
            )
          }
        }
      }
    }

    // Active Pending Items
    items(pendingTickets) { ticket ->
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, yellowColor.copy(alpha = 0.6f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_pending_item_${ticket.ticketId.lowercase()}")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = ticket.visitorName,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Slate900
              )
              Text(
                text = "Mobile: ${ticket.visitorMobile} • Pass ID: #${ticket.ticketId}",
                fontSize = 11.sp,
                color = Slate500
              )
            }
            StatusBadge(status = ticket.status)
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Destination & Stated Purpose Details
          Surface(
            color = Slate50,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Destination: ${ticket.targetDestination}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text("Stated Purpose: \"${ticket.purpose}\"", fontSize = 12.sp, color = Slate700)

              if (ticket.vehicleNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Slate700, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Plate: ${ticket.vehicleNumber}",
                    fontSize = 11.sp,
                    color = Slate800,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              if (ticket.vehicleSnapshotUri != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = greenColor, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("CameraX Vehicle Snapshot Verified", fontSize = 10.sp, color = greenColor, fontWeight = FontWeight.Medium)
                }
              }

              if (ticket.bypassReception) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "⚡ Direct Gate-to-Approver (Bypassed Reception)",
                  fontSize = 10.sp,
                  color = Amber700,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action Buttons: RED (Reject), YELLOW (Review/Hold), GREEN (Approve)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // RED BUTTON: REJECT
            OutlinedButton(
              onClick = {
                rejectTicketId = ticket.ticketId
              },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = redColor.copy(alpha = 0.08f),
                contentColor = redColor
              ),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, redColor),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("dashboard_reject_button_${ticket.ticketId.lowercase()}")
            ) {
              Icon(Icons.Default.Close, contentDescription = null, tint = redColor, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reject", fontWeight = FontWeight.Bold, color = redColor, fontSize = 12.sp)
            }

            // GREEN BUTTON: APPROVE (triggers sound & vibration feedback)
            Button(
              onClick = {
                feedbackHelper.triggerApproveFeedback()
                onSignOff(ticket.ticketId, currentApproverName, true, "Approved & Signed-off by Authority")
              },
              colors = ButtonDefaults.buttonColors(containerColor = greenColor),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1.8f)
                .height(44.dp)
                .testTag("dashboard_approve_button_${ticket.ticketId.lowercase()}")
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Approve Pass", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }

    // Signed-off History
    if (signedOffTickets.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "SIGNED-OFF AUDIT LOG (${signedOffTickets.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Slate600,
          letterSpacing = 0.8.sp
        )
      }

      items(signedOffTickets) { ticket ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(ticket.visitorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Destination: ${ticket.targetDestination} • Purpose: ${ticket.purpose}", fontSize = 11.sp, color = Slate600)
              Text(
                text = "Decision: ${if (ticket.status == TicketStatus.REJECTED) "REJECTED (RED)" else "APPROVED (GREEN)"} by ${ticket.approverName ?: "Authority"}",
                fontSize = 10.sp,
                color = if (ticket.status == TicketStatus.REJECTED) redColor else greenColor,
                fontWeight = FontWeight.Bold
              )
              if (ticket.rejectionReason != null) {
                Text("Reason: ${ticket.rejectionReason}", fontSize = 10.sp, color = redColor)
              }
            }
            StatusBadge(status = ticket.status)
          }
        }
      }
    }
  }

  // Rejection Reason Confirmation Dialog
  if (rejectTicketId != null) {
    AlertDialog(
      onDismissRequest = { rejectTicketId = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Cancel, contentDescription = null, tint = redColor)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reject Visitor Pass", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Provide a rejection reason to inform Security and the visitor:")
          OutlinedTextField(
            value = rejectionReason,
            onValueChange = { rejectionReason = it },
            label = { Text("Reason for Denial") },
            placeholder = { Text("e.g. Resident unavailable, incorrect flat") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            // Trigger reject sound & vibration
            feedbackHelper.triggerRejectFeedback()
            onSignOff(rejectTicketId!!, currentApproverName, false, rejectionReason.ifBlank { "Rejected by authority" })
            rejectTicketId = null
            rejectionReason = ""
          },
          colors = ButtonDefaults.buttonColors(containerColor = redColor)
        ) {
          Text("Confirm Denial")
        }
      },
      dismissButton = {
        TextButton(onClick = { rejectTicketId = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
