package com.dyondza.backend.service

import com.dyondza.backend.model.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object SessionService {

    fun completeSession(studentIdStr: String, req: SessionCompleteRequest): SessionCompleteResponse {
        val validation = AntiCheatEngine.validateSession(req)
        val studentId = try { UUID.fromString(studentIdStr) } catch (e: Exception) { null }
            ?: throw IllegalArgumentException("ID de estudante inválido")

        return transaction {
            val studentRow = Students.select { Students.id eq studentId }.singleOrNull()
                ?: throw IllegalArgumentException("Estudante não encontrado no sistema.")

            var xpEarned = 0
            if (validation.isValid && validation.status == "COMPLETED") {
                val base = (req.durationActualSeconds / 60) * 10
                val bonus = if (req.warningsCount == 0) 50 else (30 - req.warningsCount * 10).coerceAtLeast(0)
                xpEarned = base + bonus
            }

            val newSessionId = UUID.randomUUID()
            FocusSessions.insert {
                it[id] = newSessionId
                it[FocusSessions.studentId] = studentId
                it[topic] = req.topic
                it[durationRequestedSeconds] = req.durationRequestedSeconds
                it[durationActualSeconds] = req.durationActualSeconds
                it[warningsCount] = req.warningsCount
                it[status] = validation.status
                it[validated] = validation.isValid
                it[xpRewarded] = xpEarned
                it[deviceSignature] = req.deviceTelemetry?.clientSignature
            }

            val oldXp = studentRow[Students.xpAccumulated]
            val oldLevel = studentRow[Students.level]
            val newXp = oldXp + xpEarned
            val expectedLevel = Math.min(50, Math.floor(1.0 + newXp / 100.0)).toInt()
            val levelUpgraded = expectedLevel > oldLevel

            if (xpEarned > 0) {
                Students.update({ Students.id eq studentId }) {
                    it[xpAccumulated] = newXp
                    it[level] = expectedLevel
                }
                // Atualiza em tempo real o ranking no Redis
                RankingService.updateStudentXp(studentIdStr, studentRow[Students.province], newXp)
            }

            SessionCompleteResponse(
                sessionId = newSessionId.toString(),
                status = validation.status,
                validated = validation.isValid,
                xpEarned = xpEarned,
                levelUpgraded = levelUpgraded,
                currentLevel = expectedLevel,
                totalXp = newXp,
                systemMessage = validation.reason
            )
        }
    }
}
