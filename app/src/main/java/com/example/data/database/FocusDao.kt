package com.example.data.database

import androidx.room.*
import com.example.data.model.FocusSession
import com.example.data.model.StudentRank
import com.example.data.model.School
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    // --- SESSÕES DE FOCO ---

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<FocusSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Update
    suspend fun updateSession(session: FocusSession)

    @Query("UPDATE focus_sessions SET isSynced = 1 WHERE id = :sessionId")
    suspend fun markAsSynced(sessionId: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllSessions()

    // --- LEADERBOARD / RANKINGS ---

    @Query("SELECT * FROM student_ranks ORDER BY totalXp DESC")
    fun getLeaderboard(): Flow<List<StudentRank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRanks(ranks: List<StudentRank>)

    @Query("DELETE FROM student_ranks")
    suspend fun clearAllRanks()

    // --- ESCOLAS REGISTRADAS / RECOMENDAÇÃO ---

    @Query("SELECT * FROM registered_schools ORDER BY name ASC")
    fun getAllSchools(): Flow<List<School>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSchool(school: School)
}
