package com.sailapp.data.db.dao

import androidx.room.*
import com.sailapp.data.db.entities.RegattaSession
import com.sailapp.data.db.entities.StartLine
import kotlinx.coroutines.flow.Flow

@Dao
interface RegattaDao {

    @Query("SELECT * FROM regatta_sessions ORDER BY id DESC")
    fun getAllSessions(): Flow<List<RegattaSession>>

    @Query("SELECT * FROM regatta_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): RegattaSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RegattaSession): Long

    @Update
    suspend fun updateSession(session: RegattaSession)

    @Delete
    suspend fun deleteSession(session: RegattaSession)

    @Query("SELECT * FROM start_lines WHERE regattaSessionId = :sessionId LIMIT 1")
    suspend fun getStartLineForSession(sessionId: Long): StartLine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStartLine(startLine: StartLine): Long

    @Update
    suspend fun updateStartLine(startLine: StartLine)

    @Delete
    suspend fun deleteStartLine(startLine: StartLine)
}
