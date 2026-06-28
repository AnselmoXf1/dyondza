package com.dyondza.backend.config

import com.dyondza.backend.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

object DatabaseConfig {
    fun init() {
        val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/dyondza"
        val dbUser = System.getenv("DB_USER") ?: "postgres"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
        val isPostgres = dbUrl.startsWith("jdbc:postgresql")

        val config = HikariConfig().apply {
            if (isPostgres) {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = dbUrl
                username = dbUser
                password = dbPassword
            } else {
                // Fallback para H2 in-memory caso esteja rodando localmente sem Postgres
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:mem:dyondza;DB_CLOSE_DELAY=-1"
            }
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Students, FocusSessions, Missions, StudentMissions)
            seedDefaultMissionsIfEmpty()
        }
    }

    private fun seedDefaultMissionsIfEmpty() {
        if (Missions.selectAll().count() == 0L) {
            val now = LocalDateTime.now()
            Missions.insert {
                it[id] = UUID.fromString("2c62c3f8-62d0-4bf6-a579-450f38c642c6")
                it[title] = "Foco de Ferro"
                it[description] = "Acumule 45 minutos de estudo concentrado hoje"
                it[missionType] = "DAILY"
                it[targetValue] = 2700 // 45 minutos em segundos
                it[targetMetric] = "FOCUS_SECONDS"
                it[xpReward] = 50
                it[activeFrom] = now.minusDays(1)
                it[activeTo] = now.plusDays(1)
            }
            Missions.insert {
                it[id] = UUID.fromString("0e83b4b5-512c-461b-94aa-7132049e7bdf")
                it[title] = "Mestre Consistente"
                it[description] = "Complete 3 sessões de foco sem alertas"
                it[missionType] = "WEEKLY"
                it[targetValue] = 3
                it[targetMetric] = "CLEAN_SESSIONS"
                it[xpReward] = 200
                it[activeFrom] = now.minusDays(7)
                it[activeTo] = now.plusDays(7)
            }
        }
    }
}
