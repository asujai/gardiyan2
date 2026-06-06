package com.gardiyan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_usage_session")
data class ActiveUsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val packageName: String,
    val appName: String,
    val entryAtMillis: Long,
    val lastSeenAtMillis: Long,
    val isActive: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
