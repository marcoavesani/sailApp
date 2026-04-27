package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("tripId")]
)
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAtUtcMs: Long,
    val type: String = "generic", // generic/start_pin/committee_boat/mark/mob
    val notes: String = ""
)
