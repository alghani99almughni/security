package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.model.Student
import com.example.model.Ticket
import com.example.model.TicketStatus
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ParentScreen(
  students: List<Student>,
  tickets: List<Ticket>,
  onSubmitTicket: (studentId: String, reason: String) -> Unit
) {
  var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
  var reasonInput by remember { mutableStateOf("") }

  // Update selection if students change
  LaunchedEffect(students) {
    if (selectedStudent == null && students.isNotEmpty()) {
      selectedStudent = students.first()
    }
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Parent Info Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Emerald500),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Parent / Resident Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Signed in as John Doe (Mobile: 9876543210)", color = Slate400, fontSize = 12.sp)
          }
        }
      }
    }

    // New Request Form
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "PRE-REQUEST GATE PASS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Pre-registered entry generates an instant QR pass upon approver sign-off, allowing instant fast-scan at the gate.",
            fontSize = 11.sp,
            color = Slate500
          )
          Spacer(modifier = Modifier.height(10.dp))

          if (students.isNotEmpty()) {
            Text("Select Child / Person:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              students.forEach { student ->
                val isSelected = student.studentId == selectedStudent?.studentId
                FilterChip(
                  selected = isSelected,
                  onClick = { selectedStudent = student },
                  label = { Text("${student.studentName} (${student.className})", fontSize = 12.sp) },
                  modifier = Modifier.testTag("child_chip_${student.studentName.lowercase().replace(" ", "_")}")
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = reasonInput,
            onValueChange = { reasonInput = it },
            label = { Text("Purpose of Early Pickup or Visit") },
            placeholder = { Text("e.g. Doctor appointment, urgent family trip, visitor arriving") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("pickup_reason_input")
          )

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              if (selectedStudent != null && reasonInput.isNotBlank()) {
                onSubmitTicket(selectedStudent!!.studentId, reasonInput)
                reasonInput = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
            modifier = Modifier.fillMaxWidth().testTag("submit_early_pickup_button"),
            enabled = reasonInput.isNotBlank() && selectedStudent != null
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit for Approver Sign-off")
          }
        }
      }
    }

    // Active Passes Header
    item {
      Text(
        text = "MY PASSES & QR CODES",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Slate600,
        letterSpacing = 1.sp
      )
    }

    // List of Tickets
    items(tickets) { ticket ->
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
            Column {
              Text(
                text = ticket.targetDestination,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Slate900
              )
              Text(
                text = "Pass #${ticket.ticketId} • Logged: ${ticket.createdAt}",
                fontSize = 11.sp,
                color = Slate400
              )
            }
            StatusBadge(status = ticket.status)
          }

          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = Slate100,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Purpose: \"${ticket.purpose}\"",
              fontSize = 12.sp,
              color = Slate700,
              modifier = Modifier.padding(10.dp)
            )
          }

          // If Approved, show dynamic QR Code!
          AnimatedVisibility(
            visible = ticket.status == TicketStatus.APPROVED || ticket.status == TicketStatus.INSIDE_PREMISES
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
            ) {
              Text(
                text = "OFFICIAL DIGITAL QR GATE PASS",
                fontWeight = FontWeight.Black,
                color = Blue600,
                fontSize = 12.sp,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(8.dp))

              QrCodeCanvas(
                token = ticket.qrToken ?: ticket.ticketId,
                modifier = Modifier.size(170.dp)
              )

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Token: ${ticket.qrToken ?: "SMARTCHECK-QR-VALID"}",
                fontSize = 10.sp,
                color = Slate400,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Flash this QR at Security Gate for instant entry",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Emerald600
              )
            }
          }

          if (ticket.status == TicketStatus.REJECTED) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = Rose100,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Rejected by Approver: ${ticket.rejectionReason ?: "Schedule conflict"}",
                color = Rose600,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }
      }
    }
  }
}
