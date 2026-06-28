package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- DTOs DO CLIENTE MOBILE PARA COMUNICAÇÃO COM O KTOR BACKEND ---

@JsonClass(generateAdapter = true)
data class ClientRegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "province") val province: String,
    @Json(name = "schoolName") val schoolName: String? = null
)

@JsonClass(generateAdapter = true)
data class ClientRegisterResponse(
    @Json(name = "studentId") val studentId: String,
    @Json(name = "name") val name: String,
    @Json(name = "level") val level: Int,
    @Json(name = "xpAccumulated") val xpAccumulated: Int,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class ClientUsageLogDto(
    @Json(name = "packageName") val packageName: String,
    @Json(name = "timestampStart") val timestampStart: Long,
    @Json(name = "durationMs") val durationMs: Long
)

@JsonClass(generateAdapter = true)
data class ClientDeviceTelemetryDto(
    @Json(name = "deviceSalt") val deviceSalt: String,
    @Json(name = "usageLogs") val usageLogs: List<ClientUsageLogDto>,
    @Json(name = "clientSignature") val clientSignature: String
)

@JsonClass(generateAdapter = true)
data class ClientSessionCompleteRequest(
    @Json(name = "topic") val topic: String,
    @Json(name = "durationRequestedSeconds") val durationRequestedSeconds: Int,
    @Json(name = "durationActualSeconds") val durationActualSeconds: Int,
    @Json(name = "warningsCount") val warningsCount: Int,
    @Json(name = "deviceTelemetry") val deviceTelemetry: ClientDeviceTelemetryDto? = null
)

@JsonClass(generateAdapter = true)
data class ClientSessionCompleteResponse(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "status") val status: String,
    @Json(name = "validated") val validated: Boolean,
    @Json(name = "xpEarned") val xpEarned: Int,
    @Json(name = "levelUpgraded") val levelUpgraded: Boolean,
    @Json(name = "currentLevel") val currentLevel: Int,
    @Json(name = "totalXp") val totalXp: Int,
    @Json(name = "systemMessage") val systemMessage: String
)

@JsonClass(generateAdapter = true)
data class ClientMissionDto(
    @Json(name = "missionId") val missionId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "missionType") val missionType: String,
    @Json(name = "progress") val progress: Int,
    @Json(name = "targetValue") val targetValue: Int,
    @Json(name = "xpReward") val xpReward: Int,
    @Json(name = "isCompleted") val isCompleted: Boolean,
    @Json(name = "completedAt") val completedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ClientMissionsCheckResponse(
    @Json(name = "missions") val missions: List<ClientMissionDto>
)

@JsonClass(generateAdapter = true)
data class ClientLeaderboardEntryDto(
    @Json(name = "rank") val rank: Int,
    @Json(name = "studentId") val studentId: String,
    @Json(name = "name") val name: String,
    @Json(name = "xp") val xp: Int,
    @Json(name = "level") val level: Int
)

@JsonClass(generateAdapter = true)
data class ClientRankingResponse(
    @Json(name = "region") val region: String,
    @Json(name = "totalStudentsCount") val totalStudentsCount: Long,
    @Json(name = "leaderboard") val leaderboard: List<ClientLeaderboardEntryDto>
)

@JsonClass(generateAdapter = true)
data class ClientQuizGenerateRequest(
    @Json(name = "topic") val topic: String
)

@JsonClass(generateAdapter = true)
data class ClientQuizQuestionDto(
    @Json(name = "question") val question: String,
    @Json(name = "options") val options: List<String>,
    @Json(name = "correctOptionIndex") val correctOptionIndex: Int,
    @Json(name = "explanation") val explanation: String
)

@JsonClass(generateAdapter = true)
data class ClientQuizGenerateResponse(
    @Json(name = "topic") val topic: String,
    @Json(name = "questions") val questions: List<ClientQuizQuestionDto>,
    @Json(name = "cached") val cached: Boolean
)

// --- INTERFACE RETROFIT ---

interface DyondzaBackendApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: ClientRegisterRequest): ClientRegisterResponse

    @POST("api/v1/session/complete")
    suspend fun completeSession(
        @Header("Authorization") authHeader: String,
        @Body request: ClientSessionCompleteRequest
    ): ClientSessionCompleteResponse

    @GET("api/v1/missions/check")
    suspend fun checkMissions(
        @Header("Authorization") authHeader: String
    ): ClientMissionsCheckResponse

    @GET("api/v1/ranking/{region}")
    suspend fun getRanking(
        @Header("Authorization") authHeader: String,
        @Path("region") region: String
    ): ClientRankingResponse

    @POST("api/v1/quiz/generate")
    suspend fun generateQuiz(
        @Header("Authorization") authHeader: String,
        @Body request: ClientQuizGenerateRequest
    ): ClientQuizGenerateResponse
}

object DyondzaApiClient {
    // Endereço do Servidor Ktor em Produção (Cloud Run ou Emulador Local Android 10.0.2.2:8080)
    var BASE_URL = "http://10.0.2.2:8080/"
    var jwtToken: String = ""

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: DyondzaBackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DyondzaBackendApi::class.java)
    }

    fun getAuthHeader(): String {
        return "Bearer $jwtToken"
    }
}
