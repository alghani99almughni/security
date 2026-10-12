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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.DownloadSpreadsheetScriptDialog
import com.example.ui.theme.*
import com.example.util.SharingUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TenantAdminScreen(
  tenant: Tenant?,
  users: List<User>,
  students: List<Student>,
  analytics: TenantAnalytics = TenantAnalytics(),
  onToggleReception: (Boolean) -> Unit = {},
  onCreateUser: (email: String, username: String, mobile: String, name: String, role: UserRole, designationOrFlat: String) -> Unit,
  onCreateStudent: (name: String, className: String, section: String, fatherName: String, fatherMobile: String, busNumber: String) -> Unit,
  onImportStudentsCsv: ((String) -> com.example.util.CsvUtils.CsvParseResult<Student>)? = null,
  onImportApproversCsv: ((String) -> com.example.util.CsvUtils.CsvParseResult<User>)? = null
) {
  val context = LocalContext.current
  // 5 Tabs as requested:
  // 0: Dashboard, 1: Security, 2: Reception, 3: Approver, 4: Parents/Visitor
  var selectedTab by remember { mutableStateOf(0) }

  var showDownloadScriptDialog by remember { mutableStateOf(false) }
  var activeCsvDialogType by remember { mutableStateOf<com.example.ui.components.CsvType?>(null) }

  // Reception state
  var receptionEnabled by remember(tenant) { mutableStateOf(tenant?.hasReception ?: false) }
  var receptionStaff1 by remember(tenant) { mutableStateOf(tenant?.receptionStaffNames?.getOrNull(0) ?: "") }
  var receptionStaff2 by remember(tenant) { mutableStateOf(tenant?.receptionStaffNames?.getOrNull(1) ?: "") }
  var receptionFeedbackMsg by remember { mutableStateOf<String?>(null) }

  // Dialogs for creating each role with automatic WhatsApp APK sharing
  var showCreateSecurityDialog by remember { mutableStateOf(false) }
  var showCreateReceptionDialog by remember { mutableStateOf(false) }
  var showCreateApproverDialog by remember { mutableStateOf(false) }
  var showCreateParentVisitorDialog by remember { mutableStateOf(false) }

  // Post-creation share alert
  var newlyCreatedUserForShare by remember { mutableStateOf<Pair<User, String>?>(null) }

  val monthDisplay = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(14.dp)
  ) {
    // Tenant Header Card
    Card(
      colors = CardDefaults.cardColors(containerColor = Navy800),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = tenant?.name ?: "Tenant Organization",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
            Text(
              text = "Category: ${tenant?.categoryDisplayName ?: "Facility"} • Role: ${tenant?.assignedRole ?: "Administrator"}",
              color = Blue100,
              fontSize = 11.sp
            )
            Text(
              text = "Logins Managed Under: ${tenant?.adminEmail ?: "admin@tenant.com"}",
              color = Emerald400,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Button(
            onClick = { showDownloadScriptDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("tenant_download_sheet_button")
          ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Apps Script", fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 5 TABS: a) Dashboard, b) Security, c) Reception, d) Approver, e) Parents/Visitor
    ScrollableTabRow(
      selectedTabIndex = selectedTab,
      containerColor = Slate100,
      contentColor = Blue600,
      edgePadding = 8.dp,
      modifier = Modifier.clip(RoundedCornerShape(10.dp))
    ) {
      val securityCount = users.count { it.role == UserRole.SECURITY }
      val receptionCount = users.count { it.role == UserRole.RECEPTION }
      val approverCount = users.count { it.role == UserRole.APPROVER }
      val visitorCount = users.count { it.role == UserRole.PARENT || it.role == UserRole.VISITOR } + students.size

      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("a) Dashboard", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("b) Security ($securityCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("c) Reception ($receptionCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = selectedTab == 3,
        onClick = { selectedTab = 3 },
        text = { Text("d) Approver ($approverCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = selectedTab == 4,
        onClick = { selectedTab = 4 },
        text = { Text("e) Parents/Visitor ($visitorCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    when (selectedTab) {
      // TAB a: DASHBOARD
      0 -> {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Text(
              text = "DAY-WISE & MONTH-WISE PERFORMANCE METRICS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Slate600,
              letterSpacing = 0.5.sp
            )
          }

          // Top Row: Approved, Rejected, Pending
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MetricCard(
                title = "Approved Today",
                value = "${analytics.todayApproved}",
                sub = "Valid Passes",
                color = Color(0xFF16A34A),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
              )

              MetricCard(
                title = "Pending Today",
                value = "${analytics.todayPending}",
                sub = "Needs Sign-off",
                color = Color(0xFFEAB308),
                icon = Icons.Default.HourglassEmpty,
                modifier = Modifier.weight(1f)
              )

              MetricCard(
                title = "Rejected Today",
                value = "${analytics.todayRejected}",
                sub = "Access Denied",
                color = Color(0xFFDC2626),
                icon = Icons.Default.Cancel,
                modifier = Modifier.weight(1f)
              )
            }
          }

          // Middle Row: Visitors Today & Month-wise
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MetricCard(
                title = "Visitors of the Day",
                value = "${analytics.todayTotalVisitors}",
                sub = "Today (${analytics.currentDate})",
                color = Blue600,
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
              )

              MetricCard(
                title = "Month-Wise Visitors",
                value = "${analytics.monthTotalVisitors}",
                sub = monthDisplay,
                color = Indigo600,
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f)
              )

              MetricCard(
                title = "Total Month Passes",
                value = "${analytics.monthTotalRequests}",
                sub = "All Gate Requests",
                color = Slate700,
                icon = Icons.Default.ReceiptLong,
                modifier = Modifier.weight(1f)
              )
            }
          }

          // Share Snapshot View via WhatsApp & Email
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Slate900),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Performance Snapshot", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                  }
                  Surface(color = Emerald500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text("WhatsApp / Email", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Broadcast today's approved, pending, and rejected metrics directly to management via WhatsApp.",
                  fontSize = 11.sp,
                  color = Slate400
                )

                Spacer(modifier = Modifier.height(10.dp))

                val report = SharingUtils.formatPerformanceReport(tenant, analytics)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { SharingUtils.shareViaWhatsApp(context, report) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("share_snapshot_whatsapp_button")
                  ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      SharingUtils.shareViaEmail(context, "Gate Pass Report - ${tenant?.name}", report)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email Report", fontSize = 11.sp)
                  }
                }
              }
            }
          }

          // Apps Script & Google Sheet setup (Accessible only to Tenant Admin)
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Apps Script Integration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                  }

                  Button(
                    onClick = { showDownloadScriptDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                  ) {
                    Text("Setup Guide", fontSize = 10.sp)
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Copy the Apps Script code and paste it into Extensions > Apps Script in your Google Spreadsheet. All entries and approvals sync automatically to Drive Folder: 1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL.",
                  fontSize = 11.sp,
                  color = Slate600
                )
              }
            }
          }
        }
      }

      // TAB b: SECURITY
      1 -> {
        val securityUsers = users.filter { it.role == UserRole.SECURITY }
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("SECURITY GUARD ROLES (${securityUsers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate700)
                Text("Gate pass generation & vehicle ANPR scanning", fontSize = 10.sp, color = Slate400)
              }

              Button(
                onClick = { showCreateSecurityDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_security_id_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Security ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (securityUsers.isEmpty()) {
            item {
              Surface(
                color = Slate100,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.Security, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("No Security Guards Assigned Yet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate700)
                  Text("Tap 'Create Security ID' to add guards and share the APK with their credentials via WhatsApp.", fontSize = 11.sp, color = Slate500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
              }
            }
          } else {
            items(securityUsers) { guard ->
              RoleUserCard(
                user = guard,
                roleTitle = "Security Gate Officer",
                badgeColor = Color(0xFFDC2626),
                icon = Icons.Default.Security,
                tenant = tenant,
                onShareWhatsApp = {
                  SharingUtils.shareApkViaWhatsApp(
                    context = context,
                    roleTitle = "Security Gate Officer",
                    userName = guard.name,
                    email = guard.email,
                    mobile = guard.mobile,
                    temporaryPin = "1234",
                    tenant = tenant,
                    extraInfo = guard.designationOrFlat
                  )
                }
              )
            }
          }
        }
      }

      // TAB c: RECEPTION
      2 -> {
        val receptionUsers = users.filter { it.role == UserRole.RECEPTION }
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // Reception Toggle Card
          item {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = if (receptionEnabled) Color(0xFFFAF5FF) else Slate100
              ),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (receptionEnabled) Color(0xFFC084FC) else Slate300
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.MeetingRoom,
                      contentDescription = null,
                      tint = if (receptionEnabled) Color(0xFF7C3AED) else Slate600,
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Reception Clearance: ${if (receptionEnabled) "ENABLED" else "DISABLED"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (receptionEnabled) Color(0xFF581C87) else Slate800
                      )
                      Text(
                        text = if (receptionEnabled) "Maximum 2 reception staff names allowed" else "If disabled: bypass directly to Approver",
                        fontSize = 11.sp,
                        color = Slate500
                      )
                    }
                  }

                  Switch(
                    checked = receptionEnabled,
                    onCheckedChange = { checked ->
                      receptionEnabled = checked
                      onToggleReception(checked)
                      receptionFeedbackMsg = if (checked) "Reception clearance enabled" else "Reception disabled (passes bypass directly to Approver)"
                    },
                    modifier = Modifier.testTag("toggle_reception_switch")
                  )
                }

                if (receptionEnabled) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Text("Quick Setup (Up to 2 Staff Names):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF581C87))
                  Spacer(modifier = Modifier.height(6.dp))

                  OutlinedTextField(
                    value = receptionStaff1,
                    onValueChange = { receptionStaff1 = it },
                    label = { Text("Receptionist 1 Name") },
                    placeholder = { Text("e.g. Officer Anita") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  OutlinedTextField(
                    value = receptionStaff2,
                    onValueChange = { receptionStaff2 = it },
                    label = { Text("Receptionist 2 Name (Optional)") },
                    placeholder = { Text("e.g. Officer Suresh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Button(
                    onClick = {
                      val domain = tenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"
                      if (receptionStaff1.isNotBlank()) {
                        onCreateUser("reception1@$domain", "reception1", "9876543210", receptionStaff1.trim(), UserRole.RECEPTION, "Front Desk 1")
                      }
                      if (receptionStaff2.isNotBlank()) {
                        onCreateUser("reception2@$domain", "reception2", "9876543211", receptionStaff2.trim(), UserRole.RECEPTION, "Front Desk 2")
                      }
                      receptionFeedbackMsg = "Reception staff assigned! Share credentials below."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Provision Reception Staff")
                  }
                } else {
                  Spacer(modifier = Modifier.height(6.dp))
                  Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Direct Flow Active: Visitor passes route directly from Security to Approver / Flat Owner.",
                      fontSize = 11.sp,
                      color = Emerald700,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }

                if (receptionFeedbackMsg != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(receptionFeedbackMsg!!, color = Emerald600, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          if (receptionEnabled) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("RECEPTION STAFF ACCOUNTS (${receptionUsers.size}/2)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate700)
                if (receptionUsers.size < 2) {
                  Button(
                    onClick = { showCreateReceptionDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Receptionist", fontSize = 11.sp)
                  }
                }
              }
            }

            items(receptionUsers) { staff ->
              RoleUserCard(
                user = staff,
                roleTitle = "Front Desk Receptionist",
                badgeColor = Color(0xFF7C3AED),
                icon = Icons.Default.MeetingRoom,
                tenant = tenant,
                onShareWhatsApp = {
                  SharingUtils.shareApkViaWhatsApp(
                    context = context,
                    roleTitle = "Front Desk Receptionist",
                    userName = staff.name,
                    email = staff.email,
                    mobile = staff.mobile,
                    temporaryPin = "1234",
                    tenant = tenant,
                    extraInfo = staff.designationOrFlat
                  )
                }
              )
            }
          }
        }
      }

      // TAB d: APPROVER
      3 -> {
        val approverUsers = users.filter { it.role == UserRole.APPROVER }
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // CSV Hub Banner for Flat Owners & Approvers
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Slate100),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Amber100),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = Amber800, modifier = Modifier.size(18.dp))
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text("Flat Owners CSV Hub", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                    Text("Download template & batch upload flat owner / approver data", fontSize = 10.sp, color = Slate600)
                  }
                }
                Button(
                  onClick = { activeCsvDialogType = com.example.ui.components.CsvType.FLAT_OWNERS },
                  colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(30.dp).testTag("open_flat_owners_csv_hub_button")
                ) {
                  Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("CSV Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("APPROVERS & SIGN-OFF AUTHORITIES (${approverUsers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate700)
                Text("Flat Owners, Teachers, Department Heads", fontSize = 10.sp, color = Slate400)
              }

              Button(
                onClick = { showCreateApproverDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_approver_id_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Approver ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (approverUsers.isEmpty()) {
            item {
              Surface(
                color = Slate100,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("No Approvers Created Yet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate700)
                  Text("Tap 'Create Approver ID' to register flat owners or department heads and share their APK credentials via WhatsApp.", fontSize = 11.sp, color = Slate500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
              }
            }
          } else {
            items(approverUsers) { approver ->
              RoleUserCard(
                user = approver,
                roleTitle = "Approval Authority",
                badgeColor = Amber700,
                icon = Icons.Default.VerifiedUser,
                tenant = tenant,
                onShareWhatsApp = {
                  SharingUtils.shareApkViaWhatsApp(
                    context = context,
                    roleTitle = "Approval Authority",
                    userName = approver.name,
                    email = approver.email,
                    mobile = approver.mobile,
                    temporaryPin = "1234",
                    tenant = tenant,
                    extraInfo = approver.designationOrFlat
                  )
                }
              )
            }
          }
        }
      }

      // TAB e: PARENTS / VISITOR
      4 -> {
        val visitorUsers = users.filter { it.role == UserRole.PARENT || it.role == UserRole.VISITOR }
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // CSV Hub Banner for Parents & Students
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Emerald100),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text("Parents & Students CSV Hub", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                    Text("Download template & batch capture students & parent contacts", fontSize = 10.sp, color = Slate600)
                  }
                }
                Button(
                  onClick = { activeCsvDialogType = com.example.ui.components.CsvType.PARENTS_STUDENTS },
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(30.dp).testTag("open_parents_students_csv_hub_button")
                ) {
                  Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("CSV Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (tenant?.type == TenantType.APARTMENT) "RESIDENTS & VISITORS" else "PARENTS & VISITORS",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = Slate700
                )
                Text("Registered pass applicants (${visitorUsers.size + students.size} accounts)", fontSize = 10.sp, color = Slate400)
              }

              Button(
                onClick = { showCreateParentVisitorDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_parent_visitor_id_button")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Member", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Registered Users
          items(visitorUsers) { visitor ->
            RoleUserCard(
              user = visitor,
              roleTitle = if (visitor.role == UserRole.PARENT) "Parent / Guardian" else "Registered Visitor",
              badgeColor = Color(0xFF16A34A),
              icon = if (visitor.role == UserRole.PARENT) Icons.Default.FamilyRestroom else Icons.Default.Person,
              tenant = tenant,
              onShareWhatsApp = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = if (visitor.role == UserRole.PARENT) "Parent Passholder" else "Visitor Passholder",
                  userName = visitor.name,
                  email = visitor.email,
                  mobile = visitor.mobile,
                  temporaryPin = "1234",
                  tenant = tenant,
                  extraInfo = visitor.designationOrFlat
                )
              }
            )
          }

          // Master Students / Flats linked
          items(students) { student ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier.size(34.dp).clip(CircleShape).background(Emerald100),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.School, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(student.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                  Text("Parent: ${student.fatherName} • Mob: ${student.fatherMobile}", fontSize = 10.sp, color = Slate600)
                  Text("Unit / Class: ${student.className}", fontSize = 10.sp, color = Blue600)
                }

                Button(
                  onClick = {
                    val msg = """
👋 Hello ${student.fatherName},
Your child ${student.studentName} (${student.className}) is registered with ${tenant?.name}.
Login with mobile ${student.fatherMobile} in the SmartCheck app to request gate passes and early pickup clearance.
                    """.trimIndent()
                    SharingUtils.shareViaWhatsApp(context, msg)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.height(26.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("WhatsApp", fontSize = 10.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  // DIALOG: CREATE SECURITY GUARD ID
  if (showCreateSecurityDialog) {
    CreateRoleUserDialog(
      role = UserRole.SECURITY,
      roleTitle = "Security Guard",
      tenant = tenant,
      onDismiss = { showCreateSecurityDialog = false },
      onSave = { email, username, mobile, name, desig, pin ->
        onCreateUser(email, username, mobile, name, UserRole.SECURITY, desig)
        showCreateSecurityDialog = false
        val user = User(userId = "sec_${System.currentTimeMillis()}", email = email, username = username, name = name, role = UserRole.SECURITY, mobile = mobile, tenantId = tenant?.tenantId ?: "", designationOrFlat = desig)
        newlyCreatedUserForShare = Pair(user, pin)
      }
    )
  }

  // DIALOG: CREATE RECEPTIONIST ID
  if (showCreateReceptionDialog) {
    CreateRoleUserDialog(
      role = UserRole.RECEPTION,
      roleTitle = "Receptionist",
      tenant = tenant,
      onDismiss = { showCreateReceptionDialog = false },
      onSave = { email, username, mobile, name, desig, pin ->
        onCreateUser(email, username, mobile, name, UserRole.RECEPTION, desig)
        showCreateReceptionDialog = false
        val user = User(userId = "rec_${System.currentTimeMillis()}", email = email, username = username, name = name, role = UserRole.RECEPTION, mobile = mobile, tenantId = tenant?.tenantId ?: "", designationOrFlat = desig)
        newlyCreatedUserForShare = Pair(user, pin)
      }
    )
  }

  // DIALOG: CREATE APPROVER ID
  if (showCreateApproverDialog) {
    CreateRoleUserDialog(
      role = UserRole.APPROVER,
      roleTitle = "Approver / Flat Owner",
      tenant = tenant,
      onDismiss = { showCreateApproverDialog = false },
      onSave = { email, username, mobile, name, desig, pin ->
        onCreateUser(email, username, mobile, name, UserRole.APPROVER, desig)
        showCreateApproverDialog = false
        val user = User(userId = "app_${System.currentTimeMillis()}", email = email, username = username, name = name, role = UserRole.APPROVER, mobile = mobile, tenantId = tenant?.tenantId ?: "", designationOrFlat = desig)
        newlyCreatedUserForShare = Pair(user, pin)
      }
    )
  }

  // DIALOG: CREATE PARENT / VISITOR ID
  if (showCreateParentVisitorDialog) {
    CreateRoleUserDialog(
      role = if (tenant?.type == TenantType.SCHOOL) UserRole.PARENT else UserRole.VISITOR,
      roleTitle = if (tenant?.type == TenantType.SCHOOL) "Parent / Guardian" else "Visitor / Resident",
      tenant = tenant,
      onDismiss = { showCreateParentVisitorDialog = false },
      onSave = { email, username, mobile, name, desig, pin ->
        val role = if (tenant?.type == TenantType.SCHOOL) UserRole.PARENT else UserRole.VISITOR
        onCreateUser(email, username, mobile, name, role, desig)
        if (tenant?.type == TenantType.SCHOOL) {
          onCreateStudent(name + "'s Child", desig, "A", name, mobile, "")
        }
        showCreateParentVisitorDialog = false
        val user = User(userId = "vis_${System.currentTimeMillis()}", email = email, username = username, name = name, role = role, mobile = mobile, tenantId = tenant?.tenantId ?: "", designationOrFlat = desig)
        newlyCreatedUserForShare = Pair(user, pin)
      }
    )
  }

  // DIALOG: PROMPT IMMEDIATE SHARE APK VIA WHATSAPP WITH RECORDED CREDENTIALS
  if (newlyCreatedUserForShare != null) {
    val (user, pin) = newlyCreatedUserForShare!!
    AlertDialog(
      onDismissRequest = { newlyCreatedUserForShare = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
          Spacer(modifier = Modifier.width(8.dp))
          Text("ID Created Successfully!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("The account has been recorded and linked to tenant: ${tenant?.adminEmail}", fontSize = 12.sp, color = Slate700)
          Surface(
            color = Slate100,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Name: ${user.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              Text("Role: ${user.role.displayName}", fontSize = 11.sp, color = Blue600)
              Text("Login Email: ${user.email}", fontSize = 11.sp)
              Text("Passcode: $pin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text("Post: ${user.designationOrFlat}", fontSize = 11.sp, color = Slate600)
            }
          }
          Text("Would you like to share the APK file and credentials via WhatsApp right now?", fontSize = 11.sp, color = Slate600)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            SharingUtils.shareApkViaWhatsApp(
              context = context,
              roleTitle = user.role.displayName,
              userName = user.name,
              email = user.email,
              mobile = user.mobile,
              temporaryPin = pin,
              tenant = tenant,
              extraInfo = user.designationOrFlat
            )
            newlyCreatedUserForShare = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share APK via WhatsApp")
        }
      },
      dismissButton = {
        TextButton(onClick = { newlyCreatedUserForShare = null }) {
          Text("Later")
        }
      }
    )
  }

  // DIALOG: APPS SCRIPT SETUP (ONLY ACCESSIBLE TO TENANT ADMIN)
  if (showDownloadScriptDialog) {
    DownloadSpreadsheetScriptDialog(
      tenant = tenant,
      onDismiss = { showDownloadScriptDialog = false }
    )
  }

  // DIALOG: CSV TEMPLATE DOWNLOAD & BULK UPLOAD HUB
  activeCsvDialogType?.let { csvType ->
    com.example.ui.components.CsvImportExportDialog(
      csvType = csvType,
      tenantId = tenant?.tenantId ?: "TENANT-001",
      onDismiss = { activeCsvDialogType = null },
      onImportCompleted = { count ->
        activeCsvDialogType = null
      },
      onParseAndImport = { csvContent ->
        if (csvType == com.example.ui.components.CsvType.PARENTS_STUDENTS) {
          onImportStudentsCsv?.invoke(csvContent)
            ?: com.example.util.CsvUtils.parseStudentsParentsCsv(csvContent, tenant?.tenantId ?: "TENANT-001")
        } else {
          onImportApproversCsv?.invoke(csvContent)
            ?: com.example.util.CsvUtils.parseFlatOwnersCsv(csvContent, tenant?.tenantId ?: "TENANT-001")
        }
      }
    )
  }
}

@Composable
private fun RoleUserCard(
  user: User,
  roleTitle: String,
  badgeColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tenant: Tenant?,
  onShareWhatsApp: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(34.dp).clip(CircleShape).background(badgeColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
        Text("Email: ${user.email} • Mob: ${user.mobile}", fontSize = 10.sp, color = Slate500)
        Text("Post: ${user.designationOrFlat}", fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
      }

      Button(
        onClick = onShareWhatsApp,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(28.dp)
      ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Share APK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun CreateRoleUserDialog(
  role: UserRole,
  roleTitle: String,
  tenant: Tenant?,
  onDismiss: () -> Unit,
  onSave: (email: String, username: String, mobile: String, name: String, designation: String, pin: String) -> Unit
) {
  val defaultDomain = remember(tenant) {
    tenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"
  }
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var mobile by remember { mutableStateOf("") }
  var designation by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("1234") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Create $roleTitle ID", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Once created, the APK file and credentials will be sent via WhatsApp. Login will be saved under tenant email: ${tenant?.adminEmail}",
          fontSize = 11.sp,
          color = Slate500
        )

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            if (email.isBlank() && it.isNotBlank()) {
              val sanitized = it.lowercase().replace(" ", "")
              email = "${sanitized}@$defaultDomain"
            }
          },
          label = { Text("Person Name *") },
          placeholder = { Text("e.g. Ramesh Kumar") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = mobile,
          onValueChange = { mobile = it },
          label = { Text("WhatsApp Mobile Number *") },
          placeholder = { Text("e.g. 9876543210") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Login Email ID *") },
          placeholder = { Text("ramesh@$defaultDomain") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = designation,
          onValueChange = { designation = it },
          label = {
            Text(
              when (role) {
                UserRole.SECURITY -> "Gate / Post Name (e.g. Gate 1)"
                UserRole.RECEPTION -> "Desk / Counter (e.g. Counter A)"
                UserRole.APPROVER -> "Flat / Department (e.g. Flat 302 / HR)"
                else -> "Unit / Reference"
              }
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = pin,
          onValueChange = { pin = it },
          label = { Text("Temporary Passcode / PIN") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
          Text(errorMessage!!, color = Rose500, fontSize = 11.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank() || email.isBlank() || mobile.isBlank()) {
            errorMessage = "Please enter Name, Mobile, and Email."
          } else {
            val username = email.substringBefore("@")
            onSave(email.trim(), username, mobile.trim(), name.trim(), designation.ifBlank { role.displayName }, pin.ifBlank { "1234" })
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
      ) {
        Text("Save & Share APK")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  sub: String,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate900)
      Text(sub, fontSize = 9.sp, color = Slate500)
    }
  }
}
