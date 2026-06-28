package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DATA CLASSES DO MODELO DE REQUEST/RESPONSE GEMINI COM MOSHI ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: Map<String, Any>? = null,
    @Json(name = "required") val required: List<String>? = null,
    @Json(name = "items") val items: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "responseSchema") val responseSchema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseSchema") val responseSchema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

// --- INTERFACES DO SERVICE RETROFIT ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- REPRESENTAÇÕES DOS MODELOS DO QUIZ DA IA ---

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    @Json(name = "question") val question: String,
    @Json(name = "options") val options: List<String>,
    @Json(name = "correctOptionIndex") val correctOptionIndex: Int,
    @Json(name = "explanation") val explanation: String
)

@JsonClass(generateAdapter = true)
data class InstantQuiz(
    @Json(name = "questions") val questions: List<QuizQuestion>
)

/**
 * Serviço de IA que conecta com o Gemini API utilizando Retrofit e Moshi.
 * Fornece feedback pedagógico preditivo e quizzes de validação de forma escalável.
 */
class GeminiCoachService {

    private val apiService: GeminiApiService

    init {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Gera recomendações premium e personalizadas para o estudante após sessões de estudo.
     */
    suspend fun getStudyCoachAdvice(topic: String, focusDurationMinutes: Long): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Para dicas personalizadas em tempo real da IA, configure a GEMINI_API_KEY no painel de segredos do AI Studio!\n\n💡 Dica Geral: Tente fazer pausas de 5 minutos a cada 25 minutos de estudo (Método Pomodoro)."
        }

        val prompt = "O estudante acabou de concluir uma sessão de foco estudando o tópico '$topic' por $focusDurationMinutes minutos. Como Coach de Estudos Inteligente da startup Dyondza, gere uma recomendação curta, motivadora e prática de pausa ativa ou dica de memorização para ajudá-lo a reter melhor o assunto estudado. Escreva em português, de forma amigável e focado em alta performance."

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7),
            systemInstruction = Content(parts = listOf(Part(text = "Você é o Coach de Estudos da startup Dyondza, focado em neurociência do aprendizado e alta performance escolar.")))
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Muito bem! Mantenha a consistência nos estudos."
        } catch (e: Exception) {
            Log.e("GeminiCoachService", "Erro ao obter dicas do Coach: ${e.message}")
            "Parabéns pelo esforço! Que tal uma pequena caminhada para oxigenar o cérebro antes de continuar?"
        }
    }

    /**
     * Gera um quiz interativo com 3 perguntas de múltipla escolha para validar o aprendizado do tópico via servidor Ktor.
     */
    suspend fun generateQuizForTopic(topic: String): InstantQuiz = withContext(Dispatchers.IO) {
        val fallbackQuiz = getFallbackQuiz(topic)
        try {
            val req = ClientQuizGenerateRequest(topic = topic)
            val res = DyondzaApiClient.api.generateQuiz(DyondzaApiClient.getAuthHeader(), req)
            if (res.questions.isNotEmpty()) {
                val mappedQuestions = res.questions.map { q ->
                    QuizQuestion(
                        question = q.question,
                        options = q.options,
                        correctOptionIndex = q.correctOptionIndex,
                        explanation = q.explanation
                    )
                }
                Log.d("GeminiCoachService", "Quiz gerado via Servidor Ktor (Cached: ${res.cached})!")
                InstantQuiz(questions = mappedQuestions)
            } else {
                fallbackQuiz
            }
        } catch (e: Exception) {
            Log.e("GeminiCoachService", "Erro ao buscar quiz no servidor Ktor: ${e.message}. Usando fallback offline.")
            fallbackQuiz
        }
    }

    private fun getFallbackQuiz(topic: String): InstantQuiz {
        return InstantQuiz(
            questions = listOf(
                QuizQuestion(
                    question = "Qual é o principal foco para consolidar o conhecimento sobre '$topic'?",
                    options = listOf(
                        "Revisão ativa e prática imediata de exercícios",
                        "Apenas ler passivamente anotações antigas",
                        "Estudar sem pausas até a exaustão mental",
                        "Decorar fórmulas sem entender o conceito base"
                    ),
                    correctOptionIndex = 0,
                    explanation = "A revisão ativa com testes práticos (Active Recall) é comprovadamente a técnica mais eficaz na retenção a longo prazo de qualquer assunto."
                ),
                QuizQuestion(
                    question = "Em relação a '$topic', o que é fundamental para evitar a Curva do Esquecimento?",
                    options = listOf(
                        "Esquecer o tema por um mês antes de revisar",
                        "Realizar revisões espaçadas periódicas",
                        "Estudar apenas no dia anterior ao teste",
                        "Apenas reler o texto original continuamente"
                    ),
                    correctOptionIndex = 1,
                    explanation = "A repetição espaçada resgata a informação da memória antes que ela decaia, consolidando as sinapses neurais do aprendizado."
                ),
                QuizQuestion(
                    question = "Qual destas atitudes maximiza a retenção do estudo de '$topic'?",
                    options = listOf(
                        "Tentar explicar o assunto em termos simples (Técnica de Feynman)",
                        "Manter o telefone com notificações ativas ao lado",
                        "Estudar ouvindo podcasts sobre outros temas",
                        "Fazer anotações literais copiando cada frase do livro"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Explicar conceitos difíceis de forma simplificada expõe lacunas na nossa compreensão e ativa processos profundos de síntese cognitiva."
                )
            )
        )
    }
}
