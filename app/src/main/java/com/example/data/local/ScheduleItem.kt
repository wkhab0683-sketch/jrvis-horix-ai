package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val time: String, // e.g. "09:00 AM" or "15:30"
    val date: String, // e.g. "2026-09-30" or "Today"
    val category: String = "WORK", // PROTOCOL, WORK, SYSTEM, SECURITY, PERSONAL
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
