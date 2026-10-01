package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ScheduleItem::class, JarvisLog::class, JarvisProtocol::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_core_database"
                )
                    .addCallback(JarvisDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class JarvisDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.jarvisDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: JarvisDao) {
                // Initial Protocols
                dao.insertProtocols(
                    listOf(
                        JarvisProtocol(
                            id = "MORNING_BRIEF",
                            title = "Protocol: Morning Briefing",
                            description = "Full agenda readout, system readiness check, and environmental diagnostics.",
                            triggerKeyword = "morning brief",
                            isActive = true,
                            iconName = "wb_sunny"
                        ),
                        JarvisProtocol(
                            id = "DEEP_FOCUS",
                            title = "Protocol: Deep Work & Focus",
                            description = "Mute audio disturbances, prioritize critical tasks, and optimize device bandwidth.",
                            triggerKeyword = "focus mode",
                            isActive = false,
                            iconName = "center_focus_strong"
                        ),
                        JarvisProtocol(
                            id = "NIGHT_WATCH",
                            title = "Protocol: Night Watch / Defense",
                            description = "Review remaining deliverables, activate low-illumination mode, and arm device security.",
                            triggerKeyword = "night watch",
                            isActive = false,
                            iconName = "nightlight_round"
                        ),
                        JarvisProtocol(
                            id = "CLEAN_SLATE",
                            title = "Protocol: Clean Slate",
                            description = "Purge completed operational tasks, flush memory caches, and recalibrate diagnostics.",
                            triggerKeyword = "clean slate",
                            isActive = false,
                            iconName = "cleaning_services"
                        )
                    )
                )

                // Initial Schedule Items
                dao.insertScheduleItem(
                    ScheduleItem(
                        title = "Mark 85 Armor Diagnostic & Calibration",
                        description = "Verify arc reactor harmonic output and thruster telemetry.",
                        time = "09:30 AM",
                        date = "Today",
                        category = "SYSTEM",
                        priority = "HIGH",
                        isCompleted = false
                    )
                )
                dao.insertScheduleItem(
                    ScheduleItem(
                        title = "Stark Industries Board Briefing",
                        description = "Review quarterly AI clean energy deployment milestones.",
                        time = "02:00 PM",
                        date = "Today",
                        category = "WORK",
                        priority = "HIGH",
                        isCompleted = false
                    )
                )
                dao.insertScheduleItem(
                    ScheduleItem(
                        title = "Daily Neural Sync & Voice Model Calibration",
                        description = "Optimize low-latency speech synthesis models across personal devices.",
                        time = "06:30 PM",
                        date = "Today",
                        category = "PROTOCOL",
                        priority = "MEDIUM",
                        isCompleted = false
                    )
                )

                // Welcome message
                dao.insertLog(
                    JarvisLog(
                        sender = "JARVIS",
                        message = "Good day, sir. All core systems are online and standing by. Voice recognition and scheduling protocols are active at your command.",
                        actionTag = "SYSTEM_READY"
                    )
                )
            }
        }
    }
}
