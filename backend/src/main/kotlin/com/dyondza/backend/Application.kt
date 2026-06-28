package com.dyondza.backend

import com.dyondza.backend.config.DatabaseConfig
import com.dyondza.backend.config.RedisConfig
import com.dyondza.backend.routes.apiRoutes
import com.dyondza.backend.service.AuthService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Inicializa Conexões de Banco de Dados e Cache
    DatabaseConfig.init()
    RedisConfig.init()

    // 2. Plugins de Monitoramento e Serialização
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // 3. Configuração de CORS (Habilita comunicação segura para KMP Mobile / Web)
    install(CORS) {
        anyHost() // Em produção estrita, restringir aos domínios do Dyondza
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    // 4. Autenticação JWT
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                com.auth0.jwt.JWT.require(AuthService.algorithm)
                    .withAudience(AuthService.AUDIENCE)
                    .withIssuer(AuthService.ISSUER)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("studentId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // 5. Roteamento da API REST
    routing {
        get("/") {
            call.respondText("Dyondza Gamification & Anti-Cheat Backend Server Running 🚀")
        }
        apiRoutes()
    }
}
