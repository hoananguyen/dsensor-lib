package com.hoan.dsensor.storage

import androidx.room.Dao
import androidx.room.Query

@Dao
internal abstract class SessionDao : BaseDao<Session> {
    @Query("DELETE FROM session_table")
    abstract fun deleteAll()

    @Query("SELECT * from session_table ORDER BY start_time DESC")
    abstract fun getAllSessions(): List<Session>?

    @Query("SELECT * from session_table ORDER BY start_time DESC LIMIT 1")
    abstract fun getLastSession(): Session?

    @Query("SELECT * from session_table ORDER BY start_time DESC LIMIT :numberOfSessions")
    abstract fun getLastSessions(numberOfSessions: Int): List<Session>?
}