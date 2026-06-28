package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa a classificação (Ranking) de um estudante no ecossistema escolar.
 * Sincronizado globalmente em tempo real com o Firestore para rankear turmas e escolas.
 */
@Entity(tableName = "student_ranks")
data class StudentRank(
    @PrimaryKey val studentId: String,
    val name: String,
    val schoolClass: String, // ex: "Turma A"
    val totalFocusMinutes: Long, // Tempo total acumulado de foco validado
    val completedSessions: Int, // Total de sessões concluídas com sucesso
    val totalXp: Int, // XP acumulado para gamificação
    val level: Int, // Nível atual do estudante baseado no XP
    val currentRank: Int, // Posição atual do ranking da classe/escola
    val province: String = "Maputo", // Província de Moçambique
    val school: String = "Escola Secundária Josina Machel", // Nome da Escola
    val focusArea: String = "Ciências e Tecnologia", // Área de dedicação escolhida
    val globalRank: Int = currentRank, // Posição nacional/global
    val avatarUrl: String = "", // URL para avatar customizado do aluno
    val lastActive: Long = System.currentTimeMillis()
) {
    /**
     * Converte o ranking para um Map compatível com o Firestore do Firebase.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "studentId" to studentId,
            "name" to name,
            "schoolClass" to schoolClass,
            "totalFocusMinutes" to totalFocusMinutes,
            "completedSessions" to completedSessions,
            "totalXp" to totalXp,
            "level" to level,
            "currentRank" to currentRank,
            "province" to province,
            "school" to school,
            "focusArea" to focusArea,
            "globalRank" to globalRank,
            "avatarUrl" to avatarUrl,
            "lastActive" to lastActive
        )
    }

    companion object {
        /**
         * Cria uma instância de StudentRank a partir de um Map vindo do Firestore.
         */
        fun fromFirestoreMap(map: Map<String, Any>): StudentRank {
            return StudentRank(
                studentId = map["studentId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                schoolClass = map["schoolClass"] as? String ?: "",
                totalFocusMinutes = (map["totalFocusMinutes"] as? Number)?.toLong() ?: 0L,
                completedSessions = (map["completedSessions"] as? Number)?.toInt() ?: 0,
                totalXp = (map["totalXp"] as? Number)?.toInt() ?: 0,
                level = (map["level"] as? Number)?.toInt() ?: 1,
                currentRank = (map["currentRank"] as? Number)?.toInt() ?: 0,
                province = map["province"] as? String ?: "Maputo",
                school = map["school"] as? String ?: "Escola Secundária Josina Machel",
                focusArea = map["focusArea"] as? String ?: "Ciências e Tecnologia",
                globalRank = (map["globalRank"] as? Number)?.toInt() ?: ((map["currentRank"] as? Number)?.toInt() ?: 0),
                avatarUrl = map["avatarUrl"] as? String ?: "",
                lastActive = (map["lastActive"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
