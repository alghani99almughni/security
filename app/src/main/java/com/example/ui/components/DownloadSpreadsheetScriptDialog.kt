package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoogleAppsScriptTemplate
import com.example.model.Tenant
import com.example.ui.theme.*
import com.example.util.SharingUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadSpreadsheetScriptDialog(
  tenant: Tenant?,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val yearMonth = remember { SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date()) }
  val monthDisplay = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
  val masterDriveFolder = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"

  var showCodePreview by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Emerald600),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Google Apps Script & Spreadsheet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Copy & Paste into Google Sheets", fontSize = 11.sp, color = Slate500)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Step-by-Step Guide
        Surface(
          color = Slate900,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Easy 3-Step Setup (Fixes 404 & Lookup Errors)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("1. Open any Google Spreadsheet (or create a new one).", fontSize = 11.sp, color = Slate300)
            Text("2. Click 'Extensions' > 'Apps Script' in the top menu.", fontSize = 11.sp, color = Slate300)
            Text("3. Paste the code below and click 'Deploy' > 'New deployment' > 'Web app' (Execute as: Me, Who has access: Anyone).", fontSize = 11.sp, color = Slate300)
            Spacer(modifier = Modifier.height(6.dp))
            Text("All logs, passes, and visitor records will sync automatically without manual spreadsheet creation!", fontSize = 10.sp, color = Emerald300)
          }
        }

        // Action: Copy Apps Script Code
        Card(
          colors = CardDefaults.cardColors(containerColor = Slate100),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Google Apps Script (Code.gs)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
              Button(
                onClick = {
                  SharingUtils.copyToClipboard(context, "Apps Script Code", GoogleAppsScriptTemplate.SCRIPT_CODE)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Code", fontSize = 11.sp)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { showCodePreview = !showCodePreview },
              colors = ButtonDefaults.buttonColors(containerColor = Slate700),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(if (showCodePreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showCodePreview) "Hide Code Preview" else "View Code Preview (${GoogleAppsScriptTemplate.SCRIPT_CODE.lines().size} lines)", fontSize = 11.sp)
            }

            if (showCodePreview) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = GoogleAppsScriptTemplate.SCRIPT_CODE.take(1500) + "\n... [Remaining lines copied when you tap 'Copy Code']",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Slate800,
                lineHeight = 12.sp,
                modifier = Modifier
                  .background(Color.White, RoundedCornerShape(6.dp))
                  .border(1.dp, Slate300, RoundedCornerShape(6.dp))
                  .padding(8.dp)
                  .fillMaxWidth()
              )
            }
          }
        }

        // WhatsApp Share for script & instructions
        Button(
          onClick = {
            val setupGuide = """
📝 SMARTCHECK GOOGLE SHEETS & APPS SCRIPT SETUP
━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Create or open your Google Spreadsheet.
2. In the top toolbar, go to Extensions -> Apps Script.
3. Replace the default code with the SmartCheck Apps Script code.
4. Click Deploy -> New deployment -> Select type: Web app.
5. Set 'Who has access' to 'Anyone'.
6. Copy the resulting Web App URL and paste it in SmartCheck Settings!
Drive Folder: $masterDriveFolder
Current Active Partition: SmartCheck_$yearMonth
━━━━━━━━━━━━━━━━━━━━━━━━━━━
            """.trimIndent()
            SharingUtils.shareViaWhatsApp(context, setupGuide)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("share_script_guide_whatsapp_button")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share Script Instructions via WhatsApp")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
      ) {
        Text("Done")
      }
    }
  )
}
