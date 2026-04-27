package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "regatta_sessions",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("tripId")]
)
data class RegattaSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long? = null,
    val name: String,
    val startTimeUtcMs: Long? = null,
    val countdownDurationS: Int = 300,
    val timerState: String = "idle" // idle/running/stopped
)
