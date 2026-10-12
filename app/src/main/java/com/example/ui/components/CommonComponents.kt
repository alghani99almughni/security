package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun SmartCheckTopBar(
  currentRole: UserRole,
  selectedTenant: Tenant?,
  currentUser: User? = null,
  onRoleSelected: (UserRole) -> Unit,
  onOpenSyncSettings: () -> Unit,
  onLogout: () -> Unit = {}
) {
  Surface(
    color = Slate900,
    tonalElevation = 6.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Blue600),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "SmartCheck Shield",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "SMARTCHECK",
              color = Color.White,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              fontSize = 15.sp
            )
            Text(
              text = if (currentUser != null) "${currentUser.name} • ${selectedTenant?.name ?: "Facility"}" else (selectedTenant?.name ?: "Multi-Tenant Platform"),
              color = Slate400,
              fontSize = 11.sp,
              maxLines = 1
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (currentRole == UserRole.PLATFORM_ADMIN) {
            IconButton(
              onClick = onOpenSyncSettings,
              modifier = Modifier.testTag("sync_settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Google Sync Settings",
                tint = Emerald400Variant
              )
            }
          }

          if (currentUser != null) {
            IconButton(
              onClick = onLogout,
              modifier = Modifier.testTag("logout_button")
            ) {
              Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Switch User / Logout",
                tint = Slate400
              )
            }
          }
        }
      }

      // Role Switcher Chips: Only visible to Platform Admin / Developer!
      // Other roles (Security, Reception, Approver, Parent, Tenant Admin) stay strictly in their dedicated app view!
      if (currentRole == UserRole.PLATFORM_ADMIN) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          UserRole.values().forEach { role ->
            val isSelected = role == currentRole
            val chipBg = if (isSelected) Color(role.badgeColorHex) else Slate800
            val chipText = if (isSelected) Color.White else Slate400

            Surface(
              shape = RoundedCornerShape(20.dp),
              color = chipBg,
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onRoleSelected(role) }
                .testTag("role_chip_${role.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color(role.badgeColorHex))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = role.displayName,
                  color = chipText,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }
        }
      } else {
        // Show role app badge for dedicated app feel
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = Color(currentRole.badgeColorHex).copy(alpha = 0.2f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = when (currentRole) {
              UserRole.SECURITY -> "🛡️ DEDICATED SECURITY GATE APP"
              UserRole.RECEPTION -> "🏢 DEDICATED FRONT DESK RECEPTION APP"
              UserRole.APPROVER -> "✅ DEDICATED APPROVALS APP"
              UserRole.PARENT, UserRole.VISITOR -> "🎫 DEDICATED VISITOR PASS APP"
              UserRole.ADMIN -> "🏢 TENANT MANAGEMENT CONSOLE"
              else -> currentRole.displayName
            },
            color = Color(currentRole.badgeColorHex),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }
    }
  }
}

@Composable
fun StatusBadge(status: TicketStatus) {
  val (bg, textColor) = when (status) {
    TicketStatus.PENDING_RECEPTION -> Color(0xFFEDE9FE) to Color(0xFF6D28D9)
    TicketStatus.PENDING_APPROVAL -> Amber100 to Amber600
    TicketStatus.APPROVED -> Emerald100 to Emerald600
    TicketStatus.INSIDE_PREMISES -> Blue100 to Blue600
    TicketStatus.CLOSED -> Slate200 to Slate800
    TicketStatus.REJECTED -> Rose100 to Rose600
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bg
  ) {
    Text(
      text = status.displayName,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

/**
 * Stylized high-contrast QR Code canvas for verified passes
 */
@Composable
fun QrCodeCanvas(
  token: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White)
      .border(2.dp, Slate200, RoundedCornerShape(16.dp))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cornerSize = w * 0.22f

      // Top-Left Finder Pattern
      drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(0f, 0f),
        size = Size(cornerSize, cornerSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 14f)
      )
      drawRoundRect(
        color = Color(0xFF2563EB),
        topLeft = Offset(cornerSize * 0.28f, cornerSize * 0.28f),
        size = Size(cornerSize * 0.44f, cornerSize * 0.44f),
        cornerRadius = CornerRadius(4f, 4f)
      )

      // Top-Right Finder Pattern
      drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(w - cornerSize, 0f),
        size = Size(cornerSize, cornerSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 14f)
      )
      drawRoundRect(
        color = Color(0xFF2563EB),
        topLeft = Offset(w - cornerSize + cornerSize * 0.28f, cornerSize * 0.28f),
        size = Size(cornerSize * 0.44f, cornerSize * 0.44f),
        cornerRadius = CornerRadius(4f, 4f)
      )

      // Bottom-Left Finder Pattern
      drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(0f, h - cornerSize),
        size = Size(cornerSize, cornerSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 14f)
      )
      drawRoundRect(
        color = Color(0xFF2563EB),
        topLeft = Offset(cornerSize * 0.28f, h - cornerSize + cornerSize * 0.28f),
        size = Size(cornerSize * 0.44f, cornerSize * 0.44f),
        cornerRadius = CornerRadius(4f, 4f)
      )

      // Stylized Data Matrix Dots based on hash
      val hash = token.hashCode().toLong()
      val step = w / 9f
      for (r in 0..8) {
        for (c in 0..8) {
          val inCorner = (r < 3 && c < 3) || (r < 3 && c > 5) || (r > 5 && c < 3)
          if (!inCorner) {
            val bit = ((hash ushr (r * 3 + c)) and 1L) == 1L
            if (bit || (r + c) % 2 == 0) {
              drawRoundRect(
                color = if ((r + c) % 3 == 0) Color(0xFF1E3A8A) else Color(0xFF0F172A),
                topLeft = Offset(c * step + step * 0.15f, r * step + step * 0.15f),
                size = Size(step * 0.7f, step * 0.7f),
                cornerRadius = CornerRadius(4f, 4f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun GoogleSyncDialog(
  config: GoogleSyncConfig,
  onDismiss: () -> Unit,
  onSave: (webAppUrl: String, sheetId: String, folderId: String) -> Unit
) {
  var urlInput by remember { mutableStateOf(config.webAppUrl) }
  var sheetIdInput by remember { mutableStateOf(config.sheetId) }
  var folderIdInput by remember { mutableStateOf(config.folderId) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Sync, contentDescription = null, tint = Blue600)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Google Sheet & Drive Integration", fontSize = 17.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Connects directly to your Google Spreadsheet & Drive folder (1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL). All approvals, tickets, and checkpoints sync live.",
          fontSize = 12.sp,
          color = Slate600
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = urlInput,
          onValueChange = { urlInput = it },
          label = { Text("Apps Script Web App URL") },
          placeholder = { Text("https://script.google.com/macros/s/.../exec") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sync_url_input")
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = sheetIdInput,
          onValueChange = { sheetIdInput = it },
          label = { Text("Google Sheet ID") },
          placeholder = { Text("e.g. 1a2B3c4D5e6F7g8...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sync_sheet_id_input")
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = folderIdInput,
          onValueChange = { folderIdInput = it },
          label = { Text("Google Drive Folder ID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sync_folder_id_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(urlInput, sheetIdInput, folderIdInput)
          onDismiss()
        },
        modifier = Modifier.testTag("save_sync_config_button")
      ) {
        Text("Save Configuration")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
