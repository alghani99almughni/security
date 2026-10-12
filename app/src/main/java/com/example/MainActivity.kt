package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.UserRole
import com.example.ui.components.GoogleSyncDialog
import com.example.ui.components.SmartCheckTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate50
import com.example.ui.viewmodel.SmartCheckViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: SmartCheckViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var showSyncDialog by remember { mutableStateOf(false) }
        var activeAdminTestScreen by remember { mutableStateOf<String?>(null) }

        if (uiState.currentUser == null) {
          LoginScreen(
            tenants = uiState.tenants,
            users = uiState.users,
            serviceHealth = uiState.appServiceHealth,
            onPingHealth = { viewModel.pingServiceHealth() },
            onCreateTenant = { name, code, type, customCategory, assignedRole, email, contactName, mobile, hasReception, receptionStaffNames ->
              viewModel.createTenant(
                name = name,
                code = code,
                type = type,
                customCategory = customCategory,
                assignedRole = assignedRole,
                adminEmail = email,
                contactName = contactName,
                contactMobile = mobile,
                hasReception = hasReception,
                receptionStaffNames = receptionStaffNames
              )
            },
            onLoginSubmit = { email, onResult ->
              viewModel.loginWithEmail(email) { success, msg, role ->
                onResult(success, msg)
              }
            }
          )
        } else {
          Scaffold(
            modifier = Modifier
              .fillMaxSize()
              .background(Slate50),
            topBar = {
              SmartCheckTopBar(
                currentRole = uiState.currentRole,
                selectedTenant = uiState.selectedTenant,
                currentUser = uiState.currentUser,
                onRoleSelected = { role ->
                  activeAdminTestScreen = null
                  viewModel.setRole(role)
                },
                onOpenSyncSettings = { showSyncDialog = true },
                onLogout = {
                  activeAdminTestScreen = null
                  viewModel.logout()
                }
              )
            }
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              when (uiState.currentRole) {
                UserRole.SECURITY -> {
                  SecurityScreen(
                    tenant = uiState.selectedTenant,
                    tickets = uiState.tickets,
                    onGeneratePass = { visitorName, mobile, purpose, destination, vehicleNumber, bypassReception, snapshotUri ->
                      viewModel.generateGatePass(visitorName, mobile, purpose, destination, vehicleNumber, bypassReception, snapshotUri)
                    },
                    onFastScan = { ticketId, guardName ->
                      viewModel.verifyPreRegisteredQrAtGate(ticketId, guardName)
                    },
                    onClosePassAtExit = { ticketId, guardName ->
                      viewModel.closeGatePassAtExit(ticketId, guardName)
                    },
                    onApprovePass = { ticketId, approverName, isApproved ->
                      viewModel.approverSignOff(ticketId, approverName, isApproved, "Gate Officer Clearance")
                    }
                  )
                }

                UserRole.APPROVER -> {
                  ApprovalsDashboard(
                    tenant = uiState.selectedTenant,
                    tickets = uiState.tickets,
                    onSignOff = { ticketId, approverName, isApproved, remarks ->
                      viewModel.approverSignOff(ticketId, approverName, isApproved, remarks)
                    },
                    onPullPendingApprovals = { onComplete ->
                      viewModel.pullPendingApprovals(onComplete)
                    }
                  )
                }

                UserRole.RECEPTION -> {
                  ReceptionScreen(
                    tenant = uiState.selectedTenant,
                    tickets = uiState.tickets,
                    onConfirmAtReception = { ticketId, operatorName ->
                      viewModel.confirmAtReception(ticketId, operatorName)
                    },
                    onToggleReception = { enabled ->
                      uiState.selectedTenant?.let {
                        viewModel.toggleTenantReception(it.tenantId, enabled)
                      }
                    }
                  )
                }

                UserRole.PLATFORM_ADMIN -> {
                  when (activeAdminTestScreen) {
                    "vehicle_scanner" -> {
                      VehicleScannerScreen(
                        onVehicleScanned = { bitmap, photoUri, timestamp, plateNumber, base64 ->
                          viewModel.generateGatePass(
                            visitorName = "Test Visitor (Camera Scan)",
                            visitorMobile = "9876543210",
                            purpose = "Vehicle Security Test",
                            destination = "Main Gate / Facility Checkpoint",
                            vehicleNumber = plateNumber,
                            bypassReception = !(uiState.selectedTenant?.hasReception ?: true),
                            vehicleSnapshotUri = photoUri
                          )
                          activeAdminTestScreen = null
                        },
                        onDismiss = { activeAdminTestScreen = null }
                      )
                    }

                    "qr_scanner" -> {
                      QrScannerScreen(
                        tickets = uiState.tickets,
                        onValidateToken = { token, onResult ->
                          val (status, msg, ticket) = viewModel.validateScannedQrToken(token)
                          val resultState = when (status) {
                            "SUCCESS" -> QrScanResultState.SUCCESS
                            "PENDING" -> QrScanResultState.PENDING
                            else -> QrScanResultState.FAILURE
                          }
                          onResult(resultState, msg, ticket)
                        },
                        onAccessGranted = { ticketId ->
                          viewModel.verifyPreRegisteredQrAtGate(ticketId, "Admin Tester")
                          activeAdminTestScreen = null
                        },
                        onDismiss = { activeAdminTestScreen = null }
                      )
                    }

                    "approvals" -> {
                      ApprovalsDashboard(
                        tenant = uiState.selectedTenant,
                        tickets = uiState.tickets,
                        onSignOff = { ticketId, approverName, isApproved, remarks ->
                          viewModel.approverSignOff(ticketId, approverName, isApproved, remarks)
                        },
                        onPullPendingApprovals = { onComplete ->
                          viewModel.pullPendingApprovals(onComplete)
                        }
                      )
                    }

                    else -> {
                      PlatformAdminScreen(
                        tenants = uiState.tenants,
                        selectedTenant = uiState.selectedTenant,
                        tickets = uiState.tickets,
                        serviceHealth = uiState.appServiceHealth,
                        tenantRegistry = uiState.tenantRegistry,
                        onPingHealth = { viewModel.pingServiceHealth() },
                        onSelectTenant = { tenant -> viewModel.selectTenant(tenant) },
                        onSwitchRole = { role -> viewModel.setRole(role) },
                        onLaunchVehicleScanner = { activeAdminTestScreen = "vehicle_scanner" },
                        onLaunchQrScanner = { activeAdminTestScreen = "qr_scanner" },
                        onLaunchApprovalsDashboard = { activeAdminTestScreen = "approvals" },
                        onGenerateTestPass = {
                          viewModel.generateGatePass(
                            visitorName = "Simulated Visitor (${uiState.selectedTenant?.code ?: "TST"})",
                            visitorMobile = "9880011223",
                            purpose = "Maintenance & Deliveries",
                            destination = if (uiState.selectedTenant?.type == com.example.model.TenantType.APARTMENT) "Flat 402 - Mr. Sharma" else "Grade 5A - Parent Pickup",
                            vehicleNumber = "KA-05-MN-8841",
                            bypassReception = !(uiState.selectedTenant?.hasReception ?: true)
                          )
                        },
                        onCreateTenant = { name, code, type, customCategory, assignedRole, email, contactName, mobile, hasReception, receptionStaffNames ->
                          viewModel.createTenant(
                            name = name,
                            code = code,
                            type = type,
                            customCategory = customCategory,
                            assignedRole = assignedRole,
                            adminEmail = email,
                            contactName = contactName,
                            contactMobile = mobile,
                            hasReception = hasReception,
                            receptionStaffNames = receptionStaffNames
                          )
                        }
                      )
                    }
                  }
                }

              UserRole.ADMIN -> {
                TenantAdminScreen(
                  tenant = uiState.selectedTenant,
                  users = uiState.users,
                  students = uiState.students,
                  analytics = viewModel.getTenantAnalytics(uiState.selectedTenant?.tenantId ?: ""),
                  onToggleReception = { enabled ->
                    uiState.selectedTenant?.let {
                      viewModel.toggleTenantReception(it.tenantId, enabled)
                    }
                  },
                  onCreateUser = { email, user, mob, name, role, desig ->
                    viewModel.createUser(email, user, mob, name, role, desig)
                  },
                  onCreateStudent = { name, cName, sec, fName, fMob, bus ->
                    viewModel.createStudent(name, cName, sec, fName, fMob, bus)
                  },
                  onImportStudentsCsv = { csv -> viewModel.importStudentsCsv(csv) },
                  onImportApproversCsv = { csv -> viewModel.importApproversCsv(csv) }
                )
              }

              UserRole.PARENT, UserRole.VISITOR -> {
                ParentScreen(
                  students = uiState.students,
                  tickets = uiState.tickets,
                  onSubmitTicket = { studentId, reason ->
                    val student = uiState.students.find { it.studentId == studentId }
                    val childName = student?.studentName ?: "Child"
                    val className = student?.className ?: "Class"
                    viewModel.generateGatePass(
                      visitorName = "John Doe (Parent)",
                      visitorMobile = "9876543210",
                      purpose = "Early Pickup: $reason",
                      destination = "$childName ($className)",
                      vehicleNumber = "KA-01-AB-1234",
                      bypassReception = !(uiState.selectedTenant?.hasReception ?: true)
                    )
                  }
                )
              }
            }

            if (showSyncDialog) {
              GoogleSyncDialog(
                config = uiState.syncConfig,
                onDismiss = { showSyncDialog = false },
                onSave = { url, sheetId, folderId ->
                  viewModel.updateGoogleSyncConfig(url, sheetId, folderId)
                }
              )
            }
          }
        }
      }
    }
  }
}
}
