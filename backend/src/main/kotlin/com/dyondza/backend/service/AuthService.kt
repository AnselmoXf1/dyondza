package com.dyondza.backend.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dyondza.backend.model.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

object AuthService {
    const val SECRET = "dyondza-super-secret-jwt-key-production-2026"
    const val ISSUER = "http://dyondza-auth-server"
    const val AUDIENCE = "http://dyondza-mobile-client"

    val algorithm: Algorithm = Algorithm.HMAC256(SECRET)

    fun register(req: RegisterRequest): RegisterResponse {
        return transaction {
            // Verifica se email já existe
            val existing = Students.select { Students.email eq req.email }.singleOrNull()
            if (existing != null) {
                val studentId = existing[Students.id].toString()
                val token = generateToken(studentId, req.email)
                return@transaction RegisterResponse(
                    studentId = studentId,
                    name = existing[Students.name],
                    level = existing[Students.level],
                    xpAccumulated = existing[Students.xpAccumulated],
                    createdAt = existing[Students.createdAt].toString(),
                    token = token
                )
            }

            val newId = UUID.randomUUID()
            val authUid = "auth_" + UUID.randomUUID().toString().substring(0, 8)
            val now = OffsetDateTime.now()

            Students.insert {
                it[id] = newId
                it[Students.authUid] = authUid
                it[name] = req.name
                it[email] = req.email
                it[province] = req.province
                it[schoolName] = req.schoolName ?: "Escola Secundária Josina Machel"
                it[xpAccumulated] = 0
                it[level] = 1
            }

            // Atualiza ranking no Redis
            RankingService.updateStudentXp(newId.toString(), req.province, 0)

            val token = generateToken(newId.toString(), req.email)

            RegisterResponse(
                studentId = newId.toString(),
                name = req.name,
                level = 1,
                xpAccumulated = 0,
                createdAt = now.toString(),
                token = token
            )
        }
    }

    fun generateToken(studentId: String, email: String): String {
        return JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("studentId", studentId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000L * 30)) // 30 dias
            .sign(algorithm)
    }
}
