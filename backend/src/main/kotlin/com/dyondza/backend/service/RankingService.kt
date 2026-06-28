package com.dyondza.backend.service

import com.dyondza.backend.config.RedisConfig
import com.dyondza.backend.model.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object RankingService {
    const val GLOBAL_KEY = "leaderboard:global"
    fun provinceKey(province: String) = "leaderboard:province:${province.lowercase()}"

    fun updateStudentXp(studentId: String, province: String, totalXp: Int) {
        val score = totalXp.toDouble()
        RedisConfig.zadd(GLOBAL_KEY, score, studentId)
        RedisConfig.zadd(provinceKey(province), score, studentId)
    }

    fun getRanking(region: String): RankingResponse {
        val key = if (region.equals("global", ignoreCase = true)) {
            GLOBAL_KEY
        } else {
            provinceKey(region)
        }

        val topScores = RedisConfig.zrevrangeWithScores(key, 0, 19)
        
        return transaction {
            if (topScores.isEmpty()) {
                // Se o Redis não tiver dados (ex: restart ou clean), reconstruir a partir do Postgres
                val allStudents = Students.selectAll().orderBy(Students.xpAccumulated, org.jetbrains.exposed.sql.SortOrder.DESC).limit(20)
                val leaderboard = allStudents.mapIndexed { index, row ->
                    val id = row[Students.id].toString()
                    val xp = row[Students.xpAccumulated]
                    val prov = row[Students.province]
                    updateStudentXp(id, prov, xp)
                    LeaderboardEntryDto(
                        rank = index + 1,
                        studentId = id,
                        name = row[Students.name],
                        xp = xp,
                        level = row[Students.level]
                    )
                }
                val totalCount = Students.selectAll().count()
                return@transaction RankingResponse(
                    region = region,
                    totalStudentsCount = totalCount,
                    leaderboard = leaderboard
                )
            }

            val studentIds = topScores.mapNotNull { 
                try { UUID.fromString(it.first) } catch (e: Exception) { null } 
            }

            val studentMap = Students.select { Students.id inList studentIds }
                .associateBy { it[Students.id].toString() }

            val leaderboard = topScores.mapIndexedNotNull { index, (idStr, score) ->
                val row = studentMap[idStr] ?: return@mapIndexedNotNull null
                LeaderboardEntryDto(
                    rank = index + 1,
                    studentId = idStr,
                    name = row[Students.name],
                    xp = score.toInt(),
                    level = row[Students.level]
                )
            }

            val totalCount = Students.selectAll().count()

            RankingResponse(
                region = region,
                totalStudentsCount = totalCount,
                leaderboard = leaderboard
            )
        }
    }
}
