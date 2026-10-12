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

@JsonClass(generateAdapter = true)
data class RemoteTicketDto(
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
  val approverName: String? = null,
  val approverSignOffTime: String? = null,
  val gateEntryTime: String = "",
  val gateExitTime: String? = null,
  val createdAt: String = "",
  val remarks: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncPayload(
  val action: String,
  val tenantId: String,
  val ticketId: String? = null,
  val visitorName: String? = null,
  val visitorMobile: String? = null,
  val purpose: String? = null,
  val targetDestination: String? = null,
  val vehicleNumber: String? = null,
  val vehicleSnapshotUri: String? = null,
  val status: String? = null,
  val qrToken: String? = null,
  val bypassReception: Boolean? = null,
  val approverName: String? = null,
  val remarks: String? = null,
  val gateEntryTime: String? = null,
  val gateExitTime: String? = null,
  val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncApiResponse(
  val status: String? = "SUCCESS",
  val message: String? = null,
  val tickets: List<RemoteTicketDto>? = null,
  val count: Int? = 0
)

interface GoogleAppsScriptApi {

  /**
   * Push visitor log / gate pass entry to Google Sheet
   */
  @POST
  suspend fun pushVisitorLog(
    @Url url: String,
    @Body payload: SyncPayload
  ): Response<SyncApiResponse>

  /**
   * Pull active pending approvals for a tenant from Google Sheet
   */
  @GET
  suspend fun pullPendingApprovals(
    @Url url: String,
    @Query("action") action: String = "GET_PENDING_APPROVALS",
    @Query("tenantId") tenantId: String
  ): Response<SyncApiResponse>

  /**
   * Post approval sign-off or rejection back to the tenant's sheet
   */
  @POST
  suspend fun updateApprovalDecision(
    @Url url: String,
    @Body payload: SyncPayload
  ): Response<SyncApiResponse>
}

object RetrofitClient {

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    }
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .addInterceptor(logging)
      .build()
  }

  val api: GoogleAppsScriptApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://script.google.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
      .build()
      .create(GoogleAppsScriptApi::class.java)
  }

  val service: SmartCheckApiService by lazy {
    Retrofit.Builder()
      .baseUrl("https://script.google.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
      .build()
      .create(SmartCheckApiService::class.java)
  }
}
