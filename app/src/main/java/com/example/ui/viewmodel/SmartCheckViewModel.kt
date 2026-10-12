package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SmartCheckRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SmartCheckUiState(
  val currentUser: User? = null,
  val currentRole: UserRole = UserRole.SECURITY,
  val selectedTenant: Tenant? = null,
  val tenants: List<Tenant> = emptyList(),
  val users: List<User> = emptyList(),
  val students: List<Student> = emptyList(),
  val tickets: List<Ticket> = emptyList(),
  val actions: List<TicketAction> = emptyList(),
  val syncConfig: GoogleSyncConfig = GoogleSyncConfig(),
  val tenantRegistry: List<MultipleTenantRegisterEntry> = emptyList(),
  val appServiceHealth: AppServiceHealth = AppServiceHealth(),
  val message: String? = null
)

private data class CoreContext(
  val currentUser: User?,
  val role: UserRole,
  val tenant: Tenant,
  val sync: GoogleSyncConfig,
  val health: AppServiceHealth
)

private data class BaseData(
  val tenants: List<Tenant>,
  val users: List<User>,
  val students: List<Student>,
  val tickets: List<Ticket>,
  val actions: List<TicketAction>
)

private data class DataContext(
  val tenants: List<Tenant>,
  val users: List<User>,
  val students: List<Student>,
  val tickets: List<Ticket>,
  val actions: List<TicketAction>,
  val registry: List<MultipleTenantRegisterEntry>
)

