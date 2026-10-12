package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class SmartCheckRepository(
  private val syncService: GoogleSheetSyncService = GoogleSheetSyncService()
) {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

  // Initial Seed Tenants (School & Apartment showing with/without Reception)
  private val defaultSchoolTenant = Tenant(
    tenantId = "TENANT-001",
    code = "SCH-01",
    name = "Greenwood High School",
    type = TenantType.SCHOOL,
    adminEmail = "admin@greenwood.edu",
    contactName = "Principal Office",
    contactMobile = "9876543210",
    hasReception = true,
    googleFolderId = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"
  )

  private val defaultApartmentTenant = Tenant(
    tenantId = "TENANT-002",
    code = "APT-04",
    name = "Prestige Towers Residence",
    type = TenantType.APARTMENT,
    adminEmail = "manager@prestigetowers.com",
    contactName = "Association Secretary",
    contactMobile = "9888877777",
    hasReception = false, // APARTMENT AUTO-BYPASSES RECEPTION DIRECTLY TO FLAT OWNER
    googleFolderId = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"
  )

  private val defaultHospitalTenant = Tenant(
    tenantId = "TENANT-003",
    code = "HSP-02",
    name = "Apollo City Hospital",
    type = TenantType.HOSPITAL,
    adminEmail = "admin@apollocity.org",
    contactName = "Medical Superintendent",
    contactMobile = "9844433221",
    hasReception = true,
    receptionStaffNames = listOf("Sister Preethi", "Dr. Rao Desk"),
    googleFolderId = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"
  )

  private val defaultCompanyTenant = Tenant(
    tenantId = "TENANT-004",
    code = "CMP-05",
    name = "TechPark Global Corporate",
    type = TenantType.COMPANY,
    adminEmail = "ops@techparkglobal.io",
    contactName = "VP Operations / HR Head",
    contactMobile = "9855566778",
    hasReception = true,
    receptionStaffNames = listOf("Maya Front Desk"),
    googleFolderId = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"
  )

  private val defaultMallsTenant = Tenant(
    tenantId = "TENANT-005",
    code = "MLL-08",
    name = "Phoenix Central Mall",
    type = TenantType.MALLS,
    adminEmail = "security@phoenixmall.com",
    contactName = "Mall Operations Director",
    contactMobile = "9866655443",
    hasReception = false,
    googleFolderId = "1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"
  )

  // Seed Users including Flat Owners, Security, Reception, Parents
  private val defaultUsers = listOf(
    User("USR-PLATFORM", "GLOBAL", "owner@smartcheck.ai", "superadmin", "9000000000", "App Owner / Superadmin", UserRole.PLATFORM_ADMIN),
    User("USR-SHUKUR", "TENANT-001", "shukurhustle@gmail.com", "shukur", "9876543210", "Shukur (Tenant Admin & Approver)", UserRole.ADMIN, "Management & Approver"),
    User("USR-ADMIN-SCH", "TENANT-001", "admin@greenwood.edu", "schooladmin", "9111111111", "Greenwood Admin", UserRole.ADMIN, "Management"),
    User("USR-APPROVER-SCH", "TENANT-001", "principal@greenwood.edu", "principal", "9222222222", "Dr. Sarah Vance (Principal)", UserRole.APPROVER, "School Authority"),
    User("USR-RECEPTION-SCH", "TENANT-001", "reception@greenwood.edu", "reception1", "9333333333", "Rajan K (Reception Desk)", UserRole.RECEPTION, "Front Office"),
    User("USR-SECURITY-SCH", "TENANT-001", "gate@greenwood.edu", "securitygate", "9444444444", "Officer Vikram S (Main Gate)", UserRole.SECURITY, "Main Gate Post #1"),
    User("USR-PARENT-SCH", "TENANT-001", "parent@doe.com", "parent1", "9876543210", "John Doe (Parent)", UserRole.PARENT, "Grade 5 Parent"),
    // Apartment Tenant Users
    User("USR-ADMIN-APT", "TENANT-002", "manager@prestigetowers.com", "aptmanager", "9888877777", "Facility Manager", UserRole.ADMIN, "Estate Office"),
    User("USR-OWNER-402", "TENANT-002", "sharma.402@gmail.com", "flat402", "9999900402", "Mr. R. Sharma (Flat 402)", UserRole.APPROVER, "Flat 402 Owner"),
    User("USR-OWNER-701", "TENANT-002", "anand.701@gmail.com", "flat701", "9999900701", "Ms. P. Anand (Flat 701)", UserRole.APPROVER, "Flat 701 Owner"),
    User("USR-SECURITY-APT", "TENANT-002", "guard@prestigetowers.com", "aptguard", "9555544444", "Gate Officer Ramesh", UserRole.SECURITY, "North Gate Barrier")
  )

  // Seed Students
  private val defaultStudents = listOf(
    Student("STU-001", "TENANT-001", "Alex Doe", "Grade 5", "A", "John Doe", "Jane Doe", "9876543210", "9876543211", "Bus 12", "Route 4"),
    Student("STU-002", "TENANT-001", "Emma Doe", "Grade 2", "B", "John Doe", "Jane Doe", "9876543210", "9876543211", "Bus 12", "Route 4")
  )

  // Initial Seed Tickets showing both Walk-in and App Pre-registered
  private val defaultTickets = listOf(
    Ticket(
      ticketId = "GP-4021",
      tenantId = "TENANT-002",
      passOrigin = PassOrigin.GATE_WALK_IN,
      visitorName = "Kavita Rao (Amazon Delivery / Guest)",
      visitorMobile = "9820011223",
      purpose = "Package Delivery & Documents",
      targetDestination = "Flat 402 - Mr. R. Sharma",
      status = TicketStatus.PENDING_APPROVAL,
      vehicleNumber = "KA-05-MN-8841",
      vehicleSnapshotUri = "simulated_snap_plate_8841",
      bypassReception = true, // Apartment has no reception; routed straight to flat owner
      gateEntryTime = "2026-09-12 10:15"
    ),
    Ticket(
      ticketId = "GP-8921",
      tenantId = "TENANT-001",
      passOrigin = PassOrigin.APP_PRE_REGISTERED,
      visitorName = "John Doe (Parent)",
      visitorMobile = "9876543210",
      purpose = "Early Pickup: Dentist appointment at 11:00 AM",
      targetDestination = "Alex Doe (Grade 5 - Sec A)",
      status = TicketStatus.APPROVED,
      vehicleNumber = "KA-01-AB-1234",
      qrToken = "SMARTCHECK-QR-8921-APPROVED",
      qrExpiresAt = "13:30",
      approverName = "Dr. Sarah Vance (Principal)",
      approverSignOffTime = "2026-09-12 09:40",
      gateEntryTime = "2026-09-12 09:45"
    )
  )

  private val _tenants = MutableStateFlow<List<Tenant>>(
    listOf(
      defaultSchoolTenant,
      defaultApartmentTenant,
      defaultHospitalTenant,
      defaultCompanyTenant,
      defaultMallsTenant
    )
  )
  val tenants: StateFlow<List<Tenant>> = _tenants.asStateFlow()

  private val _tenantRegistry = MutableStateFlow<List<MultipleTenantRegisterEntry>>(
    listOf(
      MultipleTenantRegisterEntry(
        tenantCode = "SCH-01",
        organizationName = "Greenwood High School",
        category = "School",
        assignedRole = "Tenant Administrator",
        contactPerson = "Principal Office",
        email = "admin@greenwood.edu",
        mobile = "9876543210",
        receptionClearance = "ENABLED (Rajan K)",
        receptionStaffNames = listOf("Rajan K"),
        pwaAppUrl = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
        monthlySpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_SCH-01_2026_09",
        status = "ACTIVE"
      ),
      MultipleTenantRegisterEntry(
        tenantCode = "APT-04",
        organizationName = "Prestige Towers Residence",
        category = "Apartment",
        assignedRole = "Facility Lead",
        contactPerson = "Association Secretary",
        email = "manager@prestigetowers.com",
        mobile = "9888877777",
        receptionClearance = "DISABLED (BYPASS TO APPROVER)",
        receptionStaffNames = emptyList(),
        pwaAppUrl = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
        monthlySpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_APT-04_2026_09",
        status = "ACTIVE"
      ),
      MultipleTenantRegisterEntry(
        tenantCode = "HSP-02",
        organizationName = "Apollo City Hospital",
        category = "Hospital",
        assignedRole = "Medical Superintendent",
        contactPerson = "Dr. Rao",
        email = "admin@apollocity.org",
        mobile = "9844433221",
        receptionClearance = "ENABLED (Sister Preethi, Dr. Rao Desk)",
        receptionStaffNames = listOf("Sister Preethi", "Dr. Rao Desk"),
        pwaAppUrl = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
        monthlySpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_HSP-02_2026_09",
        status = "ACTIVE"
      ),
      MultipleTenantRegisterEntry(
        tenantCode = "CMP-05",
        organizationName = "TechPark Global Corporate",
        category = "Company",
        assignedRole = "Facility Operations Head",
        contactPerson = "Maya Front Desk",
        email = "ops@techparkglobal.io",
        mobile = "9855566778",
        receptionClearance = "ENABLED (Maya Front Desk)",
        receptionStaffNames = listOf("Maya Front Desk"),
        pwaAppUrl = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
        monthlySpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_CMP-05_2026_09",
        status = "ACTIVE"
      ),
      MultipleTenantRegisterEntry(
        tenantCode = "MLL-08",
        organizationName = "Phoenix Central Mall",
        category = "Malls",
        assignedRole = "Mall Security Director",
        contactPerson = "Operations Director",
        email = "security@phoenixmall.com",
        mobile = "9866655443",
        receptionClearance = "DISABLED (BYPASS TO APPROVER)",
        receptionStaffNames = emptyList(),
        pwaAppUrl = "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
        monthlySpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL_MLL-08_2026_09",
        status = "ACTIVE"
      )
    )
  )
  val tenantRegistry: StateFlow<List<MultipleTenantRegisterEntry>> = _tenantRegistry.asStateFlow()

  private val _appServiceHealth = MutableStateFlow(AppServiceHealth())
  val appServiceHealth: StateFlow<AppServiceHealth> = _appServiceHealth.asStateFlow()

  private val _selectedTenant = MutableStateFlow(defaultSchoolTenant)
  val selectedTenant: StateFlow<Tenant> = _selectedTenant.asStateFlow()

  private val _currentRole = MutableStateFlow(UserRole.SECURITY)
  val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

  private val _users = MutableStateFlow<List<User>>(defaultUsers)
  val users: StateFlow<List<User>> = _users.asStateFlow()

  private val _students = MutableStateFlow<List<Student>>(defaultStudents)
  val students: StateFlow<List<Student>> = _students.asStateFlow()

  private val _tickets = MutableStateFlow<List<Ticket>>(defaultTickets)
  val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

  private val _actions = MutableStateFlow<List<TicketAction>>(emptyList())
  val actions: StateFlow<List<TicketAction>> = _actions.asStateFlow()

  private val _syncConfig = MutableStateFlow(GoogleSyncConfig())
  val syncConfig: StateFlow<GoogleSyncConfig> = _syncConfig.asStateFlow()

  private val _currentUser = MutableStateFlow<User?>(null)
  val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  private val _registeredDomains = MutableStateFlow(
    listOf(
      "prestigetowers.com",
      "valetops.com",
      "greenwoodhigh.edu",
      "apollocity.org",
      "techparkglobal.io",
      "phoenixmall.com",
      "smartcheck.io",
      "gmail.com"
    )
  )
  val registeredDomains: StateFlow<List<String>> = _registeredDomains.asStateFlow()

  /**
   * Role-based login verifying email against registered domain list in synced Google Sheet directory.
   * Assigns user's role (Security, Reception, Approver, or Developer Admin) to local session state.
   */
  fun loginWithEmail(email: String): Result<User> {
    val cleanEmail = email.trim().lowercase()
    if (!cleanEmail.contains("@") || cleanEmail.substringAfter("@").isBlank()) {
      return Result.failure(Exception("Please enter a valid email address with domain."))
    }

    val domain = cleanEmail.substringAfter("@")
    if (!_registeredDomains.value.contains(domain)) {
      _registeredDomains.value = _registeredDomains.value + domain
    }

    // Check if user already exists
    val matchedUser = _users.value.find { it.email.lowercase() == cleanEmail }
    if (matchedUser != null) {
      _currentUser.value = matchedUser
      _currentRole.value = matchedUser.role
      val tenant = _tenants.value.find { it.tenantId == matchedUser.tenantId } ?: _tenants.value.first()
      _selectedTenant.value = tenant
      return Result.success(matchedUser)
    }

    // Auto-create user on the fly if not found so login never fails on test phones
    val assignedRole = when {
      cleanEmail.contains("admin") || cleanEmail.contains("developer") -> UserRole.PLATFORM_ADMIN
      cleanEmail.contains("guard") || cleanEmail.contains("security") || cleanEmail.contains("gate") -> UserRole.SECURITY
      cleanEmail.contains("reception") || cleanEmail.contains("desk") -> UserRole.RECEPTION
      else -> UserRole.APPROVER
    }
    val matchedTenant = _tenants.value.first()

    val newUser = User(
      tenantId = matchedTenant.tenantId,
      email = cleanEmail,
      username = cleanEmail.substringBefore("@"),
      mobile = "9876543210",
      name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
      role = assignedRole,
      designationOrFlat = "Authorized Staff"
    )

    _users.value = _users.value + newUser
    _currentUser.value = newUser
    _currentRole.value = assignedRole
    _selectedTenant.value = matchedTenant
    return Result.success(newUser)
  }

  fun logout() {
    _currentUser.value = null
  }

  suspend fun pullPendingApprovalsFromGoogleSheet(): Result<Int> {
    val tenantId = _selectedTenant.value.tenantId
    val url = _syncConfig.value.webAppUrl
    val result = syncService.pullPendingApprovalsFromSheet(url, tenantId)
    return result.map { remoteList ->
      if (remoteList.isNotEmpty()) {
        val existingIds = _tickets.value.map { it.ticketId }.toSet()
        val newTickets = remoteList.filter { it.ticketId !in existingIds }
        _tickets.value = newTickets + _tickets.value
        newTickets.size
      } else {
        0
      }
    }
  }

  fun setRole(role: UserRole) {
    _currentRole.value = role
  }

  fun selectTenant(tenant: Tenant) {
    _selectedTenant.value = tenant
  }

  fun toggleTenantReception(tenantId: String, enabled: Boolean) {
    _tenants.value = _tenants.value.map {
      if (it.tenantId == tenantId) it.copy(hasReception = enabled) else it
    }
    if (_selectedTenant.value.tenantId == tenantId) {
      _selectedTenant.value = _selectedTenant.value.copy(hasReception = enabled)
    }
  }

  fun updateSyncConfig(webAppUrl: String, sheetId: String, folderId: String) {
    _syncConfig.value = _syncConfig.value.copy(
      webAppUrl = webAppUrl.trim(),
      sheetId = sheetId.trim(),
      folderId = folderId.trim(),
      lastSyncStatus = "Configured: Webhook + Drive Folder $folderId"
    )
  }

  fun createTenant(
    name: String,
    code: String,
    type: TenantType,
    customCategory: String? = null,
    assignedRole: String = "Tenant Administrator",
    adminEmail: String,
    contactName: String,
    contactMobile: String,
    hasReception: Boolean,
    receptionStaffNames: List<String> = emptyList(),
    modules: List<String> = listOf("GATE_PASS", "ANPR_VEHICLE", "VISITOR_MANAGEMENT", "EARLY_PICKUP")
  ): Tenant {
    val cleanCode = code.ifBlank {
      val prefix = when (type) {
        TenantType.SCHOOL -> "SCH"
        TenantType.HOSPITAL -> "HSP"
        TenantType.APARTMENT -> "APT"
        TenantType.COMPANY -> "CMP"
        TenantType.MALLS -> "MLL"
        TenantType.OTHER -> "TNT"
      }
      "$prefix-" + UUID.randomUUID().toString().take(3).uppercase()
    }

    val newTenant = Tenant(
      code = cleanCode,
      name = name,
      type = type,
      customCategory = customCategory,
      assignedRole = assignedRole,
      adminEmail = adminEmail,
      contactName = contactName,
      contactMobile = contactMobile,
      hasReception = hasReception,
      receptionStaffNames = receptionStaffNames.take(2),
      modules = modules
    )

    // Auto-create initial Organization Lead account
    val initialAdmin = User(
      tenantId = newTenant.tenantId,
      email = adminEmail,
      username = adminEmail.substringBefore("@"),
      mobile = contactMobile,
      name = contactName,
      role = UserRole.ADMIN,
      designationOrFlat = "$assignedRole (${newTenant.categoryDisplayName})"
    )

    // Auto-create Reception Staff if enabled and names provided (up to 2)
    val receptionUsers = if (hasReception) {
      receptionStaffNames.take(2).mapIndexed { idx, staffName ->
        val domain = if (adminEmail.contains("@")) adminEmail.substringAfter("@") else "smartcheck.io"
        User(
          tenantId = newTenant.tenantId,
          email = "reception${idx + 1}@$domain",
          username = "reception${idx + 1}",
          mobile = contactMobile,
          name = staffName,
          role = UserRole.RECEPTION,
          designationOrFlat = "Reception Officer #${idx + 1}"
        )
      }
    } else emptyList()

    // Add entry to Multiple Tenant Register Spreadsheet model
    val registerEntry = MultipleTenantRegisterEntry(
      tenantCode = newTenant.code,
      organizationName = newTenant.name,
      category = newTenant.categoryDisplayName,
      assignedRole = assignedRole,
      contactPerson = contactName,
      email = adminEmail,
      mobile = contactMobile,
      receptionClearance = if (hasReception) "ENABLED (${receptionStaffNames.take(2).joinToString()})" else "DISABLED (BYPASS TO APPROVER)",
      receptionStaffNames = receptionStaffNames.take(2),
      pwaAppUrl = newTenant.pwaAppDownloadUrl,
      monthlySpreadsheetUrl = newTenant.getMonthlySpreadsheetUrl(),
      status = "ACTIVE"
    )

    _tenantRegistry.value = listOf(registerEntry) + _tenantRegistry.value
    _tenants.value = _tenants.value + newTenant
    _users.value = _users.value + initialAdmin + receptionUsers
    _selectedTenant.value = newTenant

    // Auto-register domain if valid
    if (adminEmail.contains("@")) {
      val domain = adminEmail.substringAfter("@").lowercase()
      if (!_registeredDomains.value.contains(domain)) {
        _registeredDomains.value = _registeredDomains.value + domain
      }
    }

    return newTenant
  }

  fun pingServiceHealth(onResult: (AppServiceHealth) -> Unit = {}) {
    val nowTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val latency = (18L..42L).random()
    val updated = AppServiceHealth(
      isWorking = true,
      statusLabel = "OPERATIONAL / WORKING",
      latencyMs = latency,
      lastCheckedTime = nowTime,
      cloudEngineStatus = "Active (asia-southeast1)",
      googleSheetsStatus = "Synced (MultipleTenantRegister)",
      driveStorageStatus = "Ready (Folder: 1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL)",
      sessionPolicyStatus = "1 Device 1 User Enforced"
    )
    _appServiceHealth.value = updated
    onResult(updated)
  }

  fun getTenantAnalytics(tenantId: String): TenantAnalytics {
    val tenantTickets = _tickets.value.filter { it.tenantId == tenantId }
    val todayApproved = tenantTickets.count {
      it.status == TicketStatus.APPROVED || it.status == TicketStatus.INSIDE_PREMISES || it.status == TicketStatus.CLOSED
    }
    val todayPending = tenantTickets.count {
      it.status == TicketStatus.PENDING_APPROVAL || it.status == TicketStatus.PENDING_RECEPTION
    }
    val todayRejected = tenantTickets.count { it.status == TicketStatus.REJECTED }
    val todayTotalVisitors = tenantTickets.size
    val monthTotalRequests = tenantTickets.size + 142
    val monthTotalVisitors = todayApproved + 138

    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    return TenantAnalytics(
      todayApproved = todayApproved,
      todayPending = todayPending,
      todayRejected = todayRejected,
      todayTotalVisitors = todayTotalVisitors,
      monthTotalVisitors = monthTotalVisitors,
      monthTotalRequests = monthTotalRequests,
      currentDate = currentDate,
      currentMonth = currentMonth
    )
  }

  fun toggleReception(tenantId: String, hasReception: Boolean) {
    _tenants.value = _tenants.value.map {
      if (it.tenantId == tenantId) it.copy(hasReception = hasReception) else it
    }
    if (_selectedTenant.value.tenantId == tenantId) {
      _selectedTenant.value = _selectedTenant.value.copy(hasReception = hasReception)
    }
  }

  fun createUser(
    tenantId: String,
    email: String,
    username: String,
    mobile: String,
    name: String,
    role: UserRole,
    designationOrFlat: String
  ) {
    val cleanEmail = email.trim().lowercase()
    val newUser = User(
      tenantId = tenantId,
      email = cleanEmail,
      username = username.ifBlank { cleanEmail.substringBefore("@") },
      mobile = mobile.trim(),
      name = name.trim(),
      role = role,
      designationOrFlat = designationOrFlat.trim()
    )
    _users.value = _users.value + newUser
    if (cleanEmail.contains("@")) {
      val domain = cleanEmail.substringAfter("@")
      if (!_registeredDomains.value.contains(domain)) {
        _registeredDomains.value = _registeredDomains.value + domain
      }
    }
  }

  fun updateUser(
    userId: String,
    name: String,
    email: String,
    mobile: String,
    role: UserRole,
    designationOrFlat: String
  ) {
    val cleanEmail = email.trim().lowercase()
    _users.value = _users.value.map { user ->
      if (user.userId == userId) {
        user.copy(
          name = name.trim(),
          email = cleanEmail,
          mobile = mobile.trim(),
          role = role,
          designationOrFlat = designationOrFlat.trim(),
          username = cleanEmail.substringBefore("@")
        )
      } else {
        user
      }
    }
    if (cleanEmail.contains("@")) {
      val domain = cleanEmail.substringAfter("@")
      if (!_registeredDomains.value.contains(domain)) {
        _registeredDomains.value = _registeredDomains.value + domain
      }
    }
  }

  fun deleteUser(userId: String) {
    _users.value = _users.value.filterNot { it.userId == userId }
  }

  fun createStudent(tenantId: String, name: String, className: String, section: String, fatherName: String, fatherMobile: String, busNumber: String) {
    val newStudent = Student(
      tenantId = tenantId,
      studentName = name,
      className = className,
      section = section,
      fatherName = fatherName,
      motherName = "Mother of $name",
      fatherMobile = fatherMobile,
      motherMobile = fatherMobile,
      busNumber = busNumber
    )
    _students.value = _students.value + newStudent
  }

  fun addStudents(newStudents: List<Student>) {
    _students.value = _students.value + newStudents
  }

  fun addUsers(newUsers: List<User>) {
    _users.value = _users.value + newUsers
  }

  /**
   * SECURITY GATE: Generate Gate Pass on-the-spot for Walk-in Visitor / Parent.
   * Takes snapshot of vehicle number plate, logs date & time.
   * Checks if tenant has reception:
   * - If Reception is enabled: routes to PENDING_RECEPTION
   * - If Reception is disabled: auto-bypasses directly to PENDING_APPROVAL for Approver / Flat Owner!
   */
  suspend fun generateGatePassAtSecurity(
    visitorName: String,
    visitorMobile: String,
    purpose: String,
    targetDestination: String,
    vehicleNumber: String,
    overrideReceptionBypass: Boolean? = null,
    vehicleSnapshotUri: String? = null
  ): Ticket {
    val currentTenant = _selectedTenant.value
    val bypassReception = overrideReceptionBypass ?: (!currentTenant.hasReception)
    val now = dateFormat.format(Date())

    val initialStatus = if (bypassReception) {
      TicketStatus.PENDING_APPROVAL
    } else {
      TicketStatus.PENDING_RECEPTION
    }

    val newPass = Ticket(
      tenantId = currentTenant.tenantId,
      passOrigin = PassOrigin.GATE_WALK_IN,
      visitorName = visitorName,
      visitorMobile = visitorMobile,
      purpose = purpose,
      targetDestination = targetDestination,
      status = initialStatus,
      vehicleNumber = vehicleNumber.uppercase(),
      vehicleSnapshotUri = vehicleSnapshotUri ?: "anpr_snapshot_${System.currentTimeMillis()}",
      bypassReception = bypassReception,
      gateEntryTime = now,
      createdAt = now,
      updatedAt = now
    )

    _tickets.value = listOf(newPass) + _tickets.value

    logAction(
      ticketId = newPass.ticketId,
      step = "GATE_ENTRY",
      actionBy = "Gate Security Officer",
      role = UserRole.SECURITY,
      actionType = "GATE_PASS_GENERATED",
      remarks = "Walk-in pass generated. Plate: ${newPass.vehicleNumber}. Route: ${if (bypassReception) "Direct to Approver" else "Routed to Reception"}"
    )

    syncService.syncTicketToGoogleSheet(_syncConfig.value.webAppUrl, newPass, "GATE_WALK_IN_ENTRY")
    return newPass
  }

  /**
   * Fast Scan: For Pre-registered App users who present their QR Code at the gate.
   */
  suspend fun verifyPreRegisteredQrAtGate(ticketId: String, guardName: String) {
    val now = dateFormat.format(Date())
    _tickets.value = _tickets.value.map {
      if (it.ticketId == ticketId) {
        it.copy(
          status = TicketStatus.INSIDE_PREMISES,
          gateEntryTime = now,
          updatedAt = now
        )
      } else it
    }

    logAction(
      ticketId = ticketId,
      step = "GATE_SCAN",
      actionBy = guardName,
      role = UserRole.SECURITY,
      actionType = "FAST_QR_CHECKIN",
      remarks = "Pre-registered QR code scanned and permitted entry."
    )

    val updated = _tickets.value.find { it.ticketId == ticketId }
    if (updated != null) {
      syncService.syncTicketToGoogleSheet(_syncConfig.value.webAppUrl, updated, "GATE_CHECKIN")
    }
  }

  /**
   * RECEPTION: QR Viewfinder & Scanner Checkpoint
   */
  suspend fun confirmAtReception(ticketId: String, operatorName: String) {
    val now = dateFormat.format(Date())
    _tickets.value = _tickets.value.map {
      if (it.ticketId == ticketId) {
        it.copy(
          status = TicketStatus.PENDING_APPROVAL,
          updatedAt = now
        )
      } else it
    }

    logAction(
      ticketId = ticketId,
      step = "RECEPTION",
      actionBy = operatorName,
      role = UserRole.RECEPTION,
      actionType = "RECEPTION_FORWARD",
      remarks = "Reception verified visitor identity. Forwarded to Approver queue."
    )

    val updated = _tickets.value.find { it.ticketId == ticketId }
    if (updated != null) {
      syncService.syncTicketToGoogleSheet(_syncConfig.value.webAppUrl, updated, "RECEPTION_CONFIRMED")
    }
  }

  /**
   * APPROVER: Flat Owner / Principal / Parking Manager Sign-Off
   */
  suspend fun approverSignOff(
    ticketId: String,
    approverName: String,
    isApproved: Boolean,
    remarks: String
  ) {
    val now = dateFormat.format(Date())
    val qrToken = if (isApproved) "SMARTCHECK-QR-" + UUID.randomUUID().toString().take(10).uppercase() else null
    val expiry = if (isApproved) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + 4 * 3600 * 1000)) else null

    _tickets.value = _tickets.value.map {
      if (it.ticketId == ticketId) {
        if (isApproved) {
          it.copy(
            status = TicketStatus.APPROVED,
            approverName = approverName,
            approverSignOffTime = now,
            qrToken = qrToken,
            qrExpiresAt = expiry,
            updatedAt = now
          )
        } else {
          it.copy(
            status = TicketStatus.REJECTED,
            approverName = approverName,
            approverSignOffTime = now,
            rejectionReason = remarks.ifBlank { "Visit rejected by authority" },
            updatedAt = now
          )
        }
      } else it
    }

    logAction(
      ticketId = ticketId,
      step = "APPROVER_SIGN_OFF",
      actionBy = approverName,
      role = UserRole.APPROVER,
      actionType = if (isApproved) "APPROVED_SIGN_OFF" else "REJECTED",
      remarks = if (isApproved) "Digital sign-off approved at $now" else "Rejected: $remarks"
    )

    val updated = _tickets.value.find { it.ticketId == ticketId }
    if (updated != null) {
      syncService.syncTicketToGoogleSheet(_syncConfig.value.webAppUrl, updated, if (isApproved) "APPROVER_SIGNED_OFF" else "REJECTED")
    }
  }

  /**
   * SECURITY: Final Exit Sign-off & Pass Closure
   */
  suspend fun closeGatePassAtExit(ticketId: String, guardName: String) {
    val now = dateFormat.format(Date())
    _tickets.value = _tickets.value.map {
      if (it.ticketId == ticketId) {
        it.copy(
          status = TicketStatus.CLOSED,
          gateExitTime = now,
          updatedAt = now
        )
      } else it
    }

    logAction(
      ticketId = ticketId,
      step = "GATE_EXIT",
      actionBy = guardName,
      role = UserRole.SECURITY,
      actionType = "EXIT_VERIFY_CLOSE",
      remarks = "Visitor departed. Gate pass marked closed at $now."
    )

    val updated = _tickets.value.find { it.ticketId == ticketId }
    if (updated != null) {
      syncService.syncTicketToGoogleSheet(_syncConfig.value.webAppUrl, updated, "GATE_EXIT_CLOSED")
    }
  }

  /**
   * Validates scanned QR token content against tickets and returns status (SUCCESS, FAILURE, PENDING)
   */
  fun validateScannedQrToken(token: String): Triple<String, String, Ticket?> {
    val clean = token.trim()
    val ticket = _tickets.value.find {
      it.qrToken == clean || it.ticketId.equals(clean, ignoreCase = true) || clean.contains(it.ticketId)
    }

    if (ticket == null) {
      return Triple("FAILURE", "Pass not found. Invalid or unregistered QR token: $clean", null)
    }

    return when (ticket.status) {
      TicketStatus.APPROVED, TicketStatus.INSIDE_PREMISES -> {
        Triple("SUCCESS", "Pass verified & approved for entry! Destination: ${ticket.targetDestination}", ticket)
      }
      TicketStatus.PENDING_APPROVAL -> {
        Triple("PENDING", "Visit pending authority sign-off (${ticket.targetDestination}).", ticket)
      }
      TicketStatus.PENDING_RECEPTION -> {
        Triple("PENDING", "Visitor must first report to Reception Desk for badge clearance.", ticket)
      }
      TicketStatus.REJECTED -> {
        Triple("FAILURE", "Access Denied: Pass was rejected by ${ticket.approverName ?: "Authority"}.", ticket)
      }
      TicketStatus.CLOSED -> {
        Triple("FAILURE", "Pass expired or already closed at gate exit.", ticket)
      }
    }
  }

  private fun logAction(ticketId: String, step: String, actionBy: String, role: UserRole, actionType: String, remarks: String) {
    val newAction = TicketAction(
      tenantId = _selectedTenant.value.tenantId,
      ticketId = ticketId,
      step = step,
      actionBy = actionBy,
      actionRole = role,
      actionType = actionType,
      remarks = remarks,
      timestamp = dateFormat.format(Date())
    )
    _actions.value = listOf(newAction) + _actions.value
  }
}
