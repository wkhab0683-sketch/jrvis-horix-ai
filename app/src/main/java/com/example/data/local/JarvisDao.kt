package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // Schedule
    @Query("SELECT * FROM schedule_items ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllScheduleItems(): Flow<List<ScheduleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItem(item: ScheduleItem): Long

    @Update
    suspend fun updateScheduleItem(item: ScheduleItem)

    @Delete
    suspend fun deleteScheduleItem(item: ScheduleItem)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleItemById(id: Long)

    @Query("UPDATE schedule_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setScheduleItemCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM schedule_items WHERE isCompleted = 1")
    suspend fun clearCompletedSchedules()

    // Logs / Chat Memory
    @Query("SELECT * FROM jarvis_logs ORDER BY id DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<JarvisLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: JarvisLog): Long

    @Query("DELETE FROM jarvis_logs")
    suspend fun clearLogs()

    // Protocols
    @Query("SELECT * FROM jarvis_protocols")
    fun getAllProtocols(): Flow<List<JarvisProtocol>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocols(protocols: List<JarvisProtocol>)

    @Update
    suspend fun updateProtocol(protocol: JarvisProtocol)

    @Query("UPDATE jarvis_protocols SET isActive = :isActive WHERE id = :id")
    suspend fun setProtocolActive(id: String, isActive: Boolean)
}
