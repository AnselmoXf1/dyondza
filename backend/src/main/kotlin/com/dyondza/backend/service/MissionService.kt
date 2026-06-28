package com.dyondza.backend.service

import com.dyondza.backend.model.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object MissionService {
    fun checkMissions(studentIdStr: String): MissionsCheckResponse {
        val studentId = try { UUID.fromString(studentIdStr) } catch (e: Exception) { null }
            ?: return MissionsCheckResponse(emptyList())

        return transaction {
            val allMissions = Missions.selectAll()
            val progressMap = StudentMissions.select { StudentMissions.studentId eq studentId }
                .associateBy { it[StudentMissions.missionId] }

            val dtoList = allMissions.map { row ->
                val mId = row[Missions.id]
                val progRow = progressMap[mId]
                val progress = progRow?.get(StudentMissions.currentProgress) ?: 0
                val completedAt = progRow?.get(StudentMissions.completedAt)?.toString()
                val target = row[Missions.targetValue]

                MissionDto(
                    missionId = mId.toString(),
                    title = row[Missions.title],
                    description = row[Missions.description],
                    missionType = row[Missions.missionType],
                    progress = progress,
                    targetValue = target,
                    xpReward = row[Missions.xpReward],
                    isCompleted = progress >= target || completedAt != null,
                    completedAt = completedAt
                )
            }

            MissionsCheckResponse(dtoList)
        }
    }
}
