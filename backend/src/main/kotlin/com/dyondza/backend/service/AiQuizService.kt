package com.dyondza.backend.service

import com.dyondza.backend.config.RedisConfig
import com.dyondza.backend.model.QuizGenerateRequest
import com.dyondza.backend.model.QuizGenerateResponse
import com.dyondza.backend.model.QuizQuestionDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object AiQuizService {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation)
    }

    suspend fun generateQuiz(req: QuizGenerateRequest): QuizGenerateResponse {
        val topicKey = "quiz:" + req.topic.trim().lowercase()
        
        // 1. Cache-Aside: Verifica se já existe no Redis
        val cachedJson = RedisConfig.get(topicKey)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val questions = json.decodeFromString<List<QuizQuestionDto>>(cachedJson)
                return QuizGenerateResponse(topic = req.topic, questions = questions, cached = true)
            } catch (e: Exception) {
                // Ignore e regere em caso de falha no parse
            }
        }

        // 2. Chamada à API da Gemini no Servidor
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
        var questions: List<QuizQuestionDto>? = null

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Gere um quiz educacional de 3 perguntas de múltipla escolha sobre o tópico: "${req.topic}".
                    Retorne EXCLUSIVAMENTE um array JSON puro, sem formatação markdown ou blocos de código, no formato:
                    [
                      {
                        "question": "Pergunta clara?",
                        "options": ["Opção A", "Opção B", "Opção C", "Opção D"],
                        "correctOptionIndex": 0,
                        "explanation": "Explicação pedagógica rápida."
                      }
                    ]
                """.trimIndent()

                val requestBody = buildJsonObject {
                    put("contents", buildJsonArray {
                        add(buildJsonObject {
                            put("parts", buildJsonArray {
                                add(buildJsonObject { put("text", prompt) })
                            })
                        })
                    })
                }

                val response: HttpResponse = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }

                if (response.status.isSuccess()) {
                    val responseText = response.body<String>()
                    val element = json.parseToJsonElement(responseText)
                    val candidateText = element.jsonObject["candidates"]?.jsonArray?.get(0)
                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.get(0)
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                    val cleanJson = candidateText.replace("```json", "").replace("```", "").trim()
                    questions = json.decodeFromString<List<QuizQuestionDto>>(cleanJson)
                }
            } catch (e: Exception) {
                println("Erro na chamada Server-Side ao Gemini: ${e.message}")
            }
        }

        // Fallback robusto caso a chave não esteja definida ou a API falhe
        if (questions == null || questions.isEmpty()) {
            questions = listOf(
                QuizQuestionDto(
                    question = "Qual é o principal objetivo do estudo concentrado em '${req.topic}'?",
                    options = listOf("Absorção profunda e retenção de conhecimento", "Distração contínua", "Decoração temporária sem entendimento", "Uso de redes sociais"),
                    correctOptionIndex = 0,
                    explanation = "O foco elimina interrupções cognitivas, facilitando a neuroplasticidade e retenção de longo prazo."
                ),
                QuizQuestionDto(
                    question = "Como o método de foco sem interrupções auxilia no aprendizado?",
                    options = listOf("Diminui a carga cognitiva inútil", "Aumenta o cansaço mental rapidamente", "Impede o pensamento crítico", "Requer multitarefa excessiva"),
                    correctOptionIndex = 0,
                    explanation = "Ao evitar a multitarefa, o cérebro economiza energia mental e consolida melhor as informações."
                ),
                QuizQuestionDto(
                    question = "Qual atitude demonstra maestria durante uma sessão de estudo?",
                    options = listOf("Manter o dispositivo focado apenas no app de estudo", "Alternar abas a cada 5 minutos", "Ignorar alertas do temporizador", "Estudar em ambientes ruidosos sem pausa"),
                    correctOptionIndex = 0,
                    explanation = "A disciplina e o isolamento de notificações externas são os pilares dos alunos de alto desempenho no Dyondza."
                )
            )
        }

        // 3. Salva no Cache Redis por 24 horas (86400 segundos)
        try {
            RedisConfig.setex(topicKey, 86400L, json.encodeToString(questions))
        } catch (e: Exception) {
            // ignore
        }

        return QuizGenerateResponse(topic = req.topic, questions = questions, cached = false)
    }
}
