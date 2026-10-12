package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TenantType(val displayName: String, val defaultHasReception: Boolean) {
  SCHOOL("School", true),
  HOSPITAL("Hospital", true),
  APARTMENT("Apartment", false),
  COMPANY("Company", true),
  MALLS("Malls", false),
  OTHER("Other Facility", false)
}

enum class UserRole(val displayName: String, val badgeColorHex: Long) {
  PLATFORM_ADMIN("Platform Admin", 0xFF6366F1),
  ADMIN("Tenant Admin", 0xFF0284C7),
  APPROVER("Approver / Flat Owner / Principal", 0xFFD97706),
  RECEPTION("Reception", 0xFF7C3AED),
  SECURITY("Gate Security", 0xFFDC2626),
  PARENT("Parent / Resident", 0xFF059669),
  VISITOR("Walk-in Visitor", 0xFF64748B)
}

enum class TicketStatus(val displayName: String, val stepIndex: Int) {
  PENDING_RECEPTION("At Reception Desk", 1),
  PENDING_APPROVAL("Pending Approver Sign-off", 2),
  APPROVED("Approved / QR Pass Ready", 3),
  REJECTED("Rejected by Approver", 0),
  INSIDE_PREMISES("Inside Premises", 4),
  CLOSED("Exited / Gate Pass Closed", 5)
}

enum class PassOrigin {
  GATE_WALK_IN, // Generated at the gate by Security
  APP_PRE_REGISTERED // Pre-requested via app by Parent / Resident / Visitor
}

data class Tenant(
  val tenantId: String = "TENANT-" + UUID.randomUUID().toString().take(6).uppercase(),
  val code: String,
  val name: String,
  val type: TenantType,
  val customCategory: String? = null,
  val assignedRole: String = "Tenant Administrator",
  val adminEmail: String,
  val contactName: String,
  val contactMobile: String,
  val hasReception: Boolean = type.defaultHasReception,
  val receptionStaffNames: List<String> = emptyList(), // Maximum 2 receptionists
  val securityStaffNames: List<String> = emptyList(),
  val googleFolderId: String = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL",
  val googleSheetId: String = "",
  val status: String = "ACTIVE",
  val modules: List<String> = listOf("GATE_PASS", "ANPR_VEHICLE", "VISITOR_MANAGEMENT", "EARLY_PICKUP"),
  val pwaAppDownloadUrl: String = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
  val createdAt: String = "2026-09-12"
) {
  val categoryDisplayName: String
    get() = if (type == TenantType.OTHER && !customCategory.isNullOrBlank()) customCategory else type.displayName

  /**
   * Generates dynamic monthly spreadsheet URL partitioned automatically based on the month and year
   */
  fun getMonthlySpreadsheetUrl(yearMonth: String = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())): String {
    return "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_${code}_$yearMonth"
  }

  /**
   * File name of the monthly spreadsheet with embedded AppScript
   */
  fun getMonthlySheetName(yearMonth: String = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())): String {
    val cleanName = name.replace(Regex("[^a-zA-Z0-9]"), "_")
    return "SmartCheck_${cleanName}_$yearMonth"
  }
}

data class User(
  val userId: String = "USR-" + UUID.randomUUID().toString().take(6).uppercase(),
  val tenantId: String,
  val email: String,
  val username: String,
  val mobile: String,
  val name: String,
  val role: UserRole,
  val designationOrFlat: String = "General", // e.g. "Flat 402", "Grade 5 Class Teacher", "Security Post #1"
  val active: Boolean = true,
  val createdAt: String = "2026-09-12"
)

data class Student(
  val studentId: String = "STU-" + UUID.randomUUID().toString().take(6).uppercase(),
  val tenantId: String,
  val studentName: String,
  val className: String,
  val section: String,
  val fatherName: String,
  val motherName: String,
  val fatherMobile: String,
  val motherMobile: String,
  val busNumber: String = "Bus 12",
  val routeNumber: String = "Route 4",
  val transportType: String = "SCHOOL_BUS",
  val createdAt: String = "2026-09-12"
)

data class Ticket(
  val ticketId: String = "GP-" + UUID.randomUUID().toString().take(6).uppercase(),
  val tenantId: String,
  val passOrigin: PassOrigin = PassOrigin.GATE_WALK_IN,
  val visitorName: String,
  val visitorMobile: String,
  val purpose: String, // e.g. "Early pickup for child", "Guest visiting Flat 402", "AC Maintenance"
  val targetDestination: String, // e.g. "Flat 402 - Mr. Sharma" or "Grade 5A - Alex Doe" or "Bay 14"
  val status: TicketStatus = TicketStatus.PENDING_APPROVAL,
  val vehicleNumber: String = "",
  val vehicleSnapshotUri: String? = null,
  val qrToken: String? = null,
  val qrExpiresAt: String? = null,
  val bypassReception: Boolean = false, // If true, skipped reception straight to approver
  val approverUserId: String? = null,
  val approverName: String? = null,
  val approverSignOffTime: String? = null,
  val rejectionReason: String? = null,
  val gateEntryTime: String = "2026-09-12 10:15",
  val gateExitTime: String? = null,
  val createdAt: String = "2026-09-12 10:15",
  val updatedAt: String = "2026-09-12 10:15"
)

data class TicketAction(
  val actionId: String = "ACT-" + UUID.randomUUID().toString().take(6).uppercase(),
  val tenantId: String,
  val ticketId: String,
  val step: String,
  val actionBy: String,
  val actionRole: UserRole,
  val actionType: String,
  val remarks: String,
  val timestamp: String = "2026-09-12 10:15"
)

data class GoogleSyncConfig(
  val webAppUrl: String = "",
  val sheetId: String = "",
  val folderId: String = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL",
  val lastSyncStatus: String = "Embedded Folder Linked (1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL)",
  val isSyncing: Boolean = false,
  val autoSync: Boolean = true
)

data class TenantAnalytics(
  val todayApproved: Int = 0,
  val todayPending: Int = 0,
  val todayRejected: Int = 0,
  val todayTotalVisitors: Int = 0,
  val monthTotalVisitors: Int = 0,
  val monthTotalRequests: Int = 0,
  val currentDate: String = "2026-09-12",
  val currentMonth: String = "September 2026"
)

data class MultipleTenantRegisterEntry(
  val registerId: String = "REG-" + UUID.randomUUID().toString().take(6).uppercase(),
  val tenantCode: String,
  val organizationName: String,
  val category: String,
  val assignedRole: String = "Tenant Administrator",
  val contactPerson: String,
  val email: String,
  val mobile: String,
  val receptionClearance: String = "ENABLED",
  val receptionStaffNames: List<String> = emptyList(),
  val pwaAppUrl: String = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
  val monthlySpreadsheetUrl: String = "",
  val registeredAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
  val status: String = "ACTIVE"
)

data class AppServiceHealth(
  val isWorking: Boolean = true,
  val statusLabel: String = "OPERATIONAL / WORKING",
  val latencyMs: Long = 24L,
  val lastCheckedTime: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
  val cloudEngineStatus: String = "Active (asia-southeast1)",
  val googleSheetsStatus: String = "Synced (MultipleTenantRegister)",
  val driveStorageStatus: String = "Ready (Folder: 1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL)",
  val sessionPolicyStatus: String = "1 Device 1 User Enforced"
)
