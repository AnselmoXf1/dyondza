package com.dyondza.backend.routes

import com.dyondza.backend.model.*
import com.dyondza.backend.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.apiRoutes() {
    route("/api/v1") {
        // A. Autenticação e Registro (Livre)
        post("/auth/register") {
            try {
                val req = call.receive<RegisterRequest>()
                val res = AuthService.register(req)
                call.respond(HttpStatusCode.Created, res)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Erro no registro")))
            }
        }

        // B. Rotas Autenticadas (JWT)
        authenticate("auth-jwt") {
            post("/session/complete") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val studentId = principal?.payload?.getClaim("studentId")?.asString()
                        ?: throw IllegalArgumentException("Estudante não autenticado")

                    val req = call.receive<SessionCompleteRequest>()
                    val res = SessionService.completeSession(studentId, req)
                    call.respond(HttpStatusCode.OK, res)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Falha ao processar sessão")))
                }
            }

            get("/missions/check") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val studentId = principal?.payload?.getClaim("studentId")?.asString() ?: ""
                    val res = MissionService.checkMissions(studentId)
                    call.respond(HttpStatusCode.OK, res)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Erro nas missões")))
                }
            }

            get("/ranking/{region}") {
                try {
                    val region = call.parameters["region"] ?: "global"
                    val res = RankingService.getRanking(region)
                    call.respond(HttpStatusCode.OK, res)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Erro ao buscar ranking")))
                }
            }

            post("/quiz/generate") {
                try {
                    val req = call.receive<QuizGenerateRequest>()
                    val res = AiQuizService.generateQuiz(req)
                    call.respond(HttpStatusCode.OK, res)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Erro ao gerar quiz IA")))
                }
            }
        }
    }
}
