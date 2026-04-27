package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TripSegment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId"), Index("segmentId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val segmentId: Long,
    val timestampUtcMs: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val sogMps: Float? = null,
    val cogDeg: Float? = null,
    val headingDeg: Float? = null,
    val horizontalAccuracyM: Float? = null,
    val verticalAccuracyM: Float? = null,
    val speedAccuracyMps: Float? = null,
    val bearingAccuracyDeg: Float? = null,
    val provider: String? = null,
    val batteryPercent: Int? = null,
    val isValidForStats: Boolean = true,
    val rawJson: String? = null
)
