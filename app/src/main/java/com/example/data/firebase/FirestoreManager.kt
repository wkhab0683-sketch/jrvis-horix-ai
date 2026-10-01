package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.ScheduleItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class UserProfileData(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: String = "",
    val lastLoginAt: String = ""
)

data class AiCreationRecord(
    val id: String = "",
    val userId: String = "",
    val type: String = "IMAGE", // MUSIC, IMAGE, VIDEO, TRANSCRIPTION
    val model: String = "",
    val prompt: String = "",
    val outputData: String = "",
    val status: String = "COMPLETED",
    val createdAt: String = ""
)

data class FirestoreChatRecord(
    val id: String = "",
    val userId: String = "",
    val role: String = "user", // user, model
    val text: String = "",
    val modelUsed: String = "gemini-3.5-flash",
    val groundingType: String = "NONE", // NONE, SEARCH, MAPS
    val timestamp: Long = System.currentTimeMillis()
)

class FirestoreManager(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        val app = FirebaseApp.getInstance()
        FirebaseFirestore.getInstance(app, databaseId)
    }

    suspend fun syncUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val docRef = firestore.collection("users").document(uid)
        val data = mapOf(
            "userId" to uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: "Operative"),
            "lastLoginAt" to Date().toString()
        )
        try {
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    // Schedules Sync
    suspend fun saveScheduleToCloud(userId: String, item: ScheduleItem): String {
        val docId = if (item.id > 0) item.id.toString() else "sched_${System.currentTimeMillis()}"
        val docRef = firestore.collection("users").document(userId).collection("schedules").document(docId)
        val data = mapOf(
            "id" to docId,
            "userId" to userId,
            "title" to item.title,
            "description" to item.description,
            "time" to item.time,
            "date" to item.date,
            "category" to item.category,
            "priority" to item.priority,
            "isCompleted" to item.isCompleted,
            "createdAt" to Date(item.createdAt).toString()
        )
        docRef.set(data).await()
        return docId
    }

    suspend fun deleteScheduleFromCloud(userId: String, scheduleId: String) {
        try {
            firestore.collection("users").document(userId)
                .collection("schedules").document(scheduleId).delete().await()
        } catch (_: Exception) {}
    }

    fun observeUserSchedules(userId: String): Flow<List<ScheduleItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ScheduleItem(
                            id = doc.id.hashCode().toLong(),
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            time = doc.getString("time") ?: "",
                            date = doc.getString("date") ?: "",
                            category = doc.getString("category") ?: "WORK",
                            priority = doc.getString("priority") ?: "MEDIUM",
                            isCompleted = doc.getBoolean("isCompleted") ?: false
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    // AI Creations (Music, Images, Videos, Transcripts)
    suspend fun saveAiCreation(userId: String, creation: AiCreationRecord) {
        val docId = creation.id.ifEmpty { "create_${System.currentTimeMillis()}" }
        val docRef = firestore.collection("users").document(userId).collection("creations").document(docId)
        val data = mapOf(
            "id" to docId,
            "userId" to userId,
            "type" to creation.type,
            "model" to creation.model,
            "prompt" to creation.prompt,
            "outputData" to creation.outputData,
            "status" to creation.status,
            "createdAt" to Date().toString()
        )
        docRef.set(data).await()
    }

    fun observeUserCreations(userId: String): Flow<List<AiCreationRecord>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("creations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val creations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        AiCreationRecord(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            type = doc.getString("type") ?: "IMAGE",
                            model = doc.getString("model") ?: "",
                            prompt = doc.getString("prompt") ?: "",
                            outputData = doc.getString("outputData") ?: "",
                            status = doc.getString("status") ?: "COMPLETED",
                            createdAt = doc.getString("createdAt") ?: ""
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(creations)
            }

        awaitClose { listener.remove() }
    }

    // Chat Message History
    suspend fun saveChatMessage(userId: String, message: FirestoreChatRecord) {
        val docId = message.id.ifEmpty { "msg_${System.currentTimeMillis()}" }
        val docRef = firestore.collection("users").document(userId).collection("chat_messages").document(docId)
        val data = mapOf(
            "id" to docId,
            "userId" to userId,
            "role" to message.role,
            "text" to message.text,
            "modelUsed" to message.modelUsed,
            "groundingType" to message.groundingType,
            "timestamp" to message.timestamp
        )
        docRef.set(data).await()
    }

    fun observeUserChatMessages(userId: String): Flow<List<FirestoreChatRecord>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("chat_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        FirestoreChatRecord(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            role = doc.getString("role") ?: "user",
                            text = doc.getString("text") ?: "",
                            modelUsed = doc.getString("modelUsed") ?: "gemini-3.5-flash",
                            groundingType = doc.getString("groundingType") ?: "NONE",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}
