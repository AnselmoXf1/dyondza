package com.dyondza.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val province: String,
    val schoolName: String? = null
)

@Serializable
data class RegisterResponse(
    val studentId: String,
    val name: String,
    val level: Int,
    val xpAccumulated: Int,
    val createdAt: String,
    val token: String
)

@Serializable
data class UsageLogDto(
    val packageName: String,
    val timestampStart: Long,
    val durationMs: Long
)

@Serializable
data class DeviceTelemetryDto(
    val deviceSalt: String,
    val usageLogs: List<UsageLogDto>,
    val clientSignature: String
)

@Serializable
data class SessionCompleteRequest(
    val topic: String,
    val durationRequestedSeconds: Int,
    val durationActualSeconds: Int,
    val warningsCount: Int,
    val deviceTelemetry: DeviceTelemetryDto? = null
)

@Serializable
data class SessionCompleteResponse(
    val sessionId: String,
    val status: String,
    val validated: Boolean,
    val xpEarned: Int,
    val levelUpgraded: Boolean,
    val currentLevel: Int,
    val totalXp: Int,
    val systemMessage: String
)

@Serializable
data class MissionDto(
    val missionId: String,
    val title: String,
    val description: String,
    val missionType: String,
    val progress: Int,
    val targetValue: Int,
    val xpReward: Int,
    val isCompleted: Boolean,
    val completedAt: String? = null
)

@Serializable
data class MissionsCheckResponse(
    val missions: List<MissionDto>
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val studentId: String,
    val name: String,
    val xp: Int,
    val level: Int
)

@Serializable
data class RankingResponse(
    val region: String,
    val totalStudentsCount: Long,
    val leaderboard: List<LeaderboardEntryDto>
)

@Serializable
data class QuizGenerateRequest(
    val topic: String
)

@Serializable
data class QuizQuestionDto(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@Serializable
data class QuizGenerateResponse(
    val topic: String,
    val questions: List<QuizQuestionDto>,
    val cached: Boolean = false
)
