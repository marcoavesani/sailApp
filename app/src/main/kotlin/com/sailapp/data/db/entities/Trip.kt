package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtUtcMs: Long,
    val startedAtUtcMs: Long? = null,
    val endedAtUtcMs: Long? = null,
    val status: String = "planned", // planned/recording/paused/completed/interrupted/merged/discarded
    val totalDistanceM: Double = 0.0,
    val notes: String = ""
)
