package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa uma sessão de estudo focado (Focus Session) realizada pelo estudante.
 * Projetado para persistência offline-first com Room e sincronização em escala global com o Firestore.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String, // UUID gerado para garantir unicidade global no Firestore
    val studentId: String,
    val studentName: String,
    val schoolClass: String, // Ex: "Turma A - 10º Ano"
    val topic: String, // Assunto estudado
    val startTime: Long, // Epoch timestamp de início
    val endTime: Long, // Epoch timestamp de término
    val durationSeconds: Long, // Duração de foco líquido validado
    val distractionsCount: Int, // Número de vezes que abriu apps proibidos
    val maxWarningsExceeded: Boolean, // Se ultrapassou o limite de 3 avisos
    val xpEarned: Int, // XP recebido pela sessão
    val isSynced: Boolean = false // Flag para controle de sincronização com o Firestore
) {
    /**
     * Converte o modelo para um Map compatível com o Firestore do Firebase.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "studentId" to studentId,
            "studentName" to studentName,
            "schoolClass" to schoolClass,
            "topic" to topic,
            "startTime" to startTime,
            "endTime" to endTime,
            "durationSeconds" to durationSeconds,
            "distractionsCount" to distractionsCount,
            "maxWarningsExceeded" to maxWarningsExceeded,
            "xpEarned" to xpEarned,
            "timestamp" to System.currentTimeMillis() // Para queries de ordenação no Firestore
        )
    }

    companion object {
        /**
         * Cria uma instância de FocusSession a partir de um Map vindo do Firestore.
         */
        fun fromFirestoreMap(map: Map<String, Any>): FocusSession {
            return FocusSession(
                id = map["id"] as? String ?: "",
                studentId = map["studentId"] as? String ?: "",
                studentName = map["studentName"] as? String ?: "",
                schoolClass = map["schoolClass"] as? String ?: "",
                topic = map["topic"] as? String ?: "",
                startTime = (map["startTime"] as? Number)?.toLong() ?: 0L,
                endTime = (map["endTime"] as? Number)?.toLong() ?: 0L,
                durationSeconds = (map["durationSeconds"] as? Number)?.toLong() ?: 0L,
                distractionsCount = (map["distractionsCount"] as? Number)?.toInt() ?: 0,
                maxWarningsExceeded = map["maxWarningsExceeded"] as? Boolean ?: false,
                xpEarned = (map["xpEarned"] as? Number)?.toInt() ?: 0,
                isSynced = true
            )
        }
    }
}
