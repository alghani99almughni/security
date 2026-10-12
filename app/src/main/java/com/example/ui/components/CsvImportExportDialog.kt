package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.CsvUtils

enum class CsvType {
  PARENTS_STUDENTS,
  FLAT_OWNERS
}

@Composable
fun CsvImportExportDialog(
  csvType: CsvType,
  tenantId: String,
  onDismiss: () -> Unit,
  onImportCompleted: (count: Int) -> Unit,
  onParseAndImport: (csvText: String) -> CsvUtils.CsvParseResult<*>
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(0) } // 0: Download Template, 1: Upload CSV
  var rawCsvInput by remember { mutableStateOf("") }
  var parseFeedback by remember { mutableStateOf<CsvUtils.CsvParseResult<*>?>(null) }
  var isProcessing by remember { mutableStateOf(false) }

  val title = if (csvType == CsvType.PARENTS_STUDENTS) {
    "Parents & Students CSV Hub"
  } else {
    "Flat Owners & Approvers CSV Hub"
  }

  val templateContent = if (csvType == CsvType.PARENTS_STUDENTS) {
    CsvUtils.STUDENTS_PARENTS_TEMPLATE
  } else {
    CsvUtils.FLAT_OWNERS_TEMPLATE
  }

  val templateFileName = if (csvType == CsvType.PARENTS_STUDENTS) {
    "smartcheck_parents_students_template.csv"
  } else {
    "smartcheck_flat_owners_template.csv"
  }

  // File Picker Launcher for CSV file selection
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val stream = context.contentResolver.openInputStream(it)
        val text = stream?.bufferedReader().use { r -> r?.readText() } ?: ""
        if (text.isNotBlank()) {
          rawCsvInput = text
          selectedTab = 1
          val result = onParseAndImport(text)
          parseFeedback = result
          Toast.makeText(context, "Loaded CSV: ${result.parsedCount} rows found", Toast.LENGTH_SHORT).show()
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to read CSV: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .padding(12.dp)
        .testTag("csv_import_export_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (csvType == CsvType.PARENTS_STUDENTS) Emerald100 else Amber100),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (csvType == CsvType.PARENTS_STUDENTS) Icons.Default.People else Icons.Default.Apartment,
                contentDescription = null,
                tint = if (csvType == CsvType.PARENTS_STUDENTS) Emerald600 else Amber700
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
              Text("Template download & batch capture", fontSize = 11.sp, color = Slate500)
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row: 0 = Template & Download, 1 = Upload & Import
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Slate100,
          contentColor = Blue600,
          modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("1. Download Template", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("2. Upload & Capture", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TAB 0: DOWNLOAD TEMPLATE & FORMAT DETAILS
        if (selectedTab == 0) {
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Blue600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Official CSV Format Specification", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = if (csvType == CsvType.PARENTS_STUDENTS) {
                      "Columns required:\n1. Student Name\n2. Class\n3. Section\n4. Parent Name\n5. Mobile\n6. Bus Number\n7. Email"
                    } else {
                      "Columns required:\n1. Owner Name\n2. Flat Number\n3. Block / Tower\n4. Mobile\n5. Email\n6. Intercom / Designation"
                    },
                    fontSize = 11.sp,
                    color = Slate700,
                    lineHeight = 16.sp
                  )
                }
              }
            }

            item {
              Text("CSV PREVIEW TEMPLATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
              Surface(
                color = Slate900,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = templateContent,
                  color = Emerald400,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }

            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Copy to Clipboard
                OutlinedButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CSV Template", templateContent)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Template copied to clipboard!", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).testTag("copy_csv_template_button")
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Copy Template", fontSize = 11.sp)
                }

                // Share / Download File
                Button(
                  onClick = {
                    val uri = CsvUtils.createTemplateFile(context, templateFileName, templateContent)
                    if (uri != null) {
                      val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      }
                      context.startActivity(Intent.createChooser(sendIntent, "Share CSV Template"))
                    } else {
                      Toast.makeText(context, "Failed to create template file", Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).testTag("download_csv_template_button")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Share / Save File", fontSize = 11.sp)
                }
              }
            }
          }
        }

        // TAB 1: UPLOAD & IMPORT
        if (selectedTab == 1) {
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Pick CSV File
                Button(
                  onClick = { filePickerLauncher.launch("text/*") },
                  colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).testTag("select_csv_file_button")
                ) {
                  Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Pick CSV File", fontSize = 11.sp)
                }

                // Paste Template Sample
                OutlinedButton(
                  onClick = {
                    rawCsvInput = templateContent
                    val result = onParseAndImport(templateContent)
                    parseFeedback = result
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).testTag("paste_sample_csv_button")
                ) {
                  Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Use Sample Data", fontSize = 11.sp)
                }
              }
            }

            item {
              Text("OR PASTE CSV CONTENT DIRECTLY:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
              OutlinedTextField(
                value = rawCsvInput,
                onValueChange = {
                  rawCsvInput = it
                  if (it.isNotBlank()) {
                    parseFeedback = onParseAndImport(it)
                  } else {
                    parseFeedback = null
                  }
                },
                placeholder = { Text("Paste CSV text here...", fontSize = 11.sp) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(130.dp)
                  .testTag("csv_input_field"),
                textStyle = androidx.compose.ui.text.TextStyle(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp
                )
              )
            }

            // Live Validation Outcome
            parseFeedback?.let { feedback ->
              item {
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (feedback.success) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                  ),
                  shape = RoundedCornerShape(10.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (feedback.success) Emerald400 else Rose500
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = if (feedback.success) Icons.Default.CheckCircle else Icons.Default.Error,
                          contentDescription = null,
                          tint = if (feedback.success) Emerald600 else Rose600,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = if (feedback.success) "Validation Passed" else "Validation Errors",
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = if (feedback.success) Emerald800 else Rose800
                        )
                      }
                      Text(
                        text = "${feedback.parsedCount} ready • ${feedback.errorCount} skipped",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                      )
                    }

                    if (feedback.errors.isNotEmpty()) {
                      Spacer(modifier = Modifier.height(6.dp))
                      feedback.errors.take(3).forEach { err ->
                        Text("• $err", fontSize = 10.sp, color = Rose600)
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Commit Import Action Button
          Button(
            onClick = {
              val feedback = parseFeedback
              if (feedback != null && feedback.success && feedback.parsedCount > 0) {
                onImportCompleted(feedback.parsedCount)
                Toast.makeText(context, "Successfully imported ${feedback.parsedCount} records!", Toast.LENGTH_SHORT).show()
                onDismiss()
              } else {
                Toast.makeText(context, "Please provide valid CSV data first", Toast.LENGTH_SHORT).show()
              }
            },
            enabled = parseFeedback?.success == true && (parseFeedback?.parsedCount ?: 0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("confirm_import_csv_button")
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Import ${parseFeedback?.parsedCount ?: 0} Records into Tenant",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }
    }
  }
}
