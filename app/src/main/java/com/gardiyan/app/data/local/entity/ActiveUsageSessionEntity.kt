package com.gardiyan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "active_usage_session")
data class ActiveUsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val packageName: String,
    val appName: String,
    val entryAtMillis: Long,
    val lastSeenAtMillis: Long,
    @ColumnInfo(defaultValue = "0")
    val entryElapsedRealtime: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val lastSeenElapsedRealtime: Long = 0L,
    val isActive: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
