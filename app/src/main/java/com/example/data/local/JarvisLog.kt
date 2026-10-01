package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_logs")
data class JarvisLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER", "JARVIS", "SYSTEM"
    val message: String,
    val actionTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_protocols")
data class JarvisProtocol(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val triggerKeyword: String,
    val isActive: Boolean = false,
    val iconName: String = "shield"
)
