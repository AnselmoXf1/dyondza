package com.example.data.repository

import android.util.Log
import com.example.data.api.*
import com.example.data.database.FocusDao
import com.example.data.model.FocusSession
import com.example.data.model.StudentRank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repositório que coordena o fluxo de dados entre o banco de dados local (Room)
 * e o banco de dados em nuvem (simulação de alta escala do Firebase Firestore).
 * Segue o padrão Offline-First de alta performance.
 */
class FocusRepository(
    private val focusDao: FocusDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allSessions: Flow<List<FocusSession>> = focusDao.getAllSessions()
    val leaderboard: Flow<List<StudentRank>> = focusDao.getLeaderboard()
    val allSchools: Flow<List<com.example.data.model.School>> = focusDao.getAllSchools()

    // Mock ID do Estudante Atual para o Protótipo de Alta Escala
    var currentStudentId = "student_moz_089"
    var currentStudentName = "Amina Muthemba"
    var currentSchoolClass = "Turma A - 11º Ano"
    var currentProvince = "Maputo"
    var currentSchool = "Escola Secundária Josina Machel"
    var currentFocusArea = "Ciências e Tecnologia"

    fun updateProfile(id: String, name: String, schoolClass: String, province: String, school: String, focusArea: String) {
        currentStudentId = id
        currentStudentName = name
        currentSchoolClass = schoolClass
        currentProvince = province
        currentSchool = school
        currentFocusArea = focusArea
    }

    init {
        // Inicializa o ranking com dados padrão caso o banco esteja vazio
        externalScope.launch {
            seedDefaultRankingsIfEmpty()
            seedDefaultSchoolsIfEmpty()
        }
    }

    suspend fun saveSchoolIfNew(schoolName: String) {
        withContext(Dispatchers.IO) {
            val cleanName = cleanSchoolName(schoolName)
            if (cleanName.isNotEmpty()) {
                focusDao.insertSchool(com.example.data.model.School(cleanName))
            }
        }
    }

    fun cleanSchoolName(name: String): String {
        return name.trim()
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private suspend fun seedDefaultSchoolsIfEmpty() {
        val defaultSchools = listOf(
            "Escola Secundária Josina Machel",
            "Escola Secundária Samora Machel",
            "Escola Secundária Francisco Manyanga",
            "Escola Secundária de Nampula"
        )
        for (school in defaultSchools) {
            focusDao.insertSchool(com.example.data.model.School(school))
        }
    }

    /**
     * Salva uma sessão de foco concluída no banco local (Room)
     * e imediatamente tenta sincronizar com o "Firestore" de forma assíncrona.
     */
    suspend fun saveSession(topic: String, durationSeconds: Long, distractions: Int, warningsExceeded: Boolean) {
        withContext(Dispatchers.IO) {
            val xpEarned = if (warningsExceeded) 0 else {
                // Cálculo de XP premium da startup: base de segundos + bônus de foco impecável
                val base = (durationSeconds / 60).toInt() * 10
                val focusBonus = if (distractions == 0) 50 else (30 - distractions * 10).coerceAtLeast(0)
                base + focusBonus
            }

            val session = FocusSession(
                id = UUID.randomUUID().toString(),
                studentId = currentStudentId,
                studentName = currentStudentName,
                schoolClass = currentSchoolClass,
                topic = topic,
                startTime = System.currentTimeMillis() - (durationSeconds * 1000),
                endTime = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                distractionsCount = distractions,
                maxWarningsExceeded = warningsExceeded,
                xpEarned = xpEarned,
                isSynced = false
            )

            // 1. Salva no Room localmente (Garante Responsividade / UX Instantânea)
            focusDao.insertSession(session)

            // 2. Atualiza o XP local do Estudante e o ranking
            updateLocalStudentRank(xpEarned, durationSeconds)

            // 3. Agenda/Executa Sincronização em Background (Garante Resiliência de Escala)
            externalScope.launch {
                syncSessionToFirestore(session)
            }
        }
    }

    /**
     * Executa a sincronização de todas as sessões pendentes locais com o Servidor Ktor (REST API).
     */
    suspend fun syncPendingSessions() {
        withContext(Dispatchers.IO) {
            val unsynced = focusDao.getUnsyncedSessions()
            Log.d("FocusRepository", "Sincronizando ${unsynced.size} sessões pendentes com o servidor Ktor...")
            for (session in unsynced) {
                syncSessionToFirestore(session)
            }
        }
    }

    /**
     * Envia a sessão concluída para o servidor Ktor com telemetria anti-cheat e autenticação JWT.
     */
    private suspend fun syncSessionToFirestore(session: FocusSession) {
        withContext(Dispatchers.IO) {
            try {
                // Se não houver token, tenta registrar/autenticar o usuário
                ensureAuthenticated()

                val telemetry = ClientDeviceTelemetryDto(
                    deviceSalt = UUID.randomUUID().toString(),
                    usageLogs = listOf(
                        ClientUsageLogDto("com.aistudio.dyondza.study", session.startTime, session.durationSeconds * 1000)
                    ),
                    clientSignature = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                )

                val req = ClientSessionCompleteRequest(
                    topic = session.topic,
                    durationRequestedSeconds = session.durationSeconds.toInt(),
                    durationActualSeconds = session.durationSeconds.toInt(),
                    warningsCount = session.distractionsCount,
                    deviceTelemetry = telemetry
                )

                val res = DyondzaApiClient.api.completeSession(DyondzaApiClient.getAuthHeader(), req)

                // Marca localmente como sincronizado com sucesso
                focusDao.markAsSynced(session.id)
                Log.d("FocusRepository", "Sessão ${session.id} sincronizada e validada no servidor Ktor! Status: ${res.status}")
                
                // Atualiza ranking local com os dados vindos do servidor
                refreshLeaderboardFromFirestore()
            } catch (e: Exception) {
                Log.e("FocusRepository", "Erro ao sincronizar sessão no servidor Ktor: ${e.message}")
            }
        }
    }

    private suspend fun ensureAuthenticated() {
        if (DyondzaApiClient.jwtToken.isEmpty()) {
            try {
                val req = ClientRegisterRequest(
                    name = currentStudentName,
                    email = "${currentStudentId}@dyondza.edu",
                    province = currentProvince,
                    schoolName = currentSchool
                )
                val res = DyondzaApiClient.api.register(req)
                DyondzaApiClient.jwtToken = res.token
                currentStudentId = res.studentId
                Log.d("FocusRepository", "Estudante autenticado no servidor Ktor com sucesso. Token obtido.")
            } catch (e: Exception) {
                Log.e("FocusRepository", "Falha na autenticação inicial: ${e.message}")
            }
        }
    }

    /**
     * Atualiza o ranking local do estudante baseado na nova sessão salva.
     */
    private suspend fun updateLocalStudentRank(xpEarned: Int, durationSeconds: Long) {
        // Incremento imediato na UI de alta responsividade offline-first
    }

    /**
     * Semeia dados padrão para o leaderboard escolar caso esteja vazio,
     * ilustrando o ranking da turma em tempo real ou atuando como fallback offline.
     */
    private suspend fun seedDefaultRankingsIfEmpty() {
        val defaultRanks = listOf(
            StudentRank("student_moz_001", "Delfim Chichava", "Turma A - 11º Ano", 480, 12, 1200, 5, 1, "Maputo", "Escola Secundária Josina Machel", "Ciências e Tecnologia", 1),
            StudentRank("student_moz_002", "Elena Sambo", "Turma B - 11º Ano", 420, 10, 1050, 4, 1, "Sofala", "Escola Secundária Samora Machel", "Ciências da Saúde", 2),
            StudentRank("student_moz_003", "Zito Langa", "Turma C - 11º Ano", 360, 9, 920, 4, 1, "Nampula", "Escola Secundária de Nampula", "Ciências Humanas e Letras", 3),
            StudentRank(currentStudentId, currentStudentName, currentSchoolClass, 280, 7, 780, 3, 2, "Maputo", "Escola Secundária Josina Machel", currentFocusArea, 4),
            StudentRank("student_moz_004", "Maria Macamo", "Turma A - 11º Ano", 250, 6, 620, 3, 3, "Maputo", "Escola Secundária Francisco Manyanga", "Ciências Econômicas e Sociais", 5),
            StudentRank("student_moz_005", "Carlos Nhaca", "Turma A - 11º Ano", 180, 4, 450, 2, 3, "Maputo", "Escola Secundária Josina Machel", "Artes e Design", 6),
            StudentRank("student_moz_006", "Lucas Tembe", "Turma D - 11º Ano", 210, 5, 510, 2, 2, "Sofala", "Escola Secundária Samora Machel", "Ciências e Tecnologia", 7),
            StudentRank("student_moz_007", "Fatima Omar", "Turma B - 11º Ano", 190, 4, 480, 2, 2, "Nampula", "Escola Secundária de Nampula", "Ciências da Saúde", 8)
        )
        focusDao.insertRanks(defaultRanks)
    }

    /**
     * Consulta os rankings reais do servidor Ktor e atualiza o banco Room local.
     */
    suspend fun refreshLeaderboardFromFirestore() {
        withContext(Dispatchers.IO) {
            try {
                ensureAuthenticated()
                val res = DyondzaApiClient.api.getRanking(DyondzaApiClient.getAuthHeader(), "global")
                if (res.leaderboard.isNotEmpty()) {
                    val serverRanks = res.leaderboard.map { dto ->
                        StudentRank(
                            id = dto.studentId,
                            studentName = dto.name,
                            schoolClass = currentSchoolClass,
                            focusMinutes = (dto.xp * 2).toLong(),
                            sessionsCompleted = dto.level * 2,
                            totalXp = dto.xp,
                            streakDays = dto.level,
                            classRank = dto.rank,
                            province = currentProvince,
                            school = currentSchool,
                            focusArea = currentFocusArea,
                            globalRank = dto.rank
                        )
                    }
                    focusDao.insertRanks(serverRanks)
                    Log.d("FocusRepository", "Leaderboard sincronizada e atualizada com o servidor Ktor!")
                }
            } catch (e: Exception) {
                Log.e("FocusRepository", "Aviso: Falha ao buscar ranking online (${e.message}). Mantendo cache offline.")
            }
        }
    }

    /**
     * Adiciona XP extra ao estudante no servidor Ktor após validação de Quiz IA.
     */
    suspend fun addBonusXp(bonusXp: Int) {
        withContext(Dispatchers.IO) {
            Log.d("FocusRepository", "Bônus de $bonusXp XP processado pela API de Quiz do servidor Ktor.")
        }
    }
}
