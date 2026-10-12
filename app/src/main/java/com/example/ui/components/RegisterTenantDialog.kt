package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Tenant
import com.example.model.TenantType
import com.example.ui.theme.*
import com.example.util.SharingUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterTenantDialog(
  onDismiss: () -> Unit,
  onSaveAndGrantAccess: (
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
  var organizationName by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<TenantType?>(TenantType.APARTMENT) }
  var isCreateNewCategory by remember { mutableStateOf(false) }
  var customCategoryName by remember { mutableStateOf("") }

  var assignedRole by remember { mutableStateOf("Tenant Administrator") }
  val availableRoles = listOf("Tenant Administrator", "Facility Lead", "Security Lead", "Approver / Flat Owner", "Medical Superintendent", "Operations Director")

  var personName by remember { mutableStateOf("") }
  var emailId by remember { mutableStateOf("") }
  var mobileNumber by remember { mutableStateOf("") }

  var hasReception by remember { mutableStateOf(false) }
  var receptionist1 by remember { mutableStateOf("") }
  var receptionist2 by remember { mutableStateOf("") }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var createdTenantResult by remember { mutableStateOf<Tenant?>(null) }

  // If a tenant has been successfully created and access granted, show the Share Dialog
  if (createdTenantResult != null) {
    val t = createdTenantResult!!
    AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Emerald600),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Access Granted & Registered!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Added to Multiple Tenant Register", fontSize = 11.sp, color = Emerald600)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            color = Slate100,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
              Text("Category: ${t.categoryDisplayName}", fontSize = 12.sp, color = Slate600)
              Text("Login Email: ${t.adminEmail}", fontSize = 12.sp, color = Blue600, fontWeight = FontWeight.SemiBold)
              Text("Reception: ${if (t.hasReception) "Enabled (${t.receptionStaffNames.joinToString()})" else "Disabled (Bypasses to Approver)"}", fontSize = 11.sp, color = Slate600)
            }
          }

          Text("SHARE APP ACCESS WITH TENANT:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)

          val inviteText = SharingUtils.formatTenantInvite(t, t.adminEmail)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                SharingUtils.shareApkViaWhatsApp(
                  context = context,
                  roleTitle = t.assignedRole,
                  userName = t.contactName,
                  email = t.adminEmail,
                  mobile = t.contactMobile,
                  temporaryPin = "1234",
                  tenant = t,
                  extraInfo = "Tenant Administrator"
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("share_tenant_whatsapp_button")
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Send APK on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { SharingUtils.shareViaEmail(context, "SmartCheck Gate Pass - Credentials for ${t.name}", inviteText) },
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("share_tenant_email_button")
            ) {
              Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Email Details", fontSize = 12.sp)
            }
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
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Blue600),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Create Tenant & Grant Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Adds to Multiple Tenant Register Spreadsheet", fontSize = 11.sp, color = Slate500)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Organization Name
        OutlinedTextField(
          value = organizationName,
          onValueChange = { organizationName = it; errorMessage = null },
          label = { Text("Organization Name *") },
          placeholder = { Text("Mention the organization / facility name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_org_name_input"),
          shape = RoundedCornerShape(10.dp)
        )

        // Category Selection
        Text("CATEGORY (SELECT OR CREATE NEW):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val predefinedCategories = listOf(
            TenantType.SCHOOL to "School",
            TenantType.HOSPITAL to "Hospital",
            TenantType.APARTMENT to "Apartment",
            TenantType.COMPANY to "Company",
            TenantType.MALLS to "Malls"
          )

          predefinedCategories.forEach { (type, label) ->
            val isSelected = !isCreateNewCategory && selectedCategory == type
            FilterChip(
              selected = isSelected,
              onClick = {
                isCreateNewCategory = false
                selectedCategory = type
                hasReception = type.defaultHasReception
              },
              label = { Text(label, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Blue600,
                selectedLabelColor = Color.White
              )
            )
          }

          FilterChip(
            selected = isCreateNewCategory,
            onClick = {
              isCreateNewCategory = true
              selectedCategory = TenantType.OTHER
            },
            label = { Text("+ Create New", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White
            )
          )
        }

        if (isCreateNewCategory) {
          OutlinedTextField(
            value = customCategoryName,
            onValueChange = { customCategoryName = it },
            label = { Text("New Category Name *") },
            placeholder = { Text("e.g. University, Airport, Warehouse") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }

        // Assign Organization Role
        Text("ASSIGN ORGANIZATION ROLE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          availableRoles.forEach { role ->
            val isSelected = assignedRole == role
            FilterChip(
              selected = isSelected,
              onClick = { assignedRole = role },
              label = { Text(role, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Slate800,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        // Person Name Correct
        OutlinedTextField(
          value = personName,
          onValueChange = { personName = it; errorMessage = null },
          label = { Text("Person Name (Point of Contact) *") },
          placeholder = { Text("Enter correct person name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_person_name_input"),
          shape = RoundedCornerShape(10.dp)
        )

        // Email ID Correct
        OutlinedTextField(
          value = emailId,
          onValueChange = { emailId = it; errorMessage = null },
          label = { Text("Email ID (Correct login address) *") },
          placeholder = { Text("e.g. manager@prestige.com") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth().testTag("tenant_email_input"),
          shape = RoundedCornerShape(10.dp)
        )

        // Mobile Number Correct
        OutlinedTextField(
          value = mobileNumber,
          onValueChange = { mobileNumber = it; errorMessage = null },
          label = { Text("Mobile Number (Correct 10 digits) *") },
          placeholder = { Text("e.g. 9876543210") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth().testTag("tenant_mobile_input"),
          shape = RoundedCornerShape(10.dp)
        )

        // Reception Clearance (Enable / Disable)
        Surface(
          color = if (hasReception) Color(0xFFF5F3FF) else Slate100,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (hasReception) Color(0xFFDDD6FE) else Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Reception Clearance: ${if (hasReception) "ENABLED" else "DISABLED"}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (hasReception) Color(0xFF6D28D9) else Slate700
                )
                Text(
                  text = if (hasReception) "Give receptionist names (maximum 2)" else "Disabled: Bypasses directly to Approver / Flat Owner",
                  fontSize = 10.sp,
                  color = Slate500
                )
              }
              Switch(
                checked = hasReception,
                onCheckedChange = { hasReception = it }
              )
            }

            if (hasReception) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = receptionist1,
                onValueChange = { receptionist1 = it },
                label = { Text("Receptionist 1 Name") },
                placeholder = { Text("e.g. Anita Desk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
              )
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedTextField(
                value = receptionist2,
                onValueChange = { receptionist2 = it },
                label = { Text("Receptionist 2 Name (Optional)") },
                placeholder = { Text("e.g. Suresh Desk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }
        }

        if (errorMessage != null) {
          Text(text = errorMessage!!, color = Rose600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (organizationName.isBlank()) {
            errorMessage = "Please enter Organization Name"
            return@Button
          }
          if (isCreateNewCategory && customCategoryName.isBlank()) {
            errorMessage = "Please enter the new Category name"
            return@Button
          }
          if (personName.isBlank()) {
            errorMessage = "Please enter correct Person Name"
            return@Button
          }
          if (emailId.isBlank() || !emailId.contains("@")) {
            errorMessage = "Please enter a valid correct Email ID"
            return@Button
          }
          if (mobileNumber.isBlank() || mobileNumber.length < 8) {
            errorMessage = "Please enter a correct Mobile Number"
            return@Button
          }

          val categoryType = if (isCreateNewCategory) TenantType.OTHER else (selectedCategory ?: TenantType.APARTMENT)
          val receptionList = mutableListOf<String>()
          if (hasReception) {
            if (receptionist1.isNotBlank()) receptionList.add(receptionist1.trim())
            if (receptionist2.isNotBlank()) receptionList.add(receptionist2.trim())
          }

          val prefix = when (categoryType) {
            TenantType.SCHOOL -> "SCH"
            TenantType.HOSPITAL -> "HSP"
            TenantType.APARTMENT -> "APT"
            TenantType.COMPANY -> "CMP"
            TenantType.MALLS -> "MLL"
            TenantType.OTHER -> "TNT"
          }
          val generatedCode = "$prefix-" + (100..999).random()

          val created = onSaveAndGrantAccess(
            organizationName.trim(),
            generatedCode,
            categoryType,
            if (isCreateNewCategory) customCategoryName.trim() else null,
            assignedRole,
            emailId.trim(),
            personName.trim(),
            mobileNumber.trim(),
            hasReception,
            receptionList
          )
          createdTenantResult = created
        },
        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("save_and_grant_access_button")
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save & Grant Access")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
