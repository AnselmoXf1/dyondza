package com.dyondza.backend.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Students : Table("students") {
    val id = uuid("id").autoGenerate()
    val authUid = varchar("auth_uid", 128).uniqueIndex()
    val name = varchar("name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val province = varchar("province", 50)
    val schoolName = varchar("school_name", 150).nullable()
    val xpAccumulated = integer("xp_accumulated").default(0)
    val level = integer("level").default(1)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object FocusSessions : Table("focus_sessions") {
    val id = uuid("id").autoGenerate()
    val studentId = reference("student_id", Students.id)
    val topic = varchar("topic", 100)
    val durationRequestedSeconds = integer("duration_requested_seconds")
    val durationActualSeconds = integer("duration_actual_seconds")
    val warningsCount = integer("warnings_count").default(0)
    val status = varchar("status", 30) // COMPLETED, INTERRUPTED, CHEAT_DETECTED
    val validated = bool("validated").default(false)
    val xpRewarded = integer("xp_rewarded").default(0)
    val deviceSignature = varchar("device_signature", 255).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Missions : Table("missions") {
    val id = uuid("id").autoGenerate()
    val title = varchar("title", 150)
    val description = text("description")
    val missionType = varchar("mission_type", 30) // DAILY, WEEKLY, EVENT
    val targetValue = integer("target_value")
    val targetMetric = varchar("target_metric", 50)
    val xpReward = integer("xp_reward").default(50)
    val activeFrom = datetime("active_from")
    val activeTo = datetime("active_to")

    override val primaryKey = PrimaryKey(id)
}

object StudentMissions : Table("student_missions") {
    val studentId = reference("student_id", Students.id)
    val missionId = reference("mission_id", Missions.id)
    val currentProgress = integer("current_progress").default(0)
    val completedAt = datetime("completed_at").nullable()

    override val primaryKey = PrimaryKey(studentId, missionId)
}
