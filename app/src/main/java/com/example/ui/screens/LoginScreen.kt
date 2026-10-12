package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppServiceHealth
import com.example.model.Tenant
import com.example.model.TenantType
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.DownloadSpreadsheetScriptDialog
import com.example.ui.components.RegisterTenantDialog
import com.example.ui.theme.*
import com.example.util.SharingUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
  tenants: List<Tenant>,
  users: List<User>,
  serviceHealth: AppServiceHealth = AppServiceHealth(),
  onPingHealth: () -> Unit = {},
  onCreateTenant: ((
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
  ) -> Tenant)? = null,
  onLoginSubmit: (email: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
  val context = LocalContext.current
  var emailInput by remember { mutableStateOf("shukurhustle@gmail.com") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(false) }

  var showCreateTenantDialog by remember { mutableStateOf(false) }
  var showDownloadScriptDialog by remember { mutableStateOf(false) }

  val submitLogin = {
    if (emailInput.isBlank()) {
      errorMessage = "Please enter an email address"
    } else {
      errorMessage = null
      isLoading = true
      onLoginSubmit(emailInput.trim()) { success, msg ->
        isLoading = false
        if (success) {
          successMessage = msg
        } else {
          errorMessage = msg
        }
      }
    }
  }

  Scaffold(
    containerColor = Slate50
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(16.dp))

        // Brand Logo & Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Slate900)
              .border(2.dp, Emerald500, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "SmartCheck Security",
              tint = Emerald400,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "SmartCheck Gate Pass",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = Slate900
            )
            Text(
              text = "Multi-Tenant Cloud Security & Visitor Management",
              fontSize = 11.sp,
              color = Slate500
            )
          }
        }
      }

      // 1. FRONT PAGE DASHBOARD: NUMBER OF TENANTS & APP SERVICE STATUS
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Slate900),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Dashboard: Number of Tenants in Front Page
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = Blue400, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "DASHBOARD: NUMBER OF TENANTS",
                  color = Slate300,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              }

              Surface(
                color = Blue600,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "${tenants.size} Organizations",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Breakdown
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              val schoolCount = tenants.count { it.type == TenantType.SCHOOL }
              val hospitalCount = tenants.count { it.type == TenantType.HOSPITAL }
              val aptCount = tenants.count { it.type == TenantType.APARTMENT }
              val companyCount = tenants.count { it.type == TenantType.COMPANY }
              val mallsCount = tenants.count { it.type == TenantType.MALLS }

              TenantFrontPill("School: $schoolCount", Icons.Default.School, Blue400)
              TenantFrontPill("Hospital: $hospitalCount", Icons.Default.LocalHospital, Rose500)
              TenantFrontPill("Apartment: $aptCount", Icons.Default.Apartment, Emerald400)
              TenantFrontPill("Company: $companyCount", Icons.Default.Business, Amber500)
              TenantFrontPill("Malls: $mallsCount", Icons.Default.ShoppingCart, Color(0xFFC084FC))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Slate700)
            Spacer(modifier = Modifier.height(10.dp))

            // APP Service Working / Not Working Status
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
                    .background(if (serviceHealth.isWorking) Emerald400 else Rose500)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "APP SERVICE: ${serviceHealth.statusLabel}",
                  color = if (serviceHealth.isWorking) Emerald400 else Rose400,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black
                )
              }

              Button(
                onClick = onPingHealth,
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ping", fontSize = 10.sp, color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Latency: ${serviceHealth.latencyMs}ms • Google Sheets Sync: ${serviceHealth.googleSheetsStatus}",
              color = Slate300,
              fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Create Tenant & Share APK via WhatsApp buttons right on front page!
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showCreateTenantDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("front_page_create_tenant_button")
              ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Tenant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  val tenant = tenants.firstOrNull()
                  SharingUtils.shareApkViaWhatsApp(
                    context = context,
                    roleTitle = "All Roles Handover",
                    userName = "SmartCheck User",
                    email = "admin@${tenant?.adminEmail?.substringAfter("@") ?: "smartcheck.io"}",
                    mobile = "9876543210",
                    temporaryPin = "1234",
                    tenant = tenant,
                    extraInfo = "Universal Role APK Handover"
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("front_page_share_apk_whatsapp_button")
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share APK WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Three Color Status Indicators: Red (Security/Reject), Yellow (Pending Review), Green (Approved Entry)
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(vertical = 10.dp, horizontal = 14.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          StatusPillar(
            color = Color(0xFFDC2626),
            label = "RED",
            desc = "Denial / Stop"
          )
          Box(modifier = Modifier.height(28.dp).width(1.dp).background(Slate200))
          StatusPillar(
            color = Color(0xFFEAB308),
            label = "YELLOW",
            desc = "Pending Review"
          )
          Box(modifier = Modifier.height(28.dp).width(1.dp).background(Slate200))
          StatusPillar(
            color = Color(0xFF16A34A),
            label = "GREEN",
            desc = "Approved Entry"
          )
        }
      }

      // Login Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Enterprise Account Access",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Single-device authentication with Google Sheet domain validation",
              fontSize = 11.sp,
              color = Slate500
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
              value = emailInput,
              onValueChange = {
                emailInput = it
                errorMessage = null
              },
              label = { Text("Corporate / Registered Email") },
              placeholder = { Text("username@domain.com") },
              leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = Slate400)
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = { submitLogin() }
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input"),
              shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = errorMessage!!,
                color = Rose600,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }

            if (successMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = successMessage!!,
                color = Emerald600,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = { submitLogin() },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Slate900),
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("login_submit_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login & Grant Access", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          }
        }
      }

      // Quick-Select Registry Identities
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PLATFORM REGISTRY PROFILES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.8.sp
          )
          Text(
            text = "Tap to Auto-fill & Login",
            fontSize = 11.sp,
            color = Indigo600,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Render preset registry users
      items(users) { user ->
        val tenant = tenants.find { it.tenantId == user.tenantId }
        val roleColor = when (user.role) {
          UserRole.SECURITY -> Color(0xFFDC2626) // Red
          UserRole.APPROVER -> Color(0xFFEAB308) // Yellow / Amber
          UserRole.ADMIN -> Color(0xFF0284C7)
          UserRole.PLATFORM_ADMIN -> Color(0xFF6366F1)
          UserRole.RECEPTION -> Color(0xFF7C3AED)
          UserRole.PARENT, UserRole.VISITOR -> Color(0xFF16A34A) // Green
        }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              emailInput = user.email
              submitLogin()
            }
            .testTag("user_quick_pick_${user.userId}"),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = when (user.role) {
                    UserRole.SECURITY -> Icons.Default.Security
                    UserRole.APPROVER -> Icons.Default.VerifiedUser
                    UserRole.RECEPTION -> Icons.Default.MeetingRoom
                    UserRole.PARENT -> Icons.Default.FamilyRestroom
                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                    UserRole.PLATFORM_ADMIN -> Icons.Default.Cloud
                    UserRole.VISITOR -> Icons.Default.Person
                  },
                  contentDescription = null,
                  tint = roleColor,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = user.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Slate900
                )
                Text(
                  text = user.email,
                  fontSize = 12.sp,
                  color = Slate500
                )
                Text(
                  text = "${tenant?.name ?: "All Platforms"} • ${user.designationOrFlat}",
                  fontSize = 11.sp,
                  color = roleColor,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = roleColor.copy(alpha = 0.1f)
            ) {
              Text(
                text = user.role.displayName,
                color = roleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Create Tenant Dialog (from front page)
  if (showCreateTenantDialog && onCreateTenant != null) {
    RegisterTenantDialog(
      onDismiss = { showCreateTenantDialog = false },
      onSaveAndGrantAccess = { name, code, type, customCategory, assignedRole, adminEmail, contactName, contactMobile, hasReception, receptionStaffNames ->
        onCreateTenant(name, code, type, customCategory, assignedRole, adminEmail, contactName, contactMobile, hasReception, receptionStaffNames)
      }
    )
  }

  // Download Spreadsheet & Apps Script Dialog (from front page)
  if (showDownloadScriptDialog) {
    DownloadSpreadsheetScriptDialog(
      tenant = tenants.firstOrNull(),
      onDismiss = { showDownloadScriptDialog = false }
    )
  }
}

@Composable
private fun TenantFrontPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
  Surface(
    color = Slate800,
    shape = RoundedCornerShape(6.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(label, color = Slate200, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun StatusPillar(
  color: Color,
  label: String,
  desc: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(
        text = label,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        color = color
      )
      Text(
        text = desc,
        fontSize = 9.sp,
        color = Slate400
      )
    }
  }
}
