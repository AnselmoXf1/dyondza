package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiCoachService
import com.example.data.api.InstantQuiz
import com.example.data.database.AppDatabase
import com.example.data.model.FocusSession
import com.example.data.model.StudentRank
import com.example.data.repository.FocusRepository
import com.example.service.FocusTrackerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlin.math.sqrt
import kotlin.math.pow

sealed interface QuizUiState {
    object Idle : QuizUiState
    object Loading : QuizUiState
    data class Success(val quiz: InstantQuiz) : QuizUiState
    data class Error(val message: String) : QuizUiState
    data class Completed(val score: Int, val xpEarned: Int) : QuizUiState
}

/**
 * ViewModel central da arquitetura MVVM do Dyondza.
 * Conecta e gerencia de forma limpa as ações do usuário nas telas e a lógica assíncrona.
 */
class FocusViewModel(
    private val repository: FocusRepository,
    private val geminiService: GeminiCoachService = GeminiCoachService()
) : ViewModel() {

    // Liga-se diretamente ao StateFlow do serviço de monitoramento
    val sessionState: StateFlow<FocusTrackerService.FocusSessionState> = FocusTrackerService.sessionState

    // Histórico de estudos e Leaderboard vindos do Room reativamente
    val studyHistory: StateFlow<List<FocusSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CÁLCULOS E PROGRESSO BASEADOS EM HORAS DE ESTUDO ACUMULADAS ---
    val totalStudyHours: StateFlow<Double> = repository.allSessions
        .map { list -> list.sumOf { it.durationSeconds }.toDouble() / 3600.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentLevel: StateFlow<Int> = totalStudyHours
        .map { hours -> (1 + Math.floor(sqrt(hours * 2.0))).toInt().coerceIn(1, 50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val progressToNextLevel: StateFlow<Float> = totalStudyHours
        .map { hours ->
            val level = (1 + Math.floor(sqrt(hours * 2.0))).toInt().coerceIn(1, 50)
            val minHours = (level - 1).toDouble().pow(2) / 2.0
            val maxHours = level.toDouble().pow(2) / 2.0
            val range = maxHours - minHours
            if (range > 0.0) {
                ((hours - minHours) / range).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Controle do Modal/Dialog de comemoração de Level Up
    private val _levelUpCelebration = MutableStateFlow<Int?>(null)
    val levelUpCelebration: StateFlow<Int?> = _levelUpCelebration.asStateFlow()

    fun dismissLevelUpCelebration() {
        _levelUpCelebration.value = null
    }

    private var lastObservedLevel = -1

    init {
        viewModelScope.launch {
            currentLevel.collect { newLevel ->
                if (lastObservedLevel != -1 && newLevel > lastObservedLevel) {
                    // Level Up! Disparar áudio retrô sintético e exibir comemoração visual animada
                    _levelUpCelebration.value = newLevel
                    playLevelUpSynthSound()
                }
                lastObservedLevel = newLevel
            }
        }
    }

    private fun playLevelUpSynthSound() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                // C5 (523 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 120)
                delay(150)
                // E5 (659 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 120)
                delay(150)
                // G5 (784 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 120)
                delay(150)
                // C6 (1046 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 350)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playSessionCompletedSound() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                // E5 (659 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 100)
                delay(120)
                // G5 (784 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 100)
                delay(120)
                // B5 (987 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 100)
                delay(120)
                // E6 (1318 Hz)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val leaderboard: StateFlow<List<StudentRank>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado das dicas geradas pela IA Coach
    private val _coachAdvice = MutableStateFlow<String?>(null)
    val coachAdvice: StateFlow<String?> = _coachAdvice.asStateFlow()

    // Estado do Quiz de Validação da IA
    private val _quizUiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizUiState: StateFlow<QuizUiState> = _quizUiState.asStateFlow()

    // Respostas selecionadas pelo usuário durante o Quiz ativo
    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    // Controle de estado para sincronizações em andamento
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Informações do Aluno logado (Startup Scale Profile)
    private val _studentName = MutableStateFlow(repository.currentStudentName)
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentProvince = MutableStateFlow(repository.currentProvince)
    val studentProvince: StateFlow<String> = _studentProvince.asStateFlow()

    private val _studentSchool = MutableStateFlow(repository.currentSchool)
    val studentSchool: StateFlow<String> = _studentSchool.asStateFlow()

    private val _studentClass = MutableStateFlow(repository.currentSchoolClass)
    val studentClass: StateFlow<String> = _studentClass.asStateFlow()

    private val _studentFocusArea = MutableStateFlow(repository.currentFocusArea)
    val studentFocusArea: StateFlow<String> = _studentFocusArea.asStateFlow()

    // Lista de todas as escolas recomendadas registradas
    val allSchools: StateFlow<List<com.example.data.model.School>> = repository.allSchools
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(false) // Começa deslogado para exibir Splash/Login
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Configuration Settings (Definições funcionais)
    private val _isNotificationEnabled = MutableStateFlow(true)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isDndModeEnabled = MutableStateFlow(true)
    val isDndModeEnabled: StateFlow<Boolean> = _isDndModeEnabled.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(45)
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    // Resumo da Sessão Recém Concluída
    data class SessionSummary(
        val topic: String,
        val durationSeconds: Long,
        val warningsCount: Int,
        val xpEarned: Int
    )

    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary: StateFlow<SessionSummary?> = _sessionSummary.asStateFlow()

    fun dismissSessionSummary() {
        _sessionSummary.value = null
    }

    fun toggleNotificationBlocking(enabled: Boolean) {
        _isNotificationEnabled.value = enabled
    }

    fun updateSettings(notifications: Boolean, dnd: Boolean, dailyGoal: Int) {
        _isNotificationEnabled.value = notifications
        _isDndModeEnabled.value = dnd
        _dailyGoalMinutes.value = dailyGoal
    }

    fun login(email: String, name: String, province: String, school: String, focusArea: String = "Ciências e Tecnologia") {
        _studentProvince.value = province.ifEmpty { "Maputo" }
        _studentSchool.value = school.ifEmpty { "Escola Secundária Josina Machel" }
        _studentFocusArea.value = focusArea.ifEmpty { "Ciências e Tecnologia" }
        repository.updateProfile(
            id = "student_moz_" + email.hashCode().toString().takeLast(4),
            name = name.ifEmpty { "Amina Muthemba" },
            schoolClass = "Turma A - 11º Ano",
            province = _studentProvince.value,
            school = _studentSchool.value,
            focusArea = _studentFocusArea.value
        )
        _studentName.value = repository.currentStudentName
        _isLoggedIn.value = true

        // Salva a escola inserida para futuras recomendações
        viewModelScope.launch {
            repository.saveSchoolIfNew(_studentSchool.value)
        }
    }

    fun register(name: String, email: String, province: String, school: String, focusArea: String) {
        _studentProvince.value = province.ifEmpty { "Maputo" }
        _studentSchool.value = school.ifEmpty { "Escola Secundária Josina Machel" }
        _studentFocusArea.value = focusArea.ifEmpty { "Ciências e Tecnologia" }
        repository.updateProfile(
            id = "student_moz_" + email.hashCode().toString().takeLast(4),
            name = name.ifEmpty { "Amina Muthemba" },
            schoolClass = "Turma A - 11º Ano",
            province = _studentProvince.value,
            school = _studentSchool.value,
            focusArea = _studentFocusArea.value
        )
        _studentName.value = name
        _isLoggedIn.value = true

        // Salva a escola inserida para futuras recomendações
        viewModelScope.launch {
            repository.saveSchoolIfNew(_studentSchool.value)
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    fun updateProfileInfo(name: String, province: String, school: String, focusArea: String) {
        _studentFocusArea.value = focusArea.ifEmpty { "Ciências e Tecnologia" }
        repository.updateProfile(
            id = repository.currentStudentId,
            name = name,
            schoolClass = repository.currentSchoolClass,
            province = province,
            school = school,
            focusArea = _studentFocusArea.value
        )
        _studentName.value = name
        _studentProvince.value = province
        _studentSchool.value = school

        // Salva a escola inserida para futuras recomendações
        viewModelScope.launch {
            repository.saveSchoolIfNew(school)
        }
    }

    init {
        // Observa as conclusões de sessões para puxar feedbacks do Gemini Coach imediatamente
        viewModelScope.launch {
            sessionState.collect { state ->
                if (state.justCompleted && state.lastCompletedDuration > 0) {
                    fetchCoachAdvice(state.topic, state.lastCompletedDuration / 60)
                    
                    val baseMinutes = state.lastCompletedDuration / 60
                    val baseLimit = if (baseMinutes > 0) baseMinutes else 1
                    val penalty = (state.warningsCount * 5).toLong()
                    val xpEarned = ((baseLimit * 12) + 25 - penalty).coerceAtLeast(15L).toInt()
                    
                    _sessionSummary.value = SessionSummary(
                        topic = state.topic,
                        durationSeconds = state.lastCompletedDuration,
                        warningsCount = state.warningsCount,
                        xpEarned = xpEarned
                    )
                    
                    playSessionCompletedSound()
                }
            }
        }
    }

    /**
     * Aciona o serviço de primeiro plano para iniciar o rastreamento da sessão de foco.
     */
    fun startSession(context: Context, topic: String, countdownDurationSeconds: Long = 0L) {
        val intent = Intent(context, FocusTrackerService::class.java).apply {
            action = FocusTrackerService.ACTION_START
            putExtra(FocusTrackerService.EXTRA_TOPIC, topic)
            if (countdownDurationSeconds > 0L) {
                putExtra(FocusTrackerService.EXTRA_COUNTDOWN_DURATION, countdownDurationSeconds)
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        // Limpa estados de quiz anteriores
        _quizUiState.value = QuizUiState.Idle
        _selectedAnswers.value = emptyMap()
    }

    /**
     * Encerra com sucesso a sessão ativa, salvando os dados.
     */
    fun stopSession(context: Context) {
        val intent = Intent(context, FocusTrackerService::class.java).apply {
            action = FocusTrackerService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Cancela e invalida a sessão ativa.
     */
    fun cancelSession(context: Context) {
        val intent = Intent(context, FocusTrackerService::class.java).apply {
            action = FocusTrackerService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    /**
     * Consulta as dicas do Coach de Estudos Inteligente via Gemini API.
     */
    fun fetchCoachAdvice(topic: String, durationMinutes: Long) {
        viewModelScope.launch {
            _coachAdvice.value = "Analisando foco com a IA..."
            val advice = geminiService.getStudyCoachAdvice(topic, durationMinutes)
            _coachAdvice.value = advice
        }
    }

    /**
     * Solicita à IA a criação de um Quiz sob demanda sobre o tema estudado.
     */
    fun generateQuiz(topic: String) {
        viewModelScope.launch {
            _quizUiState.value = QuizUiState.Loading
            _selectedAnswers.value = emptyMap()
            try {
                val quiz = geminiService.generateQuizForTopic(topic)
                _quizUiState.value = QuizUiState.Success(quiz)
            } catch (e: Exception) {
                _quizUiState.value = QuizUiState.Error("Não foi possível gerar o quiz: ${e.message}")
            }
        }
    }

    /**
     * Registra a resposta escolhida pelo aluno em uma pergunta específica.
     */
    fun selectAnswer(questionIndex: Int, answerIndex: Int) {
        val current = _selectedAnswers.value.toMutableMap()
        current[questionIndex] = answerIndex
        _selectedAnswers.value = current
    }

    /**
     * Avalia o resultado do quiz e concede recompensas (XP de gamificação) sincronizadas com o repositório.
     */
    fun submitQuiz(quiz: InstantQuiz) {
        val answers = _selectedAnswers.value
        var correctCount = 0
        quiz.questions.forEachIndexed { index, question ->
            if (answers[index] == question.correctOptionIndex) {
                correctCount++
            }
        }

        // 100 XP extras para acerto impecável (3/3), 50 XP para 2/3, 0 XP para menos que isso
        val bonusXp = when (correctCount) {
            3 -> 100
            2 -> 50
            else -> 0
        }

        viewModelScope.launch {
            if (bonusXp > 0) {
                repository.addBonusXp(bonusXp)
            }
            _quizUiState.value = QuizUiState.Completed(correctCount, bonusXp)
        }
    }

    /**
     * Sincronização manual com o "Firestore" de alta performance.
     */
    fun syncWithFirestore() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncPendingSessions()
            repository.refreshLeaderboardFromFirestore()
            _isSyncing.value = false
        }
    }

    /**
     * Força a simulação de uma infração de abertura de aplicativo proibido.
     * Facilita a validação de comportamento e auditoria técnica.
     */
    fun simulateDistraction(context: Context, packageName: String) {
        FocusTrackerService.triggerSimulatedWarning(context, packageName)
    }

    /**
     * Factory de conveniência para instanciar o ViewModel com escopo de banco de dados.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
                val dao = AppDatabase.getDatabase(context).focusDao()
                val repo = FocusRepository(dao)
                @Suppress("UNCHECKED_CAST")
                return FocusViewModel(repo) as T
            }
            throw IllegalArgumentException("ViewModel desconhecido")
        }
    }
}
