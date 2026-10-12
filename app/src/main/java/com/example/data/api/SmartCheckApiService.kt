package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Request model for pushing a visitor log entry or gate pass to Google Apps Script Web App.
 */
@JsonClass(generateAdapter = true)
data class VisitorLogRequest(
  val action: String = "PUSH_VISITOR_LOG",
  val tenantId: String,
  val ticketId: String,
  val visitorName: String,
  val visitorMobile: String,
  val purpose: String,
  val targetDestination: String,
  val vehicleNumber: String = "",
  val vehicleSnapshotUri: String? = null,
  val vehicleSnapshotBase64: String? = null,
  val timestamp: String,
  val qrToken: String? = null,
  val status: String = "PENDING_APPROVAL",
  val bypassReception: Boolean = false,
  val gateEntryTime: String? = null
)

/**
 * Response model received after pushing a visitor log entry.
 */
@JsonClass(generateAdapter = true)
data class VisitorLogResponse(
  val status: String = "SUCCESS",
  val message: String? = null,
  val ticketId: String? = null,
  val syncTimestamp: String? = null
)

/**
 * DTO representing an individual pending approval item fetched from Google Apps Script.
 */
@JsonClass(generateAdapter = true)
data class PendingApprovalDto(
  val ticketId: String = "",
  val tenantId: String = "",
  val visitorName: String = "",
  val visitorMobile: String = "",
  val purpose: String = "",
  val targetDestination: String = "",
  val vehicleNumber: String = "",
  val vehicleSnapshotUri: String? = null,
  val status: String = "PENDING_APPROVAL",
  val qrToken: String? = null,
  val gateEntryTime: String = "",
  val createdAt: String = "",
  val bypassReception: Boolean = false
)

/**
 * Response model for querying the pending approval queue for a tenant.
 */
@JsonClass(generateAdapter = true)
data class PendingApprovalsResponse(
  val status: String = "SUCCESS",
  val message: String? = null,
  val count: Int = 0,
  val tickets: List<PendingApprovalDto> = emptyList()
)

/**
 * Request model to submit an approval or rejection decision.
 */
@JsonClass(generateAdapter = true)
data class ApprovalDecisionRequest(
  val action: String = "SUBMIT_APPROVAL_DECISION",
  val tenantId: String,
  val ticketId: String,
  val isApproved: Boolean,
  val approverName: String,
  val remarks: String? = null,
  val decisionTimestamp: String
)

/**
 * Response model for approval decision submission.
 */
@JsonClass(generateAdapter = true)
data class ApprovalDecisionResponse(
  val status: String = "SUCCESS",
  val message: String? = null,
  val ticketId: String? = null
)

/**
 * QR Validation Response from API
 */
@JsonClass(generateAdapter = true)
data class QrValidationResponse(
  val status: String = "SUCCESS", // SUCCESS, FAILURE, PENDING
  val isValid: Boolean = true,
  val ticket: PendingApprovalDto? = null,
  val message: String? = null
)

/**
 * SmartCheck REST API Service interface interacting with Google Apps Script Web App.
 */
interface SmartCheckApiService {

  /**
   * Pushes a visitor pass/log entry to the Google Sheet.
   */
  @POST
  suspend fun pushVisitorLog(
    @Url url: String,
    @Body request: VisitorLogRequest
  ): Response<VisitorLogResponse>

  /**
   * Fetches the queue of pending approvals for a specific tenant.
   */
  @GET
  suspend fun fetchPendingApprovals(
    @Url url: String,
    @Query("action") action: String = "GET_PENDING_APPROVALS",
    @Query("tenantId") tenantId: String
  ): Response<PendingApprovalsResponse>

  /**
   * Submits an approver's sign-off decision (Approve or Reject).
   */
  @POST
  suspend fun submitApprovalDecision(
    @Url url: String,
    @Body request: ApprovalDecisionRequest
  ): Response<ApprovalDecisionResponse>

  /**
   * Validates a scanned QR token against Google Sheet / Backend
   */
  @GET
  suspend fun validateQrToken(
    @Url url: String,
    @Query("action") action: String = "VALIDATE_QR_TOKEN",
    @Query("tenantId") tenantId: String,
    @Query("qrToken") qrToken: String
  ): Response<QrValidationResponse>
}