class SmartCheckViewModel(
  private val repository: SmartCheckRepository = SmartCheckRepository()
) : ViewModel() {

  private val coreFlow = combine(
    repository.currentUser,
    repository.currentRole,
    repository.selectedTenant,
    repository.syncConfig,
    repository.appServiceHealth
  ) { user, role, tenant, sync, health ->
    CoreContext(user, role, tenant, sync, health)
  }

  private val baseDataFlow = combine(
    repository.tenants,
    repository.users,
    repository.students,
    repository.tickets,
    repository.actions
  ) { tenants, users, students, tickets, actions ->
    BaseData(tenants, users, students, tickets, actions)
  }

  private val dataFlow = combine(
    baseDataFlow,
    repository.tenantRegistry
  ) { base, registry ->
    DataContext(base.tenants, base.users, base.students, base.tickets, base.actions, registry)
  }

  val uiState: StateFlow<SmartCheckUiState> = combine(
    coreFlow,
    dataFlow
  ) { core, data ->
    val tId = core.tenant.tenantId
    SmartCheckUiState(
      currentUser = core.currentUser,
      currentRole = core.role,
      selectedTenant = core.tenant,
      tenants = data.tenants,
      users = data.users.filter { it.tenantId == tId || it.role == UserRole.PLATFORM_ADMIN },
      students = data.students.filter { it.tenantId == tId },
      tickets = data.tickets.filter { it.tenantId == tId },
      actions = data.actions.filter { it.tenantId == tId },
      syncConfig = core.sync,
      tenantRegistry = data.registry,
      appServiceHealth = core.health
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartCheckUiState())

  val registeredDomains: StateFlow<List<String>> = repository.registeredDomains

  fun loginWithEmail(email: String, onResult: (Boolean, String, UserRole?) -> Unit) {
    val result = repository.loginWithEmail(email)
    result.onSuccess { user ->
      onResult(true, "Welcome, ${user.name} (${user.role.displayName})", user.role)
    }.onFailure { err ->
      onResult(false, err.message ?: "Authentication failed", null)
    }
  }

  fun validateScannedQrToken(token: String): Triple<String, String, Ticket?> {
    return repository.validateScannedQrToken(token)
  }

  fun logout() {
    repository.logout()
  }

  fun pullPendingApprovals(onComplete: (Int) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.pullPendingApprovalsFromGoogleSheet()
      onComplete(res.getOrDefault(0))
    }
  }

  fun setRole(role: UserRole) {
    repository.setRole(role)
  }

  fun selectTenant(tenant: Tenant) {
    repository.selectTenant(tenant)
  }

  fun toggleTenantReception(tenantId: String, enabled: Boolean) {
    repository.toggleTenantReception(tenantId, enabled)
  }

  fun updateGoogleSyncConfig(webAppUrl: String, sheetId: String, folderId: String) {
    repository.updateSyncConfig(webAppUrl, sheetId, folderId)
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
    return repository.createTenant(
      name = name,
      code = code,
      type = type,
      customCategory = customCategory,
      assignedRole = assignedRole,
      adminEmail = adminEmail,
      contactName = contactName,
      contactMobile = contactMobile,
      hasReception = hasReception,
      receptionStaffNames = receptionStaffNames,
      modules = modules
    )
  }

  fun pingServiceHealth(onResult: (AppServiceHealth) -> Unit = {}) {
    repository.pingServiceHealth(onResult)
  }

  fun getTenantAnalytics(tenantId: String): TenantAnalytics {
    return repository.getTenantAnalytics(tenantId)
  }


  fun createUser(
    email: String,
    username: String,
    mobile: String,
    name: String,
    role: UserRole,
    designationOrFlat: String
  ) {
    val tenantId = uiState.value.selectedTenant?.tenantId ?: "TENANT-001"
    repository.createUser(tenantId, email, username, mobile, name, role, designationOrFlat)
  }

  fun createStudent(name: String, className: String, section: String, fatherName: String, fatherMobile: String, busNumber: String) {
    val tenantId = uiState.value.selectedTenant?.tenantId ?: "TENANT-001"
    repository.createStudent(tenantId, name, className, section, fatherName, fatherMobile, busNumber)
  }

  fun importStudentsCsv(csvContent: String): com.example.util.CsvUtils.CsvParseResult<Student> {
    val tenantId = uiState.value.selectedTenant?.tenantId ?: "TENANT-001"
    val result = com.example.util.CsvUtils.parseStudentsParentsCsv(csvContent, tenantId)
    if (result.success && result.items.isNotEmpty()) {
      repository.addStudents(result.items)
    }
    return result
  }

  fun importApproversCsv(csvContent: String): com.example.util.CsvUtils.CsvParseResult<User> {
    val tenantId = uiState.value.selectedTenant?.tenantId ?: "TENANT-001"
    val result = com.example.util.CsvUtils.parseFlatOwnersCsv(csvContent, tenantId)
    if (result.success && result.items.isNotEmpty()) {
      repository.addUsers(result.items)
    }
    return result
  }

  // Security Gate on-the-spot pass generation
  fun generateGatePass(
    visitorName: String,
    visitorMobile: String,
    purpose: String,
    destination: String,
    vehicleNumber: String,
    bypassReception: Boolean? = null,
    vehicleSnapshotUri: String? = null
  ) {
    viewModelScope.launch {
      repository.generateGatePassAtSecurity(visitorName, visitorMobile, purpose, destination, vehicleNumber, bypassReception, vehicleSnapshotUri)
    }
  }

  // Pre-registered App QR fast verification at gate
  fun verifyPreRegisteredQrAtGate(ticketId: String, guardName: String) {
    viewModelScope.launch {
      repository.verifyPreRegisteredQrAtGate(ticketId, guardName)
    }
  }

  // Reception scan and forward to Approver
  fun confirmAtReception(ticketId: String, operatorName: String) {
    viewModelScope.launch {
      repository.confirmAtReception(ticketId, operatorName)
    }
  }

  // Approver digital sign-off with Date and Time
  fun approverSignOff(ticketId: String, approverName: String, isApproved: Boolean, remarks: String) {
    viewModelScope.launch {
      repository.approverSignOff(ticketId, approverName, isApproved, remarks)
    }
  }

  // Exit Gate pass closure
  fun closeGatePassAtExit(ticketId: String, guardName: String) {
    viewModelScope.launch {
      repository.closeGatePassAtExit(ticketId, guardName)
    }
  }
}
