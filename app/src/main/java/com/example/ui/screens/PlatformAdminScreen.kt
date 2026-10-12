package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.GoogleAppsScriptTemplate
import com.example.model.*
import com.example.ui.components.DownloadSpreadsheetScriptDialog
import com.example.ui.components.RegisterTenantDialog
import com.example.ui.theme.*
import com.example.util.SharingUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformAdminScreen(
  tenants: List<Tenant>,
  selectedTenant: Tenant?,
  tickets: List<Ticket> = emptyList(),
  serviceHealth: AppServiceHealth = AppServiceHealth(),
  tenantRegistry: List<MultipleTenantRegisterEntry> = emptyList(),
  onPingHealth: () -> Unit = {},
  onSelectTenant: (Tenant) -> Unit,
  onSwitchRole: (UserRole) -> Unit = {},
  onLaunchVehicleScanner: () -> Unit = {},
  onLaunchQrScanner: () -> Unit = {},
  onLaunchApprovalsDashboard: () -> Unit = {},
  onGenerateTestPass: () -> Unit = {},
  onCreateTenant: (
    name: String,
    code: String,
    type: TenantType,
    customCategory: String?,
    assignedRole: String,
    adminEmail: String,
    contactName: String,
    contactMobile: String,
    hasReception: Boolean,
    receptionStaffNames: List<String>
  ) -> Tenant
) {
  val context = LocalContext.current
  var showCreateDialog by remember { mutableStateOf(false) }
  var showDownloadScriptDialog by remember { mutableStateOf(false) }
  var developerTab by remember { mutableStateOf(0) }
  val yearMonth = remember { SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date()) }

  // Custom APK WhatsApp share builder state
  var customName by remember { mutableStateOf("") }
  var customMobile by remember { mutableStateOf("") }
  var customEmail by remember { mutableStateOf("") }
  var customRole by remember { mutableStateOf(UserRole.SECURITY) }
  var customDesignation by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(14.dp)
  ) {
    // Developer Header Bar
    Card(
      colors = CardDefaults.cardColors(containerColor = Slate900),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
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
                .background(Blue600),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Developer Platform Console", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
              Text("Multi-Tenant Control & WhatsApp Deployment", color = Slate400, fontSize = 11.sp)
            }
          }

          // Service Health Status Badge
          Surface(
            color = if (serviceHealth.isWorking) Emerald500.copy(alpha = 0.2f) else Rose500.copy(alpha = 0.2f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (serviceHealth.isWorking) Emerald500 else Rose500
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (serviceHealth.isWorking) Emerald400 else Rose500)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (serviceHealth.isWorking) "SERVICE: WORKING" else "SERVICE: ERROR",
                color = if (serviceHealth.isWorking) Emerald400 else Rose400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Developer Control Tabs
    TabRow(
      selectedTabIndex = developerTab,
      containerColor = Slate100,
      contentColor = Blue600,
      modifier = Modifier.clip(RoundedCornerShape(10.dp))
    ) {
      Tab(
        selected = developerTab == 0,
        onClick = { developerTab = 0 },
        text = { Text("1. Tenant Creation & Script", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = developerTab == 1,
        onClick = { developerTab = 1 },
        text = { Text("2. Share APK via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
      Tab(
        selected = developerTab == 2,
        onClick = { developerTab = 2 },
        text = { Text("3. Sandbox Testing", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    when (developerTab) {
      // TAB 0: TENANT CREATION, SPREADSHEET & APPS SCRIPT
      0 -> {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // Summary Metrics Banner
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Slate800),
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
                    Text("REGISTERED TENANTS ON FRONT PAGE", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${tenants.size} Organizations", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                  }

                  Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_tenant_button")
                  ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Tenant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Breakdowns
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  val schools = tenants.count { it.type == TenantType.SCHOOL }
                  val hospitals = tenants.count { it.type == TenantType.HOSPITAL }
                  val apartments = tenants.count { it.type == TenantType.APARTMENT }
                  val companies = tenants.count { it.type == TenantType.COMPANY }
                  val malls = tenants.count { it.type == TenantType.MALLS }

                  CategoryBadge("🏫 School", schools, Color(0xFFF59E0B))
                  CategoryBadge("🏥 Hospital", hospitals, Rose500)
                  CategoryBadge("🏢 Apartment", apartments, Emerald400)
                  CategoryBadge("💼 Company", companies, Blue500)
                  CategoryBadge("🛍️ Malls", malls, Color(0xFF8B5CF6))
                }
              }
            }
          }

          // Apps Script Download / Paste into Spreadsheet Option
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
                    Icon(Icons.Default.Code, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text("Google Apps Script Embed & Deployment", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                      Text("Copy & paste into spreadsheet to prevent 404 & lookup errors", fontSize = 10.sp, color = Slate500)
                    }
                  }

                  Button(
                    onClick = {
                      SharingUtils.copyToClipboard(context, "Google Apps Script", GoogleAppsScriptTemplate.SCRIPT_CODE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp).testTag("copy_apps_script_button")
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code.gs", fontSize = 10.sp)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                  color = Slate50,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text("1. Open your Google Sheet in browser.", fontSize = 11.sp, color = Slate700)
                    Text("2. Click 'Extensions' > 'Apps Script'.", fontSize = 11.sp, color = Slate700)
                    Text("3. Paste the code from 'Copy Code.gs' and click 'Deploy' > 'New deployment' (Type: Web app, Who has access: Anyone).", fontSize = 11.sp, color = Slate700)
                    Text("4. Once deployed, the sheet synchronizes all check-ins automatically!", fontSize = 10.sp, color = Emerald700, fontWeight = FontWeight.SemiBold)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                  onClick = { showDownloadScriptDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Open Full Apps Script & Drive Guide", fontSize = 11.sp)
                }
              }
            }
          }

          // Multiple Tenant Register Spreadsheet
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
                    Icon(Icons.Default.TableView, contentDescription = null, tint = Blue600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Multiple Tenant Register", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                  }
                  Surface(color = Blue100, shape = RoundedCornerShape(6.dp)) {
                    Text("${tenantRegistry.size} Registered", color = Blue600, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tenantRegistry.isEmpty()) {
                  Text("No registered tenants yet. Create a tenant above to populate the register.", fontSize = 11.sp, color = Slate500)
                } else {
                  tenantRegistry.forEach { entry ->
                    Surface(
                      color = Slate50,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                      Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text("${entry.organizationName} (${entry.category})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                          Text("Role: ${entry.assignedRole} • Contact: ${entry.contactPerson} (${entry.mobile})", fontSize = 10.sp, color = Slate600)
                          Text("Admin Email: ${entry.email}", fontSize = 10.sp, color = Blue600)
                        }

                        Button(
                          onClick = {
                            val msg = SharingUtils.formatRoleApkInvite(
                              roleTitle = "Tenant Administrator",
                              userName = entry.contactPerson,
                              email = entry.email,
                              mobile = entry.mobile,
                              temporaryPin = "1234",
                              tenant = Tenant(code = entry.tenantCode, name = entry.organizationName, type = TenantType.APARTMENT, adminEmail = entry.email, contactName = entry.contactPerson, contactMobile = entry.mobile, hasReception = false),
                              extraInfo = entry.assignedRole
                            )
                            SharingUtils.shareViaWhatsApp(context, msg)
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.height(28.dp)
                        ) {
                          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("Share APK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

      // TAB 1: SHARE APK VIA WHATSAPP CONTROL CENTER
      1 -> {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Share, contentDescription = null, tint = Emerald300, modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("WhatsApp APK & Credential Handover Center", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Directly broadcast the SmartCheck APK installation file and recorded user credentials to officers, approvers, and visitors without relying on external broken URLs.",
                  fontSize = 11.sp,
                  color = Emerald100
                )
              }
            }
          }

          // Quick Role Broadcast Cards
          item {
            Text("QUICK ROLE APK DISPATCH", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate600, letterSpacing = 0.5.sp)
          }

          item {
            QuickShareRoleCard(
              title = "🛡️ Security Gate Guard APK",
              subtitle = "Gate pass verification, ANPR vehicle plate scan",
              buttonColor = Color(0xFFDC2626),
              onShare = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = "Security Gate Officer",
                  userName = "Officer Rajesh",
                  email = "security@${selectedTenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}",
                  mobile = "9876543210",
                  temporaryPin = "1234",
                  tenant = selectedTenant,
                  extraInfo = "Main Gate 1"
                )
              }
            )
          }

          item {
            QuickShareRoleCard(
              title = "🏢 Front Desk Reception APK",
              subtitle = "Visitor identity verification & queue routing",
              buttonColor = Color(0xFF7C3AED),
              onShare = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = "Front Desk Receptionist",
                  userName = "Officer Anita",
                  email = "reception@${selectedTenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}",
                  mobile = "9876543211",
                  temporaryPin = "1234",
                  tenant = selectedTenant,
                  extraInfo = "Reception Desk A"
                )
              }
            )
          }

          item {
            QuickShareRoleCard(
              title = "✅ Approver & Authority APK",
              subtitle = "Flat owner / Manager one-tap green approve & red reject",
              buttonColor = Amber700,
              onShare = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = "Approver / Flat Owner",
                  userName = "Dr. Robert Smith",
                  email = "approver@${selectedTenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}",
                  mobile = "9876543212",
                  temporaryPin = "1234",
                  tenant = selectedTenant,
                  extraInfo = "Unit 402 / Director"
                )
              }
            )
          }

          item {
            QuickShareRoleCard(
              title = "🎫 Parent & Visitor Pass APK",
              subtitle = "Student early pickup request, pass QR code view",
              buttonColor = Color(0xFF16A34A),
              onShare = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = "Visitor / Parent Passholder",
                  userName = "Ananya Sharma",
                  email = "parent@${selectedTenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}",
                  mobile = "9876543213",
                  temporaryPin = "1234",
                  tenant = selectedTenant,
                  extraInfo = "Parent of Rohan (Grade 5A)"
                )
              }
            )
          }

          // Custom Recipient Dispatcher
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Custom WhatsApp APK Dispatcher", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                Text("Send APK & login to any phone number:", fontSize = 11.sp, color = Slate500)

                OutlinedTextField(
                  value = customName,
                  onValueChange = { customName = it },
                  label = { Text("Recipient Name") },
                  placeholder = { Text("e.g. Inspector Sharma") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                  value = customMobile,
                  onValueChange = { customMobile = it },
                  label = { Text("WhatsApp Phone Number") },
                  placeholder = { Text("e.g. 9845012345") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                  value = customEmail,
                  onValueChange = { customEmail = it },
                  label = { Text("Login Email ID") },
                  placeholder = { Text("sharma@${selectedTenant?.adminEmail?.substringAfter("@") ?: "tenant.com"}") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                Button(
                  onClick = {
                    SharingUtils.shareApkViaWhatsApp(
                      context = context,
                      roleTitle = customRole.displayName,
                      userName = customName.ifBlank { "SmartCheck User" },
                      email = customEmail.ifBlank { "user@${selectedTenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}" },
                      mobile = customMobile.ifBlank { "WhatsApp" },
                      temporaryPin = "1234",
                      tenant = selectedTenant,
                      extraInfo = customDesignation.ifBlank { "Assigned Staff" }
                    )
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Send APK & Credentials via WhatsApp", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // TAB 2: SANDBOX TESTING
      2 -> {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Text("DEVELOPER SANDBOX & HARDWARE SIMULATION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate600, letterSpacing = 0.5.sp)
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              SandboxLauncherCard(
                title = "CameraX Vehicle",
                sub = "ANPR Plate Reticle",
                color = Emerald600,
                icon = Icons.Default.CameraAlt,
                onClick = onLaunchVehicleScanner,
                modifier = Modifier.weight(1f)
              )

              SandboxLauncherCard(
                title = "QR Scanner",
                sub = "ZXing Hardware",
                color = Color(0xFF7C3AED),
                icon = Icons.Default.QrCodeScanner,
                onClick = onLaunchQrScanner,
                modifier = Modifier.weight(1f)
              )
            }
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              SandboxLauncherCard(
                title = "Approvals UI",
                sub = "Green / Red Review",
                color = Amber700,
                icon = Icons.Default.VerifiedUser,
                onClick = onLaunchApprovalsDashboard,
                modifier = Modifier.weight(1f)
              )

              SandboxLauncherCard(
                title = "Simulate Pass",
                sub = "Create Test Ticket",
                color = Blue600,
                icon = Icons.Default.AddCard,
                onClick = onGenerateTestPass,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }

  // CREATE TENANT DIALOG
  if (showCreateDialog) {
    RegisterTenantDialog(
      onDismiss = { showCreateDialog = false },
      onSaveAndGrantAccess = { name, code, type, customCat, role, adminEmail, contactName, mobile, hasReception, staffNames ->
        onCreateTenant(name, code, type, customCat, role, adminEmail, contactName, mobile, hasReception, staffNames)
      }
    )
  }

  // APPS SCRIPT SETUP DIALOG
  if (showDownloadScriptDialog) {
    DownloadSpreadsheetScriptDialog(
      tenant = selectedTenant,
      onDismiss = { showDownloadScriptDialog = false }
    )
  }
}

@Composable
private fun CategoryBadge(label: String, count: Int, color: Color) {
  Surface(
    color = Slate700,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate600)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.width(4.dp))
      Surface(
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("$count", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
      }
    }
  }
}

@Composable
private fun QuickShareRoleCard(
  title: String,
  subtitle: String,
  buttonColor: Color,
  onShare: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
        Text(subtitle, fontSize = 11.sp, color = Slate500)
      }

      Button(
        onClick = onShare,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Share APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun SandboxLauncherCard(
  title: String,
  sub: String,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
      Text(sub, fontSize = 10.sp, color = Slate500)
    }
  }
}
