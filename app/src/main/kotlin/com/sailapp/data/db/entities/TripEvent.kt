package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_events",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class TripEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val segmentId: Long? = null,
    val timestampUtcMs: Long,
    val type: String,
    val message: String = "",
    val metadataJson: String? = null
)
