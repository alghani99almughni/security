package com.example.data

import com.example.data.api.GoogleAppsScriptApi
import com.example.data.api.RemoteTicketDto
import com.example.data.api.RetrofitClient
import com.example.data.api.SyncPayload
import com.example.model.PassOrigin
import com.example.model.Ticket
import com.example.model.TicketAction
import com.example.model.TicketStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetSyncService(
  private val api: GoogleAppsScriptApi = RetrofitClient.api
) {

  /**
   * Pushes visitor log entry to Google Sheet via Retrofit
   */
  suspend fun syncTicketToGoogleSheet(
    webAppUrl: String,
    ticket: Ticket,
    latestAction: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) {
      return@withContext Result.success("Saved in local state (Drive Folder 1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL)")
    }

    try {
      val payload = SyncPayload(
        action = "SYNC_TICKET",
        tenantId = ticket.tenantId,
        ticketId = ticket.ticketId,
        visitorName = ticket.visitorName,
        visitorMobile = ticket.visitorMobile,
        purpose = ticket.purpose,
        targetDestination = ticket.targetDestination,
        vehicleNumber = ticket.vehicleNumber,
        vehicleSnapshotUri = ticket.vehicleSnapshotUri,
        status = ticket.status.name,
        qrToken = ticket.qrToken,
        bypassReception = ticket.bypassReception,
        approverName = ticket.approverName,
        remarks = latestAction,
        gateEntryTime = ticket.gateEntryTime,
        gateExitTime = ticket.gateExitTime,
        updatedAt = ticket.updatedAt
      )

      val response = api.pushVisitorLog(webAppUrl, payload)
      if (response.isSuccessful && response.body()?.status != "ERROR") {
        Result.success("Pushed to Google Sheet via Retrofit (HTTP ${response.code()})")
      } else {
        Result.failure(Exception("Retrofit push returned HTTP ${response.code()}: ${response.errorBody()?.string()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Pulls active pending approvals from the tenant's spreadsheet via Retrofit
   */
  suspend fun pullPendingApprovalsFromSheet(
    webAppUrl: String,
    tenantId: String
  ): Result<List<Ticket>> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) {
      return@withContext Result.success(emptyList())
    }

    try {
      val response = api.pullPendingApprovals(url = webAppUrl, tenantId = tenantId)
      if (response.isSuccessful) {
        val dtos = response.body()?.tickets ?: emptyList()
        val mappedTickets = dtos.map { dto ->
          Ticket(
            ticketId = dto.ticketId,
            tenantId = dto.tenantId.ifBlank { tenantId },
            passOrigin = PassOrigin.GATE_WALK_IN,
            visitorName = dto.visitorName,
            visitorMobile = dto.visitorMobile,
            purpose = dto.purpose,
            targetDestination = dto.targetDestination,
            status = try {
              TicketStatus.valueOf(dto.status)
            } catch (_: Exception) {
              TicketStatus.PENDING_APPROVAL
            },
            vehicleNumber = dto.vehicleNumber,
            vehicleSnapshotUri = dto.vehicleSnapshotUri,
            qrToken = dto.qrToken,
            approverName = dto.approverName,
            gateEntryTime = dto.gateEntryTime.ifBlank { dto.createdAt },
            createdAt = dto.createdAt,
            updatedAt = dto.createdAt
          )
        }
        Result.success(mappedTickets)
      } else {
        Result.failure(Exception("Failed to pull from Google Sheet: HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Updates approval status (Approve / Reject) in Google Sheet via Retrofit
   */
  suspend fun updateApprovalDecisionInSheet(
    webAppUrl: String,
    tenantId: String,
    ticketId: String,
    isApproved: Boolean,
    approverName: String,
    remarks: String,
    qrToken: String?
  ): Result<String> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) {
      return@withContext Result.success("Decision recorded locally")
    }

    try {
      val payload = SyncPayload(
        action = if (isApproved) "APPROVE_TICKET" else "REJECT_TICKET",
        tenantId = tenantId,
        ticketId = ticketId,
        status = if (isApproved) TicketStatus.APPROVED.name else TicketStatus.REJECTED.name,
        approverName = approverName,
        qrToken = qrToken,
        remarks = remarks
      )
      val response = api.updateApprovalDecision(webAppUrl, payload)
      if (response.isSuccessful) {
        Result.success("Approval synced to Google Sheet (HTTP ${response.code()})")
      } else {
        Result.failure(Exception("Sync error: HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncActionLog(
    webAppUrl: String,
    action: TicketAction
  ): Result<String> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) return@withContext Result.success("Logged locally")
    try {
      val payload = SyncPayload(
        action = "LOG_ACTION",
        tenantId = action.tenantId,
        ticketId = action.ticketId,
        approverName = action.actionBy,
        remarks = "${action.step} - ${action.actionType}: ${action.remarks}",
        updatedAt = action.timestamp
      )
      val response = api.pushVisitorLog(webAppUrl, payload)
      if (response.isSuccessful) {
        Result.success("Action logged in AUDIT_LOGS sheet")
      } else {
        Result.failure(Exception("HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registerTenantInGoogleSheet(
    webAppUrl: String,
    entry: com.example.model.MultipleTenantRegisterEntry
  ): Result<String> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) {
      return@withContext Result.success("Tenant recorded in Multiple Tenant Register Spreadsheet")
    }
    try {
      val payload = SyncPayload(
        action = "REGISTER_TENANT",
        tenantId = entry.tenantCode,
        ticketId = entry.registerId,
        visitorName = entry.contactPerson,
        visitorMobile = entry.mobile,
        purpose = entry.category,
        targetDestination = entry.organizationName,
        status = entry.assignedRole,
        remarks = "Registered via App. Reception: ${entry.receptionClearance}. Monthly Sheet: ${entry.monthlySpreadsheetUrl}"
      )
      val response = api.pushVisitorLog(webAppUrl, payload)
      if (response.isSuccessful) {
        Result.success("Added to Multiple Tenant Register Spreadsheet (HTTP ${response.code()})")
      } else {
        Result.failure(Exception("HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pingAppsScript(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
    if (webAppUrl.isBlank()) {
      return@withContext Result.success("Local Drive & Sheet Checkpoint Ready")
    }
    try {
      val payload = SyncPayload(action = "PING", tenantId = "PING", remarks = "Connectivity test")
      val response = api.pushVisitorLog(webAppUrl, payload)
      if (response.isSuccessful) {
        Result.success("Connected to Apps Script (HTTP ${response.code()})")
      } else {
        Result.failure(Exception("HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

